package com.mitarifamitaxi.taximetrousuario.viewmodels.onboarding

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.LocalUserManager
import com.mitarifamitaxi.taximetrousuario.helpers.getFirebaseAuthErrorMessage
import com.mitarifamitaxi.taximetrousuario.helpers.isValidEmail
import com.mitarifamitaxi.taximetrousuario.models.AuthProvider
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.models.LocalUser
import com.mitarifamitaxi.taximetrousuario.states.LoginState
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel(context: Context, private val appViewModel: AppViewModel) : ViewModel() {

    private val appContext = context.applicationContext

    private val _uiState = MutableStateFlow(LoginState())
    val uiState: StateFlow<LoginState> = _uiState

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "LoginViewModel"
    }

    fun onUserNameChange(value: String) = _uiState.update {
        it.copy(userName = value)
    }

    fun onPasswordChange(value: String) = _uiState.update {
        it.copy(password = value)
    }

    fun onRememberMeChange(value: Boolean) = _uiState.update {
        it.copy(rememberMe = value)
    }

    fun showSelectRoleDialog() {
        _uiState.update { it.copy(showDialogSelectRole = true) }
    }

    fun hideSelectRoleDialog() {
        _uiState.update { it.copy(showDialogSelectRole = false) }
    }

    fun setTempData(mustCompleteProfile: Boolean, tempUserData: LocalUser?) = _uiState.update {
        it.copy(mustCompleteProfile = mustCompleteProfile, tempUserData = tempUserData)
    }

    fun login(onSuccess: () -> Unit) {
        _uiState.update { state ->
            state.copy(
                userNameIsError = state.userName.isBlank(),
                passwordIsError = state.password.isBlank(),
                userNameErrorMessage = if (state.userName.isBlank()) appContext.getString(R.string.required_field) else "",
                passwordErrorMessage = if (state.password.isBlank()) appContext.getString(R.string.required_field) else ""
            )
        }

        val st = _uiState.value
        if (st.userNameIsError || st.passwordIsError) return

        if (!st.userName.isValidEmail()) {
            _uiState.update {
                it.copy(
                    userNameIsError = true,
                    userNameErrorMessage = appContext.getString(R.string.invalid_email)
                )
            }
            return
        }

        appViewModel.isLoading = true

        viewModelScope.launch {
            try {
                val cred = auth.signInWithEmailAndPassword(st.userName.trim(), st.password).await()
                val user = cred.user ?: throw Exception("No user")
                getUserInfo(user.uid, AuthProvider.email) { exists ->
                    appViewModel.isLoading = false
                    if (exists) onSuccess()
                    else appViewModel.showMessage(
                        DialogType.ERROR,
                        appContext.getString(R.string.something_went_wrong),
                        appContext.getString(R.string.error_user_not_found)
                    )
                }
            } catch (e: Exception) {
                appViewModel.isLoading = false
                handleAuthError(e)
            }
        }
    }

    val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(appContext.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(appContext, gso)
    }

    fun handleSignInResult(data: Intent?, onResult: (Pair<Boolean, LocalUser?>) -> Unit) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account: GoogleSignInAccount? = task.getResult(ApiException::class.java)
            if (account != null) {
                firebaseAuthWithGoogle(account.idToken!!, onResult)
            } else {
                Log.e(TAG, "Google Sign-In failed: No account found")
                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = appContext.getString(R.string.error_google_sign_in)
                )
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Google Sign-In failed: ${e.localizedMessage}")
            //onResult(Pair("Error signing in with Google", null))
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.something_went_wrong),
                message = appContext.getString(R.string.error_google_sign_in)
            )
        }
    }

    private fun firebaseAuthWithGoogle(
        idToken: String,
        onResult: (Pair<Boolean, LocalUser?>) -> Unit
    ) {
        appViewModel.isLoading = true
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.d(TAG, "Firebase Sign-In success. User: ${user?.displayName}")
                    viewModelScope.launch {

                        getUserInfo(user?.uid ?: "", AuthProvider.google) { exists ->
                            appViewModel.isLoading = false
                            if (exists) {
                                onResult(Pair(false, null))
                            } else {
                                user?.let {
                                    val userData = LocalUser(
                                        id = it.uid,
                                        email = it.email,
                                        firstName = it.displayName?.split(" ")?.get(0),
                                        lastName = it.displayName?.split(" ")?.get(1),
                                        mobilePhone = it.phoneNumber,
                                        authProvider = AuthProvider.google
                                    )
                                    onResult(Pair(true, userData))
                                }
                            }
                        }

                    }
                } else {
                    appViewModel.isLoading = false
                    Log.e(TAG, "Firebase Sign-In failed: ${task.exception}")
                    appViewModel.showMessage(
                        type = DialogType.ERROR,
                        title = appContext.getString(R.string.something_went_wrong),
                        message = appContext.getString(R.string.error_google_sign_in)
                    )
                }
            }
    }

    private suspend fun getUserInfo(
        uid: String,
        provider: AuthProvider,
        cb: (Boolean) -> Unit
    ) {
        try {
            val doc = db.collection("users").document(uid).get().await()
            if (doc.exists()) {
                val user = doc.toObject<LocalUser>()!!.apply { authProvider = provider }
                LocalUserManager(appContext).saveUserState(user)
                cb(true)
            } else cb(false)
        } catch (e: Exception) {
            appViewModel.showMessage(
                DialogType.ERROR,
                appContext.getString(R.string.something_went_wrong),
                appContext.getString(R.string.error_getting_user_info)
            )
        }
    }

    private fun handleAuthError(e: Exception) {
        when (e) {
            is FirebaseAuthInvalidCredentialsException -> {
                _uiState.update {
                    it.copy(
                        userNameIsError = true,
                        passwordIsError = true,
                        userNameErrorMessage = appContext.getString(R.string.wrong_credentials),
                        passwordErrorMessage = appContext.getString(R.string.wrong_credentials)
                    )
                }
            }

            is FirebaseAuthInvalidUserException,
            is FirebaseAuthException -> {
                val msg = getFirebaseAuthErrorMessage(appContext, e.errorCode)
                appViewModel.showMessage(
                    DialogType.ERROR,
                    appContext.getString(R.string.something_went_wrong),
                    msg
                )
            }

            else -> appViewModel.showMessage(
                DialogType.ERROR,
                appContext.getString(R.string.something_went_wrong),
                appContext.getString(R.string.general_error)
            )
        }
    }
}

class LoginViewModelFactory(
    private val context: Context,
    private val appViewModel: AppViewModel
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(cls: Class<T>): T {
        return LoginViewModel(context, appViewModel) as T
    }
}
