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
import com.mitarifamitaxi.taximetrousuario.BuildConfig
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.activities.BaseActivity
import com.mitarifamitaxi.taximetrousuario.components.adds.BottomBannerAd
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomButton
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomCheckBox
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomMultilineTextField
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomTextField
import com.mitarifamitaxi.taximetrousuario.components.ui.TopHeaderView
import com.mitarifamitaxi.taximetrousuario.helpers.K
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily
import com.mitarifamitaxi.taximetrousuario.states.PqrsState
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
            modifier = Modifier
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
                modifier = Modifier
                    .weight(1f)
                    .padding(29.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {

                    CustomTextField(
                        value = uiState.plate,
                        onValueChange = { viewModel.onPlateChange(it.uppercase()) },
                        placeholder = stringResource(id = R.string.plate),
                        leadingIcon = Icons.Rounded.DirectionsCar,
                        isError = uiState.plateIsError,
                        errorMessage = uiState.plateErrorMessage
                    )
                }

                Column(
                    modifier = Modifier.padding(top = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.reason_complaint),
                        color = colorResource(id = R.color.gray1),
                        fontSize = 16.sp,
                        fontFamily = MontserratFamily,
                        fontWeight = FontWeight.Companion.Medium,
                        textAlign = TextAlign.Companion.Start,
                        modifier = Modifier
                            .padding(bottom = 10.dp)
                    )

                    uiState.pqrsData.reasons.forEach { reason ->

                        val isSelected = uiState.reasonsSelected.any { it.key == reason.key }

                        CustomCheckBox(
                            text = reason.name.orEmpty(),
                            checked = isSelected,
                            onValueChange = { checked ->
                                viewModel.onReasonToggled(reason, checked)
                            }
                        )

                    }

                    if (uiState.reasonsSelected.any { it.key == "OTHER" }) {
                        Spacer(modifier = Modifier.height(10.dp))
                        CustomMultilineTextField(
                            value = uiState.otherValue,
                            onValueChange = { viewModel.onOtherValueChange(it) },
                            placeholder = stringResource(id = R.string.other_reason),
                            isError = uiState.isOtherValueError,
                            errorMessage = uiState.otherValueErrorMessage
                        )
                    }

                }

                Spacer(modifier = Modifier.weight(1f))

                CustomButton(
                    text = stringResource(id = R.string.create_pqr).uppercase(),
                    onClick = onClickSendPqr,
                    modifier = Modifier
                        .padding(top = K.GENERAL_PADDING),
                )

            }

            BottomBannerAd(adId = BuildConfig.PQRS_AD_UNIT_ID)

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