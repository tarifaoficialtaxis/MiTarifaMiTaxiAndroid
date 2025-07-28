package com.mitarifamitaxi.taximetrousuario.viewmodels.profile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FirebaseFirestore
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.ContactsCatalogManager
import com.mitarifamitaxi.taximetrousuario.helpers.FirebaseStorageUtils
import com.mitarifamitaxi.taximetrousuario.helpers.LocalUserManager
import com.mitarifamitaxi.taximetrousuario.helpers.UserLocationManager
import com.mitarifamitaxi.taximetrousuario.helpers.getFirebaseAuthErrorMessage
import com.mitarifamitaxi.taximetrousuario.helpers.isValidEmail
import com.mitarifamitaxi.taximetrousuario.helpers.toBitmap
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.states.ProfileState
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ProfileViewModel(context: Context, private val appViewModel: AppViewModel) : ViewModel() {

    private val appContext = context.applicationContext
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(ProfileState())
    val uiState: StateFlow<ProfileState> = _uiState

    private val _hideKeyboardEvent = MutableLiveData<Boolean>()
    val hideKeyboardEvent: LiveData<Boolean> get() = _hideKeyboardEvent

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    sealed class NavigationEvent {
        object LogOutComplete : NavigationEvent()
        object Finish : NavigationEvent()
        object LaunchGoogleSignIn : NavigationEvent()
    }

    val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(appContext.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(appContext, gso)
    }

    init {
        checkCameraPermission()

        val currentUser = appViewModel.uiState.value.userData

        onFistNameChange(currentUser?.firstName ?: "")
        onLastNameChange(currentUser?.lastName ?: "")
        onMobilePhoneChange(currentUser?.mobilePhone ?: "")
        onEmailChange(currentUser?.email ?: "")
        onFamilyNumberChange(currentUser?.familyNumber ?: "")
        onSupportNumberChange(currentUser?.supportNumber ?: "")
        //onImageSelected(currentUser?.profilePicture?.toUri())
        onOriginalProfilePictureUrlChange(currentUser?.profilePicture ?: "")

        viewModelScope.launch {
            getTripsByUserId(currentUser?.id ?: "")
        }
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

    fun onFamilyNumberChange(value: String) = _uiState.update {
        it.copy(familyNumber = value)
    }

    fun onSupportNumberChange(value: String) = _uiState.update {
        it.copy(supportNumber = value)
    }

    fun onTempImageUriChange(value: Uri?) = _uiState.update {
        it.copy(tempImageUri = value)
    }

    fun onShowDialogSelectPhotoChange(value: Boolean) = _uiState.update {
        hideKeyboardEvent
        it.copy(showDialogSelectPhoto = value)
    }

    fun onShowDialogPassword(value: Boolean) = _uiState.update {
        it.copy(showPasswordPopUp = value)
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

    fun onOriginalProfilePictureUrlChange(picture: String) {
        _uiState.update {
            it.copy(
                originalProfilePictureUrl = picture,
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

    fun resetHideKeyboardEvent() {
        _hideKeyboardEvent.value = false
    }

    private suspend fun getTripsByUserId(userId: String) {
        try {
            appViewModel.setLoading(true)
            val tripsSnapshot = FirebaseFirestore.getInstance()
                .collection("trips")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            _uiState.update {
                it.copy(tripsCount = tripsSnapshot.size())
            }


            if (!tripsSnapshot.isEmpty) {
                val trips = tripsSnapshot.documents
                val distance = trips.sumOf { it.getDouble("distance") ?: 0.0 }
                _uiState.update {
                    it.copy(distanceCount = (distance / 1000).toInt())
                }
            }
            appViewModel.setLoading(false)
        } catch (error: Exception) {
            Log.e("ProfileViewModel", "Error fetching trips: ${error.message}")
            appViewModel.setLoading(false)
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.something_went_wrong),
                message = appContext.getString(R.string.error_fetching_trips),
            )
        }
    }

    fun handleUpdate() {

        val stateVal = _uiState.value

        /*if (stateVal.imageUri == null) {
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.profile_photo_required),
                message = appContext.getString(R.string.must_select_profile_photo),
            )
        }*/

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

            newState
        }

        if (_uiState.value.firstNameIsError || _uiState.value.lastNameIsError || _uiState.value.mobilePhoneIsError || _uiState.value.emailIsError) {
            return
        }

        viewModelScope.launch {
            appViewModel.setLoading(true)

            val userId = appViewModel.uiState.value.userData?.id

            val finalImageUrl: String? =
                if (stateVal.imageUri.toString() != stateVal.originalProfilePictureUrl) {
                    val uploadedUrl = withContext(Dispatchers.IO) {
                        stateVal.imageUri
                            ?.toBitmap(appContext)
                            ?.let { bitmap ->
                                FirebaseStorageUtils.uploadImage("appFiles/$userId/profilePicture", bitmap)
                            }
                    }
                    stateVal.originalProfilePictureUrl.let { oldUrl ->
                        FirebaseStorageUtils.deleteImage(oldUrl)
                    }
                    uploadedUrl
                } else {
                    stateVal.originalProfilePictureUrl
                }


            val updatedUser = appViewModel.uiState.value.userData?.copy(
                firstName = stateVal.firstName,
                lastName = stateVal.lastName,
                profilePicture = finalImageUrl,
                mobilePhone = stateVal.mobilePhone,
                familyNumber = stateVal.familyNumber,
                supportNumber = stateVal.supportNumber
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
                                "profilePicture" to user.profilePicture,
                                "mobilePhone" to user.mobilePhone,
                                "familyNumber" to user.familyNumber,
                                "supportNumber" to user.supportNumber
                            )
                        ).await()

                    appViewModel.updateLocalUser(user)
                    LocalUserManager(appContext).saveUserState(user)
                    appViewModel.setLoading(false)
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
                Log.e("ProfileViewModel", "Error updating user: ${error.message}")
                appViewModel.setLoading(false)
                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = appContext.getString(R.string.error_updating_user)
                )
            } finally {
                appViewModel.setLoading(false)
            }
        }
    }

    fun onDeleteAccountClicked() {
        _hideKeyboardEvent.value = true

        appViewModel.showMessage(
            type = DialogType.ERROR,
            title = appContext.getString(R.string.delete_account_question),
            message = appContext.getString(R.string.delete_account_message),
            buttonText = appContext.getString(R.string.delete_account),
            onButtonClicked = {
                handleDeleteAccount()
            }
        )
    }

    fun handleDeleteAccount() {

        viewModelScope.launch {
            try {
                appViewModel.setLoading(true)

                // Delete Firebase Auth User
                deleteFirebaseAuthUser()

            } catch (error: Exception) {
                // Catch errors from Firestore deletion
                Log.e("ProfileViewModel", "Error deleting account data: ${error.message}", error)
                appViewModel.setLoading(false)
                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = appContext.getString(R.string.error_deleting_account)
                )
            }
        }
    }

    fun getUserAuthType() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            for (profile in user.providerData) {
                val providerId = profile.providerId
                Log.d("AuthProviderCheck", "Provider ID: $providerId")
                if (providerId == EmailAuthProvider.PROVIDER_ID) {
                    Log.d("AuthProviderCheck", "Usuario autenticado con Correo/Contraseña")
                    onShowDialogPassword(true)
                } else if (providerId == GoogleAuthProvider.PROVIDER_ID) {
                    Log.d("AuthProviderCheck", "Usuario autenticado con Google")
                    viewModelScope.launch {
                        _navigationEvents.emit(NavigationEvent.LaunchGoogleSignIn)
                    }
                }

            }
        }
    }

    fun authenticateUserByEmailAndPassword(password: String) {

        appViewModel.setLoading(true)

        viewModelScope.launch {
            try {
                val userCredential =
                    auth.signInWithEmailAndPassword(_uiState.value.email.trim(), password).await()
                val user = userCredential.user
                if (user != null) {
                    deleteFirebaseAuthUser()
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

    fun deleteFirebaseAuthUser() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = appViewModel.uiState.value.userData?.id ?: ""

        if (currentUser == null || userId.isEmpty()) {
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.something_went_wrong),
                message = appContext.getString(R.string.error_deleting_account)
            )
            return
        }

        appViewModel.setLoading(true)

        viewModelScope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()

                // 1. Obtener todos los trips del usuario
                val tripsSnapshot = firestore.collection("trips")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                // 2. Crear y ejecutar el batch para eliminar todos los trips
                if (tripsSnapshot.documents.isNotEmpty()) {
                    val batch = firestore.batch()
                    tripsSnapshot.documents.forEach { tripDoc ->
                        batch.delete(tripDoc.reference)
                    }
                    batch.commit().await()
                    Log.d("ProfileViewModel", "Deleted ${tripsSnapshot.size()} trips for user $userId")
                }

                // 3. Eliminar el documento del usuario
                firestore.collection("users").document(userId).delete().await()
                Log.d("ProfileViewModel", "Deleted Firestore user document $userId")

                // 4. Eliminar la carpeta completa del usuario en Firebase Storage
                FirebaseStorageUtils.deleteFolder("appFiles/$userId/")

                // 5. Eliminar el usuario de Firebase Auth
                currentUser.delete().await()
                Log.d("ProfileViewModel", "Deleted Firebase Auth user ${currentUser.uid}")

                // 6. Mostrar mensaje de éxito
                appViewModel.setLoading(false)
                appViewModel.showMessage(
                    type = DialogType.SUCCESS,
                    title = appContext.getString(R.string.warning),
                    message = appContext.getString(R.string.account_deleted_message),
                    buttonText = appContext.getString(R.string.accept),
                    showCloseButton = false,
                    onButtonClicked = { logOut() }
                )

            } catch (authError: FirebaseAuthRecentLoginRequiredException) {
                appViewModel.setLoading(false)
                Log.w("ProfileViewModel", "Auth deletion failed: Re-authentication required.", authError)
                getUserAuthType()

            } catch (e: Exception) {
                appViewModel.setLoading(false)
                Log.e("ProfileViewModel", "Error deleting user: ${e.message}", e)
                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = appContext.getString(R.string.error_deleting_account)
                )
            }
        }
    }

    fun handleSignInResult(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account: GoogleSignInAccount? = task.getResult(ApiException::class.java)
            if (account != null) {
                firebaseAuthWithGoogle(account.idToken!!)
            } else {
                Log.e("ProfileViewModel", "Google Sign-In failed: No account found")
                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = appContext.getString(R.string.error_google_sign_in)
                )
            }
        } catch (e: ApiException) {
            Log.e("ProfileViewModel", "Google Sign-In failed: ${e.localizedMessage}")
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.something_went_wrong),
                message = appContext.getString(R.string.error_google_sign_in)
            )
        }
    }

    private fun firebaseAuthWithGoogle(
        idToken: String
    ) {
        appViewModel.setLoading(true)
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    deleteFirebaseAuthUser()
                } else {
                    appViewModel.setLoading(false)
                    Log.e("ProfileViewModel", "Firebase Sign-In failed: ${task.exception}")
                    appViewModel.showMessage(
                        type = DialogType.ERROR,
                        title = appContext.getString(R.string.something_went_wrong),
                        message = appContext.getString(R.string.error_google_sign_in)
                    )
                }
            }
    }

    fun logOut() {
        LocalUserManager(appContext).deleteUserState()
        UserLocationManager(appContext).deleteUserLocationState()
        ContactsCatalogManager(appContext).deleteContactsState()
        viewModelScope.launch {
            _navigationEvents.emit(NavigationEvent.LogOutComplete)
        }
    }

}

class ProfileViewModelFactory(
    private val context: Context,
    private val appViewModel: AppViewModel
) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProfileViewModel(context, appViewModel) as T
    }
}