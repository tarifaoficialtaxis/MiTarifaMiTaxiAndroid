package com.mitarifamitaxi.taximetrousuario.viewmodels.profile.driver

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.LocalUserManager
import com.mitarifamitaxi.taximetrousuario.helpers.isValidEmail
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DriverProfilePersonalInfoViewModel(context: Context, private val appViewModel: AppViewModel) :
    ViewModel() {

    private val appContext = context.applicationContext

    var firstName by mutableStateOf(appViewModel.userData?.firstName)
    var lastName by mutableStateOf(appViewModel.userData?.lastName)
    var documentNumber by mutableStateOf(appViewModel.userData?.documentNumber)
    var mobilePhone by mutableStateOf(appViewModel.userData?.mobilePhone)
    var email by mutableStateOf(appViewModel.userData?.email)
    var familyNumber by mutableStateOf(appViewModel.userData?.familyNumber)
    var supportNumber by mutableStateOf(appViewModel.userData?.supportNumber)

    private val _hideKeyboardEvent = MutableLiveData<Boolean>()
    val hideKeyboardEvent: LiveData<Boolean> get() = _hideKeyboardEvent

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    sealed class NavigationEvent {
        object Finish : NavigationEvent()
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

        viewModelScope.launch {
            appViewModel.isLoading = true
            val updatedUser = appViewModel.userData?.copy(
                firstName = firstName,
                lastName = lastName,
                documentNumber = documentNumber,
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