package com.mitarifamitaxi.taximetrousuario.viewmodels.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.FirebaseStorageUtils
import com.mitarifamitaxi.taximetrousuario.helpers.K
import com.mitarifamitaxi.taximetrousuario.helpers.LocalUserManager
import com.mitarifamitaxi.taximetrousuario.helpers.getFirebaseAuthErrorMessage
import com.mitarifamitaxi.taximetrousuario.helpers.isValidEmail
import com.mitarifamitaxi.taximetrousuario.helpers.isValidPassword
import com.mitarifamitaxi.taximetrousuario.helpers.toBitmap
import com.mitarifamitaxi.taximetrousuario.models.AuthProvider
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.models.LocalUser
import com.mitarifamitaxi.taximetrousuario.models.UserRole
import com.mitarifamitaxi.taximetrousuario.states.RegisterState
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RegisterViewModel(context: Context, private val appViewModel: AppViewModel) : ViewModel() {

    private val appContext = context.applicationContext

    private val _uiState = MutableStateFlow(RegisterState())
    val uiState: StateFlow<RegisterState> = _uiState

    init {
        checkCameraPermission()

        /*if (K.IS_DEV) {
            _uiState.update {
                it.copy(
                    firstName = "Mateo",
                    lastName = "Ortiz",
                    mobilePhone = "3167502612",
                    email = "mateotest4@yopmail.com",
                    password = "12345678#",
                    confirmPassword = "12345678#"
                )
            }
        }*/
    }

    fun onFistNameChange(value: String) = _uiState.update {
        it.copy(firstName = value)
    }

    fun onLastNameChange(value: String) = _uiState.update {
        it.copy(lastName = value)
    }

    fun onMobilePhoneChange(value: String) = _uiState.update {
        it.copy(mobilePhone = value)
    }

    fun onEmailChange(value: String) = _uiState.update {
        it.copy(email = value)
    }

    fun onPasswordChange(value: String) = _uiState.update {
        it.copy(password = value)
    }

    fun onConfirmPasswordChange(value: String) = _uiState.update {
        it.copy(confirmPassword = value)
    }

    fun onTempImageUriChange(value: Uri?) = _uiState.update {
        it.copy(tempImageUri = value)
    }

    fun onShowDialogSelectPhotoChange(value: Boolean) = _uiState.update {
        it.copy(showDialogSelectPhoto = value)
    }


    private fun checkCameraPermission() {
        _uiState.update {
            it.copy(
                hasCameraPermission = ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            )
        }
    }

    fun onPermissionResult(isGranted: Boolean) {
        _uiState.update {
            it.copy(
                hasCameraPermission = isGranted,
            )
        }
    }

    fun onImageSelected(uri: Uri?) {
        _uiState.update {
            it.copy(
                imageUri = uri,
            )
        }
    }

    fun onImageCaptured(success: Boolean) {
        if (success) {
            _uiState.update {
                it.copy(
                    imageUri = _uiState.value.tempImageUri
                )
            }
        }
    }

    fun register(onResult: (Pair<Boolean, String?>) -> Unit) {

        val stateVal = _uiState.value

        if (stateVal.imageUri == null) {
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.profile_photo_required),
                message = appContext.getString(R.string.must_select_profile_photo),
            )
        }

        _uiState.update { state ->
            state.copy(
                firstNameIsError = state.firstName.isBlank(),
                firstNameErrorMessage = if (state.firstName.isBlank()) appContext.getString(R.string.required_field) else "",
                lastNameIsError = state.lastName.isBlank(),
                lastNameErrorMessage = if (state.lastName.isBlank()) appContext.getString(R.string.required_field) else "",
                mobilePhoneIsError = state.mobilePhone.isBlank(),
                mobilePhoneErrorMessage = if (state.mobilePhone.isBlank()) appContext.getString(R.string.required_field) else "",
                emailIsError = state.email.isBlank(),
                emailErrorMessage = if (state.email.isBlank()) appContext.getString(R.string.required_field) else "",
                passwordIsError = state.password.isBlank(),
                passwordErrorMessage = if (state.password.isBlank()) appContext.getString(R.string.required_field) else "",
                confirmPasswordIsError = state.confirmPassword.isBlank(),
                confirmPasswordErrorMessage = if (state.confirmPassword.isBlank()) appContext.getString(
                    R.string.required_field
                ) else "",
            )
        }

        _uiState.update { state ->
            var newState = state

            if (state.email.isNotBlank()) {
                newState = if (!state.email.isValidEmail()) {
                    state.copy(
                        emailIsError = true,
                        emailErrorMessage = appContext.getString(R.string.invalid_email)
                    )
                } else {
                    state.copy(emailIsError = false)
                }
            }

            if (state.password.isNotBlank() && state.confirmPassword.isNotBlank()) {
                newState = when {

                    !state.password.isValidPassword() -> {
                        state.copy(
                            passwordIsError = true,
                            passwordErrorMessage = appContext.getString(R.string.error_invalid_password)
                        )
                    }

                    state.password != state.confirmPassword -> {
                        state.copy(
                            confirmPasswordIsError = true,
                            confirmPasswordErrorMessage = appContext.getString(R.string.passwords_do_not_match)
                        )
                    }

                    else -> state.copy(passwordIsError = false, confirmPasswordIsError = false)
                }
            }

            newState
        }

        if (_uiState.value.firstNameIsError || _uiState.value.lastNameIsError || _uiState.value.mobilePhoneIsError || _uiState.value.emailIsError || _uiState.value.passwordIsError || _uiState.value.confirmPasswordIsError || _uiState.value.imageUri == null) {
            return
        }

        viewModelScope.launch {
            try {
                appViewModel.setLoading(true)

                // Create user with email and password in Firebase Auth
                val authResult =
                    FirebaseAuth.getInstance()
                        .createUserWithEmailAndPassword(
                            stateVal.email.trim(),
                            stateVal.password.trim()
                        )
                        .await()
                val user = authResult.user ?: throw Exception("User creation failed")

                val imageUrl = withContext(Dispatchers.IO) {
                    stateVal.imageUri.let { uri ->
                        uri?.toBitmap(appContext)
                            ?.let { bitmap ->
                                FirebaseStorageUtils.uploadImage("profilePictures", bitmap)
                            }
                    }
                }

                val userMap = hashMapOf(
                    "id" to user.uid,
                    "firstName" to stateVal.firstName,
                    "lastName" to stateVal.lastName,
                    "mobilePhone" to stateVal.mobilePhone.trim(),
                    "email" to stateVal.email.trim(),
                    "profilePicture" to imageUrl,
                    "authProvider" to AuthProvider.email,
                    "role" to UserRole.USER,
                )
                FirebaseFirestore.getInstance().collection("users").document(user.uid).set(userMap)
                    .await()

                appViewModel.setLoading(false)

                val localUser = LocalUser(
                    id = user.uid,
                    firstName = stateVal.firstName,
                    lastName = stateVal.lastName,
                    mobilePhone = stateVal.mobilePhone.trim(),
                    email = stateVal.email.trim(),
                    profilePicture = imageUrl,
                    authProvider = AuthProvider.email,
                    role = UserRole.USER
                )
                LocalUserManager(appContext).saveUserState(localUser)

                onResult(Pair(true, null))

            } catch (e: Exception) {
                Log.e("RegisterViewModel", "Error registering user: ${e.message}")

                appViewModel.setLoading(false)
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