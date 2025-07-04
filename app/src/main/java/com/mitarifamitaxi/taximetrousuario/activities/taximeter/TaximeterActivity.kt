package com.mitarifamitaxi.taximetrousuario.activities.taximeter

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.activities.BaseActivity
import com.mitarifamitaxi.taximetrousuario.activities.pqrs.PqrsActivity
import com.mitarifamitaxi.taximetrousuario.activities.sos.SosActivity
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomButton
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomCheckBox
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomSizedMarker
import com.mitarifamitaxi.taximetrousuario.components.ui.FloatingActionButtonRoutes
import com.mitarifamitaxi.taximetrousuario.components.ui.SpeedLimitBox
import com.mitarifamitaxi.taximetrousuario.components.ui.TaximeterInfoRow
import com.mitarifamitaxi.taximetrousuario.components.ui.TopHeaderView
import com.mitarifamitaxi.taximetrousuario.components.ui.WaitTimeBox
import com.mitarifamitaxi.taximetrousuario.helpers.K
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily
import com.mitarifamitaxi.taximetrousuario.helpers.NotificationForegroundService
import com.mitarifamitaxi.taximetrousuario.helpers.calculateBearing
import com.mitarifamitaxi.taximetrousuario.helpers.formatDigits
import com.mitarifamitaxi.taximetrousuario.helpers.formatNumberWithDots
import com.mitarifamitaxi.taximetrousuario.helpers.getShortAddress
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.models.LocalUser
import com.mitarifamitaxi.taximetrousuario.states.AppState
import com.mitarifamitaxi.taximetrousuario.states.TaximeterState
import com.mitarifamitaxi.taximetrousuario.viewmodels.taximeter.TaximeterViewModel
import com.mitarifamitaxi.taximetrousuario.viewmodels.taximeter.TaximeterViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Timer
import kotlin.concurrent.schedule

class TaximeterActivity : BaseActivity() {

    private val viewModel: TaximeterViewModel by viewModels {
        TaximeterViewModelFactory(this, appViewModel)
    }

    private val serviceIntent by lazy {
        Intent(this, NotificationForegroundService::class.java)
    }

    private fun observeViewModelEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationEvents.collect { event ->
                    when (event) {
                        is TaximeterViewModel.NavigationEvent.GoBack -> {
                            finish()
                        }

                        is TaximeterViewModel.NavigationEvent.RequestBackgroundLocationPermission -> {
                            showMessageBackgroundLocationPermissionRequired()
                        }

                        is TaximeterViewModel.NavigationEvent.StartLocationUpdateNotification -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                startForegroundServiceIfNeeded()
                            }
                        }

