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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.FirebaseStorageUtils
import com.mitarifamitaxi.taximetrousuario.helpers.LocalUserManager
import com.mitarifamitaxi.taximetrousuario.helpers.isValidEmail
import com.mitarifamitaxi.taximetrousuario.helpers.isValidPassword
import com.mitarifamitaxi.taximetrousuario.helpers.toBitmap
import com.mitarifamitaxi.taximetrousuario.models.AuthProvider
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.models.LocalUser
import com.mitarifamitaxi.taximetrousuario.models.UserRole
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegisterDriverStepTwoViewModel(context: Context, private val appViewModel: AppViewModel) :
    ViewModel() {

    private val appContext = context.applicationContext

    var imageUri by mutableStateOf<Uri?>(null)
    var tempImageUri by mutableStateOf<Uri?>(null)
        private set

    var showDialog by mutableStateOf(false)

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
        imageUri = uri
    }

    fun onImageCaptured(success: Boolean) {
        if (success) {
            imageUri = tempImageUri
        }
    }

    fun createTempImageUri() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = appContext.cacheDir
        val image = File.createTempFile(imageFileName, ".jpg", storageDir)
        tempImageUri = FileProvider.getUriForFile(
            Objects.requireNonNull(appContext),
            "${appContext.packageName}.provider",
            image
        )
    }


    fun onNext(onResult: (Pair<Boolean, String?>) -> Unit) {

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