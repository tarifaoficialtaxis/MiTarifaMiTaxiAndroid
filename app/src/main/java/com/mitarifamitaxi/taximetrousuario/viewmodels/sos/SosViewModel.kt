package com.mitarifamitaxi.taximetrousuario.viewmodels.sos

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
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
import com.mitarifamitaxi.taximetrousuario.models.ItemImageButton
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import com.mitarifamitaxi.taximetrousuario.viewmodels.UserDataUpdateEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.URLEncoder

class SosViewModel(context: Context, private val appViewModel: AppViewModel) : ViewModel() {

    private val appContext = context.applicationContext

    var showContactDialog by mutableStateOf(false)

    private val contactObj = mutableStateOf(Contact())
    var itemSelected: ItemImageButton? = null

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    sealed class NavigationEvent {
        object GoToProfile : NavigationEvent()
        object GoBack : NavigationEvent()
    }

    init {
        appViewModel.isLoading = true
        observeAppViewModelEvents()
    }

    private fun observeAppViewModelEvents() {
        viewModelScope.launch {
            appViewModel.userDataUpdateEvents.collectLatest { event ->
                when (event) {
                    is UserDataUpdateEvent.FirebaseUserUpdated -> {
                        Log.d(
                            "SosViewModel",
                            "Received FirebaseUserUpdated event. Current appViewModel city: ${appViewModel.userData?.city}"
                        )
                        getCityContacts(appViewModel.userData?.city)
                    }
                }
            }
        }
    }

    private fun getCityContacts(userCity: String?) {

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
                            appViewModel.isLoading = false
                            contactObj.value =
                                contactsDoc.toObject(Contact::class.java) ?: Contact()
                            validateShowModal()
                        } catch (e: Exception) {
                            Log.e("SosViewModel", "Error parsing contact data: ${e.message}")
                            appViewModel.isLoading = false
                            appViewModel.showMessage(
                                DialogType.ERROR,
                                appContext.getString(R.string.something_went_wrong),
                                appContext.getString(R.string.general_error)
                            )
                        }
                    } else {
                        appViewModel.isLoading = false
                        appViewModel.showMessage(
                            DialogType.ERROR,
                            appContext.getString(R.string.something_went_wrong),
                            appContext.getString(R.string.error_no_contacts_found),
                            onDismiss = {
                                goBack()
                            }
                        )
                    }
                } catch (e: Exception) {
                    Log.e("SosViewModel", "Error fetching contacts: ${e.message}")
                    appViewModel.isLoading = false
                    appViewModel.showMessage(
                        DialogType.ERROR,
                        appContext.getString(R.string.something_went_wrong),
                        appContext.getString(R.string.general_error),
                        onDismiss = {
                            goBack()
                        }
                    )
                }
            } else {
                appViewModel.isLoading = false
                appViewModel.showMessage(
                    DialogType.ERROR,
                    appContext.getString(R.string.something_went_wrong),
                    appContext.getString(R.string.error_no_city_set),
                    onDismiss = {
                        goBack()
                    }
                )
            }
        }


    }

    fun validateShowModal() {
        if (contactObj.value.showSosWarning) {
            appViewModel.showMessage(
                DialogType.WARNING,
                appContext.getString(R.string.warning),
                contactObj.value.warningMessage ?: "",
                appContext.getString(R.string.confirm),
                showCloseButton = false
            )
        }
    }

    fun validateSosAction(isCall: Boolean, onIntentReady: (Intent) -> Unit) {

        var contactNumber = ""
        var sosType = ""
        var event: String? = null

        when (itemSelected?.id) {
            "POLICE" -> {
                contactNumber = contactObj.value.policeNumber ?: ""
                sosType = appContext.getString(R.string.police)
            }

            "FIRE_FIGHTERS" -> {
                contactNumber = contactObj.value.firefightersNumber ?: ""
                sosType = appContext.getString(R.string.fire_fighters)
            }

            "AMBULANCE" -> {
                contactNumber = contactObj.value.ambulanceNumber ?: ""
                sosType = appContext.getString(R.string.ambulance)
            }

            "ANIMAL_CARE" -> {
                contactNumber = contactObj.value.animalCareNumber ?: ""
                sosType = appContext.getString(R.string.animal_care)
                event = appContext.getString(R.string.sos_animal_care)
            }

            "SUPPORT" -> {

                if (appViewModel.userData?.supportNumber != null) {
                    contactNumber = appViewModel.userData?.supportNumber ?: ""
                    sosType = appContext.getString(R.string.support)
                } else {
                    appViewModel.showMessage(
                        DialogType.WARNING,
                        appContext.getString(R.string.support_number_not_found),
                        appContext.getString(R.string.set_up_support_number),
                        appContext.getString(R.string.add_number),
                        onButtonClicked = {
                            goToProfile()
                        }
                    )
                }
            }

            "FAMILY" -> {
                if (appViewModel.userData?.familyNumber != null) {
                    contactNumber = appViewModel.userData?.familyNumber ?: ""
                    sosType = appContext.getString(R.string.family)
                } else {
                    appViewModel.showMessage(
                        DialogType.WARNING,
                        appContext.getString(R.string.family_number_not_found),
                        appContext.getString(R.string.set_up_family_number),
                        appContext.getString(R.string.add_number),
                        onButtonClicked = {
                            goToProfile()
                        }
                    )
                }
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

        val userLocation = appViewModel.userLocation
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
            "whatsapp://send?text=$messageToSend&phone=${appViewModel.userData?.countryCodeWhatsapp}${phoneNumber}"

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

    fun goBack() {
        viewModelScope.launch {
            _navigationEvents.emit(NavigationEvent.GoBack)
        }
    }
}

class SosViewModelFactory(
    private val context: Context,
    private val appViewModel: AppViewModel
) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SosViewModel::class.java)) {
            return SosViewModel(context, appViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}