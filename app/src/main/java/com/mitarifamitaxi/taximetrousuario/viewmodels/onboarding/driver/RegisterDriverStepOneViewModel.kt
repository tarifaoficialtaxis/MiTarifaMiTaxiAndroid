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
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.K
import com.mitarifamitaxi.taximetrousuario.helpers.FirebaseStorageUtils
import com.mitarifamitaxi.taximetrousuario.helpers.LocalUserManager
import com.mitarifamitaxi.taximetrousuario.helpers.getFirebaseAuthErrorMessage
import com.mitarifamitaxi.taximetrousuario.helpers.isValidEmail
import com.mitarifamitaxi.taximetrousuario.helpers.isValidPassword
import com.mitarifamitaxi.taximetrousuario.helpers.toBitmap
import com.mitarifamitaxi.taximetrousuario.models.AuthProvider
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.models.LocalUser
import com.mitarifamitaxi.taximetrousuario.models.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RegisterDriverStepOneViewModel(context: Context, private val appViewModel: AppViewModel) :
    ViewModel() {

    private val appContext = context.applicationContext

    var userId by mutableStateOf("")

    var firstName by mutableStateOf("")
    var lastName by mutableStateOf("")
    var documentNumber by mutableStateOf("")
    var mobilePhone by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")

    var authProvider by mutableStateOf(AuthProvider.email)

    var imageUri by mutableStateOf<Uri?>(null)
    var tempImageUri by mutableStateOf<Uri?>(null)

    var showDialog by mutableStateOf(false)

    var hasCameraPermission by mutableStateOf(false)
        private set

    init {
        checkCameraPermission()

        if (K.IS_DEV) {
            firstName = "Carlos"
            lastName = "Ruiz"
            mobilePhone = "3167502612"
            documentNumber = "232323232"
            email = "drivertest1@yopmail.com"
            password = "12345678#"
            confirmPassword = "12345678#"
        }
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

    fun onNextEmailPassword(onSuccess: () -> Unit) {
        if (firstName.isEmpty() ||
            lastName.isEmpty() ||
            documentNumber.isEmpty() ||
            mobilePhone.isEmpty() ||
            email.isEmpty() ||
            password.isEmpty() ||
            confirmPassword.isEmpty()
        ) {
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.attention),
                message = appContext.getString(R.string.all_fields_required),
            )
            return
        }

        if (!email.isValidEmail()) {
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.attention),
                message = appContext.getString(R.string.error_invalid_email),
            )
            return
        }

        if (!password.isValidPassword()) {
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.attention),
                message = appContext.getString(R.string.error_invalid_password)
            )
            return
        }

        if (password != confirmPassword) {
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.attention),
                message = appContext.getString(R.string.passwords_do_not_match),
            )
            return
        }

        if (imageUri == null) {
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.attention),
                message = appContext.getString(R.string.error_image_required),
            )
            return
        }

        viewModelScope.launch {
            try {
                appViewModel.isLoading = true

                val documentExists = FirebaseFirestore.getInstance()
                    .collection("users")
                    .whereEqualTo("documentNumber", documentNumber.trim())
                    .get()
                    .await()
                    .isEmpty.not()

                if (documentExists) {
                    appViewModel.isLoading = false
                    appViewModel.showMessage(
                        type = DialogType.ERROR,
                        title = appContext.getString(R.string.attention),
                        message = appContext.getString(R.string.error_document_exists)
                    )
                    return@launch
                }

                // Create user with email and password in Firebase Auth
                val authResult =
                    FirebaseAuth.getInstance()
                        .createUserWithEmailAndPassword(email.trim(), password.trim())
                        .await()
                val user = authResult.user ?: throw Exception("User creation failed")

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
                    "id" to user.uid,
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "documentNumber" to documentNumber,
                    "mobilePhone" to mobilePhone.trim(),
                    "email" to email.trim(),
                    "profilePicture" to imageUrl,
                    "role" to UserRole.DRIVER,
                )
                FirebaseFirestore.getInstance().collection("users").document(user.uid).set(userMap)
                    .await()

                appViewModel.isLoading = false

                // Save user in SharedPreferences
                val localUser = LocalUser(
                    id = user.uid,
                    firstName = firstName,
                    lastName = lastName,
                    documentNumber = documentNumber,
                    mobilePhone = mobilePhone.trim(),
                    email = email.trim(),
                    authProvider = AuthProvider.email,
                    role = UserRole.DRIVER,
                    profilePicture = imageUrl
                )
                LocalUserManager(appContext).saveUserState(localUser)

                onSuccess()

            } catch (e: Exception) {
                Log.e("RegisterDriverStepOneViewModel", "Error registering user: ${e.message}")
                appViewModel.isLoading = false
                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = appContext.getString(R.string.error_registering_user)
                )

            }

        }
    }

    fun onNextGoogle(onSuccess: () -> Unit) {
        if (firstName.isEmpty() ||
            lastName.isEmpty() ||
            documentNumber.isEmpty() ||
            mobilePhone.isEmpty()
        ) {
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.attention),
                message = appContext.getString(R.string.all_fields_required),
            )
            return
        }

        if (imageUri == null) {
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.attention),
                message = appContext.getString(R.string.error_image_required),
            )
            return
        }

        viewModelScope.launch {
            try {
                appViewModel.isLoading = true

                val documentExists = FirebaseFirestore.getInstance()
                    .collection("users")
                    .whereEqualTo("documentNumber", documentNumber.trim())
                    .get()
                    .await()
                    .isEmpty.not()

                if (documentExists) {
                    appViewModel.isLoading = false
                    appViewModel.showMessage(
                        type = DialogType.ERROR,
                        title = appContext.getString(R.string.attention),
                        message = appContext.getString(R.string.error_document_exists)
                    )
                    return@launch
                }

                // Get current user from Firebase Auth
                val user =
                    FirebaseAuth.getInstance().currentUser ?: throw Exception("User not logged in")

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
                    "id" to user.uid,
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "documentNumber" to documentNumber,
                    "mobilePhone" to mobilePhone.trim(),
                    "email" to user.email?.trim(),
                    "profilePicture" to imageUrl,
                    "role" to UserRole.DRIVER,
                )
                FirebaseFirestore.getInstance().collection("users").document(user.uid).set(userMap)
                    .await()

                val localUser = LocalUser(
                    id = user.uid,
                    firstName = firstName,
                    lastName = lastName,
                    documentNumber = documentNumber,
                    mobilePhone = mobilePhone.trim(),
                    email = email.trim(),
                    authProvider = AuthProvider.email,
                    role = UserRole.DRIVER,
                    profilePicture = imageUrl
                )
                LocalUserManager(appContext).saveUserState(localUser)

                onSuccess()

            } catch (e: Exception) {
                appViewModel.isLoading = false
                Log.e("RegisterDriverStepOneViewModel", "Error registering user: ${e.message}")

                val errorMessage = when (e) {
                    is FirebaseAuthUserCollisionException -> getFirebaseAuthErrorMessage(
                        appContext,
                        e.errorCode
                    )

                    else -> appContext.getString(R.string.general_error)
                }

                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = errorMessage
                )

            }
        }
    }
}


class RegisterDriverStepOneViewModelFactory(
    private val context: Context,
    private val appViewModel: AppViewModel
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterDriverStepOneViewModel::class.java)) {
            return RegisterDriverStepOneViewModel(context, appViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}