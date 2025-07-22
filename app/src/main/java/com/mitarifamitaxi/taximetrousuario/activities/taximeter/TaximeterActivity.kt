package com.mitarifamitaxi.taximetrousuario.activities.taximeter

import SoundButton
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.mitarifamitaxi.taximetrousuario.components.ui.SpeedLimitBox
import com.mitarifamitaxi.taximetrousuario.components.ui.TaximeterInfoRow
import com.mitarifamitaxi.taximetrousuario.components.ui.TopHeaderView
import com.mitarifamitaxi.taximetrousuario.components.ui.WaitTimeBox
import com.mitarifamitaxi.taximetrousuario.helpers.K
import com.mitarifamitaxi.taximetrousuario.helpers.LocationUpdatesService
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily
import com.mitarifamitaxi.taximetrousuario.helpers.calculateBearing
import com.mitarifamitaxi.taximetrousuario.helpers.formatDigits
import com.mitarifamitaxi.taximetrousuario.helpers.formatNumberWithDots
import com.mitarifamitaxi.taximetrousuario.models.LocalUser
import com.mitarifamitaxi.taximetrousuario.states.AppState
import com.mitarifamitaxi.taximetrousuario.states.TaximeterState
import com.mitarifamitaxi.taximetrousuario.viewmodels.taximeter.TaximeterViewModel
import com.mitarifamitaxi.taximetrousuario.viewmodels.taximeter.TaximeterViewModelFactory
import kotlinx.coroutines.launch

class TaximeterActivity : BaseActivity() {

    private val viewModel: TaximeterViewModel by viewModels {
        TaximeterViewModelFactory(this, appViewModel)
    }

