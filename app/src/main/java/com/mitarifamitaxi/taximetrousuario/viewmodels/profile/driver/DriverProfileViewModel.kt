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
import com.mitarifamitaxi.taximetrousuario.helpers.getFirebaseAuthErrorMessage
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

        appViewModel.setLoading(true)

        viewModelScope.launch {
            try {
                val userCredential =
                    auth.signInWithEmailAndPassword(
                        appViewModel.uiState.value.userData?.email!!.trim(),
                        password
                    )
                        .await()
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

                appViewModel.setLoading(false)
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
                appViewModel.setLoading(false)
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
                appViewModel.setLoading(false)
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