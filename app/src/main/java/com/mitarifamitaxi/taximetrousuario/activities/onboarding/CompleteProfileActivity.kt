package com.mitarifamitaxi.taximetrousuario.activities.onboarding

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.activities.BaseActivity
import com.mitarifamitaxi.taximetrousuario.activities.home.HomeActivity
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomButton
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomTextField
import com.mitarifamitaxi.taximetrousuario.components.ui.MainTitleText
import com.mitarifamitaxi.taximetrousuario.components.ui.ProfilePictureBox
import com.mitarifamitaxi.taximetrousuario.components.ui.RegisterHeaderBox
import com.mitarifamitaxi.taximetrousuario.components.ui.TwoOptionSelectorDialog
import com.mitarifamitaxi.taximetrousuario.helpers.K
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily
import com.mitarifamitaxi.taximetrousuario.helpers.createTempImageUri
import com.mitarifamitaxi.taximetrousuario.models.AuthProvider
import com.mitarifamitaxi.taximetrousuario.models.LocalUser
import com.mitarifamitaxi.taximetrousuario.states.CompleteProfileState
import com.mitarifamitaxi.taximetrousuario.states.ForgotPasswordState
import com.mitarifamitaxi.taximetrousuario.viewmodels.onboarding.CompleteProfileViewModel
import com.mitarifamitaxi.taximetrousuario.viewmodels.onboarding.CompleteProfileViewModelFactory

class CompleteProfileActivity : BaseActivity() {

    private val viewModel: CompleteProfileViewModel by viewModels {
        CompleteProfileViewModelFactory(this, appViewModel)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userJson = intent.getStringExtra("user_data")
        userJson?.let {
            val userData = Gson().fromJson(it, LocalUser::class.java)
            viewModel.onUserIdChange(userData.id ?: "")
            viewModel.onFistNameChange(userData.firstName ?: "")
            viewModel.onLastNameChange(userData.lastName ?: "")
            viewModel.onMobilePhoneChange(userData.mobilePhone ?: "")
            viewModel.onEmailChange(userData.email ?: "")

        }
    }

    @Composable
    override fun Content() {

        val uiState by viewModel.uiState.collectAsState()

        CompleteProfileScreen(
            uiState = uiState,
            onCompleteProfile = {
                viewModel.completeProfile(onResult = { result ->
                    if (result.first) {
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    }
                })
            }
        )

    }


    @Composable
    private fun CompleteProfileScreen(
        uiState: CompleteProfileState,
        onCompleteProfile: () -> Unit
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
            modifier = Modifier.Companion
                .fillMaxSize()
                .background(colorResource(id = R.color.white))
        ) {

            RegisterHeaderBox()

            Card(
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .padding(top = LocalConfiguration.current.screenHeightDp.dp * 0.23f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(id = R.color.white),
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.Companion.CenterHorizontally,
                    modifier = Modifier.Companion
                        .fillMaxSize()
                        .padding(
                            top = K.GENERAL_PADDING,
                            start = K.GENERAL_PADDING,
                            end = K.GENERAL_PADDING
                        )

                ) {
                    MainTitleText(
                        title = stringResource(id = R.string.complete_profile),
                        modifier = Modifier.Companion
                            .padding(bottom = K.GENERAL_PADDING)
                    )

                    Column(
                        horizontalAlignment = Alignment.Companion.CenterHorizontally,
                        modifier = Modifier.Companion
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {

                        Column(
                            horizontalAlignment = Alignment.Companion.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                isError = uiState.firstNameIsError,
                                errorMessage = uiState.firstNameErrorMessage
                            )

                            CustomTextField(
                                value = uiState.lastName,
                                onValueChange = { viewModel.onLastNameChange(it) },
                                placeholder = stringResource(id = R.string.lastName),
                                leadingIcon = Icons.Rounded.Person,
                                isError = uiState.lastNameIsError,
                                errorMessage = uiState.lastNameErrorMessage
                            )

                            CustomTextField(
                                value = uiState.mobilePhone,
                                onValueChange = { viewModel.onMobilePhoneChange(it) },
                                placeholder = stringResource(id = R.string.mobilePhone),
                                leadingIcon = Icons.Rounded.PhoneIphone,
                                keyboardType = KeyboardType.Companion.Phone,
                                isError = uiState.mobilePhoneIsError,
                                errorMessage = uiState.mobilePhoneErrorMessage,
                            )

                            CustomTextField(
                                value = uiState.email,
                                onValueChange = { },
                                placeholder = stringResource(id = R.string.email).replace(
                                    "*",
                                    ""
                                ),
                                leadingIcon = Icons.Rounded.Mail,
                                keyboardType = KeyboardType.Companion.Email,
                                isEnabled = false
                            )

                        }

                        Spacer(modifier = Modifier.Companion.weight(1f))

                        Column(
                            modifier = Modifier.Companion
                                .padding(vertical = K.GENERAL_PADDING)
                            //.background(colorResource(id = R.color.green))
                        ) {
                            CustomButton(
                                text = stringResource(id = R.string.complete_profile_action).uppercase(),
                                onClick = { onCompleteProfile() }
                            )
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
                onDismiss = {
                    viewModel.onShowDialogSelectPhotoChange(false)
                },
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

    private val sampleUiState = CompleteProfileState()

    @Preview
    @Composable
    fun ScreenPreview() {
        CompleteProfileScreen(
            uiState = sampleUiState,
            onCompleteProfile = { }
        )
    }
}