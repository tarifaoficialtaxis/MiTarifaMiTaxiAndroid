package com.mitarifamitaxi.taximetrousuario.viewmodels

import android.Manifest
import android.annotation.SuppressLint
import com.mitarifamitaxi.taximetrousuario.BuildConfig
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.models.AppVersion
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.models.LocalUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.mitarifamitaxi.taximetrousuario.activities.BaseActivity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.mitarifamitaxi.taximetrousuario.helpers.LocalUserManager
import com.mitarifamitaxi.taximetrousuario.helpers.getCityFromCoordinates
import com.mitarifamitaxi.taximetrousuario.models.CountryArea
import com.mitarifamitaxi.taximetrousuario.models.UserLocation
import com.mitarifamitaxi.taximetrousuario.models.toUpdateMapReflective
import java.util.Date
import java.util.concurrent.Executor
import com.google.firebase.database.ValueEventListener
import com.mitarifamitaxi.taximetrousuario.helpers.findRegionForCoordinates
import androidx.core.content.edit
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.mitarifamitaxi.taximetrousuario.states.AppState
import com.mitarifamitaxi.taximetrousuario.states.DialogState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


sealed class UserDataUpdateEvent {
    object FirebaseUserUpdated : UserDataUpdateEvent()
}

class AppViewModel(context: Context) : ViewModel() {

    private val appContext = context.applicationContext

    private lateinit var locationCallback: LocationCallback
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val executor: Executor = ContextCompat.getMainExecutor(context)

    private val _uiState = MutableStateFlow(AppState())
    val uiState = _uiState.asStateFlow()

    private val _userDataUpdateEvents = MutableSharedFlow<UserDataUpdateEvent>()
    val userDataUpdateEvents = _userDataUpdateEvents.asSharedFlow()

    init {
        loadUserData()
        validateAppVersion()
    }