    @SuppressLint("ImplicitSamInstance")
    private fun observeViewModelEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationEvents.collect { event ->
                    when (event) {
                        is TaximeterViewModel.TaximeterViewModelEvent.GoBack -> {
                            finish()
                        }
                        is TaximeterViewModel.TaximeterViewModelEvent.StartForegroundService -> {
                            startForegroundService(
                                Intent(this@TaximeterActivity, LocationUpdatesService::class.java)
                            )
                        }
                        is TaximeterViewModel.TaximeterViewModelEvent.StopForegroundService -> {
                            stopService(
                                Intent(this@TaximeterActivity, LocationUpdatesService::class.java)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeViewModelEvents()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        viewModel.validateLocationPermission()
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }


    @Composable
    override fun Content() {
        val appState by appViewModel.uiState.collectAsState()
        val taximeterState by viewModel.uiState.collectAsState()

        TaximeterScreen(
            appState = appState,
            taximeterState = taximeterState,
            onBack = { viewModel.showBackConfirmation() },
            onFinish = { viewModel.showFinishConfirmation() },
            onScreenshotReady = { bitmap ->
                viewModel.mapScreenshotReady(bitmap) { intent ->
                    startActivity(
                        intent
                    )
                }
            },
            onSetTakeScreenshot = {
                viewModel.setTakeMapScreenshot(it)
            }
        )
        BackHandler(enabled = true) {
            viewModel.showBackConfirmation()
        }
    }

    @OptIn(MapsComposeExperimentalApi::class)
    @Composable
    fun TaximeterScreen(
        appState: AppState,
        taximeterState: TaximeterState,
        onBack: () -> Unit,
        onFinish: () -> Unit,
        onScreenshotReady: (Bitmap) -> Unit,
        onSetTakeScreenshot: (Boolean) -> Unit
    ) {

        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        val mapHeight = screenHeight * 0.3f

        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(
                LatLng(
                    taximeterState.currentLocation.latitude ?: 0.0,
                    taximeterState.currentLocation.longitude ?: 0.0
                ), 15f
            )
        }

        LaunchedEffect(taximeterState.startLocation) {
            val targetLatLng = LatLng(
                taximeterState.startLocation.latitude ?: 0.0,
                taximeterState.startLocation.longitude ?: 0.0
            )

            val camPos = CameraPosition.builder(cameraPositionState.position)
                .target(targetLatLng)
                .zoom(15f)
                .build()
            cameraPositionState.animate(update = CameraUpdateFactory.newCameraPosition(camPos))
        }

        LaunchedEffect(taximeterState.currentLocation, taximeterState.routeCoordinates) {
            val targetLatLng = LatLng(
                taximeterState.currentLocation.latitude ?: 0.0,
                taximeterState.currentLocation.longitude ?: 0.0
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
                        update = CameraUpdateFactory.newLatLngBounds(
                            bounds,
                            padding
                        )
                    )
                    onSetTakeScreenshot(true)
                } else {
                    onSetTakeScreenshot(true)
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopHeaderView(
                    title = stringResource(id = R.string.mitarifamitaxi),
                    upperCaseTitle = false,
                    titleFontSize = 28.sp,
                    leadingIcon = Icons.Filled.ChevronLeft,
                    onClickLeading = onBack
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(mapHeight)
                ) {
                    GoogleMap(
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(zoomControlsEnabled = false),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (taximeterState.routeCoordinates.isNotEmpty()) {
                            Polyline(
                                points = taximeterState.routeCoordinates,
                                color = colorResource(id = R.color.main),
                                width = 10f
                            )
                        }

                        CustomSizedMarker(
                            position = LatLng(
                                taximeterState.startLocation.latitude ?: 0.0,
                                taximeterState.startLocation.longitude ?: 0.0
                            ),
                            drawableRes = R.drawable.flag_start,
                            width = 60,
                            height = 60
                        )


                        CustomSizedMarker(
                            position = LatLng(
                                taximeterState.currentLocation.latitude ?: 0.0,
                                taximeterState.currentLocation.longitude ?: 0.0
                            ),
                            drawableRes = R.drawable.taxi_marker,
                            width = 49,
                            height = 114
                        )

                        if (taximeterState.takeMapScreenshot) {
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

                    SoundButton(
                        isSoundEnabled = taximeterState.isSoundEnabled,
                        onClick = { viewModel.toggleSound() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 12.dp)
                    )

                }

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(colorResource(id = R.color.white))
                        .padding(K.GENERAL_PADDING)
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


                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .background(colorResource(id = R.color.white))
                            .padding(top = 10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {

                        if (taximeterState.rates.showUnits == true) {
                            TaximeterInfoRow(
                                title = stringResource(id = R.string.units),
                                value = (taximeterState.units + taximeterState.rechargeUnits).formatNumberWithDots()
                                    .toString()
                            )
                        }

                        TaximeterInfoRow(
                            title = stringResource(id = R.string.distance_made),
                            value = "${(taximeterState.distanceMade / 1000).formatDigits(1)} KM",
                        )
                        TaximeterInfoRow(
                            title = stringResource(id = R.string.time_trip),
                            value = taximeterState.formattedTime
                        )


                        if (taximeterState.rates.recharges.isNotEmpty()) {
                            Button(
                                onClick = {
                                    viewModel.onChangeIsAddRechargesOpen(!taximeterState.isRechargesOpen)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorResource(id = R.color.transparent)
                                ),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .height(25.dp)
                            ) {

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {

                                    Text(
                                        text = stringResource(id = R.string.add_recharges),
                                        fontFamily = MontserratFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = colorResource(id = R.color.main),
                                    )

                                    Icon(
                                        imageVector = if (taximeterState.isRechargesOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = colorResource(id = R.color.main),
                                        modifier = Modifier
                                            .size(32.dp)
                                    )

                                }

                            }
                        }



                        if (taximeterState.isRechargesOpen) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                taximeterState.rates.recharges.forEach { recharge ->

                                    val isSelected =
                                        taximeterState.rechargesSelected.any { it.key == recharge.key }

                                    CustomCheckBox(
                                        text = recharge.name.orEmpty(),
                                        checked = isSelected,
                                        onValueChange = { checked ->
                                            viewModel.onRechargeToggled(recharge, checked)
                                        }
                                    )

                                }

                            }
                        }


                    }

                    //Spacer(Modifier.weight(1f))

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
                        text = stringResource(id = R.string.finish_trip).uppercase(),
                        onClick = { onFinish() },
                        color = colorResource(id = R.color.gray1),
                        leadingIcon = Icons.Default.Close
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
            onBack = {},
            onFinish = {},
            onScreenshotReady = {},
            onSetTakeScreenshot = {}
        )
    }
}