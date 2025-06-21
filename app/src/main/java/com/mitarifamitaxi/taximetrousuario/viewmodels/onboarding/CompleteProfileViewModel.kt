package com.mitarifamitaxi.taximetrousuario.viewmodels.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.FirebaseStorageUtils
import com.mitarifamitaxi.taximetrousuario.helpers.LocalUserManager
import com.mitarifamitaxi.taximetrousuario.helpers.toBitmap
import com.mitarifamitaxi.taximetrousuario.models.AuthProvider
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.models.LocalUser
import com.mitarifamitaxi.taximetrousuario.models.UserRole
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CompleteProfileViewModel(context: Context, private val appViewModel: AppViewModel) :
    ViewModel() {

    private val appContext = context.applicationContext

    var imageUri by mutableStateOf<Uri?>(null)
    var tempImageUri by mutableStateOf<Uri?>(null)

    var showDialog by mutableStateOf(false)

    var userId by mutableStateOf("")

    var firstName by mutableStateOf("")
    var firstNameIsError by mutableStateOf(false)
    var firstNameErrorMessage by mutableStateOf(appContext.getString(R.string.required_field))

    var lastName by mutableStateOf("")
    var lastNameIsError by mutableStateOf(false)
    var lastNameErrorMessage by mutableStateOf(appContext.getString(R.string.required_field))

    var mobilePhone by mutableStateOf("")
    var mobilePhoneIsError by mutableStateOf(false)
    var mobilePhoneErrorMessage by mutableStateOf(appContext.getString(R.string.required_field))

    var email by mutableStateOf("")
    var authProvider by mutableStateOf(AuthProvider.google)

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

    fun completeProfile(onResult: (Pair<Boolean, String?>) -> Unit) {

        if (imageUri == null) {
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.attention),
                message = appContext.getString(R.string.error_image_required),
            )
        }

        firstNameIsError = firstName.isEmpty()
        lastNameIsError = lastName.isEmpty()
        mobilePhoneIsError = mobilePhone.isEmpty()

        if (firstNameIsError || lastNameIsError || mobilePhoneIsError || imageUri == null) {
            return
        }

        viewModelScope.launch {
            try {
                // Show loading indicator
                appViewModel.isLoading = true

                val imageUrl = withContext(Dispatchers.IO) {
                    imageUri?.let { uri ->
                        uri.toBitmap(appContext)
                            ?.let { bitmap ->
                                FirebaseStorageUtils.uploadImage("profilePictures", bitmap)
                            }
                    }
                }

                // Save user information in Firestore
                val userMap = hashMapOf(
                    "id" to userId,
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "mobilePhone" to mobilePhone.trim(),
                    "email" to email,
                    "profilePicture" to imageUrl,
                    "authProvider" to authProvider,
                    "role" to UserRole.USER,
                )
                FirebaseFirestore.getInstance().collection("users").document(userId).set(userMap)
                    .await()

                // Hide loading indicator
                appViewModel.isLoading = false

                // Save user in SharedPreferences
                val localUser = LocalUser(
                    id = userId,
                    firstName = firstName,
                    lastName = lastName,
                    mobilePhone = mobilePhone,
                    email = email,
                    profilePicture = imageUrl,
                    authProvider = authProvider,
                    role = UserRole.USER,
                )

                LocalUserManager(appContext).saveUserState(localUser)

                onResult(Pair(true, null))

            } catch (e: Exception) {
                // Hide loading indicator
                appViewModel.isLoading = false
                // Show error message
                onResult(Pair(false, e.message))
            }

        }

    }

}

class CompleteProfileViewModelFactory(
    private val context: Context,
    private val appViewModel: AppViewModel
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CompleteProfileViewModel::class.java)) {
            return CompleteProfileViewModel(context, appViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}