package com.mitarifamitaxi.taximetrousuario.viewmodels

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
import com.google.gson.Gson
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.Constants
import com.mitarifamitaxi.taximetrousuario.helpers.isValidEmail
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.models.LocalUser
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

    var dialogType by mutableStateOf(DialogType.SUCCESS)
    var showDialog by mutableStateOf(false)
    var dialogTitle by mutableStateOf("")
    var dialogMessage by mutableStateOf("")

    companion object {
        private const val TAG = "LoginViewModel"
    }

    init {
        if (Constants.IS_DEV) {
            userName = "mateotest1@yopmail.com"
            password = "12345678"
        }
    }

    fun login(onResult: (Pair<Boolean, String?>) -> Unit) {

        userNameIsValid = !userName.isEmpty()
        passwordIsValid = !password.isEmpty()

        if (!userName.isEmpty()) {

            if (!userName.isValidEmail()) {
                userNameIsValid = false
                userNameErrorMessage = appContext.getString(R.string.invalid_email)

            }
        }

        if (!userNameIsValid && !passwordIsValid) {
            return
        }

        appViewModel.isLoading = true

        viewModelScope.launch {
            try {
                val userCredential =
                    auth.signInWithEmailAndPassword(userName.trim(), password).await()
                val user = userCredential.user
                if (user != null) {
                    getUserInformation(user.uid) { userExists ->
                        appViewModel.isLoading = false
                        if (userExists) {
                            onResult(Pair(true, null))
                        } else {
                            onResult(Pair(false, null))
                        }
                    }
                }
            } catch (e: Exception) {
                appViewModel.isLoading = false

                Log.e(TAG, "Error logging in: ${e.message}")

                val errorMessage = when (e) {
                    is FirebaseAuthInvalidCredentialsException -> getFirebaseAuthErrorMessage(e.errorCode)
                    is FirebaseAuthInvalidUserException -> getFirebaseAuthErrorMessage(e.errorCode)
                    is FirebaseAuthException -> getFirebaseAuthErrorMessage(e.errorCode)
                    else -> appContext.getString(R.string.something_went_wrong)
                }

                showErrorMessage(
                    appContext.getString(R.string.something_went_wrong),
                    errorMessage
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

    val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(appContext.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(appContext, gso)
    }

    fun handleSignInResult(data: Intent?, onResult: (Pair<String, LocalUser?>) -> Unit) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account: GoogleSignInAccount? = task.getResult(ApiException::class.java)
            if (account != null) {
                firebaseAuthWithGoogle(account.idToken!!, onResult)
            } else {
                Log.e(TAG, "Google Sign-In failed: No account found")
                showErrorMessage(
                    appContext.getString(R.string.something_went_wrong),
                    appContext.getString(R.string.error_google_sign_in)
                )
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Google Sign-In failed: ${e.localizedMessage}")
            //onResult(Pair("Error signing in with Google", null))
            showErrorMessage(
                appContext.getString(R.string.something_went_wrong),
                appContext.getString(R.string.error_google_sign_in)
            )
        }
    }

    private fun firebaseAuthWithGoogle(
        idToken: String,
        onResult: (Pair<String, LocalUser?>) -> Unit
    ) {
        appViewModel.isLoading = true
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.d(TAG, "Firebase Sign-In success. User: ${user?.displayName}")
                    viewModelScope.launch {
                        getUserInformation(user?.uid ?: "", userExistsCallback = {
                            appViewModel.isLoading = false
                            if (it) {
                                onResult(Pair("home", null))
                            } else {
                                user?.let {
                                    val userData = LocalUser(
                                        id = it.uid,
                                        email = it.email,
                                        firstName = it.displayName?.split(" ")?.get(0),
                                        lastName = it.displayName?.split(" ")?.get(1),
                                        mobilePhone = it.phoneNumber
                                    )
                                    onResult(Pair("complete_profile", userData))
                                }
                            }
                        })
                    }
                } else {
                    appViewModel.isLoading = false
                    Log.e(TAG, "Firebase Sign-In failed: ${task.exception}")
                    showErrorMessage(
                        appContext.getString(R.string.something_went_wrong),
                        appContext.getString(R.string.error_google_sign_in)
                    )
                }
            }
    }

    private suspend fun getUserInformation(userId: String, userExistsCallback: (Boolean) -> Unit) {
        try {
            val userDoc = db.collection("users").document(userId).get().await()
            if (userDoc.exists()) {
                val userData = userDoc.toObject<LocalUser>()
                if (userData != null) {
                    saveUserState(userData)
                    userExistsCallback(true)
                } else {
                    throw Exception("User data not found in Firestore")
                }
            } else {
                userExistsCallback(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user information", e)
            showErrorMessage(
                appContext.getString(R.string.something_went_wrong),
                appContext.getString(R.string.error_getting_user_info)
            )
        }
    }

    private fun saveUserState(user: LocalUser) {
        val sharedPref = appContext.getSharedPreferences("UserData", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("USER_OBJECT", Gson().toJson(user))
            apply()
        }
    }

    private fun showErrorMessage(title: String, message: String) {
        showDialog = true
        dialogType = DialogType.ERROR
        dialogTitle = title
        dialogMessage = message
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