    fun setLoading(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading) }
    }

    fun updateLocalUser(user: LocalUser) {
        _uiState.update { it.copy(userData = user) }
        LocalUserManager(appContext).saveUserState(user)
    }

    fun reloadUserData() {
        loadUserData()
    }

    private fun loadUserData() {
        val loadedUserData = LocalUserManager(appContext).getUserState()
        _uiState.update { it.copy(userData = loadedUserData) }
    }

    fun validateAppVersion() {

        viewModelScope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val querySnapshot = withContext(Dispatchers.IO) {
                    firestore.collection("appVersion")
                        .document("android")
                        .get()
                        .await()
                }

                if (querySnapshot != null && querySnapshot.exists()) {
                    try {
                        val appVersionObj =
                            querySnapshot.toObject(AppVersion::class.java) ?: AppVersion()

                        if ((BuildConfig.VERSION_NAME != appVersionObj.version || BuildConfig.VERSION_CODE != appVersionObj.build) && appVersionObj.show == true) {
                            showMessage(
                                type = DialogType.WARNING,
                                title = appContext.getString(R.string.attention),
                                message = appContext.getString(R.string.new_app_version_message),
                                buttonText = appContext.getString(R.string.update),
                                showCloseButton = false,
                                onButtonClicked = {
                                    appVersionObj.urlStore?.let { openUrlStore(it) }
                                }
                            )

                        } else {
                            Log.i("AppViewModel", "App Version is up to date")
                        }


                    } catch (e: Exception) {
                        Log.e("AppViewModel", "Error parsing rates: ${e.message}")
                        showMessage(
                            type = DialogType.ERROR,
                            title = appContext.getString(R.string.something_went_wrong),
                            message = appContext.getString(R.string.general_error)
                        )
                    }
                } else {
                    showMessage(
                        type = DialogType.ERROR,
                        title = appContext.getString(R.string.something_went_wrong),
                        message = appContext.getString(R.string.general_error)
                    )
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error fetching contacts: ${e.message}")
                showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = appContext.getString(R.string.general_error)
                )
            }

        }

    }

    fun openUrlStore(urlStore: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlStore))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AppViewModel", "Could not open store URL: $urlStore", e)
            showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.error),
                message = appContext.getString(R.string.cannot_open_store_url)
            )
        }
    }


    fun showMessage(
        type: DialogType,
        title: String,
        message: String,
        buttonText: String? = null,
        showCloseButton: Boolean = true,
        onDismiss: (() -> Unit)? = null,
        onButtonClicked: (() -> Unit)? = null
    ) {
        val newDialogState = DialogState(
            show = true,
            type = type,
            title = title,
            message = message,
            buttonText = buttonText,
            showCloseButton = showCloseButton,
            onDismiss = onDismiss,
            onPrimaryActionClicked = onButtonClicked
        )
        _uiState.update { it.copy(dialogState = newDialogState) }
    }

    fun hideMessage() {
        _uiState.update { it.copy(dialogState = DialogState()) }
    }


    // Location permission and updates

    override fun onCleared() {
        stopLocationUpdates()
        super.onCleared()
    }

    fun requestLocationPermission(activity: BaseActivity) {
        if (ActivityCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            activity.locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            getCurrentLocation()
        }
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation() {

        _uiState.update { it.copy(isGettingLocation = true) }
        val cancellationTokenSource = CancellationTokenSource()

        val task: Task<Location> = fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        )

        task.addOnSuccessListener(executor) { location ->
            if (location != null) {

                val previousUserLocation = _uiState.value.userLocation
                val locationChanged = previousUserLocation == null ||
                        previousUserLocation.latitude != location.latitude ||
                        previousUserLocation.longitude != location.longitude

                if (!locationChanged) {
                    _uiState.update { it.copy(isGettingLocation = false) }

                    viewModelScope.launch {
                        _userDataUpdateEvents.emit(UserDataUpdateEvent.FirebaseUserUpdated)
                    }
                    return@addOnSuccessListener
                }

                /*else {
                    userLocation = UserLocation(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                }*/

                viewModelScope.launch {
                    getCityFromCoordinates(
                        context = appContext,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        callbackSuccess = { country, countryCode, countryCodeWhatsapp, countryCurrency ->
                            _uiState.update { it.copy(isGettingLocation = false) }

                            val userLocation = UserLocation(
                                latitude = location.latitude,
                                longitude = location.longitude
                            )

                            getCityRegionFromCountry(
                                country ?: "",
                                userLocation,
                                onResult = { cityName ->
                                    updateUserData(
                                        city = cityName ?: "",
                                        countryCode = countryCode ?: "",
                                        countryCodeWhatsapp = countryCodeWhatsapp ?: "",
                                        countryCurrency = countryCurrency ?: ""
                                    )
                                }
                            )
                        },
                        callbackError = { error ->
                            Log.e("HomeViewModel", "Error getting city: $error")
                            _uiState.update { it.copy(isGettingLocation = false) }
                            showMessage(
                                type = DialogType.ERROR,
                                title = appContext.getString(R.string.something_went_wrong),
                                message = appContext.getString(R.string.error_fetching_city)
                            )
                        }
                    )
                }

            } else {
                _uiState.update { it.copy(isGettingLocation = false) }
                showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = appContext.getString(R.string.error_fetching_location)
                )
            }
        }.addOnFailureListener {
            _uiState.update { it.copy(isGettingLocation = false) }
            showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.something_went_wrong),
                message = appContext.getString(R.string.error_fetching_location)
            )
        }
    }

    fun stopLocationUpdates() {
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun updateUserData(
        city: String,
        countryCode: String,
        countryCodeWhatsapp: String,
        countryCurrency: String
    ) {
        val currentUser = _uiState.value.userData ?: return

        val updatedUser = currentUser.copy(
            city = city,
            countryCode = countryCode,
            countryCodeWhatsapp = countryCodeWhatsapp,
            countryCurrency = countryCurrency,
            lastActive = Date()
        )

        _uiState.update {
            it.copy(userData = updatedUser)
        }

        LocalUserManager(appContext).saveUserState(updatedUser)
        updateUserDataOnFirebase(updatedUser)
    }

    fun updateUserDataOnFirebase(
        user: LocalUser
    ) {
        viewModelScope.launch {
            try {
                val userId = user.id ?: throw IllegalArgumentException("User ID is null")
                val data = user.toUpdateMapReflective()

                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .set(data, SetOptions.merge())
                    .await()
                Log.d("HomeViewModel", "User data updated in Firestore")
                _userDataUpdateEvents.emit(UserDataUpdateEvent.FirebaseUserUpdated)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to update user data in Firestore: ${e.message}")
                withContext(Dispatchers.Main) {
                    showMessage(
                        type = DialogType.ERROR,
                        title = appContext.getString(R.string.something_went_wrong),
                        message = appContext.getString(R.string.general_error)
                    )
                }
            }
        }
    }

    private fun getCityRegionFromCountry(
        country: String,
        location: UserLocation,
        onResult: ((String?) -> Unit)? = null
    ) {

        val database = FirebaseDatabase.getInstance()
        val countryRef = database.getReference("countriesRegions").child(country.lowercase())

        countryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    try {
                        val countryData =
                            snapshot.getValue(CountryArea::class.java)
                        Log.d("getCityRegionFromCountry", "City Area: $countryData")
                        if (countryData != null && countryData.features != null && location.longitude != null && location.latitude != null) {
                            val region = findRegionForCoordinates(
                                location.latitude,
                                location.longitude,
                                countryData.features
                            )
                            Log.d("getCityRegionFromCountry", "Region data: $region")
                            onResult?.invoke(region?.name)
                        }


                    } catch (e: Exception) {
                        Log.e("getCityRegionFromCountry", "Error parsing city data: ${e.message}")
                        onResult?.invoke(null)
                    }

                } else {
                    Log.d("getCityRegionFromCountry", "No city found with the given name")
                    onResult?.invoke(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("getCityRegionFromCountry", "Query cancelled or failed: ${error.message}")
                onResult?.invoke(null)
            }
        })
    }

}

class AppViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            return AppViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}