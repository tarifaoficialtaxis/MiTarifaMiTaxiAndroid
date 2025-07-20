package com.mitarifamitaxi.taximetrousuario.viewmodels.trips

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.FirebaseStorageUtils
import com.mitarifamitaxi.taximetrousuario.helpers.formatDigits
import com.mitarifamitaxi.taximetrousuario.helpers.formatNumberWithDots
import com.mitarifamitaxi.taximetrousuario.helpers.shareFormatDate
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.models.Trip
import com.mitarifamitaxi.taximetrousuario.states.TripSummaryState
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.net.URLEncoder

class TripSummaryViewModel(context: Context, private val appViewModel: AppViewModel) : ViewModel() {

    private val appContext = context.applicationContext

    private val _uiState = MutableStateFlow(TripSummaryState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    sealed class NavigationEvent {
        object GoBack : NavigationEvent()
    }

    fun setTrip(trip: Trip) {
        _uiState.update { it.copy(tripData = trip) }
    }

    fun setRouteImageLocal(image: Bitmap) {
        _uiState.update { it.copy(routeImageLocal = image) }
    }

    fun onShareNumberChange(number: String) {
        _uiState.update { it.copy(shareNumber = number, isShareNumberError = false) }
    }

    fun onShowShareDialog(show: Boolean) {
        _uiState.update { it.copy(showShareDialog = show) }
    }

    fun onChangeDetails(value: Boolean) {
        _uiState.update { it.copy(isDetails = value) }
    }

    fun onChangeIsDetailsOpen(value: Boolean) {
        _uiState.update { it.copy(isDetailsOpen = value) }
    }

    fun onDeleteAction() {
        appViewModel.showMessage(
            type = DialogType.WARNING,
            title = appContext.getString(R.string.delete_trip),
            message = appContext.getString(R.string.delete_trip_message),
            buttonText = appContext.getString(R.string.delete),
            onButtonClicked = {
                _uiState.value.tripData.id?.let { deleteTrip(it) }
            }
        )
    }

    private fun deleteTrip(tripId: String) {
        viewModelScope.launch {
            try {
                appViewModel.setLoading(true)

                _uiState.value.tripData.routeImage?.let { imageUrl ->
                    FirebaseStorageUtils.deleteImage(imageUrl)
                }

                FirebaseFirestore.getInstance().collection("trips").document(tripId).delete()
                    .await()
                appViewModel.setLoading(false)

                appViewModel.showMessage(
                    type = DialogType.SUCCESS,
                    title = appContext.getString(R.string.success),
                    message = appContext.getString(R.string.trip_deleted_successfully),
                    buttonText = appContext.getString(R.string.accept),
                    showCloseButton = false,
                    onButtonClicked = {
                        viewModelScope.launch {
                            _navigationEvents.emit(NavigationEvent.GoBack)
                        }
                    }
                )

            } catch (error: Exception) {
                Log.e("TripSummaryViewModel", "Error deleting trip: ${error.message}")
                appViewModel.setLoading(false)
                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = appContext.getString(R.string.error_on_delete_trip),
                )
            }
        }
    }


    fun sendWatsAppMessage(onIntentReady: (Intent) -> Unit) {
        val currentState = _uiState.value
        if (currentState.shareNumber.isEmpty()) {
            _uiState.update { it.copy(isShareNumberError = true) }
            return
        }

        _uiState.update { it.copy(showShareDialog = false) }

        val message = buildString {
            append("*Esta es la información de mi viaje:*\n")
            append("*Dirección de origen:* ${currentState.tripData.startAddress}\n")
            append("*Dirección de destino:* ${currentState.tripData.endAddress}\n")
            append("*Fecha de recogida:* ${currentState.tripData.startHour?.let { shareFormatDate(it) }}\n")
            append("*Fecha de llegada:* ${currentState.tripData.endHour?.let { shareFormatDate(it) }}\n")
            append(
                "*Distancia recorrida:* ${
                    currentState.tripData.distance?.let { (it / 1000).formatDigits(1) }
                } KM\n"
            )

            if (currentState.tripData.showUnits == true) {
                append("*Unidades base:* ${currentState.tripData.baseUnits?.formatNumberWithDots()}\n")
            }


            append(
                "*Tarifa base:* ${
                    currentState.tripData.baseRate?.formatNumberWithDots()
                } ${appViewModel.uiState.value.userData?.countryCurrency}\n"
            )

            if (currentState.tripData.showUnits == true) {
                append("*Unidades recargo:* ${currentState.tripData.rechargeUnits?.formatNumberWithDots()}\n")
            }

            for (recharge in currentState.tripData.recharges) {
                append(
                    "*${recharge.name}:* ${
                        ((recharge.units ?: 0.0) * (currentState.tripData.unitPrice ?: 0.0)).formatNumberWithDots()
                    } ${appViewModel.uiState.value.userData?.countryCurrency}\n"
                )
            }

            if (currentState.tripData.showUnits == true) {
                append("*Unidades totales:* ${currentState.tripData.units?.formatNumberWithDots()}\n")
            }

            append(
                "*${appContext.getString(R.string.total)}* ${
                    currentState.tripData.total?.formatNumberWithDots()
                } ${appViewModel.uiState.value.userData?.countryCurrency}"
            )
        }

        val messageToSend = URLEncoder.encode(message, "UTF-8").replace("%0A", "%0D%0A")
        val whatsappURL =
            "whatsapp://send?text=$messageToSend&phone=${appViewModel.uiState.value.userData?.countryCodeWhatsapp}${currentState.shareNumber}"

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(whatsappURL)
        }

        if (intent.resolveActivity(appContext.packageManager) != null) {
            onIntentReady(intent)
        } else {
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.something_went_wrong),
                message = appContext.getString(R.string.whatsapp_not_installed),
            )
        }
    }
}

class TripSummaryViewModelFactory(
    private val context: Context,
    private val appViewModel: AppViewModel
) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TripSummaryViewModel::class.java)) {
            return TripSummaryViewModel(context, appViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
