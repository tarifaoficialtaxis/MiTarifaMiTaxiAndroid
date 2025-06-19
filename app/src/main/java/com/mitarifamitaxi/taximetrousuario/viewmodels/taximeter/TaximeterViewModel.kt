package com.mitarifamitaxi.taximetrousuario.viewmodels.taximeter

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.activities.taximeter.TaximeterActivity
import com.mitarifamitaxi.taximetrousuario.activities.trips.TripSummaryActivity
import com.mitarifamitaxi.taximetrousuario.helpers.getAddressFromCoordinates
import com.mitarifamitaxi.taximetrousuario.helpers.isColombianHoliday
import com.mitarifamitaxi.taximetrousuario.helpers.isNightTime
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.models.Rates
import com.mitarifamitaxi.taximetrousuario.models.Trip
import com.mitarifamitaxi.taximetrousuario.models.UserLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.Locale
import java.util.concurrent.Executor
import androidx.core.graphics.scale
import androidx.core.net.toUri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import com.mitarifamitaxi.taximetrousuario.helpers.FirebaseStorageUtils
import com.mitarifamitaxi.taximetrousuario.helpers.putIfNotNull
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow


class TaximeterViewModel(context: Context, private val appViewModel: AppViewModel) :
    ViewModel() {

    private val appContext = context.applicationContext

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    sealed class NavigationEvent {
        object GoBack : NavigationEvent()
        object RequestBackgroundLocationPermission : NavigationEvent()
        object StartLocationUpdateNotification : NavigationEvent()
        object StopLocationUpdateNotification : NavigationEvent()
    }

    var startAddress by mutableStateOf("")
    var startLocation by mutableStateOf(UserLocation())

    var endAddress by mutableStateOf("")
    var endLocation by mutableStateOf(UserLocation())

    var isFabExpanded by mutableStateOf(false)

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback? = null
    private val executor: Executor = ContextCompat.getMainExecutor(context)

    var isSheetExpanded by mutableStateOf(true)

    // Taximeter values
    var total by mutableDoubleStateOf(0.0)
    var distanceMade by mutableDoubleStateOf(0.0)

    private val _units = mutableDoubleStateOf(0.0)

    var units: Double
        get() = _units.doubleValue
        set(value) {
            _units.doubleValue = value
            onUnitsChanged(value)
        }

    private val _rechargeUnits = mutableDoubleStateOf(0.0)
    var rechargeUnits: Double
        get() = _rechargeUnits.doubleValue
        set(value) {
            _rechargeUnits.doubleValue = value
            updateTotal(value)
        }

    var timeElapsed by mutableIntStateOf(0)
    var dragTimeElapsed by mutableIntStateOf(0)
    var formattedTime by mutableStateOf("0")

    var isAirportSurcharge by mutableStateOf(false)
    var isHolidaySurcharge by mutableStateOf(false)
    var isDoorToDoorSurcharge by mutableStateOf(false)

    val ratesObj = mutableStateOf(Rates())

    var isTaximeterStarted by mutableStateOf(false)
    var isMooving by mutableStateOf(false)

    var routeCoordinates by mutableStateOf<List<LatLng>>(emptyList())
    var currentPosition by mutableStateOf(startLocation)

    var previousLocation: Location? = null

    var fitCameraPosition by mutableStateOf(false)
    var isMapLoaded by mutableStateOf(false)
    var takeMapScreenshot by mutableStateOf(false)

    private var startTime by mutableStateOf("")
    private var endTime by mutableStateOf("")

    var currentSpeed by mutableIntStateOf(0)

    init {
        getCityRates(appViewModel.userData?.city)
    }

    fun validateLocationPermission() {
        val backgroundLocationGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (backgroundLocationGranted) {
            getCurrentLocation()
        } else {
            viewModelScope.launch {
                _navigationEvents.emit(NavigationEvent.RequestBackgroundLocationPermission)
            }
        }
    }

    fun requestBackgroundLocationPermission(activity: TaximeterActivity) {
        activity.backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation() {

        appViewModel.isLoading = true

        val cancellationTokenSource = CancellationTokenSource()

        val task: Task<Location> = fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        )

        task.addOnSuccessListener(executor) { location ->
            appViewModel.isLoading = false
            if (location != null) {
                currentPosition = UserLocation(
                    latitude = location.latitude,
                    longitude = location.longitude
                )
                startTaximeter()
            } else {
                FirebaseCrashlytics.getInstance()
                    .recordException(Exception("TaximeterViewModel location null"))
                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = appContext.getString(R.string.error_fetching_location)
                )
            }
        }.addOnFailureListener {
            appViewModel.isLoading = false
            FirebaseCrashlytics.getInstance().recordException(it)
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.something_went_wrong),
                message = appContext.getString(R.string.error_fetching_location)
            )
        }
    }

    private fun getCityRates(userCity: String?) {

        viewModelScope.launch {
            if (userCity != null) {
                try {
                    val firestore = FirebaseFirestore.getInstance()
                    val ratesQuerySnapshot = withContext(Dispatchers.IO) {
                        firestore.collection("rates")
                            .whereEqualTo("city", userCity)
                            .get()
                            .await()
                    }

                    if (!ratesQuerySnapshot.isEmpty) {
                        val cityRatesDoc = ratesQuerySnapshot.documents[0]
                        try {
                            ratesObj.value =
                                cityRatesDoc.toObject(Rates::class.java) ?: Rates()

                            if (ratesObj.value.validateHolidaySurcharge == true) {
                                validateSurcharges()
                            }

                        } catch (e: Exception) {
                            FirebaseCrashlytics.getInstance().recordException(e)
                            appViewModel.showMessage(
                                type = DialogType.ERROR,
                                title = appContext.getString(R.string.something_went_wrong),
                                message = appContext.getString(R.string.general_error),
                                onDismiss = {
                                    goBack()
                                }
                            )
                        }
                    } else {
                        FirebaseCrashlytics.getInstance()
                            .recordException(Exception("TaximeterViewModel ratesQuerySnapshot empty for city: $userCity"))
                        appViewModel.showMessage(
                            type = DialogType.ERROR,
                            title = appContext.getString(R.string.something_went_wrong),
                            message = appContext.getString(R.string.general_error),
                            onDismiss = {
                                goBack()
                            }
                        )
                    }
                } catch (e: Exception) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                    appViewModel.showMessage(
                        type = DialogType.ERROR,
                        title = appContext.getString(R.string.something_went_wrong),
                        message = appContext.getString(R.string.general_error),
                        onDismiss = {
                            goBack()
                        }
                    )
                }
            } else {
                FirebaseCrashlytics.getInstance()
                    .recordException(Exception("TaximeterViewModel userCity null"))
                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = appContext.getString(R.string.error_no_city_set),
                    onDismiss = {
                        goBack()
                    }
                )
            }
        }
    }

    private fun goBack() {
        viewModelScope.launch {
            _navigationEvents.emit(NavigationEvent.GoBack)
        }
    }

    private fun validateSurcharges() {
        if (isNightTime(
                ratesObj.value.nightHourSurcharge ?: 21,
                ratesObj.value.nighMinuteSurcharge ?: 0,
                ratesObj.value.morningHourSurcharge ?: 5,
                ratesObj.value.morningMinuteSurcharge ?: 30
            )
        ) {
            rechargeUnits += ratesObj.value.holidayRateUnits ?: 0.0
            isHolidaySurcharge = true
            return
        }

        if (isColombianHoliday()) {
            rechargeUnits += ratesObj.value.holidayRateUnits ?: 0.0
            isHolidaySurcharge = true
            return
        }
    }

    private fun onUnitsChanged(newValue: Double) {
        total = (newValue + rechargeUnits) * (ratesObj.value.unitPrice ?: 0.0)
    }

    private fun updateTotal(rechargeUnits: Double) {
        total = (rechargeUnits + units) * (ratesObj.value.unitPrice ?: 0.0)
    }

    fun startTaximeter() {
        isTaximeterStarted = true
        units = ratesObj.value.startRateUnits ?: 0.0
        startTime = Instant.now().toString()
        startTimer()
        startWatchLocation()

        viewModelScope.launch {
            _navigationEvents.emit(NavigationEvent.StartLocationUpdateNotification)
        }
    }

    fun showFinishConfirmation() {
        isSheetExpanded = true
        appViewModel.showMessage(
            type = DialogType.WARNING,
            title = appContext.getString(R.string.finish_your_trip),
            message = appContext.getString(R.string.you_are_about_to_finish),
            buttonText = appContext.getString(R.string.finish_trip),
            onButtonClicked = {
                stopTaximeter()
            }
        )
    }

    fun stopTaximeter() {
        viewModelScope.launch {
            _navigationEvents.emit(NavigationEvent.StopLocationUpdateNotification)
        }
        appViewModel.isLoading = true
        getAddressFromCoordinates(
            latitude = currentPosition.latitude ?: 0.0,
            longitude = currentPosition.longitude ?: 0.0,
            callbackSuccess = { address ->
                appViewModel.isLoading = false
                endAddress = address
                isTaximeterStarted = false
                stopWatchLocation()
                endTime = Instant.now().toString()
                fitCameraPosition = true
            },
            callbackError = {
                FirebaseCrashlytics.getInstance()
                    .recordException(Exception("TaximeterViewModel error on stop, ${it.message}"))
                appViewModel.isLoading = false
                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = appContext.getString(R.string.error_getting_address)
                )
            }
        )
    }

    private fun startTimer() {
        viewModelScope.launch {
            while (isTaximeterStarted) {
                timeElapsed++
                val hours = timeElapsed / 3600
                val minutes = (timeElapsed % 3600) / 60
                val seconds = timeElapsed % 60

                formattedTime = when {

                    timeElapsed < 3600 -> {
                        String.format(Locale.US, "%02d:%02d", minutes, seconds)
                    }

                    else -> {
                        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
                    }
                }

                if (!isMooving && isTaximeterStarted) {

                    dragTimeElapsed++
                    val waitTime = ratesObj.value.waitTime ?: 24

                    //Log.d("TaximeterViewModel", "waitTime: $waitTime")
                    //Log.d("TaximeterViewModel", "dragTimeElapsed: $dragTimeElapsed")
                    if (dragTimeElapsed >= waitTime) {
                        units += 1
                        dragTimeElapsed = 0
                    }
                }

                delay(1000)
            }
        }
    }

    private fun stopWatchLocation() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    private fun startWatchLocation() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(2000L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { location ->

                    currentPosition = UserLocation(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )

                    if (previousLocation == null || (previousLocation?.distanceTo(location)
                            ?: 0f) >= 25f
                    ) {
                        routeCoordinates = routeCoordinates + LatLng(
                            currentPosition.latitude ?: 0.0,
                            currentPosition.longitude ?: 0.0
                        )
                    }

                    val speedMetersPerSecond = location.speed
                    val speedKmPerHour = speedMetersPerSecond * 3.6

                    currentSpeed = speedKmPerHour.toInt()

                    //Log.d("TaximeterViewModel", "Location Speed: ${location.speed}")
                    //Log.d("TaximeterViewModel", "Speed: $speedKmPerHour")
                    //Log.d("TaximeterViewModel", "Drag Speed: ${ratesObj.value.dragSpeed}")
                    //Log.d("TaximeterViewModel", "Condition: ${speedKmPerHour > (ratesObj.value.dragSpeed ?: 0.0)}")

                    if (speedKmPerHour > (ratesObj.value.dragSpeed ?: 0.0)) {
                        isMooving = true
                        dragTimeElapsed = 0
                        val distanceCovered: Float = previousLocation?.distanceTo(location) ?: 0f
                        distanceMade += distanceCovered.toDouble()

                        val additionalUnits = distanceCovered / (ratesObj.value.meters ?: 0)
                        units += additionalUnits

                    } else {
                        isMooving = false
                    }

                    previousLocation = location

                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    fun mapScreenshotReady(bitmap: Bitmap, onIntentReady: (Intent) -> Unit) {

        val newWidth = bitmap.width / 1.3
        val newHeight = bitmap.height / 1.3
        val scaledBitmap =
            bitmap.scale(newWidth.toInt(), newHeight.toInt())

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val compressedBytes = outputStream.toByteArray()
        val compressedBitmap =
            BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)

        val baseUnits =
            if (units < (ratesObj.value.minimumRateUnits ?: 0.0)) ratesObj.value.minimumRateUnits
                ?: 0.0 else units

        val tripObj = Trip(
            startAddress = startAddress,
            startCoords = startLocation,
            endAddress = endAddress,
            endCoords = currentPosition,
            startHour = startTime,
            endHour = endTime,
            units = baseUnits + rechargeUnits,
            baseUnits = baseUnits,
            rechargeUnits = rechargeUnits,
            total = (baseUnits + rechargeUnits) * (ratesObj.value.unitPrice ?: 0.0),
            baseRate = baseUnits * (ratesObj.value.unitPrice ?: 0.0),
            distance = distanceMade,
            airportSurchargeEnabled = isAirportSurcharge,
            airportSurcharge = if (isAirportSurcharge) (ratesObj.value.airportRateUnits
                ?: 0.0) * (ratesObj.value.unitPrice ?: 0.0) else null,
            holidayOrNightSurchargeEnabled = isHolidaySurcharge,
            holidayOrNightSurcharge = if (isHolidaySurcharge) (ratesObj.value.holidayRateUnits
                ?: 0.0) * (ratesObj.value.unitPrice ?: 0.0) else null,
            doorToDoorSurchargeEnabled = isDoorToDoorSurcharge,
            doorToDoorSurcharge = if (isDoorToDoorSurcharge) (ratesObj.value.doorToDoorRateUnits
                ?: 0.0) * (ratesObj.value.unitPrice ?: 0.0) else null,
            currency = appViewModel.userData?.countryCurrency,
            routeImageLocal = compressedBitmap
        )

        saveTripData(tripData = tripObj) {
            val tripJson = Gson().toJson(tripObj)
            val intent = Intent(appContext, TripSummaryActivity::class.java)
            intent.putExtra("trip_data", tripJson)
            onIntentReady(intent)
        }

    }

    fun openGoogleMapsApp(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double,
        onIntentReady: (Intent) -> Unit
    ) {
        val url =
            "comgooglemaps://?saddr=$originLat,$originLng&daddr=$destLat,$destLng&directionsmode=driving"
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            intent.setPackage("com.google.android.apps.maps")
            onIntentReady(intent)

        } catch (e: Exception) {
            val webUrl =
                "https://www.google.com/maps/dir/?api=1&origin=$originLat,$originLng&destination=$destLat,$destLng&travelmode=driving"
            val webIntent = Intent(Intent.ACTION_VIEW, webUrl.toUri())
            onIntentReady(webIntent)

        }
    }

    fun openWazeApp(destLat: Double, destLng: Double, onIntentReady: (Intent) -> Unit) {
        val wazeUrl = "waze://?ll=$destLat,$destLng&navigate=yes"
        try {
            val intent = Intent(Intent.ACTION_VIEW, wazeUrl.toUri())
            intent.setPackage("com.waze")
            onIntentReady(intent)
        } catch (e: Exception) {
            val webUrl = "https://waze.com/ul?ll=$destLat,$destLng&navigate=yes"
            val webIntent = Intent(Intent.ACTION_VIEW, webUrl.toUri())
            onIntentReady(webIntent)
        }
    }

    fun updateSheetStateFromUI(isExpanded: Boolean) {
        isSheetExpanded = isExpanded
    }

    // Upload image and save trip


    fun saveTripData(tripData: Trip, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {

                appViewModel.isLoading = true

                val imageUrl = tripData.routeImageLocal?.let {
                    FirebaseStorageUtils.uploadImage("trips", it)
                }

                val tripDataReq = mutableMapOf<String, Any?>().apply {
                    putIfNotNull("userId", appViewModel.userData?.id)
                    putIfNotNull("startCoords", tripData.startCoords)
                    putIfNotNull("endCoords", tripData.endCoords)
                    putIfNotNull("startHour", tripData.startHour)
                    putIfNotNull("endHour", tripData.endHour)
                    putIfNotNull("distance", tripData.distance)
                    putIfNotNull("units", tripData.units)
                    putIfNotNull("baseUnits", tripData.baseUnits)
                    putIfNotNull("rechargeUnits", tripData.rechargeUnits)
                    putIfNotNull("total", tripData.total)
                    putIfNotNull("baseRate", tripData.baseRate)
                    putIfNotNull("isAirportSurcharge", tripData.airportSurchargeEnabled)
                    putIfNotNull("airportSurcharge", tripData.airportSurcharge)
                    putIfNotNull("isHolidaySurcharge", tripData.holidaySurchargeEnabled)
                    putIfNotNull("holidaySurcharge", tripData.holidaySurcharge)
                    putIfNotNull("isDoorToDoorSurcharge", tripData.doorToDoorSurchargeEnabled)
                    putIfNotNull("doorToDoorSurcharge", tripData.doorToDoorSurcharge)
                    putIfNotNull("isNightSurcharge", tripData.nightSurchargeEnabled)
                    putIfNotNull("nightSurcharge", tripData.nightSurcharge)
                    putIfNotNull(
                        "isHolidayOrNightSurcharge",
                        tripData.holidayOrNightSurchargeEnabled
                    )
                    putIfNotNull("holidayOrNightSurcharge", tripData.holidayOrNightSurcharge)
                    putIfNotNull("currency", appViewModel.userData?.countryCurrency)
                    putIfNotNull("startAddress", tripData.startAddress)
                    putIfNotNull("endAddress", tripData.endAddress)
                    putIfNotNull("routeImage", imageUrl)
                }

                FirebaseFirestore.getInstance().collection("trips").add(tripDataReq).await()
                appViewModel.isLoading = false

                onSuccess()

            } catch (error: Exception) {
                appViewModel.isLoading = false
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
        if (modelClass.isAssignableFrom(TaximeterViewModel::class.java)) {
            return TaximeterViewModel(context, appViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
