package com.mitarifamitaxi.taximetrousuario.activities.routeplanner

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitarifamitaxi.taximetrousuario.R
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalFocusManager
import com.mitarifamitaxi.taximetrousuario.activities.BaseActivity
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomButton
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomPlacePrediction
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomTextField
import com.mitarifamitaxi.taximetrousuario.components.ui.MainTitleText
import com.mitarifamitaxi.taximetrousuario.components.ui.TopHeaderView
import com.mitarifamitaxi.taximetrousuario.helpers.K
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily
import com.mitarifamitaxi.taximetrousuario.helpers.getComplementAddress
import com.mitarifamitaxi.taximetrousuario.helpers.getShortAddress
import com.mitarifamitaxi.taximetrousuario.helpers.getStreetAddress
import com.mitarifamitaxi.taximetrousuario.models.PlacePrediction
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

        RoutePlannerScreen(
            uiState = uiState,
            onStartAddressChange = viewModel::onStartAddressChange,
            onEndAddressChange = viewModel::onEndAddressChange,
            onFocusChanged = viewModel::onFocusChanged,
            onSelectPlacePrediction = viewModel::selectPlacePrediction,
            onValidateStartTrip = {
                viewModel.validateStartTrip { intent ->
                    startActivity(intent)
                }
            }
        )
    }

    @Composable
    private fun RoutePlannerScreen(
        uiState: RoutePlannerState,
        onStartAddressChange: (String) -> Unit,
        onEndAddressChange: (String) -> Unit,
        onFocusChanged: (Boolean, Boolean) -> Unit,
        onSelectPlacePrediction: (PlacePrediction) -> Unit,
        onValidateStartTrip: () -> Unit
    ) {

        val focusManager = LocalFocusManager.current

        Column(
            modifier = Modifier.Companion
                .fillMaxSize()
                .background(colorResource(id = R.color.gray4)),
        ) {
            TopHeaderView(
                title = stringResource(id = R.string.taximeter),
                leadingIcon = Icons.Filled.ChevronLeft,
                onClickLeading = {
                    finish()
                }
            )

            Column(
                modifier = Modifier
                    .padding(K.GENERAL_PADDING)
            ) {

                MainTitleText(
                    title = stringResource(id = R.string.begin_your_trip),
                    text = stringResource(id = R.string.select_start_and_end),
                    modifier = Modifier.padding(bottom = K.GENERAL_PADDING)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = K.GENERAL_PADDING)
                ) {
                    CustomTextField(
                        value = getShortAddress(uiState.startAddress),
                        onValueChange = onStartAddressChange,
                        leadingIcon = Icons.Filled.MyLocation,
                        trailingIcon = if (uiState.startAddress.isNotEmpty()) Icons.Filled.Cancel else null,
                        onClickTrailingIcon = { onStartAddressChange("") },
                        placeholder = stringResource(id = R.string.start_point),
                        onFocusChanged = { isFocused -> onFocusChanged(true, isFocused) },
                    )

                    CustomTextField(
                        value = getShortAddress(uiState.endAddress),
                        onValueChange = onEndAddressChange,
                        leadingIcon = Icons.Filled.LocationOn,
                        trailingIcon = if (uiState.endAddress.isNotEmpty()) Icons.Filled.Cancel else null,
                        onClickTrailingIcon = { onEndAddressChange("") },
                        placeholder = stringResource(id = R.string.end_point),
                        onFocusChanged = { isFocused -> onFocusChanged(false, isFocused) }
                    )

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


                CustomButton(
                    text = stringResource(id = R.string.start_trip).uppercase(),
                    onClick = onValidateStartTrip,
                    color = colorResource(id = R.color.main),
                    leadingIcon = Icons.Default.PlayArrow
                )

            }


        }
    }

    @Preview
    @Composable
    fun ScreenPreview() {

        val sampleUiState = RoutePlannerState()

        RoutePlannerScreen(
            uiState = sampleUiState,
            onStartAddressChange = {},
            onEndAddressChange = {},
            onFocusChanged = { _, _ -> },
            onSelectPlacePrediction = {},
            onValidateStartTrip = {}
        )
    }
}





