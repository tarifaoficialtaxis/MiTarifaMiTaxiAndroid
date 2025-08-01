package com.mitarifamitaxi.taximetrousuario.viewmodels.taximeter

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.activities.trips.TripSummaryActivity
import com.mitarifamitaxi.taximetrousuario.helpers.CityRatesManager
import com.mitarifamitaxi.taximetrousuario.helpers.FirebaseStorageUtils
import com.mitarifamitaxi.taximetrousuario.helpers.LocationUpdatesService
import com.mitarifamitaxi.taximetrousuario.helpers.getAddressFromCoordinates
import com.mitarifamitaxi.taximetrousuario.helpers.putIfNotNull
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.models.Rates
import com.mitarifamitaxi.taximetrousuario.models.Recharge
import com.mitarifamitaxi.taximetrousuario.models.Trip
import com.mitarifamitaxi.taximetrousuario.models.UserLocation
import com.mitarifamitaxi.taximetrousuario.states.TaximeterState
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.Locale
import java.util.concurrent.Executor
import kotlin.math.floor

class TaximeterViewModel(context: Context, private val appViewModel: AppViewModel) :
    ViewModel() {

    private val appContext = context.applicationContext

    private val _uiState = MutableStateFlow(TaximeterState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<TaximeterViewModelEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    sealed class TaximeterViewModelEvent {
        object GoBack : TaximeterViewModelEvent()
        object StartForegroundService : TaximeterViewModelEvent()
        object StopForegroundService : TaximeterViewModelEvent()
    }

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val executor: Executor = ContextCompat.getMainExecutor(context)

    private var previousLocation: Location? = null
    private var startTime by mutableStateOf("")
    private var endTime by mutableStateOf("")
    private var isMoving by mutableStateOf(false)
    private var timeElapsed by mutableIntStateOf(0)

    private var mediaPlayer: MediaPlayer? = null

    init {
        _uiState.update {
            it.copy(
                rates = CityRatesManager(appContext).getRatesState() ?: Rates(),
            )
        }
        filterRecharges()
    }


    private fun filterRecharges() {
        val filteredRecharges = _uiState.value.rates.recharges
            .filter { it.show == true }
            .sortedBy { it.order ?: 0 }
        _uiState.update { state ->
            state.copy(
                rates = state.rates.copy(recharges = filteredRecharges)
            )
        }
    }

    private fun onUnitsChanged(newValue: Double) {
        val currentRates = _uiState.value.rates
        val rechargeUnits = _uiState.value.rechargeUnits
        _uiState.update {
            it.copy(
                units = newValue,
                total = (newValue + rechargeUnits) * (currentRates.unitPrice ?: 0.0)
            )
        }
    }

    private fun updateTotal(rechargeUnits: Double) {
        val currentRates = _uiState.value.rates
        val currentUnits = _uiState.value.units
        _uiState.update {
            it.copy(
                rechargeUnits = rechargeUnits,
                total = (rechargeUnits + currentUnits) * (currentRates.unitPrice ?: 0.0)
            )
        }
    }

    fun setTakeMapScreenshot(value: Boolean) {
        _uiState.update { it.copy(takeMapScreenshot = value) }
    }

    fun onChangeIsAddRechargesOpen(value: Boolean) {
        _uiState.update { it.copy(isRechargesOpen = value) }
    }

    fun onRechargeToggled(recharge: Recharge, isChecked: Boolean) {
        val current = _uiState.value.rechargesSelected.toMutableList()
        var newRechargeUnits = _uiState.value.rechargeUnits

        if (isChecked) {
            newRechargeUnits += recharge.units ?: 0.0
            if (!current.any { it.key == recharge.key }) current += recharge
        } else {
            newRechargeUnits -= recharge.units ?: 0.0
            current.removeAll { it.key == recharge.key }
        }
        updateTotal(newRechargeUnits)
        _uiState.value = _uiState.value.copy(rechargesSelected = current)
    }

    private fun getAddressFromStartLocation(latitude: Double, longitude: Double) {
        getAddressFromCoordinates(
            latitude = latitude,
            longitude = longitude,
            callbackSuccess = { address ->
                Log.d("TaximeterVM", "Initial address: $address")
                _uiState.update {
                    it.copy(
                        startAddress = address,
                    )
                }
                appViewModel.setLoading(false)
                startTaximeter()
            },
            callbackError = {
                appViewModel.setLoading(false)
                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = appContext.getString(R.string.error_getting_address)
                )
            }
        )
    }

    fun validateLocationPermission() {
        val locationGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (locationGranted) {
            getCurrentLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        appViewModel.setLoading(true)
        val cancellationTokenSource = CancellationTokenSource()
        val task: Task<Location> = fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        )

        task.addOnSuccessListener(executor) { location ->
            if (location != null) {

                val userLocation = UserLocation(
                    latitude = location.latitude,
                    longitude = location.longitude
                )
                appViewModel.updateUserLocation(userLocation)

                _uiState.update {
                    it.copy(
                        currentLocation = userLocation,
                        startLocation = userLocation
                    )
                }
                getAddressFromStartLocation(
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            } else {
                appViewModel.setLoading(false)
                FirebaseCrashlytics.getInstance()
                    .recordException(Exception("TaximeterViewModel location null"))
                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = appContext.getString(R.string.error_fetching_location)
                )
            }
        }.addOnFailureListener {
            appViewModel.setLoading(false)
            FirebaseCrashlytics.getInstance().recordException(it)
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.something_went_wrong),
                message = appContext.getString(R.string.error_fetching_location)
            )
        }
    }

    // Start methods

    fun startTaximeter() {
        viewModelScope.launch {
            _navigationEvents.emit(TaximeterViewModelEvent.StartForegroundService)
        }

        _uiState.update {
            it.copy(
                isTaximeterStarted = true,
                units = it.rates.startRateUnits ?: 0.0,
                total = it.rates.startRateUnits?.let { units ->
                    units * (it.rates.unitPrice ?: 0.0)
                } ?: 0.0,
            )
        }
        startTime = Instant.now().toString()
        startTimer()
        observeLocationUpdates()
    }

    private fun startTimer() {
        viewModelScope.launch {
            while (_uiState.value.isTaximeterStarted) {
                timeElapsed++
                val hours = timeElapsed / 3600
                val minutes = (timeElapsed % 3600) / 60
                val seconds = timeElapsed % 60

                val formatted = when {
                    timeElapsed < 3600 -> String.format(Locale.US, "%02d:%02d", minutes, seconds)
                    else -> String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
                }
                _uiState.update { it.copy(formattedTime = formatted) }

                if (!isMoving && _uiState.value.isTaximeterStarted) {
                    val newDragTime = _uiState.value.dragTimeElapsed + 1
                    val waitTime = _uiState.value.rates.waitTime ?: 24

                    if (newDragTime >= waitTime) {
                        val newUnits =
                            _uiState.value.units + (_uiState.value.rates.waitTimeRateUnit ?: 1.0)
                        onUnitsChanged(newUnits)
                        _uiState.update { it.copy(dragTimeElapsed = 0) }
                    } else {
                        _uiState.update { it.copy(dragTimeElapsed = newDragTime) }
                    }
                }
                delay(1000)
            }
        }
    }

    private fun observeLocationUpdates() {
        LocationUpdatesService.locationUpdates
            .onEach { location ->
                if (!_uiState.value.isTaximeterStarted) return@onEach

                if (previousLocation == null) {
                    previousLocation = location
                    _uiState.update { it.copy(currentSpeed = 0) }
                    return@onEach
                }

                val timeDeltaSec = (location.time - previousLocation!!.time) / 1000f
                val distanceMeters = previousLocation!!.distanceTo(location)
                val speedMps = if (timeDeltaSec > 0) distanceMeters / timeDeltaSec else 0f
                val speedKph = (speedMps * 3.6f).toInt()

                var newRoute = _uiState.value.routeCoordinates
                if (distanceMeters >= 15f) {
                    newRoute = newRoute + LatLng(location.latitude, location.longitude)
                }

                val userLocation = UserLocation(
                    latitude = location.latitude,
                    longitude = location.longitude
                )

                _uiState.update {
                    it.copy(
                        currentSpeed = speedKph,
                        currentLocation = userLocation,
                        routeCoordinates = newRoute,
                        distanceMade = it.distanceMade + distanceMeters.toDouble()
                    )
                }

                appViewModel.updateUserLocation(userLocation)

                val dragThreshold = _uiState.value.rates.dragSpeed ?: 0.0
                isMoving = speedKph > dragThreshold

                if (isMoving) {
                    var newDistanceAccumulator =
                        _uiState.value.distanceAccumulatorForUnits + distanceMeters

                    val metersPerUnit = _uiState.value.rates.meters ?: 100
                    if (newDistanceAccumulator >= metersPerUnit) {

                        val accumulatesTime = _uiState.value.rates.accumulatesTime
                        if (accumulatesTime == null || !accumulatesTime) {
                            _uiState.update { state -> state.copy(dragTimeElapsed = 0) }
                        }

                        val unitsToAdd = floor(newDistanceAccumulator / metersPerUnit)
                        val newUnits = _uiState.value.units + unitsToAdd
                        onUnitsChanged(newUnits)

                        newDistanceAccumulator %= metersPerUnit
                    }
                    _uiState.update { it.copy(distanceAccumulatorForUnits = newDistanceAccumulator) }
                }
                validateSpeedExceeded()
                previousLocation = location
            }
            .launchIn(viewModelScope)
    }

    private fun isPlayerPlayingSafe(): Boolean {
        mediaPlayer?.let { mp ->
            return try {
                mp.isPlaying
            } catch (e: IllegalStateException) {
                Log.w("TaximeterVM", "isPlaying no disponible: ${e.message}")
                false
            }
        }
        return false
    }

    fun validateSpeedExceeded() {
        val speedExceeded =
            _uiState.value.currentSpeed > ((uiState.value.rates.speedLimit ?: 0) - 3)

        if (speedExceeded) {
            ensureMediaPlayer()
            if (!isPlayerPlayingSafe()) {
                mediaPlayer?.start()
            }
        } else {
            if (isPlayerPlayingSafe()) {
                mediaPlayer?.pause()
            }
        }
    }

    fun showBackConfirmation() {
        appViewModel.showMessage(
            type = DialogType.WARNING,
            title = appContext.getString(R.string.finish_your_trip_question),
            message = appContext.getString(R.string.you_are_about_to_finish_long),
            buttonText = appContext.getString(R.string.finish_trip),
            secondaryButtonText = appContext.getString(R.string.back_home),
            onButtonClicked = { stopTaximeter() },
            onSecondaryButtonClicked = {
                viewModelScope.launch {
                    _navigationEvents.emit(TaximeterViewModelEvent.GoBack)
                }
            }
        )
    }

    fun showFinishConfirmation() {
        appViewModel.showMessage(
            type = DialogType.WARNING,
            title = appContext.getString(R.string.finish_your_trip),
            message = appContext.getString(R.string.you_are_about_to_finish),
            buttonText = appContext.getString(R.string.finish_trip),
            onButtonClicked = { stopTaximeter() }
        )
    }

    private fun ensureMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(appContext, R.raw.soft_alert).apply {
                isLooping = true
            }
        }
    }

    fun toggleSound() {
        ensureMediaPlayer()
        mediaPlayer?.let { mp ->
            try {
                if (_uiState.value.isSoundEnabled && isPlayerPlayingSafe()) {
                    mp.setVolume(0f, 0f)
                } else {
                    mp.setVolume(1f, 1f)
                }
                _uiState.update { it.copy(isSoundEnabled = !it.isSoundEnabled) }
            } catch (e: IllegalStateException) {
                Log.w("TaximeterVM", "toggleSound falló: ${e.message}")
                mp.release()
                mediaPlayer = null
            }
        }
    }

    fun stopMediaPlayer() {
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) mp.stop()
            } catch (e: IllegalStateException) {
                Log.w("TaximeterVM", "stop() falló: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    @SuppressLint("ImplicitSamInstance")
    fun stopTaximeter() {

        viewModelScope.launch {
            _navigationEvents.emit(TaximeterViewModelEvent.StopForegroundService)
        }

        stopMediaPlayer()
        _uiState.update { it.copy(currentSpeed = 0) }
        appViewModel.setLoading(true)

        val currentPos = _uiState.value.currentLocation
        getAddressFromCoordinates(
            latitude = currentPos.latitude ?: 0.0,
            longitude = currentPos.longitude ?: 0.0,
            callbackSuccess = { address ->
                appViewModel.setLoading(false)
                finishTaximeter(address)
            },
            callbackError = {
                FirebaseCrashlytics.getInstance()
                    .recordException(Exception("TaximeterViewModel error on stop, ${it.message}"))
                appViewModel.setLoading(false)
                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = appContext.getString(R.string.error_getting_address),
                    onDismiss = {}
                )
            }
        )
    }

    fun finishTaximeter(endAddress: String) {
        _uiState.update {
            it.copy(
                endAddress = endAddress,
                isTaximeterStarted = false,
                fitCameraPosition = true
            )
        }
        endTime = Instant.now().toString()
    }

    fun mapScreenshotReady(bitmap: Bitmap, onIntentReady: (Intent) -> Unit) {

        setTakeMapScreenshot(false)
        _uiState.update { it.copy(fitCameraPosition = false) }

        val maxDim = 1280
        val ratio = minOf(
            maxDim.toFloat() / bitmap.width,
            maxDim.toFloat() / bitmap.height
        ).coerceAtMost(1f)
        val newWidth = (bitmap.width * ratio).toInt()
        val newHeight = (bitmap.height * ratio).toInt()
        val scaledBitmap = bitmap.scale(newWidth, newHeight, true)

        val state = _uiState.value
        val rates = state.rates
        val baseUnits = if (state.units < (rates.minimumRateUnits ?: 0.0))
            rates.minimumRateUnits ?: 0.0
        else
            state.units

        val totalUnits = baseUnits + state.rechargeUnits

        if (state.startAddress.isBlank()
            || state.endAddress.isBlank()
            || (state.startLocation.latitude == 0.0 && state.startLocation.longitude == 0.0)
            || (state.currentLocation.latitude == 0.0 && state.currentLocation.longitude == 0.0)
            || startTime.isEmpty()
            || endTime.isEmpty()
            || totalUnits == 0.0
        ) {
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.attention),
                message = appContext.getString(R.string.error_on_save_trip)
            )
            return
        }

        val tripObj = Trip(
            startAddress = state.startAddress,
            startCoords = state.startLocation,
            endAddress = state.endAddress,
            endCoords = state.currentLocation,
            startHour = startTime,
            endHour = endTime,
            showUnits = state.rates.showUnits,
            unitPrice = state.rates.unitPrice,
            units = totalUnits,
            baseUnits = baseUnits,
            rechargeUnits = state.rechargeUnits,
            total = (baseUnits + state.rechargeUnits) * (rates.unitPrice ?: 0.0),
            baseRate = baseUnits * (rates.unitPrice ?: 0.0),
            distance = state.distanceMade,
            recharges = state.rechargesSelected,
            currency = appViewModel.uiState.value.userData?.countryCurrency,
        )

        saveTripData(tripData = tripObj, image = scaledBitmap) {
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            val compressedBytes = outputStream.toByteArray()

            val tripJson = Gson().toJson(tripObj)
            val intent = Intent(appContext, TripSummaryActivity::class.java)
            intent.putExtra("trip_data", tripJson)
            intent.putExtra("trip_image_bytes", compressedBytes)
            onIntentReady(intent)
        }
    }

    fun saveTripData(tripData: Trip, image: Bitmap, onSuccess: () -> Unit) {

        val userId = appViewModel.uiState.value.userData?.id

        viewModelScope.launch {
            try {
                appViewModel.setLoading(true)
                var imageUrl: String? = null

                if (_uiState.value.routeCoordinates.size > 2) {
                    imageUrl = FirebaseStorageUtils.uploadImage("appFiles/$userId/trips", image)
                }

                val tripDataReq = mutableMapOf<String, Any?>().apply {
                    putIfNotNull("userId", userId)
                    putIfNotNull("startCoords", tripData.startCoords)
                    putIfNotNull("endCoords", tripData.endCoords)
                    putIfNotNull("startHour", tripData.startHour)
                    putIfNotNull("endHour", tripData.endHour)
                    putIfNotNull("distance", tripData.distance)
                    putIfNotNull("showUnits", tripData.showUnits)
                    putIfNotNull("unitPrice", tripData.unitPrice)
                    putIfNotNull("units", tripData.units)
                    putIfNotNull("baseUnits", tripData.baseUnits)
                    putIfNotNull("rechargeUnits", tripData.rechargeUnits)
                    putIfNotNull("total", tripData.total)
                    putIfNotNull("baseRate", tripData.baseRate)
                    putIfNotNull("recharges", tripData.recharges)
                    putIfNotNull("currency", appViewModel.uiState.value.userData?.countryCurrency)
                    putIfNotNull("startAddress", tripData.startAddress)
                    putIfNotNull("endAddress", tripData.endAddress)
                    putIfNotNull("routeImage", imageUrl)
                }

                FirebaseFirestore.getInstance().collection("trips").add(tripDataReq).await()
                appViewModel.setLoading(false)
                onSuccess()
            } catch (error: Exception) {
                appViewModel.setLoading(false)
                Log.e("TripSummaryViewModel", "Error saving trip data: ${error.message}")
                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = appContext.getString(R.string.error_on_save_trip),
                )
            }
        }
    }

}

class TaximeterViewModelFactory(
    private val context: Context,
    private val appViewModel: AppViewModel
) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TaximeterViewModel(context, appViewModel) as T

    }
}