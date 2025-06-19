package com.mitarifamitaxi.taximetrousuario.activities.onboarding

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.activities.BaseActivity
import com.mitarifamitaxi.taximetrousuario.activities.home.HomeActivity
import com.mitarifamitaxi.taximetrousuario.activities.sos.SosActivity
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomButton
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomTextField
import com.mitarifamitaxi.taximetrousuario.components.ui.OnboardingBottomLink
import com.mitarifamitaxi.taximetrousuario.components.ui.ProfilePictureBox
import com.mitarifamitaxi.taximetrousuario.components.ui.RegisterHeaderBox
import com.mitarifamitaxi.taximetrousuario.components.ui.TwoOptionSelectorDialog
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily
import com.mitarifamitaxi.taximetrousuario.helpers.createTempImageUri
import com.mitarifamitaxi.taximetrousuario.viewmodels.onboarding.RegisterViewModel
import com.mitarifamitaxi.taximetrousuario.viewmodels.onboarding.RegisterViewModelFactory

class RegisterActivity : BaseActivity() {

    private val viewModel: RegisterViewModel by viewModels {
        RegisterViewModelFactory(this, appViewModel)
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
                viewModel.tempImageUri = createTempImageUri(this)
                viewModel.tempImageUri?.let { uri ->
                    takePictureLauncher.launch(uri)
                }
            }
        }

        MainView(
            onLoginClicked = {
                val intent = Intent(this, LoginActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            },
            onRegisterClicked = {
                viewModel.register { registerResult ->
                    if (registerResult.first) {
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    }
                }
            }

        )

        if (viewModel.showDialog) {
            TwoOptionSelectorDialog(
                title = stringResource(id = R.string.select_profile_photo),
                primaryTitle = stringResource(id = R.string.camera),
                secondaryTitle = stringResource(id = R.string.gallery),
                primaryIcon = Icons.Default.CameraAlt,
                secondaryIcon = Icons.Default.Image,
                onDismiss = { viewModel.showDialog = false },
                onPrimaryActionClicked = {
                    if (viewModel.hasCameraPermission) {
                        viewModel.tempImageUri = createTempImageUri(this)
                        viewModel.tempImageUri?.let { uri ->
                            takePictureLauncher.launch(uri)
                        }
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    viewModel.showDialog = false
                },
                onSecondaryActionClicked = {
                    imagePickerLauncher.launch("image/*")
                    viewModel.showDialog = false
                }
            )
        }
    }

    @Composable
    private fun MainView(
        onLoginClicked: () -> Unit,
        onRegisterClicked: () -> Unit,
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
                                .padding(start = 29.dp, end = 29.dp)
                        ) {

                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.Companion.CenterHorizontally,
                                modifier = Modifier.Companion
                                    .weight(0.1f)
                            ) {

                                Text(
                                    text = stringResource(id = R.string.register),
                                    fontFamily = MontserratFamily,
                                    fontWeight = FontWeight.Companion.Bold,
                                    fontSize = 24.sp,
                                    color = colorResource(id = R.color.main)
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.Companion.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.Companion
                                    .weight(0.7f)
                                    .verticalScroll(rememberScrollState())
                            ) {

                                ProfilePictureBox(
                                    imageUri = viewModel.imageUri,
                                    onClickEdit = { viewModel.showDialog = true }
                                )

                                CustomTextField(
                                    value = viewModel.firstName,
                                    onValueChange = { viewModel.firstName = it },
                                    placeholder = stringResource(id = R.string.firstName),
                                    leadingIcon = Icons.Rounded.Person,
                                )

                                CustomTextField(
                                    value = viewModel.lastName,
                                    onValueChange = { viewModel.lastName = it },
                                    placeholder = stringResource(id = R.string.lastName),
                                    leadingIcon = Icons.Rounded.Person,
                                )

                                CustomTextField(
                                    value = viewModel.mobilePhone,
                                    onValueChange = { viewModel.mobilePhone = it },
                                    placeholder = stringResource(id = R.string.mobilePhone),
                                    leadingIcon = Icons.Rounded.PhoneIphone,
                                    keyboardType = KeyboardType.Companion.Phone
                                )

                                CustomTextField(
                                    value = viewModel.email,
                                    onValueChange = { viewModel.email = it },
                                    placeholder = stringResource(id = R.string.email),
                                    leadingIcon = Icons.Rounded.Mail,
                                    keyboardType = KeyboardType.Companion.Email
                                )

                                CustomTextField(
                                    value = viewModel.password,
                                    onValueChange = { viewModel.password = it },
                                    placeholder = stringResource(id = R.string.password),
                                    isSecure = true,
                                    leadingIcon = Icons.Rounded.Lock,
                                )

                                CustomTextField(
                                    value = viewModel.confirmPassword,
                                    onValueChange = { viewModel.confirmPassword = it },
                                    placeholder = stringResource(id = R.string.confirm_password),
                                    isSecure = true,
                                    leadingIcon = Icons.Rounded.Lock,
                                )
                            }


                            Column(
                                modifier = Modifier.Companion
                                    .weight(0.2f)
                            ) {
                                Spacer(modifier = Modifier.Companion.weight(1f))
                                CustomButton(
                                    text = stringResource(id = R.string.register_action).uppercase(),
                                    onClick = { onRegisterClicked() },
                                    modifier = Modifier.Companion
                                        .fillMaxWidth()
                                )

                                OnboardingBottomLink(
                                    text = stringResource(id = R.string.already_account),
                                    linkText = stringResource(id = R.string.login_here)
                                ) {
                                    onLoginClicked()
                                }
                            }


                        }
                    }
                }


            }
        }
    }
}