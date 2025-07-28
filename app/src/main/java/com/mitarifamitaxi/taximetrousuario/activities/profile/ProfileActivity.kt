package com.mitarifamitaxi.taximetrousuario.activities.profile

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.activities.BaseActivity
import com.mitarifamitaxi.taximetrousuario.activities.onboarding.LoginActivity
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomButton
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomPasswordPopupDialog
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomTextField
import com.mitarifamitaxi.taximetrousuario.components.ui.ProfilePictureBox
import com.mitarifamitaxi.taximetrousuario.components.ui.TwoOptionSelectorDialog
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily
import com.mitarifamitaxi.taximetrousuario.helpers.createTempImageUri
import com.mitarifamitaxi.taximetrousuario.models.AuthProvider
import com.mitarifamitaxi.taximetrousuario.states.AppState
import com.mitarifamitaxi.taximetrousuario.states.ProfileState
import com.mitarifamitaxi.taximetrousuario.viewmodels.profile.ProfileViewModel
import com.mitarifamitaxi.taximetrousuario.viewmodels.profile.ProfileViewModelFactory
import kotlinx.coroutines.launch

class ProfileActivity : BaseActivity() {

    override fun isDrawerEnabled(): Boolean = true

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(this, appViewModel)
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.handleSignInResult(result.data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeViewModelEvents()
        viewModel.hideKeyboardEvent.observe(this) { shouldHide ->
            if (shouldHide == true) {
                hideKeyboard()
                viewModel.resetHideKeyboardEvent()
            }
        }
    }

