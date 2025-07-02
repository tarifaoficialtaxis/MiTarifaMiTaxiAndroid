package com.mitarifamitaxi.taximetrousuario.viewmodels.pqrs

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.ContactsCatalogManager
import com.mitarifamitaxi.taximetrousuario.models.Contact
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.models.EmailTemplate
import com.mitarifamitaxi.taximetrousuario.states.PqrsState
import com.mitarifamitaxi.taximetrousuario.states.SosState
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PqrsViewModel(context: Context, private val appViewModel: AppViewModel) : ViewModel() {

    private val appContext = context.applicationContext

    private val _uiState = MutableStateFlow(PqrsState())
    val uiState: StateFlow<PqrsState> = _uiState

    init {
        getEmailTemplate()
        onContactChange(
            ContactsCatalogManager(appContext).getContactsState() ?: Contact()
        )
    }

    fun onPlateChange(value: String) = _uiState.update {
        it.copy(plate = value)
    }

    fun onHighFareChange(value: Boolean) = _uiState.update {
        it.copy(isHighFare = value)
    }

    fun onUserMistreatedChange(value: Boolean) = _uiState.update {
        it.copy(isUserMistreated = value)
    }

    fun onServiceAbandonmentChange(value: Boolean) = _uiState.update {
        it.copy(isServiceAbandonment = value)
    }

    fun onUnauthorizedChargesChange(value: Boolean) = _uiState.update {
        it.copy(isUnauthorizedCharges = value)
    }

    fun onNoFareNoticeChange(value: Boolean) = _uiState.update {
        it.copy(isNoFareNotice = value)
    }

    fun onDangerousDrivingChange(value: Boolean) = _uiState.update {
        it.copy(isDangerousDriving = value)
    }

    fun onOtherChange(value: Boolean) = _uiState.update {
        it.copy(isOther = value)
    }

    fun onOtherValueChange(value: String) = _uiState.update {
        it.copy(otherValue = value)
    }

    fun onContactChange(value: Contact) = _uiState.update {
        it.copy(contact = value)
    }

    fun onEmailTemplateChange(value: EmailTemplate) = _uiState.update {
        it.copy(emailTemplate = value)
    }

    private fun getEmailTemplate() {

        viewModelScope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val ratesQuerySnapshot = withContext(Dispatchers.IO) {
                    firestore.collection("emailTemplates")
                        .whereEqualTo("key", "taxiComplaint")
                        .get()
                        .await()
                }

                if (!ratesQuerySnapshot.isEmpty) {
                    val templateDoc = ratesQuerySnapshot.documents[0]
                    try {
                        val emailTemplateObj =
                            templateDoc.toObject(EmailTemplate::class.java) ?: EmailTemplate()

                        onEmailTemplateChange(emailTemplateObj)

                    } catch (e: Exception) {
                        Log.e("PqrsViewModel", "Error fetching email template: ${e.message}")
                        appViewModel.showMessage(
                            type = DialogType.ERROR,
                            title = appContext.getString(R.string.something_went_wrong),
                            message = appContext.getString(R.string.general_error)
                        )
                    }
                } else {
                    appViewModel.showMessage(
                        type = DialogType.ERROR,
                        title = appContext.getString(R.string.something_went_wrong),
                        message = appContext.getString(R.string.general_error)
                    )
                }
            } catch (e: Exception) {
                Log.e("PqrsViewModel", "Error fetching email template: ${e.message}")
                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = appContext.getString(R.string.general_error)
                )
            }

        }


    }


    fun validateSendPqr(onIntentReady: (Intent) -> Unit) {

        _uiState.update { state ->
            state.copy(
                plateIsError = state.plate.isBlank(),
                plateErrorMessage = if (state.plate.isBlank()) appContext.getString(R.string.required_field) else "",
            )
        }

        val st = _uiState.value
        if (st.plateIsError) return

        if (!st.isHighFare && !st.isUserMistreated && !st.isServiceAbandonment && !st.isUnauthorizedCharges && !st.isNoFareNotice && !st.isDangerousDriving && !st.isOther) {
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.error),
                message = appContext.getString(R.string.select_complaint_reason)
            )
            return
        }

        _uiState.update { state ->
            state.copy(
                isOtherValueError = state.isOther && state.otherValue.isBlank(),
                plateErrorMessage = if (state.isOther && state.otherValue.isBlank()) appContext.getString(
                    R.string.other_reason_required
                ) else "",
            )
        }


        if (_uiState.value.isOtherValueError) return

        sendPqrEmail(onIntentReady)

    }

    private fun sendPqrEmail(onIntentReady: (Intent) -> Unit) {

        val st = _uiState.value


        val irregularities = buildString {
            if (st.isHighFare) append("- ${appContext.getString(R.string.high_fare)}\n")
            if (st.isUserMistreated) append("- ${appContext.getString(R.string.user_mistreated)}\n")
            if (st.isServiceAbandonment) append("- ${appContext.getString(R.string.service_abandonment)}\n")
            if (st.isUnauthorizedCharges) append("- ${appContext.getString(R.string.unauthorized_charges)}\n")
            if (st.isNoFareNotice) append("- ${appContext.getString(R.string.no_fare_notice)}\n")
            if (st.isDangerousDriving) append("- ${appContext.getString(R.string.dangerous_driving)}\n")
            if (st.isOther) append("- ${st.otherValue}\n")
        }

        val fullName =
            "${appViewModel.uiState.value.userData?.firstName} ${appViewModel.uiState.value.userData?.lastName}"
        val bodyEmail = (st.emailTemplate.body ?: "")
            .replace("{city}", appViewModel.uiState.value.userData?.city ?: "")
            .replace("{user_name}", fullName)
            .replace("{plate}", st.plate)
            .replace("{newline}", "\n")
            .replace("{irregularities}", irregularities)

        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data =
                Uri.parse("mailto:${st.contact.pqrEmail}?subject=${appContext.getString(R.string.email_subject)}&body=$bodyEmail")
        }

        onIntentReady(emailIntent)

    }
}

class PqrsViewModelFactory(
    private val context: Context,
    private val appViewModel: AppViewModel
) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PqrsViewModel::class.java)) {
            return PqrsViewModel(context, appViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
