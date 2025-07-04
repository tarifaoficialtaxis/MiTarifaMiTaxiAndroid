package com.mitarifamitaxi.taximetrousuario.activities.taximeter

import android.Manifest
import android.content.Intent
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.gson.Gson
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
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily
import com.mitarifamitaxi.taximetrousuario.helpers.NotificationForegroundService
import com.mitarifamitaxi.taximetrousuario.helpers.calculateBearing
import com.mitarifamitaxi.taximetrousuario.helpers.formatDigits
import com.mitarifamitaxi.taximetrousuario.helpers.formatNumberWithDots
import com.mitarifamitaxi.taximetrousuario.helpers.getShortAddress
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.models.UserLocation
import com.mitarifamitaxi.taximetrousuario.states.AppState
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

        val startAddress = intent.getStringExtra("start_address")
        startAddress?.let {
            viewModel.startAddress = it
        }
        val startLocation = intent.getStringExtra("start_location")
        startLocation?.let {
            viewModel.startLocation = Gson().fromJson(it, UserLocation::class.java)
        }

        val endAddress = intent.getStringExtra("end_address")
        endAddress?.let {
            viewModel.endAddress = it
        }
        val endLocation = intent.getStringExtra("end_location")
        endLocation?.let {
            viewModel.endLocation = Gson().fromJson(it, UserLocation::class.java)
        }

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

        TaximeterScreen(
            appState = appState
        )
        BackHandler(enabled = true) {
            if (viewModel.isTaximeterStarted) {
                viewModel.showFinishConfirmation()
            } else {
                finish()
            }
        }
    }

    @OptIn(
        ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class,
        MapsComposeExperimentalApi::class
    )
    @Composable
    fun TaximeterScreen(
        appState: AppState
    ) {

        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        val mapHeight = screenHeight * 0.36f

        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(
                LatLng(
                    appViewModel.uiState.value.userLocation?.latitude ?: 4.60971,
                    appViewModel.uiState.value.userLocation?.longitude ?: -74.08175
                ), 15f
            )
        }


        LaunchedEffect(viewModel.currentPosition, viewModel.routeCoordinates) {
            val targetLatLng = LatLng(
                viewModel.currentPosition.latitude ?: 0.0,
                viewModel.currentPosition.longitude ?: 0.0
            )

            if (viewModel.routeCoordinates.size > 1) {
                val previousPosition =
                    viewModel.routeCoordinates[viewModel.routeCoordinates.size - 2]
                val newRotation = calculateBearing(
                    previousPosition, LatLng(
                        viewModel.currentPosition.latitude ?: 0.0,
                        viewModel.currentPosition.longitude ?: 0.0
                    )
                )

                val camPos = CameraPosition.builder(cameraPositionState.position)
                    .target(targetLatLng)
                    .zoom(15f)
                    .bearing(newRotation)
                    .build()

                cameraPositionState.animate(
                    update = CameraUpdateFactory.newCameraPosition(camPos)
                )
            }


        }

        LaunchedEffect(viewModel.fitCameraPosition) {
            if (viewModel.fitCameraPosition && viewModel.routeCoordinates.size > 1) {
                val boundsBuilder = LatLngBounds.builder()
                viewModel.routeCoordinates.forEach { boundsBuilder.include(it) }
                val bounds = boundsBuilder.build()
                val padding = 120
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(bounds, padding)
                )
                delay(1000L)
                viewModel.takeMapScreenshot = true
            } else if (viewModel.fitCameraPosition) {
                viewModel.takeMapScreenshot = true
            }
        }

        Box(modifier = Modifier.Companion.fillMaxSize()) {

            Column(
                modifier = Modifier.Companion
                    .fillMaxWidth()
            ) {
                TopHeaderView(
                    title = stringResource(id = R.string.taximeter),
                    leadingIcon = Icons.Filled.ChevronLeft,
                    onClickLeading = {
                        if (viewModel.isTaximeterStarted) {
                            viewModel.showFinishConfirmation()
                        }
                    }
                )

                GoogleMap(
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(mapHeight),
                    onMapLoaded = { viewModel.isMapLoaded = true }
                ) {

                    if (viewModel.routeCoordinates.isNotEmpty()) {
                        Polyline(
                            points = viewModel.routeCoordinates,
                            color = colorResource(id = R.color.main),
                            width = 10f
                        )
                    }


                    if (viewModel.startAddress.isNotEmpty()) {
                        CustomSizedMarker(
                            position = LatLng(
                                viewModel.startLocation.latitude ?: 0.0,
                                viewModel.startLocation.longitude ?: 0.0
                            ),
                            drawableRes = R.drawable.flag_start,
                            width = 60,
                            height = 60
                        )

                    }

                    CustomSizedMarker(
                        position = LatLng(
                            viewModel.currentPosition.latitude ?: 0.0,
                            viewModel.currentPosition.longitude ?: 0.0
                        ),
                        drawableRes = R.drawable.taxi_marker,
                        width = 27,
                        height = 57
                    )

                    if (viewModel.isMapLoaded && viewModel.takeMapScreenshot) {
                        MapEffect { map ->
                            map.snapshot { snapshot ->
                                if (snapshot != null) {
                                    viewModel.mapScreenshotReady(snapshot) { intent ->
                                        startActivity(intent)
                                    }
                                }
                            }
                        }
                    }

                }

                Column(
                    modifier = Modifier.Companion
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {

                    Text(
                        text = "$ ${
                            viewModel.total.toInt().formatNumberWithDots()
                        } ${appState.userData?.countryCurrency}",
                        color = colorResource(id = R.color.main),
                        fontSize = 36.sp,
                        fontFamily = MontserratFamily,
                        fontWeight = FontWeight.Companion.Bold,
                        textAlign = TextAlign.Companion.Center,
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                    )

                    Text(
                        text = stringResource(id = R.string.price_to_pay),
                        color = colorResource(id = R.color.gray1),
                        fontSize = 15.sp,
                        fontFamily = MontserratFamily,
                        fontWeight = FontWeight.Companion.Bold,
                        textAlign = TextAlign.Companion.Center,
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    )

                    TaximeterInfoRow(
                        title = stringResource(id = R.string.distance_made),
                        value = "${(viewModel.distanceMade / 1000).formatDigits(1)} KM",
                    )

                    TaximeterInfoRow(
                        title = stringResource(id = R.string.units_base),
                        value = viewModel.units.toInt().toString()
                    )

                    TaximeterInfoRow(
                        title = stringResource(id = R.string.units_recharge),
                        value = viewModel.rechargeUnits.toInt().toString()
                    )

                    TaximeterInfoRow(
                        title = stringResource(id = R.string.time_trip),
                        value = viewModel.formattedTime
                    )

                    Column {

                        Row(
                            modifier = Modifier.Companion
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        ) {

                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = colorResource(id = R.color.main),
                                modifier = Modifier.Companion.size(20.dp)
                            )

                            Text(
                                text = getShortAddress(viewModel.endAddress),
                                fontFamily = MontserratFamily,
                                fontWeight = FontWeight.Companion.Normal,
                                fontSize = 12.sp,
                                color = colorResource(id = R.color.gray1),
                            )

                        }

                        Box(
                            modifier = Modifier.Companion
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(colorResource(id = R.color.gray2))
                        )

                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.Companion
                            .padding(top = 10.dp)
                    ) {


                        if (viewModel.ratesObj.value.doorToDoorRateUnits != null && viewModel.ratesObj.value.doorToDoorRateUnits != 0.0) {
                            CustomCheckBox(
                                text = stringResource(id = R.string.door_to_door_surcharge).replace(
                                    ":",
                                    ""
                                ),
                                checked = viewModel.isDoorToDoorSurcharge,
                                isEnabled = viewModel.isTaximeterStarted,
                                onValueChange = {
                                    viewModel.isDoorToDoorSurcharge = it
                                    if (it) {
                                        viewModel.rechargeUnits += viewModel.ratesObj.value.doorToDoorRateUnits
                                            ?: 0.0
                                    } else {
                                        viewModel.rechargeUnits -= viewModel.ratesObj.value.doorToDoorRateUnits
                                            ?: 0.0
                                    }
                                }
                            )
                        }

                        if (viewModel.ratesObj.value.airportRateUnits != null && viewModel.ratesObj.value.airportRateUnits != 0.0) {
                            CustomCheckBox(
                                text = stringResource(id = R.string.airport_surcharge).replace(
                                    ":",
                                    ""
                                ),
                                checked = viewModel.isAirportSurcharge,
                                isEnabled = viewModel.isTaximeterStarted,
                                onValueChange = {
                                    viewModel.isAirportSurcharge = it
                                    if (it) {
                                        viewModel.rechargeUnits += viewModel.ratesObj.value.airportRateUnits
                                            ?: 0.0
                                    } else {
                                        viewModel.rechargeUnits -= viewModel.ratesObj.value.airportRateUnits
                                            ?: 0.0
                                    }
                                }
                            )
                        }

                        if (viewModel.ratesObj.value.holidayRateUnits != null && viewModel.ratesObj.value.holidayRateUnits != 0.0) {
                            CustomCheckBox(
                                text = stringResource(id = R.string.holiday_surcharge).replace(
                                    ":",
                                    ""
                                ),
                                checked = viewModel.isHolidaySurcharge,
                                isEnabled = false,
                                onValueChange = {}
                            )
                        }

                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                            .padding(vertical = 15.dp)
                    ) {
                        Box(
                            modifier = Modifier.Companion
                                .weight(1f)
                        ) {
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

                        Box(
                            modifier = Modifier.Companion
                                .weight(1f)
                        ) {
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
                        text = stringResource(id = if (viewModel.isTaximeterStarted) R.string.finish_trip else R.string.start_trip).uppercase(),
                        onClick = { if (viewModel.isTaximeterStarted) viewModel.showFinishConfirmation() else viewModel.validateLocationPermission() },
                        color = colorResource(id = if (viewModel.isTaximeterStarted) R.color.gray1 else R.color.main),
                        leadingIcon = if (viewModel.isTaximeterStarted) Icons.Default.Close else Icons.Default.PlayArrow
                    )

                }

            }
        }

        WaitTimeBox(
            time = "${viewModel.dragTimeElapsed}",
            modifier = Modifier.Companion
                .offset(y = 75.dp)
                .padding(end = 12.dp)
        )

        SpeedLimitBox(
            speed = viewModel.currentSpeed,
            speedLimit = viewModel.ratesObj.value.speedLimit ?: 0,
            units = viewModel.ratesObj.value.speedUnits ?: "km/h",
            modifier = Modifier.Companion
                .padding(start = 16.dp)
        )

        FloatingActionButtonRoutes(
            expanded = viewModel.isFabExpanded,
            onMainFabClick = { viewModel.isFabExpanded = !viewModel.isFabExpanded },
            onAction1Click = {
                viewModel.openWazeApp(
                    viewModel.endLocation.latitude ?: 0.0,
                    viewModel.endLocation.longitude ?: 0.0,
                    onIntentReady = { startActivity(it) }
                )
            },
            onAction2Click = {
                viewModel.openGoogleMapsApp(
                    viewModel.startLocation.latitude ?: 0.0,
                    viewModel.startLocation.longitude ?: 0.0,
                    viewModel.endLocation.latitude ?: 0.0,
                    viewModel.endLocation.longitude ?: 0.0,
                    onIntentReady = { startActivity(it) }
                )
            },
            modifier = Modifier.Companion
                .padding(end = 16.dp)
        )

    }
}


