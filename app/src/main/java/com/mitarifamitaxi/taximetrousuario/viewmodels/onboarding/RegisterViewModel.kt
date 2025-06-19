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
    var firstNameIsValid by mutableStateOf(true)
    var firstNameErrorMessage by mutableStateOf("")

    var lastName by mutableStateOf("")
    var lastNameIsValid by mutableStateOf(true)
    var lastNameErrorMessage by mutableStateOf("")

    var mobilePhone by mutableStateOf("")
    var mobilePhoneIsValid by mutableStateOf(true)
    var mobilePhoneErrorMessage by mutableStateOf("")

    var email by mutableStateOf("")
    var emailIsValid by mutableStateOf(true)
    var emailErrorMessage by mutableStateOf("")

    var password by mutableStateOf("")
    var passwordIsValid by mutableStateOf(true)
    var passwordErrorMessage by mutableStateOf("")

    var confirmPassword by mutableStateOf("")
    var confirmPasswordIsValid by mutableStateOf(true)
    var confirmPasswordErrorMessage by mutableStateOf("")

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

    private fun validateFields(): Boolean {
        firstNameIsValid = firstName.isNotEmpty()
        if (!firstNameIsValid) firstNameErrorMessage = appContext.getString(R.string.required_field)

        lastNameIsValid = lastName.isNotEmpty()
        if (!lastNameIsValid) lastNameErrorMessage = appContext.getString(R.string.required_field)

        mobilePhoneIsValid = mobilePhone.length == 10
        if (!mobilePhoneIsValid) {
            mobilePhoneErrorMessage = if (mobilePhone.isEmpty()) appContext.getString(R.string.required_field)
            else appContext.getString(R.string.invalid_phone_length)
        }

        emailIsValid = email.isValidEmail()
        if (!emailIsValid) {
            emailErrorMessage = if (email.isEmpty()) appContext.getString(R.string.required_field)
            else appContext.getString(R.string.error_invalid_email)
        }

        passwordIsValid = password.isValidPassword()
        if (!passwordIsValid) {
            passwordErrorMessage = if (password.isEmpty()) appContext.getString(R.string.required_field)
            else appContext.getString(R.string.error_invalid_password)
        }

        confirmPasswordIsValid = password == confirmPassword
        if (!confirmPasswordIsValid) {
            confirmPasswordErrorMessage = if (confirmPassword.isEmpty()) appContext.getString(R.string.required_field)
            else appContext.getString(R.string.passwords_do_not_match)
        }

        return firstNameIsValid && lastNameIsValid && mobilePhoneIsValid && emailIsValid && passwordIsValid && confirmPasswordIsValid
    }

    fun register(onResult: (Pair<Boolean, String?>) -> Unit) {
        if (!validateFields()) {
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