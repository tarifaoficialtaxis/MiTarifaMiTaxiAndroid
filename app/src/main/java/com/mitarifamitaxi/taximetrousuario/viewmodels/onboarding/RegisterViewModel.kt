package com.mitarifamitaxi.taximetrousuario.viewmodels.onboarding

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.Constants
import com.mitarifamitaxi.taximetrousuario.helpers.LocalUserManager
import com.mitarifamitaxi.taximetrousuario.helpers.isValidEmail
import com.mitarifamitaxi.taximetrousuario.helpers.isValidPassword
import com.mitarifamitaxi.taximetrousuario.models.AuthProvider
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.models.LocalUser
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegisterViewModel(context: Context, private val appViewModel: AppViewModel) : ViewModel() {

    private val appContext = context.applicationContext
    var firstName by mutableStateOf("")
    var lastName by mutableStateOf("")
    var mobilePhone by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")

    init {
        if (Constants.IS_DEV) {
            firstName = "Mateo"
            lastName = "Ortiz"
            mobilePhone = "3167502612"
            email = "mateotest1@yopmail.com"
            password = "12345678"
            confirmPassword = "12345678"
        }
    }

    fun register(onResult: (Pair<Boolean, String?>) -> Unit) {
        // Login logic
        if (firstName.isEmpty() || lastName.isEmpty() || mobilePhone.isEmpty() || email.isEmpty() || password.isEmpty()) {

            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.something_went_wrong),
                message = appContext.getString(R.string.all_fields_required),
            )
            return
        }

        if (!email.isValidEmail()) {
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.something_went_wrong),
                message = appContext.getString(R.string.error_invalid_email),
            )
            return
        }

        if (!password.isValidPassword()) {
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.something_went_wrong),
                message = appContext.getString(R.string.error_invalid_password)
            )
            return
        }

        if (password != confirmPassword) {
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.something_went_wrong),
                message = appContext.getString(R.string.passwords_do_not_match),
            )
            return
        }

        viewModelScope.launch {
            try {
                // Show loading indicator
                appViewModel.isLoading = true

                // Create user with email and password in Firebase Auth
                val authResult =
                    FirebaseAuth.getInstance()
                        .createUserWithEmailAndPassword(email.trim(), password.trim())
                        .await()
                val user = authResult.user ?: throw Exception("User creation failed")

                // Save user information in Firestore
                val userMap = hashMapOf(
                    "id" to user.uid,
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "mobilePhone" to mobilePhone.trim(),
                    "email" to email.trim()
                )
                FirebaseFirestore.getInstance().collection("users").document(user.uid).set(userMap)
                    .await()

                // Hide loading indicator
                appViewModel.isLoading = false

                // Save user in SharedPreferences
                val localUser = LocalUser(
                    id = user.uid,
                    firstName = firstName,
                    lastName = lastName,
                    mobilePhone = mobilePhone.trim(),
                    email = email.trim(),
                    authProvider = AuthProvider.email
                )
                LocalUserManager(appContext).saveUserState(localUser)

                onResult(Pair(true, null))

            } catch (e: Exception) {
                Log.e("RegisterViewModel", "Error registering user: ${e.message}")
                // Hide loading indicator
                appViewModel.isLoading = false

                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = appContext.getString(R.string.error_registering_user)
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