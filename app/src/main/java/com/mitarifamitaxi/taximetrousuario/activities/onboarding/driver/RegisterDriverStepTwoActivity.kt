package com.mitarifamitaxi.taximetrousuario.activities.onboarding.driver

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.activities.BaseActivity
import com.mitarifamitaxi.taximetrousuario.activities.onboarding.LoginActivity
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomButton
import com.mitarifamitaxi.taximetrousuario.components.ui.OnboardingBottomLink
import com.mitarifamitaxi.taximetrousuario.components.ui.RegisterHeaderBox
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily
import com.mitarifamitaxi.taximetrousuario.viewmodels.onboarding.driver.RegisterDriverStepTwoViewModel
import com.mitarifamitaxi.taximetrousuario.viewmodels.onboarding.driver.RegisterDriverStepTwoViewModelFactory

class RegisterDriverStepTwoActivity : BaseActivity() {
    private val viewModel: RegisterDriverStepTwoViewModel by viewModels {
        RegisterDriverStepTwoViewModelFactory(this, appViewModel)
    }

    @Composable
    override fun Content() {
        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            viewModel.onImageSelected(uri)
        }

        val takePictureLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { success ->
            viewModel.onImageCaptured(success)
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            viewModel.onPermissionResult(isGranted)
            if (isGranted) {
                viewModel.createTempImageUri()
                viewModel.tempImageUri?.let { uri ->
                    takePictureLauncher.launch(uri)
                }
            }
        }

        MainView(
            onNextClicked = {
                viewModel.onNext { result ->

                }
            }

        )


    }

    @Composable
    private fun MainView(
        onNextClicked: () -> Unit,
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
                                text = stringResource(id = R.string.personal_information),
                                fontFamily = MontserratFamily,
                                fontWeight = FontWeight.Companion.Bold,
                                fontSize = 20.sp,
                                color = colorResource(id = R.color.black),
                                modifier = Modifier.Companion
                                    .padding(vertical = 15.dp),
                            )

                            Text(
                                text = stringResource(id = R.string.register_driver_step_two_description),
                                fontFamily = MontserratFamily,
                                fontWeight = FontWeight.Companion.Normal,
                                fontSize = 16.sp,
                                color = colorResource(id = R.color.gray),
                                modifier = Modifier.Companion
                                    .padding(bottom = 20.dp)
                            )

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