    private fun observeViewModelEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationEvents.collect { event ->
                    when (event) {
                        is ProfileViewModel.NavigationEvent.LogOutComplete -> {
                            logOutAction()
                        }

                        is ProfileViewModel.NavigationEvent.Finish -> {
                            finish()
                        }

                        is ProfileViewModel.NavigationEvent.LaunchGoogleSignIn -> {
                            viewModel.googleSignInClient.revokeAccess().addOnCompleteListener {
                                val signInIntent = viewModel.googleSignInClient.signInIntent
                                googleSignInLauncher.launch(signInIntent)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun hideKeyboard() {
        this.currentFocus?.let { currentFocus ->
            val imm = this.getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        }
    }

    private fun logOutAction() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    @Composable
    override fun Content() {

        val uiState by viewModel.uiState.collectAsState()
        val appState by appViewModel.uiState.collectAsState()

        ProfileScreen(
            uiState = uiState,
            appState = appState,
            onClickBack = {
                finish()
            },
            onDeleteAccountClicked = {
                viewModel.onDeleteAccountClicked()
            },
            onUpdateClicked = {
                viewModel.handleUpdate()
            },
            onLogOutClicked = {
                viewModel.logOut()
            }
        )


    }

    @Composable
    private fun ProfileScreen(
        uiState: ProfileState,
        appState: AppState,
        onClickBack: () -> Unit,
        onDeleteAccountClicked: () -> Unit,
        onUpdateClicked: () -> Unit,
        onLogOutClicked: () -> Unit,
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(id = R.color.white)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(
                        colorResource(id = R.color.main),
                        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                    )
                    .padding(top = 20.dp)

            ) {
                Image(
                    painter = painterResource(id = R.drawable.city_background),
                    contentDescription = null,
                    contentScale = ContentScale.Companion.FillBounds,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp)
                        .align(Alignment.Companion.BottomCenter)
                        .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                )

                Column(
                    horizontalAlignment = Alignment.Companion.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Row(
                        verticalAlignment = Alignment.Companion.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {

                        Button(
                            onClick = { onClickBack() },
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Companion.Transparent
                            ),
                            shape = RectangleShape,
                            modifier = Modifier
                                .width(40.dp)
                        ) {

                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "content description",
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(0.dp),
                                tint = colorResource(id = R.color.white),
                            )

                        }

                        Text(
                            text = stringResource(id = R.string.profile).uppercase(),
                            color = colorResource(id = R.color.white),
                            fontSize = 20.sp,
                            fontFamily = MontserratFamily,
                            fontWeight = FontWeight.Companion.Bold,
                            textAlign = TextAlign.Companion.Center,
                            modifier = Modifier
                                .weight(1f)
                        )

                        Spacer(modifier = Modifier.width(40.dp))

                    }

                    ProfilePictureBox(
                        imagePath = appState.userData?.profilePicture,
                        imageUri = uiState.imageUri,
                        onClickEdit = {
                            hideKeyboard()
                            viewModel.onShowDialogSelectPhotoChange(true)
                        }
                    )

                    Text(
                        text = appState.userData?.firstName + " " + appState.userData?.lastName,
                        color = colorResource(id = R.color.white),
                        fontSize = 18.sp,
                        fontFamily = MontserratFamily,
                        fontWeight = FontWeight.Companion.Bold,
                        textAlign = TextAlign.Companion.Center,
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .fillMaxWidth()
                    )

                    Text(
                        text = stringResource(
                            id = R.string.city_param,
                            appState.userData?.city ?: ""
                        ),
                        color = colorResource(id = R.color.white),
                        fontSize = 14.sp,
                        fontFamily = MontserratFamily,
                        fontWeight = FontWeight.Companion.Normal,
                        textAlign = TextAlign.Companion.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.Companion.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.Companion.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                        )
                        {

                            Text(
                                text = uiState.tripsCount.toString(),
                                color = colorResource(id = R.color.white),
                                fontSize = 16.sp,
                                fontFamily = MontserratFamily,
                                fontWeight = FontWeight.Companion.Bold,
                                textAlign = TextAlign.Companion.Center,
                            )

                            Text(
                                text = stringResource(
                                    id = R.string.trips_made
                                ),
                                color = colorResource(id = R.color.white),
                                fontSize = 14.sp,
                                fontFamily = MontserratFamily,
                                fontWeight = FontWeight.Companion.Medium,
                                textAlign = TextAlign.Companion.Center,

                                )

                        }

                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(36.dp)
                                .background(colorResource(id = R.color.white))
                        )

                        Column(
                            horizontalAlignment = Alignment.Companion.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                        ) {

                            Text(
                                text = uiState.distanceCount.toString(),
                                color = colorResource(id = R.color.white),
                                fontSize = 16.sp,
                                fontFamily = MontserratFamily,
                                fontWeight = FontWeight.Companion.Bold,
                                textAlign = TextAlign.Companion.Center
                            )

                            Text(
                                text = stringResource(
                                    id = R.string.km_made
                                ),
                                color = colorResource(id = R.color.white),
                                fontSize = 14.sp,
                                fontFamily = MontserratFamily,
                                fontWeight = FontWeight.Companion.Medium,
                                textAlign = TextAlign.Companion.Center,
                            )


                        }
                    }

                }

            }

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .padding(top = 20.dp)
                    .padding(horizontal = 29.dp)
                    .verticalScroll(rememberScrollState())
            ) {

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
                    errorMessage = uiState.mobilePhoneErrorMessage
                )


                CustomTextField(
                    value = uiState.email,
                    onValueChange = { viewModel.onEmailChange(it) },
                    placeholder = stringResource(id = R.string.email),
                    leadingIcon = Icons.Rounded.Mail,
                    keyboardType = KeyboardType.Companion.Email,
                    isEnabled = false,
                    isError = uiState.emailIsError,
                    errorMessage = uiState.emailErrorMessage
                )

                CustomTextField(
                    value = uiState.familyNumber ?: "",
                    onValueChange = { newText ->
                        if (newText.all { it.isDigit() }) {
                            viewModel.onFamilyNumberChange(newText)
                        }
                    },
                    placeholder = stringResource(id = R.string.family_number),
                    leadingIcon = Icons.Rounded.FamilyRestroom,
                    keyboardType = KeyboardType.Companion.Phone
                )

                CustomTextField(
                    value = uiState.supportNumber ?: "",
                    onValueChange = { newText ->
                        if (newText.all { it.isDigit() }) {
                            viewModel.onSupportNumberChange(newText)
                        }
                    },
                    placeholder = stringResource(id = R.string.support_number),
                    leadingIcon = Icons.Rounded.Groups,
                    keyboardType = KeyboardType.Companion.Phone
                )

                Column(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                ) {

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .padding(top = 20.dp, bottom = 30.dp)
                            .fillMaxWidth()
                    ) {

                        CustomButton(
                            text = stringResource(id = R.string.delete_account).uppercase(),
                            color = colorResource(id = R.color.red2),
                            textColor = colorResource(id = R.color.red1),
                            onClick = { onDeleteAccountClicked() },
                        )

                        CustomButton(
                            text = stringResource(id = R.string.update).uppercase(),
                            onClick = { onUpdateClicked() },
                        )

                        CustomButton(
                            text = stringResource(id = R.string.close_session).uppercase(),
                            onClick = { onLogOutClicked() },
                            color = colorResource(id = R.color.gray1),
                            leadingIcon = Icons.AutoMirrored.Rounded.Logout
                        )
                    }


                }
            }


        }

        if (uiState.showPasswordPopUp) {
            CustomPasswordPopupDialog(
                title = stringResource(id = R.string.warning),
                message = stringResource(id = R.string.re_auth_message),
                buttonText = stringResource(id = R.string.delete_account),
                onDismiss = { viewModel.onShowDialogPassword(false) },
                onPasswordValid = { password ->
                    viewModel.onShowDialogPassword(false)
                    viewModel.authenticateUserByEmailAndPassword(password)
                }
            )
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
                    if (uiState.hasCameraPermission) {
                        val tempUri = createTempImageUri(this)
                        viewModel.onTempImageUriChange(tempUri)
                        takePictureLauncher.launch(tempUri)
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    viewModel.onShowDialogSelectPhotoChange(false)
                },
                onSecondaryActionClicked = {
                    viewModel.onShowDialogSelectPhotoChange(false)
                    imagePickerLauncher.launch("image/*")
                }
            )
        }
    }


    @Preview
    @Composable
    fun ScreenPreview() {
        ProfileScreen(
            uiState = ProfileState(),
            appState = AppState(),
            onClickBack = { },
            onDeleteAccountClicked = { },
            onUpdateClicked = { },
            onLogOutClicked = { }
        )
    }
}