package com.mitarifamitaxi.taximetrousuario.viewmodels.sos

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.ContactsCatalogManager
import com.mitarifamitaxi.taximetrousuario.models.Contact
import com.mitarifamitaxi.taximetrousuario.models.ContactCatalog
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.states.SosState
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import com.mitarifamitaxi.taximetrousuario.viewmodels.trips.MyTripsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.URLEncoder

class SosViewModel(context: Context, private val appViewModel: AppViewModel) : ViewModel() {

    private val appContext = context.applicationContext

    private val _uiState = MutableStateFlow(SosState())
    val uiState: StateFlow<SosState> = _uiState

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    sealed class NavigationEvent {
        object GoToProfile : NavigationEvent()
        object GoBack : NavigationEvent()
    }

    init {
        loadContacts(appViewModel.uiState.value.userData?.city ?: "")
    }

    fun filterContactLines() {
        val originalContact = ContactsCatalogManager(appContext).getContactsState() ?: Contact()
        val userData = appViewModel.uiState.value.userData
        val supportNumber = userData?.supportNumber
        val familyNumber = userData?.familyNumber

        val updatedLines = originalContact.lines
            .asSequence()
            .filter { it.show }
            .sortedBy { it.order }
            .map { line ->
                when (line.key) {
                    "SUPPORT" -> line.copy(
                        line2 = supportNumber,
                        whatsapp = supportNumber
                    )

                    "FAMILY" -> line.copy(
                        line2 = familyNumber,
                        whatsapp = familyNumber
                    )
                    else -> line
                }
            }
            .toList()

        _uiState.update { uiState ->
            uiState.copy(
                contact = originalContact.copy(lines = updatedLines)
            )
        }
    }


    fun showContactDialog(itemSelected: ContactCatalog? = null) {

        if (itemSelected?.key == "SUPPORT" && appViewModel.uiState.value.userData?.supportNumber.isNullOrEmpty()) {
            appViewModel.showMessage(
                DialogType.WARNING,
                appContext.getString(R.string.support_number_not_found),
                appContext.getString(R.string.set_up_support_number),
                appContext.getString(R.string.add_number),
                onButtonClicked = {
                    goToProfile()
                }
            )
            return

        }

        if (itemSelected?.key == "FAMILY" && appViewModel.uiState.value.userData?.familyNumber.isNullOrEmpty()) {
            appViewModel.showMessage(
                DialogType.WARNING,
                appContext.getString(R.string.family_number_not_found),
                appContext.getString(R.string.set_up_family_number),
                appContext.getString(R.string.add_number),
                onButtonClicked = {
                    goToProfile()
                }
            )
            return
        }


        _uiState.update { currentState ->
            currentState.copy(showContactDialog = true, contactCatalogSelected = itemSelected)
        }

    }


    fun hideContactDialog() {
        _uiState.update { currentState ->
            currentState.copy(showContactDialog = false)
        }
    }

    fun validateShowModal() {
        if (_uiState.value.contact.showSosWarning) {
            appViewModel.showMessage(
                DialogType.WARNING,
                appContext.getString(R.string.warning),
                _uiState.value.contact.warningMessage ?: "",
                appContext.getString(R.string.confirm),
                showCloseButton = false
            )
        }
    }

    fun validateSosAction(isCall: Boolean, contactNumber: String, onIntentReady: (Intent) -> Unit) {

        var sosType = _uiState.value.contactCatalogSelected?.name ?: "SOS"
        var event: String? = null

        when (_uiState.value.contactCatalogSelected?.key) {

            "ANIMAL_CARE" -> {
                event = appContext.getString(R.string.sos_animal_care)
            }

        }

        if (isCall) {
            buildIntentCall(contactNumber) { intent ->
                onIntentReady(intent)
            }
        } else {
            sendWhatsappMessage(
                contactNumber,
                sosType,
                event
            ) { intent ->
                onIntentReady(intent)
            }
        }


    }

    private fun sendWhatsappMessage(
        phoneNumber: String,
        sosType: String,
        event: String? = null,
        onIntentReady: (Intent) -> Unit
    ) {

        val userLocation = appViewModel.uiState.value.userLocation
        val message = buildString {
            append("*SOS ${sosType.uppercase()}*\n")
            if (event != null) {
                append("*${event}:*\n")
            } else {
                append("*${appContext.getString(R.string.this_is_my_location)}:*\n")
            }
            append("https://maps.google.com/?q=${userLocation?.latitude},${userLocation?.longitude}\n")
        }

        val messageToSend = URLEncoder.encode(message, "UTF-8").replace("%0A", "%0D%0A")
        val whatsappURL =
            "whatsapp://send?text=$messageToSend&phone=${appViewModel.uiState.value.userData?.countryCodeWhatsapp}${phoneNumber}"

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(whatsappURL)
        }

        if (intent.resolveActivity(appContext.packageManager) != null) {
            onIntentReady(intent)
        } else {
            appViewModel.showMessage(
                DialogType.ERROR,
                appContext.getString(R.string.something_went_wrong),
                appContext.getString(R.string.whatsapp_not_installed)
            )
        }

    }

    private fun buildIntentCall(phoneNumber: String, onIntentReady: (Intent) -> Unit) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${phoneNumber}")
        }
        onIntentReady(intent)
    }

    fun goToProfile() {
        viewModelScope.launch {
            _navigationEvents.emit(NavigationEvent.GoToProfile)
        }
    }

    private fun loadContacts(city: String) {
        viewModelScope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val ratesQuerySnapshot = withContext(Dispatchers.IO) {
                    firestore.collection("dynamicSos")
                        .whereEqualTo("city", city)
                        .get()
                        .await()
                }

                if (!ratesQuerySnapshot.isEmpty) {
                    val contactsDoc = ratesQuerySnapshot.documents[0]
                    try {
                        val contactVal =
                            contactsDoc.toObject(Contact::class.java) ?: Contact()
                        ContactsCatalogManager(appContext).saveContactsState(contactVal)
                        filterContactLines()
                        validateShowModal()
                    } catch (e: Exception) {
                        Log.e("SosViewModel", "Error parsing contact data: ${e.message}")
                    }
                } else {
                    Log.e(
                        "SosViewModel",
                        "Error fetching contacts: ${appContext.getString(R.string.error_no_contacts_found)}"
                    )

                }
            } catch (e: Exception) {
                Log.e("SosViewModel", "Error fetching contacts: ${e.message}")
            }

        }

    }

}

class SosViewModelFactory(
    private val context: Context,
    private val appViewModel: AppViewModel
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(cls: Class<T>): T {
        return SosViewModel(context, appViewModel) as T
    }
}