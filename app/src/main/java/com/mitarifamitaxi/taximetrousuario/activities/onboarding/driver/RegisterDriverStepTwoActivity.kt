package com.mitarifamitaxi.taximetrousuario.activities.onboarding.driver

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.activities.BaseActivity
import com.mitarifamitaxi.taximetrousuario.activities.onboarding.LoginActivity
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomButton
import com.mitarifamitaxi.taximetrousuario.components.ui.OnboardingBottomLink
import com.mitarifamitaxi.taximetrousuario.components.ui.PhotoCardSelector
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
                if (viewModel.isFrontImageSelected) {
                    viewModel.frontTempImageUri?.let { uri ->
                        takePictureLauncher.launch(uri)
                    }
                } else {
                    viewModel.backTempImageUri?.let { uri ->
                        takePictureLauncher.launch(uri)
                    }
                }
            }
        }

        MainView(
            onCameraClicked = { isFront ->
                viewModel.isFrontImageSelected = isFront
                if (viewModel.hasCameraPermission) {
                    viewModel.createTempImageUri()
                    if (isFront) {
                        viewModel.frontTempImageUri?.let { uri ->
                            takePictureLauncher.launch(uri)
                        }
                    } else {
                        viewModel.backTempImageUri?.let { uri ->
                            takePictureLauncher.launch(uri)
                        }
                    }
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onGalleryClicked = { isFront ->
                viewModel.isFrontImageSelected = isFront
                imagePickerLauncher.launch("image/*")
            },
            onNextClicked = {
                viewModel.onNext { result ->

                }
            }
        )
    }

    @Composable
    private fun MainView(
        onCameraClicked: (isFront: Boolean) -> Unit,
        onGalleryClicked: (isFront: Boolean) -> Unit,
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
                                text = stringResource(id = R.string.personal_information),
                                fontFamily = MontserratFamily,
                                fontWeight = FontWeight.Companion.Bold,
                                fontSize = 20.sp,
                                color = colorResource(id = R.color.black),
                                modifier = Modifier.Companion
                                    .padding(vertical = 15.dp),
                            )

                            Row(
                                verticalAlignment = Alignment.Companion.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(11.dp),
                                modifier = Modifier.Companion
                                    .fillMaxWidth()
                                    .padding(vertical = 15.dp)
                            ) {

                                Icon(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.id_card),
                                    contentDescription = "",
                                    modifier = Modifier
                                        .size(26.dp),
                                    tint = colorResource(id = R.color.main)
                                )

                                Text(
                                    text = stringResource(id = R.string.driving_license),
                                    fontFamily = MontserratFamily,
                                    fontWeight = FontWeight.Companion.Medium,
                                    fontSize = 16.sp,
                                    color = colorResource(id = R.color.black),
                                )
                            }

                            Column(
                                verticalArrangement = Arrangement.spacedBy(15.dp),
                            ) {
                                PhotoCardSelector(
                                    title = stringResource(id = R.string.front),
                                    imageUri = viewModel.frontImageUri,
                                    onClickCamera = {
                                        onCameraClicked(true)
                                    },
                                    onClickGallery = {
                                        onGalleryClicked(true)
                                    },
                                    onClickDelete = {
                                        viewModel.frontImageUri = null
                                    }
                                )

                                PhotoCardSelector(
                                    title = stringResource(id = R.string.reverse),
                                    imageUri = viewModel.backImageUri,
                                    onClickCamera = {
                                        onCameraClicked(false)
                                    },
                                    onClickGallery = {
                                        onGalleryClicked(false)
                                    },
                                    onClickDelete = {
                                        viewModel.backImageUri = null
                                    }
                                )
                            }


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

