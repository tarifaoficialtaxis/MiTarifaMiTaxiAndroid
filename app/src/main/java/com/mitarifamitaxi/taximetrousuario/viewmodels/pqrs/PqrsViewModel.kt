package com.mitarifamitaxi.taximetrousuario.viewmodels.pqrs

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.models.EmailTemplate
import com.mitarifamitaxi.taximetrousuario.models.PqrsData
import com.mitarifamitaxi.taximetrousuario.models.PqrsReasons
import com.mitarifamitaxi.taximetrousuario.states.PqrsState
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
        getPqrsData(appViewModel.uiState.value.userData?.city ?: "")
        getEmailTemplate()
    }

    fun onPlateChange(value: String) = _uiState.update {
        it.copy(plate = value)
    }

    fun onReasonToggled(reason: PqrsReasons, isChecked: Boolean) {
        val current = _uiState.value.reasonsSelected.toMutableList()
        if (isChecked) {
            if (!current.any { it.key == reason.key }) current += reason
        } else {
            current.removeAll { it.key == reason.key }
        }
        _uiState.value = _uiState.value.copy(reasonsSelected = current)
    }

    fun onOtherValueChange(value: String) = _uiState.update {
        it.copy(otherValue = value)
    }

    fun onPqrsDataChange(value: PqrsData) = _uiState.update {
        it.copy(pqrsData = value)
    }

    fun filterReasons() {
        val filteredReasons = _uiState.value.pqrsData.reasons
            .filter { it.show == true }
            .sortedBy { it.order }
        _uiState.update { state ->
            state.copy(
                pqrsData = state.pqrsData.copy(reasons = filteredReasons),
            )
        }
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
                plateErrorMessage = if (state.plate.isBlank())
                    appContext.getString(R.string.required_field)
                else
                    ""
            )
        }
        val st = _uiState.value
        if (st.plateIsError) return

        if (st.reasonsSelected.isEmpty()) {
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.attention),
                message = appContext.getString(R.string.select_complaint_reason)
            )
            return
        }

        val otherSelected = st.reasonsSelected.any { it.key == "OTHER" }
        _uiState.update { state ->
            state.copy(
                isOtherValueError = otherSelected && state.otherValue.isBlank(),
                otherValueErrorMessage = if (otherSelected && state.otherValue.isBlank())
                    appContext.getString(R.string.other_reason_required)
                else
                    ""
            )
        }
        if (_uiState.value.isOtherValueError) return

        sendPqrEmail(onIntentReady)
    }

    private fun sendPqrEmail(onIntentReady: (Intent) -> Unit) {
        val st = _uiState.value

        val irregularities = buildString {
            st.reasonsSelected.forEach { reason ->
                val line = if (reason.key == "OTHER") {
                    st.otherValue
                } else {
                    reason.name.orEmpty()
                }
                append("- $line\n")
            }
        }

        val user = appViewModel.uiState.value.userData
        val fullName = listOfNotNull(user?.firstName, user?.lastName)
            .joinToString(" ")

        val template = st.emailTemplate.body.orEmpty()
        val bodyEmail = template
            .replace("{city}", user?.city.orEmpty())
            .replace("{user_name}", fullName)
            .replace("{plate}", st.plate)
            .replace("{newline}", "\n")
            .replace("{irregularities}", irregularities)

        val destination = st.pqrsData.email.orEmpty()
        val subject = appContext.getString(R.string.email_subject)
        val uri = "mailto:$destination?subject=$subject&body=$bodyEmail"

        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse(uri)
        }

        onIntentReady(emailIntent)
    }


    private fun getPqrsData(city: String) {
        viewModelScope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val ratesQuerySnapshot = withContext(Dispatchers.IO) {
                    firestore.collection("dynamicPqrs")
                        .whereEqualTo("city", city)
                        .get()
                        .await()
                }

                if (!ratesQuerySnapshot.isEmpty) {
                    val contactsDoc = ratesQuerySnapshot.documents[0]
                    try {
                        val pqrsVal =
                            contactsDoc.toObject(PqrsData::class.java) ?: PqrsData()
                        onPqrsDataChange(pqrsVal)
                        filterReasons()
                    } catch (e: Exception) {
                        Log.e("PqrsViewModel", "Error parsing contact data: ${e.message}")
                    }
                } else {
                    Log.e(
                        "PqrsViewModel",
                        "Error fetching contacts: ${appContext.getString(R.string.error_no_contacts_found)}"
                    )

                }
            } catch (e: Exception) {
                Log.e("PqrsViewModel", "Error fetching contacts: ${e.message}")
            }

        }

    }

}

class PqrsViewModelFactory(
    private val context: Context,
    private val appViewModel: AppViewModel
) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PqrsViewModel(context, appViewModel) as T
    }
}
