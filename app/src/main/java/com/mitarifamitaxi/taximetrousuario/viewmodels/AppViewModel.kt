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
import com.mitarifamitaxi.taximetrousuario.activities.BaseActivity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.mitarifamitaxi.taximetrousuario.helpers.LocalUserManager
import com.mitarifamitaxi.taximetrousuario.helpers.getCityFromCoordinates
import com.mitarifamitaxi.taximetrousuario.models.UserLocation
import java.util.Date
import java.util.concurrent.Executor

sealed class UserDataUpdateEvent {
    object FirebaseUserUpdated : UserDataUpdateEvent()
}

class AppViewModel(context: Context) : ViewModel() {

    private val appContext = context.applicationContext

    var isLoading by mutableStateOf(false)

    var userData: LocalUser? by mutableStateOf(null)
    var userLocation: UserLocation? by mutableStateOf(null)

    var dialogType by mutableStateOf(DialogType.SUCCESS)
    var showDialog by mutableStateOf(false)
    var dialogTitle by mutableStateOf("")
    var dialogMessage by mutableStateOf("")
    var dialogButtonText: String? = null

    var dialogShowCloseButton: Boolean = true
    var dialogOnDismiss: (() -> Unit)? = null
    var dialogOnPrimaryActionClicked: (() -> Unit)? = null

    // Location variables
    private lateinit var locationCallback: LocationCallback
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val executor: Executor = ContextCompat.getMainExecutor(context)
    var isGettingLocation by mutableStateOf(false)

    private val _userDataUpdateEvents = MutableSharedFlow<UserDataUpdateEvent>()
    val userDataUpdateEvents = _userDataUpdateEvents.asSharedFlow()

    init {
        loadUserData()
        validateAppVersion()
    }

    fun reloadUserData() {
        loadUserData()
    }

    private fun loadUserData() {
        userData = LocalUserManager(appContext).getUserState()
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
        dialogType = type
        dialogTitle = title
        dialogMessage = message
        showDialog = true
        dialogButtonText = buttonText
        dialogShowCloseButton = showCloseButton
        dialogOnDismiss = onDismiss
        dialogOnPrimaryActionClicked = onButtonClicked
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

        isGettingLocation = true
        val cancellationTokenSource = CancellationTokenSource()

        val task: Task<Location> = fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        )

        task.addOnSuccessListener(executor) { location ->
            if (location != null) {

                val previousUserLocation = userLocation
                val locationChanged = previousUserLocation == null ||
                        previousUserLocation.latitude != location.latitude ||
                        previousUserLocation.longitude != location.longitude

                if (!locationChanged) {
                    isGettingLocation = false
                    viewModelScope.launch {
                        _userDataUpdateEvents.emit(UserDataUpdateEvent.FirebaseUserUpdated)
                    }
                    return@addOnSuccessListener
                } else {
                    userLocation = UserLocation(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                }

                viewModelScope.launch {
                    getCityFromCoordinates(
                        context = appContext,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        callbackSuccess = { city, countryCode, countryCodeWhatsapp, countryCurrency ->
                            isGettingLocation = false
                            updateUserData(
                                city = city ?: "",
                                countryCode = countryCode ?: "",
                                countryCodeWhatsapp = countryCodeWhatsapp ?: "",
                                countryCurrency = countryCurrency ?: ""
                            )
                        },
                        callbackError = { error ->
                            Log.e("HomeViewModel", "Error getting city: $error")
                            isGettingLocation = false
                            showMessage(
                                type = DialogType.ERROR,
                                title = appContext.getString(R.string.something_went_wrong),
                                message = appContext.getString(R.string.error_fetching_city)
                            )
                        }
                    )
                }

            } else {
                isGettingLocation = false
                showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = appContext.getString(R.string.error_fetching_location)
                )
            }
        }.addOnFailureListener {
            isGettingLocation = false
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
        userData = userData?.copy(
            city = city,
            countryCode = countryCode,
            countryCodeWhatsapp = countryCodeWhatsapp,
            countryCurrency = countryCurrency,
            lastActive = Date()
        )

        userData?.let {
            LocalUserManager(appContext).saveUserState(it)
            updateUserDataOnFirebase(it)
        }

    }

    fun updateUserDataOnFirebase(
        user: LocalUser
    ) {
        viewModelScope.launch {
            try {
                val userId = user.id ?: throw IllegalArgumentException("User ID is null")
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .set(user)
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