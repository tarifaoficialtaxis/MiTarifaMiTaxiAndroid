package com.mitarifamitaxi.taximetrousuario.viewmodels.onboarding

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.*
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
import com.mitarifamitaxi.taximetrousuario.helpers.Constants
import com.mitarifamitaxi.taximetrousuario.helpers.LocalUserManager
import com.mitarifamitaxi.taximetrousuario.helpers.isValidEmail
import com.mitarifamitaxi.taximetrousuario.models.AuthProvider
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.models.LocalUser
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel(context: Context, private val appViewModel: AppViewModel) : ViewModel() {

    private val appContext = context.applicationContext

    // Firebase Auth
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    var userName by mutableStateOf("")
    var userNameIsValid by mutableStateOf(true)
    var userNameErrorMessage by mutableStateOf(appContext.getString(R.string.required_field))

    var password by mutableStateOf("")
    var passwordIsValid by mutableStateOf(true)
    var passwordErrorMessage by mutableStateOf(appContext.getString(R.string.required_field))

    var rememberMe by mutableStateOf(false)

    var mustCompleteProfile by mutableStateOf(false)
    var tempUserData by mutableStateOf<LocalUser?>(null)
    var showDialog by mutableStateOf(false)

    companion object {
        private const val TAG = "LoginViewModel"
    }

    init {
        if (Constants.IS_DEV) {

            // USER
            userName = "mateotest1@yopmail.com"
            password = "12345678#"

            // DRIVER
            userName = "drivertest1@yopmail.com"
            password = "12345678#"

        }
    }

    fun login(loginSuccess: () -> Unit) {

        userNameIsValid = true
        passwordIsValid = true

        if (userName.isEmpty()) {
            userNameIsValid = false
            userNameErrorMessage = appContext.getString(R.string.required_field)

        } else if (!userName.isValidEmail()) {
            userNameIsValid = false
            userNameErrorMessage = appContext.getString(R.string.invalid_email)

        }

        if (password.isEmpty()) {
            passwordIsValid = false
            passwordErrorMessage = appContext.getString(R.string.required_field)
        }

        if (!userNameIsValid || !passwordIsValid) {
            return
        }

        appViewModel.isLoading = true

        viewModelScope.launch {
            try {
                val userCredential =
                    auth.signInWithEmailAndPassword(userName.trim(), password).await()
                val user = userCredential.user
                if (user != null) {
                    getUserInformation(user.uid, authProvider = AuthProvider.email) { userExists ->
                        appViewModel.isLoading = false
                        if (userExists) {
                            loginSuccess()
                        } else {
                            appViewModel.showMessage(
                                type = DialogType.ERROR,
                                title = appContext.getString(R.string.something_went_wrong),
                                message = appContext.getString(R.string.error_user_not_found)
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                appViewModel.isLoading = false

                Log.e(TAG, "Error logging in: ${e.message}")

                when (e) {
                    is FirebaseAuthInvalidUserException -> {
                        userNameIsValid = false
                        userNameErrorMessage = getFirebaseAuthErrorMessage(e.errorCode)
                    }
                    is FirebaseAuthInvalidCredentialsException -> {
                        userNameIsValid = false
                        passwordIsValid = false
                        userNameErrorMessage = getFirebaseAuthErrorMessage(e.errorCode)
                        passwordErrorMessage = getFirebaseAuthErrorMessage(e.errorCode)
                    }
                    is FirebaseAuthException -> {
                        appViewModel.showMessage(
                            type = DialogType.ERROR,
                            title = appContext.getString(R.string.something_went_wrong),
                            message = getFirebaseAuthErrorMessage(e.errorCode),
                        )
                    }
                    else -> {
                        appViewModel.showMessage(
                            type = DialogType.ERROR,
                            title = appContext.getString(R.string.something_went_wrong),
                            message = appContext.getString(R.string.something_went_wrong),
                        )
                    }
                }
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
                        getUserInformation(
                            user?.uid ?: "",
                            authProvider = AuthProvider.google,
                            userExistsCallback = {
                                appViewModel.isLoading = false
                                if (it) {
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
                            })
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

    private suspend fun getUserInformation(
        userId: String,
        authProvider: AuthProvider,
        userExistsCallback: (Boolean) -> Unit
    ) {
        try {
            val userDoc = db.collection("users").document(userId).get().await()
            if (userDoc.exists()) {
                val userData = userDoc.toObject<LocalUser>()
                if (userData != null) {
                    userData.authProvider = authProvider
                    LocalUserManager(appContext).saveUserState(userData)
                    userExistsCallback(true)
                } else {
                    throw Exception("User data not found in Firestore")
                }
            } else {
                userExistsCallback(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user information", e)
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.something_went_wrong),
                message = appContext.getString(R.string.error_getting_user_info)
            )
        }
    }
}

class LoginViewModelFactory(private val context: Context, private val appViewModel: AppViewModel) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(context, appViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
