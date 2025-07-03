package com.mitarifamitaxi.taximetrousuario.activities.routeplanner

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.mitarifamitaxi.taximetrousuario.R
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.mitarifamitaxi.taximetrousuario.activities.BaseActivity
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomButton
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomPlacePrediction
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomSizedMarker
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomTextField
import com.mitarifamitaxi.taximetrousuario.components.ui.TopHeaderView
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily
import com.mitarifamitaxi.taximetrousuario.helpers.getComplementAddress
import com.mitarifamitaxi.taximetrousuario.helpers.getShortAddress
import com.mitarifamitaxi.taximetrousuario.helpers.getStreetAddress
import com.mitarifamitaxi.taximetrousuario.models.PlacePrediction
import com.mitarifamitaxi.taximetrousuario.states.AppState
import com.mitarifamitaxi.taximetrousuario.states.RoutePlannerState
import com.mitarifamitaxi.taximetrousuario.viewmodels.routeplanner.RoutePlannerViewModel
import com.mitarifamitaxi.taximetrousuario.viewmodels.routeplanner.RoutePlannerViewModelFactory

class RoutePlannerActivity : BaseActivity() {

    private val viewModel: RoutePlannerViewModel by viewModels {
        RoutePlannerViewModelFactory(this, appViewModel)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appViewModel.requestLocationPermission(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        appViewModel.stopLocationUpdates()
    }

    @Composable
    override fun Content() {
        val uiState by viewModel.uiState.collectAsState()
        val appState by appViewModel.uiState.collectAsState()

        RoutePlannerScreen(
            uiState = uiState,
            appState = appState,
            onClickBack = { finish() },
            onLoadAddressBasedOnCoordinates = viewModel::loadAddressBasedOnCoordinates,
            onValidateAddressStates = viewModel::validateAddressStates,
            onGetRoutePreview = viewModel::getRoutePreview,
            onStartAddressChange = viewModel::onStartAddressChange,
            onEndAddressChange = viewModel::onEndAddressChange,
            onFocusChanged = viewModel::onFocusChanged,
            onSetPointOnMap = viewModel::setPointOnMap,
            onSelectPlacePrediction = viewModel::selectPlacePrediction,
            onValidateStartTrip = { onIntentReady ->
                viewModel.validateStartTrip { intent ->
                    onIntentReady(intent)
                }
            },
            onTempAddressChange = viewModel::onTempAddressChange,
            onSetPointOnMapComplete = viewModel::setPointOnMapComplete
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutePlannerScreen(
    uiState: RoutePlannerState,
    appState: AppState,
    onClickBack: () -> Unit,
    onLoadAddressBasedOnCoordinates: (Double, Double) -> Unit,
    onValidateAddressStates: () -> Unit,
    onGetRoutePreview: () -> Unit,
    onStartAddressChange: (String) -> Unit,
    onEndAddressChange: (String) -> Unit,
    onFocusChanged: (Boolean, Boolean) -> Unit,
    onSetPointOnMap: () -> Unit,
    onSelectPlacePrediction: (PlacePrediction) -> Unit,
    onValidateStartTrip: ((Intent) -> Unit) -> Unit,
    onTempAddressChange: (String) -> Unit,
    onSetPointOnMapComplete: () -> Unit
) {
    val scaffoldState = rememberBottomSheetScaffoldState()
    //val activity = LocalContext.current as? Activity

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(
                appState.userLocation?.latitude ?: 4.60971,
                appState.userLocation?.longitude ?: -74.08175
            ), 15f
        )
    }

    LaunchedEffect(uiState.startLocation) {
        if (uiState.startLocation.latitude != null && uiState.startLocation.longitude != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(
                        uiState.startLocation.latitude,
                        uiState.startLocation.longitude
                    ), 15f
                )
            )
        }
    }

    LaunchedEffect(cameraPositionState) {
        snapshotFlow { cameraPositionState.isMoving }
            .collect { isMoving ->
                if (!uiState.isSheetExpanded && !isMoving) {
                    val cameraTarget = cameraPositionState.position.target
                    onLoadAddressBasedOnCoordinates(cameraTarget.latitude, cameraTarget.longitude)
                }
            }
    }

    LaunchedEffect(uiState.startAddress, uiState.endAddress) {
        onValidateAddressStates()
    }

    LaunchedEffect(uiState.startLocation, uiState.endLocation) {
        onGetRoutePreview()
    }

