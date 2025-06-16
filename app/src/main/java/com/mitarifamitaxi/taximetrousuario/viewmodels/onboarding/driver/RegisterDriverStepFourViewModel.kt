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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Objects
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.FirebaseStorageUtils
import com.mitarifamitaxi.taximetrousuario.helpers.LocalUserManager
import com.mitarifamitaxi.taximetrousuario.helpers.toBitmap
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.models.DriverStatus
import com.mitarifamitaxi.taximetrousuario.viewmodels.UserDataUpdateEvent
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
        private set

    var backImageUri by mutableStateOf<Uri?>(null)
    var backTempImageUri by mutableStateOf<Uri?>(null)
        private set

    var sideImageUri by mutableStateOf<Uri?>(null)
    var sideTempImageUri by mutableStateOf<Uri?>(null)
        private set

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
                        appViewModel.isLoading = false
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

    fun createTempImageUri() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = appContext.cacheDir
        val image = File.createTempFile(imageFileName, ".jpg", storageDir)
        val tempImageUri = FileProvider.getUriForFile(
            Objects.requireNonNull(appContext),
            "${appContext.packageName}.provider",
            image
        )

        if (vehiclePhotoType == VehiclePhotoType.FRONT) {
            frontTempImageUri = tempImageUri
        } else if (vehiclePhotoType == VehiclePhotoType.BACK) {
            backTempImageUri = tempImageUri
        } else {
            sideTempImageUri = tempImageUri
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

            appViewModel.isLoading = true

            val frontImageUrl = frontImageUri?.let { uri ->
                uri.toBitmap(appContext)?.let { bitmap ->
                    FirebaseStorageUtils.uploadImage("vehiclesPictures", bitmap)
                }
            }

            val backImageUrl = backImageUri?.let { uri ->
                uri.toBitmap(appContext)?.let { bitmap ->
                    FirebaseStorageUtils.uploadImage("vehiclesPictures", bitmap)
                }
            }

            val sideImageUrl = sideImageUri?.let { uri ->
                uri.toBitmap(appContext)?.let { bitmap ->
                    FirebaseStorageUtils.uploadImage("vehiclesPictures", bitmap)
                }
            }

            if (frontImageUrl == null || backImageUrl == null || sideImageUrl == null) {
                appViewModel.isLoading = false
                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = appContext.getString(R.string.general_error)
                )
                return@launch
            }

            updateUserData(
                frontVehicleUrl = frontImageUrl,
                backVehicleUrl = backImageUrl,
                sideVehicleUrl = sideImageUrl
            )

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