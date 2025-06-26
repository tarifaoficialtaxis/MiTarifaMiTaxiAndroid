package com.mitarifamitaxi.taximetrousuario.viewmodels.onboarding.driver

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.FirebaseStorageUtils
import com.mitarifamitaxi.taximetrousuario.helpers.LocalUserManager
import com.mitarifamitaxi.taximetrousuario.helpers.toBitmap
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.models.DriverStatus
import com.mitarifamitaxi.taximetrousuario.viewmodels.UserDataUpdateEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RegisterDriverStepFourViewModel(context: Context, private val appViewModel: AppViewModel) :
    ViewModel() {

    enum class VehiclePhotoType {
        FRONT,
        BACK,
        SIDE
    }

    private val appContext = context.applicationContext

    var vehiclePhotoType by mutableStateOf(VehiclePhotoType.FRONT)

    var frontImageUri by mutableStateOf<Uri?>(null)
    var frontTempImageUri by mutableStateOf<Uri?>(null)

    var backImageUri by mutableStateOf<Uri?>(null)
    var backTempImageUri by mutableStateOf<Uri?>(null)

    var sideImageUri by mutableStateOf<Uri?>(null)
    var sideTempImageUri by mutableStateOf<Uri?>(null)

    var hasCameraPermission by mutableStateOf(false)
        private set

    sealed class StepFourUpdateEvent {
        object RegistrationComplete : StepFourUpdateEvent()
    }

    private val _stepFourUpdateEvents = MutableSharedFlow<StepFourUpdateEvent>()
    val stepFourUpdateEvents = _stepFourUpdateEvents.asSharedFlow()

    private fun observeAppViewModelEvents() {
        viewModelScope.launch {
            appViewModel.userDataUpdateEvents.collectLatest { event ->
                when (event) {
                    is UserDataUpdateEvent.FirebaseUserUpdated -> {
                        appViewModel.setLoading(false)
                        showSuccessMessage()
                    }
                }
            }
        }
    }

    init {
        checkCameraPermission()
        observeAppViewModelEvents()
    }

    private fun checkCameraPermission() {
        hasCameraPermission = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun onPermissionResult(isGranted: Boolean) {
        hasCameraPermission = isGranted
    }

    fun onImageSelected(uri: Uri?) {

        if (vehiclePhotoType == VehiclePhotoType.FRONT) {
            frontImageUri = uri
        } else if (vehiclePhotoType == VehiclePhotoType.BACK) {
            backImageUri = uri
        } else {
            sideImageUri = uri
        }

    }

    fun onImageCaptured(success: Boolean) {
        if (success) {
            if (vehiclePhotoType == VehiclePhotoType.FRONT) {
                frontImageUri = frontTempImageUri
            } else if (vehiclePhotoType == VehiclePhotoType.BACK) {
                backImageUri = backTempImageUri
            } else {
                sideImageUri = sideTempImageUri
            }
        }
    }

    fun onNext() {
        if (frontImageUri == null || backImageUri == null || sideImageUri == null) {
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.attention),
                message = appContext.getString(R.string.error_vehicle_pictures)
            )
            return
        }

        viewModelScope.launch {

            appViewModel.setLoading(true)

            val deferreds = listOf(frontImageUri!!, backImageUri!!, sideImageUri!!).map { uri ->
                async(Dispatchers.IO) {
                    uri.toBitmap(appContext)
                        ?.let { bitmap ->
                            FirebaseStorageUtils.uploadImage(
                                "vehiclesPictures",
                                bitmap
                            )
                        }
                }
            }

            val (frontUrl, backUrl, sideUrl) = deferreds.awaitAll()

            if (frontUrl == null || backUrl == null || sideUrl == null) {
                appViewModel.setLoading(false)
                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = appContext.getString(R.string.general_error)
                )
                return@launch
            }

            updateUserData(frontUrl, backUrl, sideUrl)

        }
    }

    private fun updateUserData(
        frontVehicleUrl: String? = null,
        backVehicleUrl: String? = null,
        sideVehicleUrl: String? = null,
    ) {

        val userData = LocalUserManager(appContext).getUserState()

        val userDataUpdated = userData?.copy(
            vehicleFrontPicture = frontVehicleUrl,
            vehicleBackPicture = backVehicleUrl,
            vehicleSidePicture = sideVehicleUrl,
            driverStatus = DriverStatus.PENDING
        )

        userDataUpdated?.let {
            LocalUserManager(appContext).saveUserState(it)
            appViewModel.updateUserDataOnFirebase(it)
        }

    }

    fun showSuccessMessage() {
        appViewModel.showMessage(
            type = DialogType.SUCCESS,
            title = appContext.getString(R.string.account_created),
            buttonText = appContext.getString(R.string.login),
            message = appContext.getString(R.string.account_created_success_message),
            onDismiss = {
                emitProcessCompletedEvent()
            },
            onButtonClicked = {
                emitProcessCompletedEvent()
            }
        )
    }

    fun emitProcessCompletedEvent() {
        viewModelScope.launch {
            _stepFourUpdateEvents.emit(StepFourUpdateEvent.RegistrationComplete)
        }
    }

}

class RegisterDriverStepFourViewModelFactory(
    private val context: Context,
    private val appViewModel: AppViewModel
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterDriverStepFourViewModel::class.java)) {
            return RegisterDriverStepFourViewModel(context, appViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}