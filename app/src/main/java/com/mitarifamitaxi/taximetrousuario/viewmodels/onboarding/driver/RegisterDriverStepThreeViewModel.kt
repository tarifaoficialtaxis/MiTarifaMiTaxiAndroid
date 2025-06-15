package com.mitarifamitaxi.taximetrousuario.viewmodels.onboarding.driver

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.LocalUserManager
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.models.LocalUser
import com.mitarifamitaxi.taximetrousuario.models.VehicleBrand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.Year
import java.util.Calendar

class RegisterDriverStepThreeViewModel(context: Context, private val appViewModel: AppViewModel) :
    ViewModel() {

    private val appContext = context.applicationContext
    private val vehicleBrandsObj = mutableStateOf(listOf<VehicleBrand>())

    sealed class StepThreeUpdateEvent {
        object FirebaseUserUpdated : StepThreeUpdateEvent()
    }

    private val _stepThreeUpdateEvents = MutableSharedFlow<StepThreeUpdateEvent>()
    val stepThreeUpdateEvents = _stepThreeUpdateEvents.asSharedFlow()

    val vehicleBrandNames: List<String>
        get() = vehicleBrandsObj.value.map { it.name ?: "" }.sorted()
    var selectedBrand by mutableStateOf<String?>(null)
        private set

    var vehicleModelsNames by mutableStateOf(listOf<String>())
    var selectedModel by mutableStateOf<String?>(null)
        private set

    var vehicleYears by mutableStateOf(listOf<String>())
    var selectedYear by mutableStateOf<String?>(null)
        private set

    var plate by mutableStateOf("")

    init {
        getVehicleBrands()
        loadListOfYears()
    }

    private fun getVehicleBrands() {
        viewModelScope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val brandsQuerySnapshot = withContext(Dispatchers.IO) {
                    firestore.collection("vehicleBrands")
                        .get()
                        .await()
                }

                if (!brandsQuerySnapshot.isEmpty) {
                    try {
                        vehicleBrandsObj.value =
                            brandsQuerySnapshot.documents.mapNotNull { it.toObject(VehicleBrand::class.java) }
                    } catch (e: Exception) {
                        Log.e(
                            "RegisterDriverStepThreeViewModel",
                            "Error converting brands: ${e.message}"
                        )
                        appViewModel.showMessage(
                            type = DialogType.ERROR,
                            title = appContext.getString(R.string.something_went_wrong),
                            message = appContext.getString(R.string.general_error)
                        )

                    }
                } else {
                    appViewModel.showMessage(
                        type = DialogType.ERROR,
                        title = appContext.getString(R.string.something_went_wrong),
                        message = appContext.getString(R.string.general_error)
                    )
                }
            } catch (e: Exception) {
                Log.e("RegisterDriverStepThreeViewModel", "Error fetching brands: ${e.message}")
                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = appContext.getString(R.string.general_error)
                )
            }
        }

    }

    fun loadListOfYears() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        vehicleYears = (currentYear downTo (currentYear - 30)).map { it.toString() }
        selectedYear = null
    }

    fun onBrandSelected(brand: String) {
        selectedBrand = brand
        val selectedBrandObj = vehicleBrandsObj.value.find { it.name == brand }
        vehicleModelsNames = (selectedBrandObj?.models)?.sorted() ?: emptyList()
        selectedModel = null
    }

    fun onModelSelected(model: String) {
        selectedModel = model
    }

    fun onYearSelected(year: String) {
        selectedYear = year
    }

    fun onNext() {
        /*if (frontImageUri == null || backImageUri == null) {
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.attention),
                message = appContext.getString(R.string.error_driving_license)
            )
            return
        }

        viewModelScope.launch {
            val frontImageUrl = frontImageUri?.let { uri ->
                uri.toBitmap(appContext)?.let { bitmap ->
                    FirebaseStorageUtils.uploadImage("drivingLicenses", bitmap)
                }
            }

            val backImageUrl = backImageUri?.let { uri ->
                uri.toBitmap(appContext)?.let { bitmap ->
                    FirebaseStorageUtils.uploadImage("drivingLicenses", bitmap)
                }
            }

            updateUserData(
                frontDrivingLicenseUrl = frontImageUrl,
                backDrivingLicenseUrl = backImageUrl
            )

        }*/
    }

    private fun updateUserData(
        frontDrivingLicenseUrl: String? = null,
        backDrivingLicenseUrl: String? = null
    ) {

        appViewModel.isLoading = true

        val userData = LocalUserManager(appContext).getUserState()

        val userDataUpdated = userData?.copy(
            frontDrivingLicense = frontDrivingLicenseUrl,
            backDrivingLicense = backDrivingLicenseUrl
        )

        userDataUpdated?.let {
            LocalUserManager(appContext).saveUserState(it)
            updateUserDataOnFirebase(it)
        }

    }

    private fun updateUserDataOnFirebase(user: LocalUser) {
        val db = FirebaseFirestore.getInstance()
        user.id?.let { userId ->
            db.collection("users")
                .document(userId)
                .set(user)
                .addOnSuccessListener {
                    appViewModel.isLoading = false
                    Log.d("RegisterDriverStepTwoViewModel", "User data updated in Firestore")
                    viewModelScope.launch {
                        _stepThreeUpdateEvents.emit(StepThreeUpdateEvent.FirebaseUserUpdated)
                    }
                }
                .addOnFailureListener { e ->
                    appViewModel.isLoading = false
                    Log.e(
                        "RegisterDriverStepTwoViewModel",
                        "Failed to update user data in Firestore: ${e.message}"
                    )
                    appViewModel.showMessage(
                        type = DialogType.ERROR,
                        title = appContext.getString(R.string.something_went_wrong),
                        message = appContext.getString(R.string.error_fetching_location)
                    )
                }
        }
    }


}

class RegisterDriverStepThreeViewModelFactory(
    private val context: Context,
    private val appViewModel: AppViewModel
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterDriverStepThreeViewModel::class.java)) {
            return RegisterDriverStepThreeViewModel(context, appViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}