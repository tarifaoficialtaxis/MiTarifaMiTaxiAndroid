package com.mitarifamitaxi.taximetrousuario.activities.onboarding.driver

import android.Manifest
import android.content.Intent
import android.os.Bundle
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
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.activities.BaseActivity
import com.mitarifamitaxi.taximetrousuario.activities.home.HomeActivity
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomButton
import com.mitarifamitaxi.taximetrousuario.components.ui.PhotoCardSelector
import com.mitarifamitaxi.taximetrousuario.components.ui.RegisterHeaderBox
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily
import com.mitarifamitaxi.taximetrousuario.helpers.createTempImageUri
import com.mitarifamitaxi.taximetrousuario.viewmodels.onboarding.driver.RegisterDriverStepFourViewModel
import com.mitarifamitaxi.taximetrousuario.viewmodels.onboarding.driver.RegisterDriverStepFourViewModel.StepFourUpdateEvent
import com.mitarifamitaxi.taximetrousuario.viewmodels.onboarding.driver.RegisterDriverStepFourViewModel.VehiclePhotoType
import com.mitarifamitaxi.taximetrousuario.viewmodels.onboarding.driver.RegisterDriverStepFourViewModelFactory
import kotlinx.coroutines.launch

class RegisterDriverStepFourActivity : BaseActivity() {
    private val viewModel: RegisterDriverStepFourViewModel by viewModels {
        RegisterDriverStepFourViewModelFactory(this, appViewModel)
    }


    private fun observeViewModelEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.stepFourUpdateEvents.collect { event ->
                    when (event) {
                        is StepFourUpdateEvent.RegistrationComplete -> {
                            startActivity(
                                Intent(
                                    this@RegisterDriverStepFourActivity,
                                    HomeActivity::class.java
                                )
                            )
                            finish()
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
                if (viewModel.vehiclePhotoType == VehiclePhotoType.FRONT) {
                    viewModel.frontTempImageUri = createTempImageUri(this)
                    viewModel.frontTempImageUri?.let { uri ->
                        takePictureLauncher.launch(uri)
                    }
                } else if (viewModel.vehiclePhotoType == VehiclePhotoType.BACK) {
                    viewModel.backTempImageUri = createTempImageUri(this)
                    viewModel.backTempImageUri?.let { uri ->
                        takePictureLauncher.launch(uri)
                    }
                } else if (viewModel.vehiclePhotoType == VehiclePhotoType.SIDE) {
                    viewModel.sideTempImageUri = createTempImageUri(this)
                    viewModel.sideTempImageUri?.let { uri ->
                        takePictureLauncher.launch(uri)
                    }
                }
            }
        }

        MainView(
            onCameraClicked = { type ->
                viewModel.vehiclePhotoType = type
                if (viewModel.hasCameraPermission) {
                    if (viewModel.vehiclePhotoType == VehiclePhotoType.FRONT) {
                        viewModel.frontTempImageUri = createTempImageUri(this)
                        viewModel.frontTempImageUri?.let { uri ->
                            takePictureLauncher.launch(uri)
                        }
                    } else if (viewModel.vehiclePhotoType == VehiclePhotoType.BACK) {
                        viewModel.backTempImageUri = createTempImageUri(this)
                        viewModel.backTempImageUri?.let { uri ->
                            takePictureLauncher.launch(uri)
                        }
                    } else if (viewModel.vehiclePhotoType == VehiclePhotoType.SIDE) {
                        viewModel.sideTempImageUri = createTempImageUri(this)
                        viewModel.sideTempImageUri?.let { uri ->
                            takePictureLauncher.launch(uri)
                        }
                    }
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onGalleryClicked = { type ->
                viewModel.vehiclePhotoType = type
                imagePickerLauncher.launch("image/*")
            },
            onNextClicked = {
                viewModel.onNext()
            }
        )
    }

    @Composable
    private fun MainView(
        onCameraClicked: (type: VehiclePhotoType) -> Unit,
        onGalleryClicked: (type: VehiclePhotoType) -> Unit,
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

                            Row(
                                verticalAlignment = Alignment.Companion.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(11.dp),
                                modifier = Modifier.Companion
                                    .fillMaxWidth()
                                    .padding(vertical = 15.dp)
                            ) {

                                Icon(
                                    imageVector = Icons.Filled.LocalTaxi,
                                    contentDescription = "",
                                    modifier = Modifier
                                        .size(26.dp),
                                    tint = colorResource(id = R.color.main)
                                )

                                Text(
                                    text = stringResource(id = R.string.vehicle_pictures),
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
                                        onCameraClicked(VehiclePhotoType.FRONT)
                                    },
                                    onClickGallery = {
                                        onGalleryClicked(VehiclePhotoType.FRONT)
                                    },
                                    onClickDelete = {
                                        viewModel.frontImageUri = null
                                    }
                                )

                                PhotoCardSelector(
                                    title = stringResource(id = R.string.reverse),
                                    imageUri = viewModel.backImageUri,
                                    onClickCamera = {
                                        onCameraClicked(VehiclePhotoType.BACK)
                                    },
                                    onClickGallery = {
                                        onGalleryClicked(VehiclePhotoType.BACK)
                                    },
                                    onClickDelete = {
                                        viewModel.backImageUri = null
                                    }
                                )

                                PhotoCardSelector(
                                    title = stringResource(id = R.string.side),
                                    imageUri = viewModel.sideImageUri,
                                    onClickCamera = {
                                        onCameraClicked(VehiclePhotoType.SIDE)
                                    },
                                    onClickGallery = {
                                        onGalleryClicked(VehiclePhotoType.SIDE)
                                    },
                                    onClickDelete = {
                                        viewModel.sideImageUri = null
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

