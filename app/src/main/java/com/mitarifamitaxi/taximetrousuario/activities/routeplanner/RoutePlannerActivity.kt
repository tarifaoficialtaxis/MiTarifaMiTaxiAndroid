package com.mitarifamitaxi.taximetrousuario.activities.routeplanner

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
        MainView()
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)
    @Composable
    fun MainView() {
        val scaffoldState = rememberBottomSheetScaffoldState()

        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(
                LatLng(
                    appViewModel.userLocation?.latitude ?: 4.60971,
                    appViewModel.userLocation?.longitude ?: -74.08175
                ), 15f
            )
        }

        LaunchedEffect(viewModel.startLocation) {
            if (viewModel.startLocation.latitude != null && viewModel.startLocation.longitude != null) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            viewModel.startLocation.latitude!!,
                            viewModel.startLocation.longitude!!
                        ), 15f
                    )
                )
            }
        }

        LaunchedEffect(cameraPositionState) {
            snapshotFlow { cameraPositionState.isMoving }
                .collect { isMoving ->
                    if (!viewModel.isSheetExpanded && !isMoving) {
                        val cameraTarget = cameraPositionState.position.target
                        viewModel.loadAddressBasedOnCoordinates(
                            cameraTarget.latitude,
                            cameraTarget.longitude
                        )
                    }
                }
        }


        LaunchedEffect(viewModel.startAddress, viewModel.endAddress) {
            viewModel.validateAddressStates()
        }

        LaunchedEffect(viewModel.startLocation, viewModel.endLocation) {
            viewModel.getRoutePreview()
        }

        LaunchedEffect(viewModel.routePoints) {
            if (viewModel.routePoints.isNotEmpty()) {
                val boundsBuilder = LatLngBounds.builder()
                viewModel.routePoints.forEach { boundsBuilder.include(it) }
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
                SheetContentView()
            },
            sheetContainerColor = colorResource(id = R.color.white),
            sheetPeekHeight = viewModel.sheetPeekHeight,
        ) {
            Column(
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .height(viewModel.mainColumnHeight)
            ) {
                TopHeaderView(
                    title = stringResource(id = R.string.taximeter),
                    leadingIcon = Icons.Filled.ChevronLeft,
                    onClickLeading = {
                        finish()
                    }
                )

                Box(modifier = Modifier.Companion.fillMaxSize()) {
                    GoogleMap(
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false
                        ),
                        modifier = Modifier.Companion.fillMaxSize(),
                    ) {
                        if (viewModel.routePoints.isNotEmpty()) {
                            Polyline(
                                points = viewModel.routePoints,
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
                        if (viewModel.endAddress.isNotEmpty()) {

                            CustomSizedMarker(
                                position = LatLng(
                                    viewModel.endLocation.latitude ?: 0.0,
                                    viewModel.endLocation.longitude ?: 0.0
                                ),
                                drawableRes = R.drawable.flag_end,
                                width = 50,
                                height = 60
                            )
                        }
                    }

                    if (!viewModel.isSheetExpanded) {

                        Image(
                            painter = painterResource(id = R.drawable.set_point),
                            contentDescription = "Location Marker",
                            modifier = Modifier.Companion
                                .align(Alignment.Companion.Center)
                                .size(45.dp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun SheetContentView() {
        Column(
            modifier = Modifier.Companion
                .height(viewModel.sheetPeekHeight - 50.dp)
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = stringResource(id = R.string.begin_your_trip),
                color = colorResource(id = R.color.main),
                fontSize = 17.sp,
                fontFamily = MontserratFamily,
                fontWeight = FontWeight.Companion.Bold,
                textAlign = TextAlign.Companion.Center,
                modifier = Modifier.Companion
                    .fillMaxWidth()
            )

            if (viewModel.isSheetExpanded) {
                SheetExpandedView()
            } else {
                SheetFoldedView()
            }

        }

    }

    @Composable
    fun SheetExpandedView() {

        val places by viewModel.places

        val focusManager = LocalFocusManager.current

        Column(
            modifier = Modifier.Companion
                .fillMaxHeight()
        ) {

            Text(
                text = stringResource(id = R.string.select_start_and_end),
                color = colorResource(id = R.color.gray1),
                fontSize = 15.sp,
                fontFamily = MontserratFamily,
                fontWeight = FontWeight.Companion.Normal,
                textAlign = TextAlign.Companion.Center,
                modifier = Modifier.Companion
                    .fillMaxWidth()
            )

            Text(
                text = stringResource(id = R.string.we_will_show_best_route),
                color = colorResource(id = R.color.gray1),
                fontSize = 15.sp,
                fontFamily = MontserratFamily,
                fontWeight = FontWeight.Companion.Bold,
                textAlign = TextAlign.Companion.Center,
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .padding(bottom = 15.dp)
            )

            Column(
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .border(
                        width = 2.dp,
                        color = colorResource(id = R.color.main),
                        shape = RoundedCornerShape(size = 15.dp)
                    )
                    .padding(horizontal = 15.dp)
                    .padding(vertical = 5.dp),
            ) {
                CustomTextField(
                    value = getShortAddress(viewModel.startAddress),
                    onValueChange = {
                        viewModel.startAddress = it
                        viewModel.loadPlacePredictions(it)
                    },
                    leadingIcon = Icons.Filled.MyLocation,
                    trailingIcon = if (viewModel.startAddress.isNotEmpty()) Icons.Filled.Cancel else null,
                    onClickTrailingIcon = { viewModel.startAddress = "" },
                    placeholder = stringResource(id = R.string.start_point),
                    onFocusChanged = { isFocused ->
                        viewModel.isStartAddressFocused = isFocused
                    },
                    focusedIndicatorColor = colorResource(id = R.color.transparent),
                    unfocusedIndicatorColor = colorResource(id = R.color.transparent),
                )

                Box(
                    modifier = Modifier.Companion
                        .height(2.dp)
                        .background(colorResource(id = R.color.gray2))
                        .fillMaxWidth()
                )

                CustomTextField(
                    value = getShortAddress(viewModel.endAddress),
                    onValueChange = {
                        viewModel.endAddress = it
                        viewModel.loadPlacePredictions(it)
                    },
                    leadingIcon = Icons.Filled.LocationOn,
                    trailingIcon = if (viewModel.endAddress.isNotEmpty()) Icons.Filled.Cancel else null,
                    onClickTrailingIcon = { viewModel.endAddress = "" },
                    placeholder = stringResource(id = R.string.end_point),
                    onFocusChanged = { isFocused ->
                        viewModel.isEndAddressFocused = isFocused
                    },
                    focusedIndicatorColor = colorResource(id = R.color.transparent),
                    unfocusedIndicatorColor = colorResource(id = R.color.transparent),
                )
            }

            Button(
                onClick = { viewModel.setPointOnMap() },
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Companion.Transparent
                ),
                shape = RectangleShape,
                modifier =
                    Modifier.Companion
                        .padding(top = 10.dp)
                        .padding(horizontal = 15.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Companion.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        5.dp,
                        Alignment.Companion.CenterHorizontally
                    ),
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                ) {

                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        tint = colorResource(id = R.color.main),
                    )

                    Text(
                        text = stringResource(id = if (viewModel.isSelectingStart) R.string.set_point_on_map else R.string.set_end_point_on_map),
                        color = colorResource(id = R.color.main),
                        fontSize = 15.sp,
                        fontFamily = MontserratFamily,
                        fontWeight = FontWeight.Companion.Bold,
                    )

                }
            }


            if (places.isNotEmpty()) {

                Column(
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    places.forEach { place ->
                        CustomPlacePrediction(
                            address = getStreetAddress(place.description),
                            region = getComplementAddress(place.description),
                            onPlaceClicked = {
                                focusManager.clearFocus()
                                viewModel.setPlacePrediction(place)
                            }
                        )
                    }
                }
            } else if ((viewModel.isStartAddressFocused && viewModel.startAddress.length > 3) || (viewModel.isEndAddressFocused && viewModel.endAddress.length > 3)) {
                Text(
                    text = stringResource(id = R.string.no_results_found),
                    color = colorResource(id = R.color.gray1),
                    fontSize = 15.sp,
                    fontFamily = MontserratFamily,
                    fontWeight = FontWeight.Companion.Normal,
                    textAlign = TextAlign.Companion.Center,
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
            }


            Spacer(modifier = Modifier.Companion.weight(1f))

            Box(
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .padding(bottom = 15.dp)
            ) {
                CustomButton(
                    text = stringResource(id = R.string.start_trip).uppercase(),
                    onClick = {
                        viewModel.validateStartTrip(onIntentReady = {
                            startActivity(it)
                        })
                    },
                    color = colorResource(id = R.color.main),
                    leadingIcon = Icons.Default.PlayArrow
                )
            }
        }
    }

    @Composable
    fun SheetFoldedView() {
        Text(
            text = stringResource(id = if (viewModel.isSelectingStart) R.string.drag_to_set_start_point else R.string.drag_to_set_end_point),
            color = colorResource(id = R.color.gray1),
            fontSize = 15.sp,
            fontFamily = MontserratFamily,
            fontWeight = FontWeight.Companion.Normal,
            textAlign = TextAlign.Companion.Center,
            modifier = Modifier.Companion
                .fillMaxWidth()
                .padding(bottom = 15.dp)
        )

        Column(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = colorResource(id = R.color.main),
                    shape = RoundedCornerShape(size = 15.dp)
                )
                .padding(horizontal = 15.dp)
                .padding(vertical = 5.dp),
        ) {
            CustomTextField(
                value = viewModel.tempAddressOnMap,
                onValueChange = { viewModel.tempAddressOnMap = it },
                leadingIcon = Icons.Filled.MyLocation,
                placeholder = stringResource(id = if (viewModel.isSelectingStart) R.string.start_point else R.string.end_point),
                focusedIndicatorColor = colorResource(id = R.color.transparent),
                unfocusedIndicatorColor = colorResource(id = R.color.transparent),
            )
        }

        Box(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .padding(top = 15.dp)
        ) {
            CustomButton(
                text = stringResource(id = if (viewModel.isSelectingStart) R.string.set_start else R.string.set_end).uppercase(),
                onClick = { viewModel.setPontOnMapComplete() },
                color = colorResource(id = R.color.main),
                leadingIcon = Icons.Default.Check
            )
        }
    }

}