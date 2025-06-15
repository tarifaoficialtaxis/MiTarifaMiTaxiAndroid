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
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Objects
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.FirebaseStorageUtils
import com.mitarifamitaxi.taximetrousuario.helpers.LocalUserManager
import com.mitarifamitaxi.taximetrousuario.helpers.toBitmap
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.models.LocalUser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
        object FirebaseUserUpdated : StepFourUpdateEvent()
    }

    private val _stepFourUpdateEvents = MutableSharedFlow<StepFourUpdateEvent>()
    val stepFourUpdateEvents = _stepFourUpdateEvents.asSharedFlow()

    init {
        checkCameraPermission()
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

        appViewModel.isLoading = true

        val userData = LocalUserManager(appContext).getUserState()

        val userDataUpdated = userData?.copy(
            vehicleFrontPicture = frontVehicleUrl,
            vehicleBackPicture = backVehicleUrl,
            vehicleSidePicture = sideVehicleUrl
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
                    Log.d("RegisterDriverStepFourViewModel", "User data updated in Firestore")
                    viewModelScope.launch {
                        _stepFourUpdateEvents.emit(StepFourUpdateEvent.FirebaseUserUpdated)
                    }
                }
                .addOnFailureListener { e ->
                    appViewModel.isLoading = false
                    Log.e(
                        "RegisterDriverStepFourViewModel",
                        "Failed to update user data in Firestore: ${e.message}"
                    )
                    appViewModel.showMessage(
                        type = DialogType.ERROR,
                        title = appContext.getString(R.string.something_went_wrong),
                        message = appContext.getString(R.string.general_error)
                    )
                }
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