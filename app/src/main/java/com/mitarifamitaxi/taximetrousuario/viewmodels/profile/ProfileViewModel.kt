package com.mitarifamitaxi.taximetrousuario.viewmodels.profile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.mitarifamitaxi.taximetrousuario.helpers.LocalUserManager
import com.mitarifamitaxi.taximetrousuario.helpers.getFirebaseAuthErrorMessage
import com.mitarifamitaxi.taximetrousuario.helpers.isValidEmail
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel(context: Context, private val appViewModel: AppViewModel) : ViewModel() {

    private val appContext = context.applicationContext
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val originalProfilePictureUrl: String? = appViewModel.userData?.profilePicture
    var imageUri by mutableStateOf<Uri?>(appViewModel.userData?.profilePicture?.toUri())
    var tempImageUri by mutableStateOf<Uri?>(null)

    var showDialog by mutableStateOf(false)

    var firstName by mutableStateOf(appViewModel.userData?.firstName)
    var lastName by mutableStateOf(appViewModel.userData?.lastName)
    var mobilePhone by mutableStateOf(appViewModel.userData?.mobilePhone)
    var email by mutableStateOf(appViewModel.userData?.email)
    var familyNumber by mutableStateOf(appViewModel.userData?.familyNumber)
    var supportNumber by mutableStateOf(appViewModel.userData?.supportNumber)

    var tripsCount by mutableIntStateOf(0)
    var distanceCount by mutableIntStateOf(0)

    var showPasswordPopUp by mutableStateOf(false)

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

    var hasCameraPermission by mutableStateOf(false)
        private set

    init {
        checkCameraPermission()
        viewModelScope.launch {
            getTripsByUserId(appViewModel.userData?.id ?: "")
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


    fun resetHideKeyboardEvent() {
        _hideKeyboardEvent.value = false
    }

    private suspend fun getTripsByUserId(userId: String) {
        try {
            appViewModel.isLoading = true
            val tripsSnapshot = FirebaseFirestore.getInstance()
                .collection("trips")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            tripsCount = tripsSnapshot.size()

            if (!tripsSnapshot.isEmpty) {
                val trips = tripsSnapshot.documents
                val distance = trips.sumOf { it.getDouble("distance") ?: 0.0 }
                distanceCount = (distance / 1000).toInt()
            }
            appViewModel.isLoading = false
        } catch (error: Exception) {
            Log.e("ProfileViewModel", "Error fetching trips: ${error.message}")
            appViewModel.isLoading = false
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.something_went_wrong),
                message = appContext.getString(R.string.error_fetching_trips),
            )
        }
    }

    fun handleUpdate() {

        if ((firstName ?: "").isEmpty() ||
            (lastName ?: "").isEmpty() ||
            (mobilePhone ?: "").isEmpty() ||
            (email ?: "").isEmpty()
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

        viewModelScope.launch {
            appViewModel.isLoading = true
            val updatedUser = appViewModel.userData?.copy(
                firstName = firstName,
                lastName = lastName,
                mobilePhone = mobilePhone,
                email = email,
                familyNumber = familyNumber,
                supportNumber = supportNumber
            )

            try {
                updatedUser?.let { user ->
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(appViewModel.userData?.id ?: "")
                        .update(
                            mapOf(
                                "firstName" to user.firstName,
                                "lastName" to user.lastName,
                                "mobilePhone" to user.mobilePhone,
                                "email" to user.email,
                                "familyNumber" to user.familyNumber,
                                "supportNumber" to user.supportNumber
                            )
                        ).await()

                    appViewModel.userData = user
                    LocalUserManager(appContext).saveUserState(user)
                    appViewModel.isLoading = false
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
                appViewModel.isLoading = false
                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = appContext.getString(R.string.error_updating_user)
                )
            } finally {
                appViewModel.isLoading = false
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
                appViewModel.isLoading = true

                // Delete Firebase Auth User
                deleteFirebaseAuthUser()

            } catch (error: Exception) {
                // Catch errors from Firestore deletion
                Log.e("ProfileViewModel", "Error deleting account data: ${error.message}", error)
                appViewModel.isLoading = false
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
                    showPasswordPopUp = true
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

        appViewModel.isLoading = true

        viewModelScope.launch {
            try {
                if (email == null || email!!.isEmpty()) {
                    appViewModel.isLoading = false
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
                    deleteFirebaseAuthUser()
                }
            } catch (e: Exception) {
                appViewModel.isLoading = false

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
        val userId = appViewModel.userData?.id ?: ""

        if (currentUser == null || userId.isEmpty()) {
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.something_went_wrong),
                message = appContext.getString(R.string.error_deleting_account)
            )
            return
        }

        viewModelScope.launch {

            try {
                currentUser.delete().await()
                Log.d("ProfileViewModel", "Deleted Firebase Auth user ${currentUser.uid}")

                // Delete Firestore Trips
                val tripsSnapshot = FirebaseFirestore.getInstance()
                    .collection("trips")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                if (tripsSnapshot.documents.isNotEmpty()) {
                    for (trip in tripsSnapshot.documents) {
                        trip.reference.delete().await()
                        Log.d("ProfileViewModel", "Deleted trip ${trip.id}")
                    }
                }

                // Delete Firestore User Document
                val userDocRef = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)

                userDocRef.delete().await()
                Log.d("ProfileViewModel", "Deleted Firestore user document $userId")

                appViewModel.isLoading = false
                appViewModel.showMessage(
                    type = DialogType.SUCCESS,
                    title = appContext.getString(R.string.warning),
                    message = appContext.getString(R.string.account_deleted_message),
                    buttonText = appContext.getString(R.string.accept),
                    showCloseButton = false,
                    onButtonClicked = {
                        logOut()
                    }
                )

            } catch (authError: FirebaseAuthRecentLoginRequiredException) {
                appViewModel.isLoading = false
                Log.w(
                    "ProfileViewModel",
                    "Auth deletion failed: Re-authentication required.",
                    authError
                )

                getUserAuthType()

            } catch (authError: Exception) {
                Log.e(
                    "ProfileViewModel",
                    "Error deleting Firebase Auth user: ${authError.message}",
                    authError
                )
                appViewModel.isLoading = false
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
        appViewModel.isLoading = true
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    deleteFirebaseAuthUser()
                } else {
                    appViewModel.isLoading = false
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
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel(context, appViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}