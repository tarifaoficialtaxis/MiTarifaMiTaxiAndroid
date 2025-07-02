package com.mitarifamitaxi.taximetrousuario.activities.pqrs

import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.activities.BaseActivity
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomButton
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomCheckBox
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomMultilineTextField
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomTextField
import com.mitarifamitaxi.taximetrousuario.components.ui.TopHeaderView
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily
import com.mitarifamitaxi.taximetrousuario.states.PqrsState
import com.mitarifamitaxi.taximetrousuario.states.SosState
import com.mitarifamitaxi.taximetrousuario.viewmodels.pqrs.PqrsViewModel
import com.mitarifamitaxi.taximetrousuario.viewmodels.pqrs.PqrsViewModelFactory

class PqrsActivity : BaseActivity() {

    private val viewModel: PqrsViewModel by viewModels {
        PqrsViewModelFactory(this, appViewModel)
    }

    @Composable
    override fun Content() {

        val uiState by viewModel.uiState.collectAsState()

        PqrsScreen(
            uiState = uiState,
            onClickSendPqr = {
                viewModel.validateSendPqr {
                    startActivity(it)
                }
            }
        )
    }

    @Composable
    private fun PqrsScreen(
        uiState: PqrsState,
        onClickSendPqr: () -> Unit,
    ) {

        Column(
            modifier = Modifier.Companion
                .fillMaxSize()
                .background(colorResource(id = R.color.white)),
        ) {
            TopHeaderView(
                title = stringResource(id = R.string.pqrs),
                leadingIcon = Icons.Filled.ChevronLeft,
                onClickLeading = {
                    finish()
                }
            )

            Column(
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .padding(29.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {

                    CustomTextField(
                        value = uiState.plate,
                        onValueChange = { viewModel.onPlateChange(it) },
                        placeholder = stringResource(id = R.string.plate),
                        leadingIcon = Icons.Rounded.DirectionsCar,
                        isError = uiState.plateIsError,
                        errorMessage = uiState.plateErrorMessage
                    )
                }

                Column(
                    modifier = Modifier.Companion.padding(top = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.reason_complaint),
                        color = colorResource(id = R.color.gray1),
                        fontSize = 16.sp,
                        fontFamily = MontserratFamily,
                        fontWeight = FontWeight.Companion.Medium,
                        textAlign = TextAlign.Companion.Start,
                        modifier = Modifier.Companion
                            .padding(bottom = 10.dp)
                    )

                    CustomCheckBox(
                        text = stringResource(id = R.string.high_fare),
                        checked = uiState.isHighFare,
                        onValueChange = { viewModel.onHighFareChange(it) }
                    )

                    CustomCheckBox(
                        text = stringResource(id = R.string.user_mistreated),
                        checked = uiState.isUserMistreated,
                        onValueChange = { viewModel.onUserMistreatedChange(it) }
                    )

                    CustomCheckBox(
                        text = stringResource(id = R.string.service_abandonment),
                        checked = uiState.isServiceAbandonment,
                        onValueChange = { viewModel.onServiceAbandonmentChange(it) }
                    )

                    CustomCheckBox(
                        text = stringResource(id = R.string.unauthorized_charges),
                        checked = uiState.isUnauthorizedCharges,
                        onValueChange = { viewModel.onUnauthorizedChargesChange(it) }
                    )

                    CustomCheckBox(
                        text = stringResource(id = R.string.no_fare_notice),
                        checked = uiState.isNoFareNotice,
                        onValueChange = { viewModel.onNoFareNoticeChange(it) }
                    )

                    CustomCheckBox(
                        text = stringResource(id = R.string.dangerous_driving),
                        checked = uiState.isDangerousDriving,
                        onValueChange = { viewModel.onDangerousDrivingChange(it) }
                    )

                    CustomCheckBox(
                        text = stringResource(id = R.string.other),
                        checked = uiState.isOther,
                        onValueChange = { viewModel.onOtherChange(it) }
                    )


                    if (uiState.isOther) {
                        Spacer(modifier = Modifier.Companion.height(10.dp))
                        CustomMultilineTextField(
                            value = uiState.otherValue,
                            onValueChange = { viewModel.onOtherValueChange(it) },
                            placeholder = stringResource(id = R.string.other_reason),
                            isError = uiState.isOtherValueError,
                            errorMessage = uiState.otherValueErrorMessage
                        )
                    }

                }

                Spacer(modifier = Modifier.Companion.weight(1f))

                CustomButton(
                    text = stringResource(id = R.string.create_pqr).uppercase(),
                    onClick = onClickSendPqr,
                )

            }


        }

    }

    @Preview
    @Composable
    fun ScreenPreview() {
        PqrsScreen(
            uiState = PqrsState(),
            onClickSendPqr = { }
        )
    }
}