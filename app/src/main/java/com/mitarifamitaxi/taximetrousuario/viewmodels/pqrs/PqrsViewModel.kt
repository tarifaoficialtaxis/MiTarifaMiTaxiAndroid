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
import com.mitarifamitaxi.taximetrousuario.models.Contact
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.models.EmailTemplate
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PqrsViewModel(context: Context, private val appViewModel: AppViewModel) : ViewModel() {

    private val appContext = context.applicationContext

    var plate by mutableStateOf("")
    var isHighFare by mutableStateOf(false)
    var isUserMistreated by mutableStateOf(false)
    var isServiceAbandonment by mutableStateOf(false)
    var isUnauthorizedCharges by mutableStateOf(false)
    var isNoFareNotice by mutableStateOf(false)
    var isDangerousDriving by mutableStateOf(false)
    var isOther by mutableStateOf(false)

    var otherValue by mutableStateOf("")

    private val contactObj = mutableStateOf(Contact())
    private val emailTemplateObj = mutableStateOf(EmailTemplate())

    init {
        getContactEmail(appViewModel.uiState.value.userData?.city)
        getEmailTemplate()
    }

    private fun getContactEmail(userCity: String?) {

        viewModelScope.launch {
            if (userCity != null) {
                try {
                    val firestore = FirebaseFirestore.getInstance()
                    val ratesQuerySnapshot = withContext(Dispatchers.IO) {
                        firestore.collection("contacts")
                            .whereEqualTo("city", userCity)
                            .get()
                            .await()
                    }

                    if (!ratesQuerySnapshot.isEmpty) {
                        val contactsDoc = ratesQuerySnapshot.documents[0]
                        try {
                            contactObj.value =
                                contactsDoc.toObject(Contact::class.java) ?: Contact()
                        } catch (e: Exception) {

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
                    Log.e("PqrsViewModel", "Error fetching contacts: ${e.message}")
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
                    message = appContext.getString(R.string.error_no_city_set)
                )
            }
        }


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
                        emailTemplateObj.value =
                            templateDoc.toObject(EmailTemplate::class.java) ?: EmailTemplate()
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

        if (plate.isEmpty()) {
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.error),
                message = appContext.getString(R.string.all_fields_required)
            )
            return
        }

        if (!isHighFare && !isUserMistreated && !isServiceAbandonment && !isUnauthorizedCharges && !isNoFareNotice && !isDangerousDriving && !isOther) {
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.error),
                message = appContext.getString(R.string.select_complaint_reason)
            )
            return
        }

        if (isOther && otherValue.isEmpty()) {
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.error),
                message = appContext.getString(R.string.other_reason_required)
            )
            return
        }


        sendPqrEmail(onIntentReady)

    }

    private fun sendPqrEmail(onIntentReady: (Intent) -> Unit) {

        val irregularities = buildString {
            if (isHighFare) append("- ${appContext.getString(R.string.high_fare)}\n")
            if (isUserMistreated) append("- ${appContext.getString(R.string.user_mistreated)}\n")
            if (isServiceAbandonment) append("- ${appContext.getString(R.string.service_abandonment)}\n")
            if (isUnauthorizedCharges) append("- ${appContext.getString(R.string.unauthorized_charges)}\n")
            if (isNoFareNotice) append("- ${appContext.getString(R.string.no_fare_notice)}\n")
            if (isDangerousDriving) append("- ${appContext.getString(R.string.dangerous_driving)}\n")
            if (isOther) append("- $otherValue\n")
        }

        val fullName = "${appViewModel.uiState.value.userData?.firstName} ${appViewModel.uiState.value.userData?.lastName}"
        val bodyEmail = (emailTemplateObj.value.body ?: "")
            .replace("{city}", appViewModel.uiState.value.userData?.city ?: "")
            .replace("{user_name}", fullName)
            .replace("{plate}", plate)
            .replace("{newline}", "\n")
            .replace("{irregularities}", irregularities)

        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data =
                Uri.parse("mailto:${contactObj.value.pqrEmail}?subject=${appContext.getString(R.string.email_subject)}&body=$bodyEmail")
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
