package com.mitarifamitaxi.taximetrousuario.viewmodels.trips

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
import com.mitarifamitaxi.taximetrousuario.helpers.FirebaseStorageUtils
import com.mitarifamitaxi.taximetrousuario.helpers.formatDigits
import com.mitarifamitaxi.taximetrousuario.helpers.formatNumberWithDots
import com.mitarifamitaxi.taximetrousuario.helpers.shareFormatDate
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.models.Trip
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.net.URLEncoder

class TripSummaryViewModel(context: Context, private val appViewModel: AppViewModel) : ViewModel() {

    private val appContext = context.applicationContext

    var isDetails by mutableStateOf(false)

    var tripData by mutableStateOf(Trip())

    var showShareDialog by mutableStateOf(false)
    var shareNumber = mutableStateOf("")
    var isShareNumberError = mutableStateOf(false)

    var isDetailsOpen by mutableStateOf(false)

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    sealed class NavigationEvent {
        object GoBack : NavigationEvent()
    }

    fun onDeleteAction() {
        appViewModel.showMessage(
            type = DialogType.WARNING,
            title = appContext.getString(R.string.delete_trip),
            message = appContext.getString(R.string.delete_trip_message),
            buttonText = appContext.getString(R.string.delete),
            onButtonClicked = {
                tripData.id?.let { deleteTrip(it) }
            }
        )
    }

    fun deleteTrip(tripId: String) {

        viewModelScope.launch {
            try {
                appViewModel.isLoading = true

                tripData.routeImage?.let { imageUrl ->
                    FirebaseStorageUtils.deleteImage(imageUrl)
                }

                FirebaseFirestore.getInstance().collection("trips").document(tripId).delete()
                    .await()
                appViewModel.isLoading = false

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
                appViewModel.isLoading = false
                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = appContext.getString(R.string.error_on_delete_trip),
                )
            }
        }
    }


    fun sendWatsAppMessage(onIntentReady: (Intent) -> Unit) {

        if (shareNumber.value.isEmpty()) {
            isShareNumberError.value = true
            return
        }

        showShareDialog = false

        val message = buildString {
            append("*Esta es la información de mi viaje:*\n")
            append("*Dirección de origen:* ${tripData.startAddress}\n")
            append("*Dirección de destino:* ${tripData.endAddress}\n")
            append("*Fecha de recogida:* ${tripData.startHour?.let { shareFormatDate(it) }}\n")
            append("*Fecha de llegada:* ${tripData.endHour?.let { shareFormatDate(it) }}\n")
            append(
                "*Distancia recorrida:* ${
                    tripData.distance?.let { (it / 1000).formatDigits(1) }
                } KM\n"
            )
            append("*Unidades base:* ${tripData.baseUnits}\n")

            append(
                "*Tarifa base:* ${
                    tripData.baseRate?.toInt()?.formatNumberWithDots()
                } ${appViewModel.userData?.countryCurrency}\n"
            )

            append("*Unidades recargo:* ${tripData.rechargeUnits}\n")

            if (tripData.airportSurchargeEnabled == true) {
                append(
                    "*Recargo aeropuerto:* ${
                        tripData.airportSurcharge?.toInt()?.formatNumberWithDots()
                    } ${appViewModel.userData?.countryCurrency}\n"
                )
            }

            if (tripData.holidayOrNightSurchargeEnabled == true) {
                append(
                    "*Recargo nocturno dominical o festivo:* ${
                        tripData.holidaySurcharge?.toInt()?.formatNumberWithDots()
                    } ${appViewModel.userData?.countryCurrency}\n"
                )
            }

            if (tripData.doorToDoorSurchargeEnabled == true) {
                append(
                    "*Recargo puerta a puerta:* ${
                        tripData.doorToDoorSurcharge?.toInt()?.formatNumberWithDots()
                    } ${appViewModel.userData?.countryCurrency}\n"
                )
            }

            if (tripData.holidaySurchargeEnabled == true) {
                append(
                    "*Recargo dominical o festivo:* ${
                        tripData.holidaySurcharge?.toInt()?.formatNumberWithDots()
                    } ${appViewModel.userData?.countryCurrency}\n"
                )
            }

            if (tripData.nightSurchargeEnabled == true) {
                append(
                    "*Recargo nocturno:* ${
                        tripData.nightSurcharge?.toInt()?.formatNumberWithDots()
                    } ${appViewModel.userData?.countryCurrency}\n"
                )
            }

            append("*Unidades totales:* ${tripData.units}\n")

            append(
                "*${appContext.getString(R.string.total)}* ${
                    tripData.total?.toInt()?.formatNumberWithDots()
                } ${appViewModel.userData?.countryCurrency}"
            )
        }

        val messageToSend = URLEncoder.encode(message, "UTF-8").replace("%0A", "%0D%0A")
        val whatsappURL =
            "whatsapp://send?text=$messageToSend&phone=${appViewModel.userData?.countryCodeWhatsapp}${shareNumber.value}"

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