package com.mitarifamitaxi.taximetrousuario.viewmodels.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.Constants
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
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RegisterViewModel(context: Context, private val appViewModel: AppViewModel) : ViewModel() {

    private val appContext = context.applicationContext

    var imageUri by mutableStateOf<Uri?>(null)
    var tempImageUri by mutableStateOf<Uri?>(null)

    var showDialog by mutableStateOf(false)

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
    var emailIsError by mutableStateOf(false)
    var emailErrorMessage by mutableStateOf(appContext.getString(R.string.required_field))

    var password by mutableStateOf("")
    var passwordIsError by mutableStateOf(false)
    var passwordErrorMessage by mutableStateOf(appContext.getString(R.string.required_field))

    var confirmPassword by mutableStateOf("")
    var confirmPasswordIsError by mutableStateOf(false)
    var confirmPasswordErrorMessage by mutableStateOf(appContext.getString(R.string.required_field))

    var hasCameraPermission by mutableStateOf(false)
        private set

    init {
        checkCameraPermission()

        /*if (Constants.IS_DEV) {
            firstName = "Mateo"
            lastName = "Ortiz"
            mobilePhone = "3167502612"
            email = "mateotest1@yopmail.com"
            password = "12345678#"
            confirmPassword = "12345678#"
        }*/
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

    fun register(onResult: (Pair<Boolean, String?>) -> Unit) {


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
        emailIsError = email.isEmpty()
        passwordIsError = password.isEmpty()
        confirmPasswordIsError = confirmPassword.isEmpty()

        if (email.isNotEmpty()) {
            emailIsError = !email.isValidEmail()
            if (emailIsError) {
                emailErrorMessage = appContext.getString(R.string.invalid_email)
            }
        }

        if (password.isNotEmpty() && confirmPassword.isNotEmpty()) {
            if (password != confirmPassword) {

                passwordIsError = true
                passwordErrorMessage = appContext.getString(R.string.passwords_do_not_match)

                confirmPasswordIsError = true
                confirmPasswordErrorMessage = appContext.getString(R.string.passwords_do_not_match)

                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.attention),
                    message = appContext.getString(R.string.passwords_do_not_match),
                )
            } else if (!confirmPassword.isValidPassword()) {

                passwordIsError = true
                passwordErrorMessage = appContext.getString(R.string.error_invalid_password)

                confirmPasswordIsError = true
                confirmPasswordErrorMessage = appContext.getString(R.string.error_invalid_password)

                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.attention),
                    message = appContext.getString(R.string.error_invalid_password)
                )
            } else {
                passwordIsError = false
                confirmPasswordIsError = false
            }
        }

        if (firstNameIsError || lastNameIsError || mobilePhoneIsError || emailIsError || passwordIsError || confirmPasswordIsError || imageUri == null) {
            return
        }

        viewModelScope.launch {
            try {
                appViewModel.isLoading = true

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

                val userMap = hashMapOf(
                    "id" to user.uid,
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "mobilePhone" to mobilePhone.trim(),
                    "email" to email.trim(),
                    "profilePicture" to imageUrl,
                    "authProvider" to AuthProvider.email,
                    "role" to UserRole.USER,
                )
                FirebaseFirestore.getInstance().collection("users").document(user.uid).set(userMap)
                    .await()

                appViewModel.isLoading = false

                val localUser = LocalUser(
                    id = user.uid,
                    firstName = firstName,
                    lastName = lastName,
                    mobilePhone = mobilePhone.trim(),
                    email = email.trim(),
                    profilePicture = imageUrl,
                    authProvider = AuthProvider.email,
                    role = UserRole.USER
                )
                LocalUserManager(appContext).saveUserState(localUser)

                onResult(Pair(true, null))

            } catch (e: Exception) {
                Log.e("RegisterViewModel", "Error registering user: ${e.message}")

                appViewModel.isLoading = false
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

class RegisterViewModelFactory(
    private val context: Context,
    private val appViewModel: AppViewModel
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            return RegisterViewModel(context, appViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}