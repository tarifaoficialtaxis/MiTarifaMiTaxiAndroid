package com.mitarifamitaxi.taximetrousuario.viewmodels.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
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
import com.mitarifamitaxi.taximetrousuario.states.CompleteProfileState
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CompleteProfileViewModel(context: Context, private val appViewModel: AppViewModel) :
    ViewModel() {

    private val appContext = context.applicationContext

    private val _uiState = MutableStateFlow(CompleteProfileState())
    val uiState: StateFlow<CompleteProfileState> = _uiState

    init {
        checkCameraPermission()
    }

    fun onUserIdChange(value: String) = _uiState.update {
        it.copy(userId = value)
    }

    fun onFistNameChange(value: String) = _uiState.update {
        it.copy(firstName = value)
    }

    fun onLastNameChange(value: String) = _uiState.update {
        it.copy(lastName = value)
    }

    fun onEmailChange(value: String) = _uiState.update {
        it.copy(email = value)
    }

    fun onMobilePhoneChange(value: String) = _uiState.update {
        it.copy(mobilePhone = value)
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
                    imageUri = _uiState.value.tempImageUri,
                )
            }
        }
    }


    fun completeProfile(onResult: (Pair<Boolean, String?>) -> Unit) {
        val stateVal = _uiState.value

        _uiState.update { state ->
            state.copy(
                firstNameIsError = state.firstName.isBlank(),
                firstNameErrorMessage = if (state.firstName.isBlank()) appContext.getString(R.string.required_field) else "",
                lastNameIsError = state.lastName.isBlank(),
                lastNameErrorMessage = if (state.lastName.isBlank()) appContext.getString(R.string.required_field) else "",
                mobilePhoneIsError = state.mobilePhone.isBlank(),
                mobilePhoneErrorMessage = if (state.mobilePhone.isBlank()) appContext.getString(R.string.required_field) else ""
            )
        }

        if (_uiState.value.firstNameIsError || _uiState.value.lastNameIsError || _uiState.value.mobilePhoneIsError) {
            return
        }

        viewModelScope.launch {
            try {
                // Show loading indicator
                appViewModel.setLoading(true)

                val imageUrl = withContext(Dispatchers.IO) {
                    stateVal.imageUri.let { uri ->
                        uri?.toBitmap(appContext)
                            ?.let { bitmap ->
                                FirebaseStorageUtils.uploadImage("profilePictures", bitmap)
                            }
                    }
                }

                // Save user information in Firestore
                val userMap = hashMapOf(
                    "id" to stateVal.userId,
                    "firstName" to stateVal.firstName,
                    "lastName" to stateVal.lastName,
                    "mobilePhone" to stateVal.mobilePhone.trim(),
                    "email" to stateVal.email,
                    "profilePicture" to imageUrl,
                    "authProvider" to AuthProvider.google,
                )
                FirebaseFirestore.getInstance().collection("users").document(stateVal.userId)
                    .set(userMap)
                    .await()

                // Hide loading indicator
                appViewModel.setLoading(false)

                // Save user in SharedPreferences
                val localUser = LocalUser(
                    id = stateVal.userId,
                    firstName = stateVal.firstName,
                    lastName = stateVal.lastName,
                    mobilePhone = stateVal.mobilePhone,
                    email = stateVal.email,
                    profilePicture = imageUrl,
                    authProvider = AuthProvider.google
                )

                LocalUserManager(appContext).saveUserState(localUser)

                onResult(Pair(true, null))

            } catch (e: Exception) {
                // Hide loading indicator
                appViewModel.setLoading(false)
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
        return CompleteProfileViewModel(context, appViewModel) as T
    }
}