                        is TaximeterViewModel.NavigationEvent.StopLocationUpdateNotification -> {
                            stopService(serviceIntent)
                        }
                    }
                }
            }
        }
    }

    val backgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.getCurrentLocation()
        } else {
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = getString(R.string.permission_required),
                message = getString(R.string.background_location_permission_error)
            )
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startForegroundService(serviceIntent)
        } else {
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = getString(R.string.permission_required),
                message = getString(R.string.notification_permission_denied)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeViewModelEvents()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        viewModel.setInitialData(
            startAddress = intent.getStringExtra("start_address"),
            startLocationJson = intent.getStringExtra("start_location"),
            endAddress = intent.getStringExtra("end_address"),
            endLocationJson = intent.getStringExtra("end_location")
        )

        Timer().schedule(500) {
            viewModel.validateLocationPermission()
            this.cancel()
        }
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    fun showMessageBackgroundLocationPermissionRequired() {
        appViewModel.showMessage(
            type = DialogType.WARNING,
            title = getString(R.string.we_need_access_to_your_location),
            message = getString(R.string.access_location_message),
            buttonText = getString(R.string.grant_permission),
            onButtonClicked = {
                viewModel.requestBackgroundLocationPermission(this)
            }
        )
    }

    private fun startForegroundServiceIfNeeded() {
        startForegroundService(serviceIntent)
    }

    @Composable
    override fun Content() {
        val appState by appViewModel.uiState.collectAsState()
        val taximeterState by viewModel.uiState.collectAsState()

        TaximeterScreen(
            appState = appState,
            taximeterState = taximeterState,
            onFinish = { if (taximeterState.isTaximeterStarted) viewModel.showFinishConfirmation() else finish() },
            onStart = { viewModel.validateLocationPermission() },
            onMapLoaded = { viewModel.setIsMapLoaded(true) },
            onScreenshotReady = { bitmap ->
                viewModel.mapScreenshotReady(bitmap) { intent ->
                    startActivity(
                        intent
                    )
                }
            },
            onToggleFab = { viewModel.toggleFab() },
            onOpenWaze = { viewModel.openWazeApp { startActivity(it) } },
            onOpenGoogleMaps = { viewModel.openGoogleMapsApp { startActivity(it) } },
            onDoorToDoorChange = { viewModel.setDoorToDoorSurcharge(it) },
            onAirportChange = { viewModel.setAirportSurcharge(it) },
            onHolidayChange = { viewModel.setHolidaySurcharge(it) },
            onSetTakeScreenshot = { viewModel.setTakeMapScreenshot(it) }
        )
        BackHandler(enabled = true) {
            if (taximeterState.isTaximeterStarted) {
                viewModel.showFinishConfirmation()
            } else {
                finish()
            }
        }
    }

    @OptIn(MapsComposeExperimentalApi::class)
    @Composable
    fun TaximeterScreen(
        appState: AppState,
        taximeterState: TaximeterState,
        onFinish: () -> Unit,
        onStart: () -> Unit,
        onMapLoaded: () -> Unit,
        onScreenshotReady: (Bitmap) -> Unit,
        onToggleFab: () -> Unit,
        onOpenWaze: () -> Unit,
        onOpenGoogleMaps: () -> Unit,
        onDoorToDoorChange: (Boolean) -> Unit,
        onAirportChange: (Boolean) -> Unit,
        onHolidayChange: (Boolean) -> Unit,
        onSetTakeScreenshot: (Boolean) -> Unit
    ) {

        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        val mapHeight = screenHeight * 0.3f

        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(
                LatLng(
                    appState.userLocation?.latitude ?: 4.60971,
                    appState.userLocation?.longitude ?: -74.08175
                ), 15f
            )
        }

        LaunchedEffect(taximeterState.currentPosition, taximeterState.routeCoordinates) {
            val targetLatLng = LatLng(
                taximeterState.currentPosition.latitude ?: 0.0,
                taximeterState.currentPosition.longitude ?: 0.0
            )

            if (taximeterState.routeCoordinates.size > 1) {
                val previousPosition =
                    taximeterState.routeCoordinates[taximeterState.routeCoordinates.size - 2]
                val newRotation = calculateBearing(previousPosition, targetLatLng)
                val camPos = CameraPosition.builder(cameraPositionState.position)
                    .target(targetLatLng)
                    .zoom(15f)
                    .bearing(newRotation)
                    .build()
                cameraPositionState.animate(update = CameraUpdateFactory.newCameraPosition(camPos))
            }
        }

        LaunchedEffect(taximeterState.fitCameraPosition) {
            if (taximeterState.fitCameraPosition) {
                if (taximeterState.routeCoordinates.size > 1) {
                    val boundsBuilder = LatLngBounds.builder()
                    taximeterState.routeCoordinates.forEach { boundsBuilder.include(it) }
                    val bounds = boundsBuilder.build()
                    val padding = 120
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngBounds(
                            bounds,
                            padding
                        )
                    )
                }
                delay(1000L)
                onSetTakeScreenshot(true)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopHeaderView(
                    title = stringResource(id = R.string.taximeter),
                    leadingIcon = Icons.Filled.ChevronLeft,
                    onClickLeading = onFinish
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(mapHeight)
                ) {
                    GoogleMap(
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(zoomControlsEnabled = false),
                        modifier = Modifier.fillMaxSize(),
                        onMapLoaded = onMapLoaded
                    ) {
                        if (taximeterState.routeCoordinates.isNotEmpty()) {
                            Polyline(
                                points = taximeterState.routeCoordinates,
                                color = colorResource(id = R.color.main),
                                width = 10f
                            )
                        }

                        if (taximeterState.startAddress.isNotEmpty()) {
                            CustomSizedMarker(
                                position = LatLng(
                                    taximeterState.startLocation.latitude ?: 0.0,
                                    taximeterState.startLocation.longitude ?: 0.0
                                ),
                                drawableRes = R.drawable.flag_start,
                                width = 60,
                                height = 60
                            )
                        }

                        CustomSizedMarker(
                            position = LatLng(
                                taximeterState.currentPosition.latitude ?: 0.0,
                                taximeterState.currentPosition.longitude ?: 0.0
                            ),
                            drawableRes = R.drawable.taxi_marker,
                            width = 27,
                            height = 57
                        )

                        if (taximeterState.isMapLoaded && taximeterState.takeMapScreenshot) {
                            MapEffect { map ->
                                map.snapshot { snapshot ->
                                    if (snapshot != null) {
                                        onScreenshotReady(snapshot)
                                    }
                                }
                            }
                        }
                    }

                    WaitTimeBox(
                        time = "${taximeterState.dragTimeElapsed}",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 12.dp, top = 12.dp)
                    )

                    SpeedLimitBox(
                        speed = taximeterState.currentSpeed,
                        speedLimit = taximeterState.rates.speedLimit ?: 0,
                        units = taximeterState.rates.speedUnits ?: "km/h",
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 12.dp, bottom = 12.dp)
                    )

                    FloatingActionButtonRoutes(
                        expanded = taximeterState.isFabExpanded,
                        onMainFabClick = onToggleFab,
                        onAction1Click = onOpenWaze,
                        onAction2Click = onOpenGoogleMaps,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 12.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(K.GENERAL_PADDING)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "$ ${
                            taximeterState.total.toInt().formatNumberWithDots()
                        } ${appState.userData?.countryCurrency}",
                        color = colorResource(id = R.color.main),
                        fontSize = 36.sp,
                        fontFamily = MontserratFamily,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = stringResource(id = R.string.price_to_pay),
                        color = colorResource(id = R.color.gray1),
                        fontSize = 15.sp,
                        fontFamily = MontserratFamily,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    )

                    TaximeterInfoRow(
                        title = stringResource(id = R.string.distance_made),
                        value = "${(taximeterState.distanceMade / 1000).formatDigits(1)} KM",
                    )
                    TaximeterInfoRow(
                        title = stringResource(id = R.string.units_base),
                        value = taximeterState.units.formatNumberWithDots().toString()
                    )
                    TaximeterInfoRow(
                        title = stringResource(id = R.string.units_recharge),
                        value = taximeterState.rechargeUnits.toInt().toString()
                    )
                    TaximeterInfoRow(
                        title = stringResource(id = R.string.time_trip),
                        value = taximeterState.formattedTime
                    )

                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = colorResource(id = R.color.main),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = getShortAddress(taximeterState.endAddress),
                                fontFamily = MontserratFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = colorResource(id = R.color.gray1),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(colorResource(id = R.color.gray2))
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(top = 10.dp)
                    ) {
                        if (taximeterState.rates.doorToDoorRateUnits != null && taximeterState.rates.doorToDoorRateUnits != 0.0) {
                            CustomCheckBox(
                                text = stringResource(id = R.string.door_to_door_surcharge).replace(
                                    ":",
                                    ""
                                ),
                                checked = taximeterState.isDoorToDoorSurcharge,
                                isEnabled = taximeterState.isTaximeterStarted,
                                onValueChange = onDoorToDoorChange
                            )
                        }
                        if (taximeterState.rates.airportRateUnits != null && taximeterState.rates.airportRateUnits != 0.0) {
                            CustomCheckBox(
                                text = stringResource(id = R.string.airport_surcharge).replace(
                                    ":",
                                    ""
                                ),
                                checked = taximeterState.isAirportSurcharge,
                                isEnabled = taximeterState.isTaximeterStarted,
                                onValueChange = onAirportChange
                            )
                        }
                        if (taximeterState.rates.holidayRateUnits != null && taximeterState.rates.holidayRateUnits != 0.0) {
                            CustomCheckBox(
                                text = stringResource(id = R.string.holiday_surcharge).replace(
                                    ":",
                                    ""
                                ),
                                checked = taximeterState.isHolidaySurcharge,
                                isEnabled = taximeterState.isTaximeterStarted,
                                onValueChange = onHolidayChange
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 15.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            CustomButton(
                                text = stringResource(id = R.string.sos).uppercase(),
                                onClick = {
                                    startActivity(
                                        Intent(
                                            this@TaximeterActivity,
                                            SosActivity::class.java
                                        )
                                    )
                                },
                                color = colorResource(id = R.color.red1),
                                leadingIcon = Icons.Rounded.WarningAmber
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CustomButton(
                                text = stringResource(id = R.string.pqrs).uppercase(),
                                onClick = {
                                    startActivity(
                                        Intent(
                                            this@TaximeterActivity,
                                            PqrsActivity::class.java
                                        )
                                    )
                                },
                                color = colorResource(id = R.color.blue2),
                                leadingIcon = Icons.AutoMirrored.Outlined.Chat
                            )
                        }
                    }

                    CustomButton(
                        text = stringResource(id = if (taximeterState.isTaximeterStarted) R.string.finish_trip else R.string.start_trip).uppercase(),
                        onClick = { if (taximeterState.isTaximeterStarted) onFinish() else onStart() },
                        color = colorResource(id = if (taximeterState.isTaximeterStarted) R.color.gray1 else R.color.main),
                        leadingIcon = if (taximeterState.isTaximeterStarted) Icons.Default.Close else Icons.Default.PlayArrow
                    )
                }
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun ScreenPreview() {
        val sampleTaximeterState = TaximeterState(
            total = 12500.0,
            distanceMade = 5234.0,
            units = 100.0,
            rechargeUnits = 25.0,
            formattedTime = "15:30",
            startAddress = "Calle Falsa 123",
            endAddress = "Avenida Siempre Viva 742",
            currentSpeed = 45,
            isTaximeterStarted = true,
            isHolidaySurcharge = true
        )
        val sampleAppState = AppState(
            userData = LocalUser(
                id = "12345",
                firstName = "John Doe",
                countryCurrency = "COP"
            )
        )

        TaximeterScreen(
            appState = sampleAppState,
            taximeterState = sampleTaximeterState,
            onFinish = {},
            onStart = {},
            onMapLoaded = {},
            onScreenshotReady = {},
            onToggleFab = {},
            onOpenWaze = {},
            onOpenGoogleMaps = {},
            onDoorToDoorChange = {},
            onAirportChange = {},
            onHolidayChange = {},
            onSetTakeScreenshot = {}
        )
    }
}