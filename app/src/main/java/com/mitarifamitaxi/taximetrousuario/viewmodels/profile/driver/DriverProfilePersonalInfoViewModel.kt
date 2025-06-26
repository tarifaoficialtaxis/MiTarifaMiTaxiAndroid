package com.mitarifamitaxi.taximetrousuario.viewmodels.profile.driver

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FirebaseFirestore
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.FirebaseStorageUtils
import com.mitarifamitaxi.taximetrousuario.helpers.LocalUserManager
import com.mitarifamitaxi.taximetrousuario.helpers.getFirebaseAuthErrorMessage
import com.mitarifamitaxi.taximetrousuario.helpers.isValidEmail
import com.mitarifamitaxi.taximetrousuario.helpers.toBitmap
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class DriverProfilePersonalInfoViewModel(context: Context, private val appViewModel: AppViewModel) :
    ViewModel() {

    private val appContext = context.applicationContext
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val originalProfilePictureUrl: String? = appViewModel.uiState.value.userData?.profilePicture
    var imageUri by mutableStateOf<Uri?>(appViewModel.uiState.value.userData?.profilePicture?.toUri())
    var tempImageUri by mutableStateOf<Uri?>(null)

    var showDialog by mutableStateOf(false)

    var hasCameraPermission by mutableStateOf(false)
        private set

    var firstName by mutableStateOf(appViewModel.uiState.value.userData?.firstName)
    var lastName by mutableStateOf(appViewModel.uiState.value.userData?.lastName)
    var documentNumber by mutableStateOf(appViewModel.uiState.value.userData?.documentNumber)
    var mobilePhone by mutableStateOf(appViewModel.uiState.value.userData?.mobilePhone)

    private val originalEmail: String? = appViewModel.uiState.value.userData?.email
    var email by mutableStateOf(appViewModel.uiState.value.userData?.email)

    var familyNumber by mutableStateOf(appViewModel.uiState.value.userData?.familyNumber)
    var supportNumber by mutableStateOf(appViewModel.uiState.value.userData?.supportNumber)

    private val _hideKeyboardEvent = MutableLiveData<Boolean>()
    val hideKeyboardEvent: LiveData<Boolean> get() = _hideKeyboardEvent

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    var showPasswordPopUp by mutableStateOf(false)

    sealed class NavigationEvent {
        object Finish : NavigationEvent()
    }

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

    fun resetHideKeyboardEvent() {
        _hideKeyboardEvent.value = false
    }

    fun handleUpdate() {

        if ((firstName ?: "").isEmpty() ||
            (lastName ?: "").isEmpty() ||
            (documentNumber ?: "").isEmpty() ||
            (mobilePhone ?: "").isEmpty() ||
            (email ?: "").isEmpty() ||
            (familyNumber ?: "").isEmpty() ||
            (supportNumber ?: "").isEmpty()
        ) {
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.something_went_wrong),
                message = appContext.getString(R.string.all_fields_required)
            )
            return
        }

        if (!(email ?: "").isValidEmail()) {
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.something_went_wrong),
                message = appContext.getString(R.string.error_invalid_email)
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
            appViewModel.setLoading(true)

            val finalImageUrl: String? = if (imageUri.toString() != originalProfilePictureUrl) {
                val uploadedUrl = withContext(Dispatchers.IO) {
                    imageUri
                        ?.toBitmap(appContext)
                        ?.let { bitmap ->
                            FirebaseStorageUtils.uploadImage("profilePictures", bitmap)
                        }
                }
                originalProfilePictureUrl?.let { oldUrl ->
                    FirebaseStorageUtils.deleteImage(oldUrl)
                }
                uploadedUrl
            } else {
                originalProfilePictureUrl
            }


            val finalEmail: String? = if (email != originalEmail) {
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                    ?: throw IllegalStateException("Usuario no autenticado")

                // Si la sesión es muy vieja, puede lanzar FirebaseAuthRecentLoginRequiredException
                firebaseUser.verifyBeforeUpdateEmail(email!!)
                    .await()
                email
            } else {
                originalEmail
            }


            val updatedUser = appViewModel.uiState.value.userData?.copy(
                firstName = firstName,
                lastName = lastName,
                mobilePhone = mobilePhone,
                email = finalEmail,
                familyNumber = familyNumber,
                supportNumber = supportNumber,
                profilePicture = finalImageUrl
            )

            try {
                updatedUser?.let { user ->
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(appViewModel.uiState.value.userData?.id ?: "")
                        .update(
                            mapOf(
                                "firstName" to user.firstName,
                                "lastName" to user.lastName,
                                "mobilePhone" to user.mobilePhone,
                                "email" to user.email,
                                "familyNumber" to user.familyNumber,
                                "supportNumber" to user.supportNumber,
                                "profilePicture" to user.profilePicture
                            )
                        ).await()

                    appViewModel.updateLocalUser(user)
                    LocalUserManager(appContext).saveUserState(user)
                    appViewModel.showMessage(
                        type = DialogType.SUCCESS,
                        title = appContext.getString(R.string.profile_updated),
                        message = appContext.getString(R.string.user_updated_successfully),
                        buttonText = appContext.getString(R.string.accept),
                        onButtonClicked = {
                            viewModelScope.launch {
                                _navigationEvents.emit(NavigationEvent.Finish)
                            }
                        }
                    )

                }
            } catch (error: Exception) {
                if (error is FirebaseAuthRecentLoginRequiredException) {
                    appViewModel.showMessage(
                        type = DialogType.WARNING,
                        title = appContext.getString(R.string.attention),
                        message = appContext.getString(R.string.for_security_login_again),
                        buttonText = appContext.getString(R.string.accept),
                        onButtonClicked = {
                            showPasswordPopUp = true
                        }
                    )
                } else {
                    Log.e(
                        "DriverProfilePersonalInfoViewModel",
                        "Error actualizando email: ${error.message}"
                    )
                    appViewModel.showMessage(
                        type = DialogType.ERROR,
                        title = appContext.getString(R.string.something_went_wrong),
                        message = appContext.getString(R.string.error_updating_user)
                    )
                }

            } finally {
                appViewModel.setLoading(false)
            }
        }
    }

    fun authenticateUserByEmailAndPassword(password: String) {

        appViewModel.setLoading(true)

        viewModelScope.launch {
            try {
                if (email == null || email!!.isEmpty()) {
                    appViewModel.setLoading(false)
                    appViewModel.showMessage(
                        type = DialogType.ERROR,
                        title = appContext.getString(R.string.something_went_wrong),
                        message = appContext.getString(R.string.error_invalid_email)
                    )
                    return@launch
                }

                val userCredential =
                    auth.signInWithEmailAndPassword(email!!.trim(), password).await()
                val user = userCredential.user
                if (user != null) {

                }
            } catch (e: Exception) {
                appViewModel.setLoading(false)

                Log.e("ProfileViewModel", "Error logging in: ${e.message}")

                val errorMessage = when (e) {
                    is FirebaseAuthInvalidCredentialsException -> getFirebaseAuthErrorMessage(
                        appContext,
                        e.errorCode
                    )

                    is FirebaseAuthInvalidUserException -> getFirebaseAuthErrorMessage(
                        appContext,
                        e.errorCode
                    )

                    is FirebaseAuthException -> getFirebaseAuthErrorMessage(appContext, e.errorCode)
                    else -> appContext.getString(R.string.something_went_wrong)
                }

                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = errorMessage,
                )

            }
        }
    }

}

class DriverProfilePersonalInfoViewModelFactory(
    private val context: Context,
    private val appViewModel: AppViewModel
) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DriverProfilePersonalInfoViewModel::class.java)) {
            return DriverProfilePersonalInfoViewModel(context, appViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}