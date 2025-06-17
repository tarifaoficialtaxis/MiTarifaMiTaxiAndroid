package com.mitarifamitaxi.taximetrousuario.viewmodels.profile.driver

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DriverProfileViewModel(context: Context, private val appViewModel: AppViewModel) :
    ViewModel() {

    private val appContext = context.applicationContext
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    var showPasswordPopUp by mutableStateOf(false)

    private val _hideKeyboardEvent = MutableLiveData<Boolean>()
    val hideKeyboardEvent: LiveData<Boolean> get() = _hideKeyboardEvent

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    sealed class NavigationEvent {
        object LogOutComplete : NavigationEvent()
        object LaunchGoogleSignIn : NavigationEvent()
    }

    val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(appContext.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(appContext, gso)
    }

    fun resetHideKeyboardEvent() {
        _hideKeyboardEvent.value = false
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
                val userCredential =
                    auth.signInWithEmailAndPassword(appViewModel.userData?.email!!.trim(), password)
                        .await()
                val user = userCredential.user
                if (user != null) {
                    deleteFirebaseAuthUser()
                }
            } catch (e: Exception) {
                appViewModel.isLoading = false

                Log.e("ProfileViewModel", "Error logging in: ${e.message}")

                val errorMessage = when (e) {
                    is FirebaseAuthInvalidCredentialsException -> getFirebaseAuthErrorMessage(e.errorCode)
                    is FirebaseAuthInvalidUserException -> getFirebaseAuthErrorMessage(e.errorCode)
                    is FirebaseAuthException -> getFirebaseAuthErrorMessage(e.errorCode)
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

    private fun getFirebaseAuthErrorMessage(errorCode: String): String {
        return when (errorCode) {
            "ERROR_INVALID_EMAIL" -> appContext.getString(R.string.error_invalid_email)
            "ERROR_INVALID_CREDENTIAL" -> appContext.getString(R.string.error_wrong_credentials)
            "ERROR_USER_NOT_FOUND" -> appContext.getString(R.string.error_user_not_found)
            "ERROR_USER_DISABLED" -> appContext.getString(R.string.error_user_disabled)
            "ERROR_TOO_MANY_REQUESTS" -> appContext.getString(R.string.error_too_many_requests)
            "ERROR_OPERATION_NOT_ALLOWED" -> appContext.getString(R.string.error_operation_not_allowed)
            else -> appContext.getString(R.string.error_authentication_failed)
        }
    }

    fun logOut() {
        LocalUserManager(appContext).deleteUserState()
        viewModelScope.launch {
            _navigationEvents.emit(NavigationEvent.LogOutComplete)
        }
    }

}

class DriverProfileViewModelFactory(
    private val context: Context,
    private val appViewModel: AppViewModel
) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DriverProfileViewModel::class.java)) {
            return DriverProfileViewModel(context, appViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}