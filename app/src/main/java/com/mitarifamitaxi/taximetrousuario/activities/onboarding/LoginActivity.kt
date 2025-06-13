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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.activities.BaseActivity
import com.mitarifamitaxi.taximetrousuario.activities.home.HomeActivity
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomButton
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomCheckBox
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomTextField
import com.mitarifamitaxi.taximetrousuario.components.ui.OnboardingBottomLink
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily
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
                if (signInResult.first == "home") {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                } else if (signInResult.first == "complete_profile") {
                    val userJson = Gson().toJson(signInResult.second)
                    val intent = Intent(this, CompleteProfileActivity::class.java)
                    intent.putExtra("user_data", userJson)
                    startActivity(intent)
                }
            }
        }
    }

    @Composable
    override fun Content() {

        MainView(
            onRestorePasswordClicked = {
                startActivity(Intent(this, ForgotPasswordActivity::class.java))
            },
            onLoginClicked = {
                viewModel.login {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                }
            },
            onRegisterClicked = {
                //startActivity(Intent(this, RegisterActivity::class.java))
                startActivity(Intent(this, SelectRoleActivity::class.java))
            },
            onGoogleSignIn = {
                viewModel.googleSignInClient.revokeAccess().addOnCompleteListener {
                    val signInIntent = viewModel.googleSignInClient.signInIntent
                    googleSignInLauncher.launch(signInIntent)
                }
            }
        )
    }


    @Composable
    private fun MainView(
        onRestorePasswordClicked: () -> Unit,
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
                Column(
                    modifier = Modifier.Companion
                        .fillMaxSize()
                        .background(colorResource(id = R.color.white))
                ) {

                    Box(
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                            .height(250.dp)
                            .background(colorResource(id = R.color.main))
                    ) {

                        Box(
                            modifier = Modifier.Companion
                                .fillMaxSize()
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.city_background),
                                contentDescription = null,
                                modifier = Modifier.Companion
                                    .fillMaxSize()
                                    .align(Alignment.Companion.BottomCenter)
                                    .offset(y = 40.dp)
                            )

                            Image(
                                painter = painterResource(id = R.drawable.logo2),
                                contentDescription = null,
                                modifier = Modifier.Companion
                                    .height(134.dp)
                                    .align(Alignment.Companion.Center)
                            )
                        }
                    }

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
                                .verticalScroll(rememberScrollState())
                                .padding(top = 29.dp, bottom = 10.dp, start = 29.dp, end = 29.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.welcome),
                                fontFamily = MontserratFamily,
                                fontWeight = FontWeight.Companion.Bold,
                                fontSize = 24.sp,
                                color = colorResource(id = R.color.main),
                                modifier = Modifier.Companion
                                    .padding(bottom = 25.dp),
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(17.dp),
                                modifier = Modifier.Companion
                                    .padding(bottom = 10.dp)
                            ) {
                                CustomTextField(
                                    value = viewModel.userName,
                                    onValueChange = { viewModel.userName = it },
                                    placeholder = stringResource(id = R.string.user_name),
                                    leadingIcon = Icons.Rounded.Person,
                                    keyboardType = KeyboardType.Companion.Email,
                                    isError = !viewModel.userNameIsValid,
                                    errorMessage = viewModel.userNameErrorMessage
                                )

                                CustomTextField(
                                    value = viewModel.password,
                                    onValueChange = { viewModel.password = it },
                                    placeholder = stringResource(id = R.string.password),
                                    isSecure = true,
                                    leadingIcon = Icons.Rounded.Lock,
                                    isError = !viewModel.passwordIsValid,
                                    errorMessage = viewModel.passwordErrorMessage
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
                                    checked = viewModel.rememberMe,
                                    onValueChange = { viewModel.rememberMe = it }
                                )

                                Button(
                                    onClick = { onRestorePasswordClicked() },
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
                                    .padding(vertical = 29.dp)
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