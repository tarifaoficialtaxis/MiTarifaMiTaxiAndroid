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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3


class RegisterDriverStepTwoViewModel(context: Context, private val appViewModel: AppViewModel) :
    ViewModel() {

    private val appContext = context.applicationContext

    var isFrontImageSelected by mutableStateOf(false)

    var frontImageUri by mutableStateOf<Uri?>(null)
    var frontTempImageUri by mutableStateOf<Uri?>(null)
        private set

    var backImageUri by mutableStateOf<Uri?>(null)
    var backTempImageUri by mutableStateOf<Uri?>(null)
        private set

    var hasCameraPermission by mutableStateOf(false)
        private set

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

        if (isFrontImageSelected) {
            frontImageUri = uri
        } else {
            backImageUri = uri
        }

    }

    fun onImageCaptured(success: Boolean) {
        if (success) {

            if (isFrontImageSelected) {
                frontImageUri = frontTempImageUri
            } else {
                backImageUri = backTempImageUri
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
        if (isFrontImageSelected) {
            frontTempImageUri = tempImageUri
        } else {
            backTempImageUri = tempImageUri
        }
    }


    fun onNext() {
        if (frontImageUri == null || backImageUri == null) {
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.attention),
                message = appContext.getString(R.string.error_driving_license)
            )
            return
        }

        viewModelScope.launch {

            appViewModel.isLoading = true

            val deferreds = listOf(frontImageUri!!, backImageUri!!).map { uri ->
                async(Dispatchers.IO) {
                    uri.toBitmap(appContext)
                        ?.let { bitmap ->
                            FirebaseStorageUtils.uploadImage(
                                "drivingLicenses",
                                bitmap
                            )
                        }
                }
            }

            val (frontUrl, backUrl) = deferreds.awaitAll()

            if (frontUrl == null || backUrl == null) {
                appViewModel.isLoading = false
                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = appContext.getString(R.string.general_error)
                )
                return@launch
            }

            updateUserData(frontUrl, backUrl)

        }
    }

    private fun updateUserData(
        frontDrivingLicenseUrl: String? = null,
        backDrivingLicenseUrl: String? = null
    ) {

        val userData = LocalUserManager(appContext).getUserState()

        val userDataUpdated = userData?.copy(
            frontDrivingLicense = frontDrivingLicenseUrl,
            backDrivingLicense = backDrivingLicenseUrl
        )

        userDataUpdated?.let {
            LocalUserManager(appContext).saveUserState(it)
            appViewModel.updateUserDataOnFirebase(it)
        }
    }
}

class RegisterDriverStepTwoViewModelFactory(
    private val context: Context,
    private val appViewModel: AppViewModel
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterDriverStepTwoViewModel::class.java)) {
            return RegisterDriverStepTwoViewModel(context, appViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}