    LaunchedEffect(uiState.routePoints) {
        if (uiState.routePoints.isNotEmpty()) {
            val boundsBuilder = LatLngBounds.builder()
            uiState.routePoints.forEach { boundsBuilder.include(it) }
            val bounds = boundsBuilder.build()
            val padding = 120
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(bounds, padding)
            )
        }
    }

    BottomSheetScaffold(
        sheetSwipeEnabled = false,
        scaffoldState = scaffoldState,
        sheetContent = {
            SheetContentView(
                uiState = uiState,
                onStartAddressChange = onStartAddressChange,
                onEndAddressChange = onEndAddressChange,
                onFocusChanged = onFocusChanged,
                onSetPointOnMap = onSetPointOnMap,
                onSelectPlacePrediction = onSelectPlacePrediction,
                onValidateStartTrip = onValidateStartTrip,
                onTempAddressChange = onTempAddressChange,
                onSetPointOnMapComplete = onSetPointOnMapComplete
            )
        },
        sheetContainerColor = colorResource(id = R.color.white),
        sheetPeekHeight = uiState.sheetPeekHeight,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(uiState.mainColumnHeight)
        ) {
            TopHeaderView(
                title = stringResource(id = R.string.taximeter),
                leadingIcon = Icons.Filled.ChevronLeft,
                onClickLeading = onClickBack
            )

            Box(modifier = Modifier.fillMaxSize()) {
                GoogleMap(
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = false),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (uiState.routePoints.isNotEmpty()) {
                        Polyline(
                            points = uiState.routePoints,
                            color = colorResource(id = R.color.main),
                            width = 10f
                        )
                    }

                    if (uiState.startAddress.isNotEmpty()) {
                        CustomSizedMarker(
                            position = LatLng(
                                uiState.startLocation.latitude ?: 0.0,
                                uiState.startLocation.longitude ?: 0.0
                            ),
                            drawableRes = R.drawable.flag_start,
                            width = 60,
                            height = 60
                        )
                    }
                    if (uiState.endAddress.isNotEmpty()) {
                        CustomSizedMarker(
                            position = LatLng(
                                uiState.endLocation.latitude ?: 0.0,
                                uiState.endLocation.longitude ?: 0.0
                            ),
                            drawableRes = R.drawable.flag_end,
                            width = 50,
                            height = 60
                        )
                    }
                }

                if (!uiState.isSheetExpanded) {
                    Image(
                        painter = painterResource(id = R.drawable.set_point),
                        contentDescription = "Location Marker",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(45.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SheetContentView(
    uiState: RoutePlannerState,
    onStartAddressChange: (String) -> Unit,
    onEndAddressChange: (String) -> Unit,
    onFocusChanged: (Boolean, Boolean) -> Unit,
    onSetPointOnMap: () -> Unit,
    onSelectPlacePrediction: (PlacePrediction) -> Unit,
    onValidateStartTrip: ((Intent) -> Unit) -> Unit,
    onTempAddressChange: (String) -> Unit,
    onSetPointOnMapComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .height(uiState.sheetPeekHeight - 50.dp)
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = stringResource(id = R.string.begin_your_trip),
            color = colorResource(id = R.color.main),
            fontSize = 17.sp,
            fontFamily = MontserratFamily,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        if (uiState.isSheetExpanded) {
            SheetExpandedView(
                uiState = uiState,
                onStartAddressChange = onStartAddressChange,
                onEndAddressChange = onEndAddressChange,
                onFocusChanged = onFocusChanged,
                onSetPointOnMap = onSetPointOnMap,
                onSelectPlacePrediction = onSelectPlacePrediction,
                onValidateStartTrip = onValidateStartTrip
            )
        } else {
            SheetFoldedView(
                uiState = uiState,
                onTempAddressChange = onTempAddressChange,
                onSetPointOnMapComplete = onSetPointOnMapComplete
            )
        }
    }
}

@Composable
fun SheetExpandedView(
    uiState: RoutePlannerState,
    onStartAddressChange: (String) -> Unit,
    onEndAddressChange: (String) -> Unit,
    onFocusChanged: (Boolean, Boolean) -> Unit,
    onSetPointOnMap: () -> Unit,
    onSelectPlacePrediction: (PlacePrediction) -> Unit,
    onValidateStartTrip: ((Intent) -> Unit) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxHeight()) {
        Text(
            text = stringResource(id = R.string.select_start_and_end),
            color = colorResource(id = R.color.gray1),
            fontSize = 15.sp,
            fontFamily = MontserratFamily,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = stringResource(id = R.string.we_will_show_best_route),
            color = colorResource(id = R.color.gray1),
            fontSize = 15.sp,
            fontFamily = MontserratFamily,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 15.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = colorResource(id = R.color.main),
                    shape = RoundedCornerShape(size = 15.dp)
                )
                .padding(horizontal = 15.dp, vertical = 5.dp),
        ) {
            CustomTextField(
                value = getShortAddress(uiState.startAddress),
                onValueChange = onStartAddressChange,
                leadingIcon = Icons.Filled.MyLocation,
                trailingIcon = if (uiState.startAddress.isNotEmpty()) Icons.Filled.Cancel else null,
                onClickTrailingIcon = { onStartAddressChange("") },
                placeholder = stringResource(id = R.string.start_point),
                onFocusChanged = { isFocused -> onFocusChanged(true, isFocused) },
                focusedIndicatorColor = colorResource(id = R.color.transparent),
                unfocusedIndicatorColor = colorResource(id = R.color.transparent),
            )

            Box(
                modifier = Modifier
                    .height(2.dp)
                    .background(colorResource(id = R.color.gray2))
                    .fillMaxWidth()
            )

            CustomTextField(
                value = getShortAddress(uiState.endAddress),
                onValueChange = onEndAddressChange,
                leadingIcon = Icons.Filled.LocationOn,
                trailingIcon = if (uiState.endAddress.isNotEmpty()) Icons.Filled.Cancel else null,
                onClickTrailingIcon = { onEndAddressChange("") },
                placeholder = stringResource(id = R.string.end_point),
                onFocusChanged = { isFocused -> onFocusChanged(false, isFocused) },
                focusedIndicatorColor = colorResource(id = R.color.transparent),
                unfocusedIndicatorColor = colorResource(id = R.color.transparent),
            )
        }

        Button(
            onClick = onSetPointOnMap,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            shape = RectangleShape,
            modifier = Modifier
                .padding(top = 10.dp)
                .padding(horizontal = 15.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = null,
                    tint = colorResource(id = R.color.main),
                )
                Text(
                    text = stringResource(id = if (uiState.isSelectingStart) R.string.set_point_on_map else R.string.set_end_point_on_map),
                    color = colorResource(id = R.color.main),
                    fontSize = 15.sp,
                    fontFamily = MontserratFamily,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (uiState.places.isNotEmpty()) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                uiState.places.forEach { place ->
                    CustomPlacePrediction(
                        address = getStreetAddress(place.description),
                        region = getComplementAddress(place.description),
                        onPlaceClicked = {
                            focusManager.clearFocus()
                            onSelectPlacePrediction(place)
                        }
                    )
                }
            }
        } else if ((uiState.isStartAddressFocused && uiState.startAddress.length > 3) || (uiState.isEndAddressFocused && uiState.endAddress.length > 3)) {
            Text(
                text = stringResource(id = R.string.no_results_found),
                color = colorResource(id = R.color.gray1),
                fontSize = 15.sp,
                fontFamily = MontserratFamily,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 15.dp)
        ) {
            CustomButton(
                text = stringResource(id = R.string.start_trip).uppercase(),
                onClick = {
                    onValidateStartTrip { intent ->
                        context.startActivity(intent)
                    }
                },
                color = colorResource(id = R.color.main),
                leadingIcon = Icons.Default.PlayArrow
            )
        }
    }
}

@Composable
fun SheetFoldedView(
    uiState: RoutePlannerState,
    onTempAddressChange: (String) -> Unit,
    onSetPointOnMapComplete: () -> Unit
) {
    Text(
        text = stringResource(id = if (uiState.isSelectingStart) R.string.drag_to_set_start_point else R.string.drag_to_set_end_point),
        color = colorResource(id = R.color.gray1),
        fontSize = 15.sp,
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Normal,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 15.dp)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = colorResource(id = R.color.main),
                shape = RoundedCornerShape(size = 15.dp)
            )
            .padding(horizontal = 15.dp, vertical = 5.dp),
    ) {
        CustomTextField(
            value = uiState.tempAddressOnMap,
            onValueChange = onTempAddressChange,
            leadingIcon = Icons.Filled.MyLocation,
            placeholder = stringResource(id = if (uiState.isSelectingStart) R.string.start_point else R.string.end_point),
            focusedIndicatorColor = colorResource(id = R.color.transparent),
            unfocusedIndicatorColor = colorResource(id = R.color.transparent),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 15.dp)
    ) {
        CustomButton(
            text = stringResource(id = if (uiState.isSelectingStart) R.string.set_start else R.string.set_end).uppercase(),
            onClick = onSetPointOnMapComplete,
            color = colorResource(id = R.color.main),
            leadingIcon = Icons.Default.Check
        )
    }
}


@Preview
@Composable
fun ScreenPreview() {

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val sampleUiState = RoutePlannerState(
        isSheetExpanded = true,
        mainColumnHeight = screenHeight * 0.4f,
        sheetPeekHeight = screenHeight * 0.6f
    )

    RoutePlannerScreen(
        uiState = sampleUiState,
        appState = AppState(),
        onClickBack = {},
        onLoadAddressBasedOnCoordinates = { _, _ -> },
        onValidateAddressStates = {},
        onGetRoutePreview = {},
        onStartAddressChange = {},
        onEndAddressChange = {},
        onFocusChanged = { _, _ -> },
        onSetPointOnMap = {},
        onSelectPlacePrediction = {},
        onValidateStartTrip = {},
        onTempAddressChange = {},
        onSetPointOnMapComplete = {}
    )
}