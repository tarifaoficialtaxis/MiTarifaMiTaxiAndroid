package com.mitarifamitaxi.taximetrousuario.activities.onboarding.driver

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.magnifier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mitarifamitaxi.taximetrousuario.activities.BaseActivity
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomButton
import com.mitarifamitaxi.taximetrousuario.components.ui.RegisterHeaderBox
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily
import kotlinx.coroutines.launch
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomDropDown
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomTextField
import com.mitarifamitaxi.taximetrousuario.viewmodels.onboarding.driver.RegisterDriverStepThreeViewModel
import com.mitarifamitaxi.taximetrousuario.viewmodels.onboarding.driver.RegisterDriverStepThreeViewModelFactory

class RegisterDriverStepThreeActivity : BaseActivity() {
    private val viewModel: RegisterDriverStepThreeViewModel by viewModels {
        RegisterDriverStepThreeViewModelFactory(this, appViewModel)
    }

    private fun observeViewModelEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.stepThreeUpdateEvents.collect { event ->
                    when (event) {
                        is RegisterDriverStepThreeViewModel.StepThreeUpdateEvent.FirebaseUserUpdated -> {
                            /*startActivity(
                                Intent(
                                    this@RegisterDriverStepThreeActivity,
                                    RegisterDriverStepThreeActivity::class.java
                                )
                            )*/
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeViewModelEvents()
    }

    @Composable
    override fun Content() {

        MainView(
            onNextClicked = {
                viewModel.onNext()
            }
        )
    }

    @Composable
    private fun MainView(
        onNextClicked: () -> Unit
    ) {

        Column {
            Box(
                modifier = Modifier.Companion
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier.Companion
                        .fillMaxSize()
                        .background(colorResource(id = R.color.white))
                ) {

                    RegisterHeaderBox()

                    Card(
                        modifier = Modifier.Companion
                            .fillMaxSize()
                            .offset(y = (-24).dp),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = colorResource(id = R.color.white),
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.Companion.CenterHorizontally,
                            modifier = Modifier.Companion
                                .fillMaxSize()
                                .padding(top = 29.dp, bottom = 10.dp, start = 29.dp, end = 29.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = stringResource(id = R.string.register),
                                fontFamily = MontserratFamily,
                                fontWeight = FontWeight.Companion.Bold,
                                fontSize = 24.sp,
                                color = colorResource(id = R.color.main)
                            )

                            Text(
                                text = stringResource(id = R.string.vehicle_information),
                                fontFamily = MontserratFamily,
                                fontWeight = FontWeight.Companion.Bold,
                                fontSize = 20.sp,
                                color = colorResource(id = R.color.black),
                                modifier = Modifier.Companion
                                    .padding(vertical = 15.dp),
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(15.dp),
                            ) {

                                CustomDropDown(
                                    leadingIcon = Icons.Default.LocalTaxi,
                                    label = stringResource(id = R.string.brand_mandatory),
                                    options = viewModel.vehicleBrandNames,
                                    selectedOptionText = viewModel.selectedBrand ?: "",
                                    onOptionSelected = { viewModel.onBrandSelected(it) }
                                )

                                CustomDropDown(
                                    leadingIcon = Icons.Default.LocalTaxi,
                                    label = stringResource(id = R.string.model_mandatory),
                                    options = viewModel.vehicleModelsNames,
                                    selectedOptionText = viewModel.selectedModel ?: "",
                                    onOptionSelected = { viewModel.onModelSelected(it) }
                                )

                                CustomDropDown(
                                    leadingIcon = Icons.Default.CalendarMonth,
                                    label = stringResource(id = R.string.year_mandatory),
                                    options = viewModel.vehicleYears,
                                    selectedOptionText = viewModel.selectedYear ?: "",
                                    onOptionSelected = { viewModel.onYearSelected(it) }
                                )

                                CustomTextField(
                                    value = viewModel.plate,
                                    onValueChange = { viewModel.plate = it.uppercase() },
                                    placeholder = stringResource(id = R.string.plate_mandatory),
                                    leadingIcon = Icons.Outlined.Pin,
                                )

                            }

                            Spacer(modifier = Modifier.Companion.weight(1f))


                            CustomButton(
                                text = stringResource(id = R.string.next).uppercase(),
                                onClick = { onNextClicked() },
                                modifier = Modifier.Companion
                                    .padding(vertical = 20.dp)
                                    .fillMaxWidth()
                            )

                        }
                    }
                }
            }
        }
    }

}

