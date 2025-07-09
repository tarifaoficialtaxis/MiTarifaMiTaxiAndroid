package com.mitarifamitaxi.taximetrousuario.activities.onboarding

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.activities.BaseActivity
import com.mitarifamitaxi.taximetrousuario.activities.home.HomeActivity
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomButton
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomTextField
import com.mitarifamitaxi.taximetrousuario.components.ui.MainTitleText
import com.mitarifamitaxi.taximetrousuario.components.ui.OnboardingBottomLink
import com.mitarifamitaxi.taximetrousuario.components.ui.ProfilePictureBox
import com.mitarifamitaxi.taximetrousuario.components.ui.RegisterHeaderBox
import com.mitarifamitaxi.taximetrousuario.components.ui.TwoOptionSelectorDialog
import com.mitarifamitaxi.taximetrousuario.helpers.K
import com.mitarifamitaxi.taximetrousuario.helpers.createTempImageUri
import com.mitarifamitaxi.taximetrousuario.states.RegisterState
import com.mitarifamitaxi.taximetrousuario.viewmodels.onboarding.RegisterViewModel
import com.mitarifamitaxi.taximetrousuario.viewmodels.onboarding.RegisterViewModelFactory

class RegisterActivity : BaseActivity() {

    private val viewModel: RegisterViewModel by viewModels {
        RegisterViewModelFactory(this, appViewModel)
    }

    @Composable
    override fun Content() {

        val uiState by viewModel.uiState.collectAsState()

        RegisterScreen(
            uiState = uiState,
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


    }

    @Composable
    private fun RegisterScreen(
        uiState: RegisterState,
        onLoginClicked: () -> Unit,
        onRegisterClicked: () -> Unit,
    ) {

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
                val tempUri = createTempImageUri(this)
                viewModel.onTempImageUriChange(tempUri)
                takePictureLauncher.launch(tempUri)
            }
        }


        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(id = R.color.white))
        ) {

            RegisterHeaderBox()

            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = LocalConfiguration.current.screenHeightDp.dp * 0.23f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(id = R.color.white),
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.Companion.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = K.GENERAL_PADDING,
                            start = K.GENERAL_PADDING,
                            end = K.GENERAL_PADDING
                        )
                ) {


                    MainTitleText(
                        title = stringResource(id = R.string.register),
                        modifier = Modifier
                            .padding(bottom = K.GENERAL_PADDING)
                    )

                    Column(
                        horizontalAlignment = Alignment.Companion.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {

                        Column(
                            horizontalAlignment = Alignment.Companion.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            ProfilePictureBox(
                                imageUri = uiState.imageUri,
                                onClickEdit = { viewModel.onShowDialogSelectPhotoChange(true) }
                            )

                            CustomTextField(
                                value = uiState.firstName,
                                onValueChange = { viewModel.onFistNameChange(it) },
                                placeholder = stringResource(id = R.string.firstName),
                                leadingIcon = Icons.Rounded.Person,
                                capitalization = KeyboardCapitalization.Words,
                                isError = uiState.firstNameIsError,
                                errorMessage = uiState.firstNameErrorMessage
                            )

                            CustomTextField(
                                value = uiState.lastName,
                                onValueChange = { viewModel.onLastNameChange(it) },
                                placeholder = stringResource(id = R.string.lastName),
                                leadingIcon = Icons.Rounded.Person,
                                capitalization = KeyboardCapitalization.Words,
                                isError = uiState.lastNameIsError,
                                errorMessage = uiState.lastNameErrorMessage
                            )

                            CustomTextField(
                                value = uiState.mobilePhone,
                                onValueChange = { newText ->
                                    if (newText.all { it.isDigit() }) {
                                        viewModel.onMobilePhoneChange(newText)
                                    }
                                },
                                placeholder = stringResource(id = R.string.mobilePhone),
                                leadingIcon = Icons.Rounded.PhoneIphone,
                                keyboardType = KeyboardType.Companion.Phone,
                                isError = uiState.mobilePhoneIsError,
                                errorMessage = uiState.mobilePhoneErrorMessage,
                            )

                            CustomTextField(
                                value = uiState.email,
                                onValueChange = { viewModel.onEmailChange(it) },
                                placeholder = stringResource(id = R.string.email),
                                leadingIcon = Icons.Rounded.Mail,
                                keyboardType = KeyboardType.Companion.Email,
                                isError = uiState.emailIsError,
                                errorMessage = uiState.emailErrorMessage
                            )

                            CustomTextField(
                                value = uiState.password,
                                onValueChange = { viewModel.onPasswordChange(it) },
                                placeholder = stringResource(id = R.string.password),
                                isSecure = true,
                                leadingIcon = Icons.Rounded.Lock,
                                isError = uiState.passwordIsError,
                                errorMessage = uiState.passwordErrorMessage
                            )

                            CustomTextField(
                                value = uiState.confirmPassword,
                                onValueChange = { viewModel.onConfirmPasswordChange(it) },
                                placeholder = stringResource(id = R.string.confirm_password),
                                isSecure = true,
                                leadingIcon = Icons.Rounded.Lock,
                                isError = uiState.confirmPasswordIsError,
                                errorMessage = uiState.confirmPasswordErrorMessage
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .padding(vertical = K.GENERAL_PADDING)
                        ) {
                            CustomButton(
                                text = stringResource(id = R.string.register_action).uppercase(),
                                onClick = { onRegisterClicked() },
                                modifier = Modifier
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



        if (uiState.showDialogSelectPhoto) {
            TwoOptionSelectorDialog(
                title = stringResource(id = R.string.select_profile_photo),
                primaryTitle = stringResource(id = R.string.camera),
                secondaryTitle = stringResource(id = R.string.gallery),
                primaryIcon = Icons.Default.CameraAlt,
                secondaryIcon = Icons.Default.Image,
                onDismiss = { viewModel.onShowDialogSelectPhotoChange(false) },
                onPrimaryActionClicked = {
                    viewModel.onShowDialogSelectPhotoChange(false)
                    if (uiState.hasCameraPermission) {
                        val tempUri = createTempImageUri(this)
                        viewModel.onTempImageUriChange(tempUri)
                        takePictureLauncher.launch(tempUri)
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onSecondaryActionClicked = {
                    viewModel.onShowDialogSelectPhotoChange(false)
                    imagePickerLauncher.launch("image/*")
                }
            )
        }

    }

    private val sampleUiState = RegisterState()

    @Preview
    @Composable
    fun ScreenPreview() {
        RegisterScreen(
            uiState = sampleUiState,
            onLoginClicked = { },
            onRegisterClicked = { }
        )
    }
}