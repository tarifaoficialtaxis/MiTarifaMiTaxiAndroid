package com.mitarifamitaxi.taximetrousuario.activities.onboarding

import android.content.Intent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.activities.BaseActivity
import com.mitarifamitaxi.taximetrousuario.activities.home.HomeActivity
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomButton
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomCheckBox
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomTextField
import com.mitarifamitaxi.taximetrousuario.components.ui.MainTitleText
import com.mitarifamitaxi.taximetrousuario.components.ui.OnboardingBottomLink
import com.mitarifamitaxi.taximetrousuario.helpers.K
import com.mitarifamitaxi.taximetrousuario.helpers.LocalUserManager
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily
import com.mitarifamitaxi.taximetrousuario.states.LoginState
import com.mitarifamitaxi.taximetrousuario.viewmodels.onboarding.LoginViewModel
import com.mitarifamitaxi.taximetrousuario.viewmodels.onboarding.LoginViewModelFactory

class LoginActivity : BaseActivity() {

    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(this, appViewModel)
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.handleSignInResult(result.data) { signInResult ->
                viewModel.setTempData(signInResult.first, signInResult.second)
                validateNextScreen()
            }
        }
    }

    private fun validateNextScreen() {
        val userState = LocalUserManager(this).getUserState()
        if (userState != null) {
            startActivity(
                Intent(this, HomeActivity::class.java)
            )
            finish()
        } else {
            if (viewModel.uiState.value.mustCompleteProfile) {
                startActivity(
                    Intent(this, CompleteProfileActivity::class.java)
                        .putExtra(
                            "user_data",
                            Gson().toJson(viewModel.uiState.value.tempUserData)
                        )
                )
            } else {
                startActivity(Intent(this, RegisterActivity::class.java))
            }
        }

    }


    @Composable
    override fun Content() {
        val uiState by viewModel.uiState.collectAsState()

        LoginScreen(
            uiState = uiState,
            onRestorePassword = { startActivity(Intent(this, ForgotPasswordActivity::class.java)) },
            onLoginClicked = {
                viewModel.login {
                    validateNextScreen()
                }
            },
            onRegisterClicked = {
                LocalUserManager(this).deleteUserState()
                viewModel.setTempData(false, null)
                startActivity(Intent(this, RegisterActivity::class.java))
            },
            onGoogleSignIn = {
                viewModel.googleSignInClient.revokeAccess().addOnCompleteListener {
                    googleSignInLauncher.launch(viewModel.googleSignInClient.signInIntent)
                }
            }
        )
    }

    @Composable
    private fun LoginScreen(
        uiState: LoginState,
        onRestorePassword: () -> Unit,
        onLoginClicked: () -> Unit,
        onRegisterClicked: () -> Unit,
        onGoogleSignIn: () -> Unit
    ) {
        Column(
            modifier = Modifier.Companion
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier.Companion
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier.Companion
                        .fillMaxSize()
                ) {

                    Box(
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                            .height(LocalConfiguration.current.screenHeightDp.dp * 0.35f)
                            .background(colorResource(id = R.color.main))
                    ) {

                        Image(
                            painter = painterResource(id = R.drawable.city_background),
                            contentDescription = null,
                            modifier = Modifier.Companion
                                .fillMaxWidth()
                                .align(Alignment.Companion.BottomCenter)
                                .padding(bottom = 30.dp)
                        )

                        Image(
                            painter = painterResource(id = R.drawable.logo2),
                            contentDescription = null,
                            modifier = Modifier.Companion
                                .height(LocalConfiguration.current.screenHeightDp.dp * 0.23f)
                                .align(Alignment.Companion.TopCenter)
                                .padding(top = 40.dp)
                        )

                    }

                    Card(
                        modifier = Modifier.Companion
                            .fillMaxSize()
                            .padding(top = LocalConfiguration.current.screenHeightDp.dp * 0.3f),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = colorResource(id = R.color.white),
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.Companion.CenterHorizontally,
                            modifier = Modifier.Companion
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(K.GENERAL_PADDING)
                        ) {

                            MainTitleText(
                                title = stringResource(id = R.string.welcome),
                                modifier = Modifier.Companion.padding(bottom = 25.dp)
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(17.dp),
                                modifier = Modifier.Companion
                                    .padding(bottom = 10.dp)
                            ) {
                                CustomTextField(
                                    value = uiState.userName,
                                    onValueChange = { viewModel.onUserNameChange(it) },
                                    placeholder = stringResource(id = R.string.user_name),
                                    leadingIcon = Icons.Rounded.Person,
                                    keyboardType = KeyboardType.Companion.Email,
                                    isError = uiState.userNameIsError,
                                    errorMessage = uiState.userNameErrorMessage
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
                            }

                            Row(
                                verticalAlignment = Alignment.Companion.CenterVertically,
                                modifier = Modifier.Companion
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,

                                ) {

                                CustomCheckBox(
                                    text = stringResource(id = R.string.remember_me),
                                    checked = uiState.rememberMe,
                                    onValueChange = { viewModel.onRememberMeChange(it) }
                                )

                                Button(
                                    onClick = { onRestorePassword() },
                                    shape = RoundedCornerShape(0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colorResource(id = R.color.transparent),
                                    ),
                                    modifier = Modifier.Companion
                                        .width(180.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.recover_password),
                                        fontFamily = MontserratFamily,
                                        fontWeight = FontWeight.Companion.Bold,
                                        fontSize = 14.sp,
                                        color = colorResource(id = R.color.main),
                                        textAlign = TextAlign.Companion.End,
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier.Companion
                                    .padding(vertical = K.GENERAL_PADDING)
                            ) {
                                CustomButton(
                                    text = stringResource(id = R.string.login).uppercase(),
                                    onClick = { onLoginClicked() }
                                )
                            }

                            Spacer(modifier = Modifier.Companion.weight(1.0f))

                            Row(
                                verticalAlignment = Alignment.Companion.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.Companion
                                    .fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier.Companion
                                        .weight(1f)
                                        .height(2.dp)
                                        .background(color = colorResource(id = R.color.gray2))
                                )

                                Text(
                                    text = stringResource(id = R.string.connect_with),
                                    fontFamily = MontserratFamily,
                                    fontWeight = FontWeight.Companion.Medium,
                                    fontSize = 14.sp,
                                    color = colorResource(id = R.color.gray1),
                                    modifier = Modifier.Companion.padding(horizontal = 10.dp)
                                )

                                Box(
                                    modifier = Modifier.Companion
                                        .weight(1f)
                                        .height(2.dp)
                                        .background(color = colorResource(id = R.color.gray2))
                                )
                            }


                            Column(
                                verticalArrangement = Arrangement.spacedBy(K.GENERAL_PADDING),
                                horizontalAlignment = Alignment.Companion.CenterHorizontally
                            )
                            {
                                Button(
                                    onClick = {
                                        onGoogleSignIn()
                                    },
                                    modifier = Modifier.Companion
                                        .padding(top = 29.dp)
                                        .width(133.dp)
                                        .height(45.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.button_google),
                                        contentDescription = null,
                                        modifier = Modifier.Companion
                                            .fillMaxSize()
                                    )
                                }

                                OnboardingBottomLink(
                                    text = stringResource(id = R.string.no_account),
                                    linkText = stringResource(id = R.string.register_here)
                                ) {
                                    onRegisterClicked()
                                }
                            }


                        }
                    }
                }


            }
        }
    }

    private val sampleUiState = LoginState(
        userName = "usuario@ejemplo.com",
        password = "••••••••",
        rememberMe = true,
        userNameIsError = false,
        passwordIsError = false,
        userNameErrorMessage = "",
        passwordErrorMessage = "",
        mustCompleteProfile = false
    )

    @Preview
    @Composable
    fun ScreenPreview() {
        LoginScreen(
            uiState = sampleUiState,
            onRestorePassword = { },
            onLoginClicked = { },
            onRegisterClicked = { },
            onGoogleSignIn = { }
        )
    }

}

