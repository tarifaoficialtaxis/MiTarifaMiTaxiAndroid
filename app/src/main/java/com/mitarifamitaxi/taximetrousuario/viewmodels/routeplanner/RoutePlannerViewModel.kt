package com.mitarifamitaxi.taximetrousuario.viewmodels.routeplanner

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.activities.taximeter.TaximeterActivity
import com.mitarifamitaxi.taximetrousuario.helpers.fetchRoute
import com.mitarifamitaxi.taximetrousuario.helpers.getAddressFromCoordinates
import com.mitarifamitaxi.taximetrousuario.helpers.getPlaceDetails
import com.mitarifamitaxi.taximetrousuario.helpers.getPlacePredictions
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.models.PlacePrediction
import com.mitarifamitaxi.taximetrousuario.models.UserLocation
import com.mitarifamitaxi.taximetrousuario.states.RoutePlannerState
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import com.mitarifamitaxi.taximetrousuario.viewmodels.UserDataUpdateEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RoutePlannerViewModel(context: Context, private val appViewModel: AppViewModel) :
    ViewModel() {

    private val appContext = context.applicationContext

    private val _uiState = MutableStateFlow(RoutePlannerState())
    val uiState = _uiState.asStateFlow()

    init {
        appViewModel.setLoading(true)
        observeAppViewModelEvents()
    }

    private fun observeAppViewModelEvents() {
        viewModelScope.launch {
            appViewModel.userDataUpdateEvents.collectLatest { event ->
                if (event is UserDataUpdateEvent.FirebaseUserUpdated) {
                    appViewModel.setLoading(false)
                    setInitialStartAddress()
                }
            }
        }
    }

    private fun setInitialStartAddress() {
        val userLocation = appViewModel.uiState.value.userLocation ?: return
        getAddressFromCoordinates(
            latitude = userLocation.latitude ?: 0.0,
            longitude = userLocation.longitude ?: 0.0,
            callbackSuccess = { address ->
                appViewModel.setLoading(false)
                _uiState.update {
                    it.copy(
                        startAddress = address,
                        isSelectingStart = false,
                        startLocation = UserLocation(userLocation.latitude, userLocation.longitude)
                    )
                }
            },
            callbackError = {
                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = appContext.getString(R.string.error_getting_address)
                )
            }
        )
    }

    fun onFocusChanged(isStart: Boolean, isFocused: Boolean) {
        if (isStart) {
            _uiState.update { it.copy(isStartAddressFocused = isFocused) }
        } else {
            _uiState.update { it.copy(isEndAddressFocused = isFocused) }
        }
    }

    fun onStartAddressChange(address: String) {
        _uiState.update { it.copy(startAddress = address) }
        loadPlacePredictions(address)
    }

    fun onEndAddressChange(address: String) {
        _uiState.update { it.copy(endAddress = address) }
        loadPlacePredictions(address)
    }

    fun getRoutePreview() {
        val (startLocation, endLocation) = _uiState.value.let { it.startLocation to it.endLocation }
        if (startLocation.latitude == null || startLocation.longitude == null || endLocation.latitude == null || endLocation.longitude == null) {
            return
        }

        fetchRoute(
            originLatitude = startLocation.latitude,
            originLongitude = startLocation.longitude,
            destinationLatitude = endLocation.latitude,
            destinationLongitude = endLocation.longitude,
            callbackSuccess = { points -> _uiState.update { it.copy(routePoints = points) } },
            callbackError = {
                appViewModel.showMessage(
                    DialogType.ERROR,
                    appContext.getString(R.string.something_went_wrong),
                    appContext.getString(R.string.error_getting_route)
                )
            }
        )
    }

    private fun loadPlacePredictions(input: String) {
        if (input.length < 2) {
            _uiState.update { it.copy(places = emptyList()) }
            return
        }
        val appState = appViewModel.uiState.value
        getPlacePredictions(
            input = input,
            latitude = appState.userLocation?.latitude ?: 0.0,
            longitude = appState.userLocation?.longitude ?: 0.0,
            country = appState.userData?.countryCode ?: "CO",
            callbackSuccess = { predictions -> _uiState.update { it.copy(places = predictions) } },
            callbackError = {
                appViewModel.showMessage(
                    DialogType.ERROR,
                    appContext.getString(R.string.something_went_wrong),
                    appContext.getString(R.string.error_getting_places)
                )
            }
        )
    }

    fun selectPlacePrediction(place: PlacePrediction) {
        _uiState.update { it.copy(places = emptyList()) }
        place.placeId?.let { placeId ->
            getPlaceDetails(
                placeId,
                callbackSuccess = { location ->
                    place.description?.let { description ->
                        if (_uiState.value.isSelectingStart) {
                            _uiState.update {
                                it.copy(
                                    startAddress = description,
                                    startLocation = location
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    endAddress = description,
                                    endLocation = location
                                )
                            }
                        }
                        getRoutePreview()
                    }
                },
                callbackError = {
                    appViewModel.showMessage(
                        DialogType.ERROR,
                        appContext.getString(R.string.something_went_wrong),
                        appContext.getString(R.string.error_getting_address)
                    )
                }
            )
        }
    }

    fun validateStartTrip(onIntentReady: (Intent) -> Unit) {
        val currentState = _uiState.value
        if (currentState.startAddress.isEmpty() || currentState.endAddress.isEmpty()) {
            appViewModel.showMessage(
                DialogType.WARNING,
                appContext.getString(R.string.attention),
                appContext.getString(R.string.select_start_and_end_points)
            )
            return
        }

        val intent = Intent(appContext, TaximeterActivity::class.java).apply {
            putExtra("start_address", currentState.startAddress)
            putExtra("start_location", Gson().toJson(currentState.startLocation))
            putExtra("end_address", currentState.endAddress)
            putExtra("end_location", Gson().toJson(currentState.endLocation))
        }
        onIntentReady(intent)
    }
}

class RoutePlannerViewModelFactory(
    private val context: Context,
    private val appViewModel: AppViewModel
) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RoutePlannerViewModel(context, appViewModel) as T
    }
}