package com.mitarifamitaxi.taximetrousuario.activities.onboarding

import android.os.Bundle
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
import androidx.compose.material.icons.rounded.Mail
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.activities.BaseActivity
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomButton
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomTextField
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily
import com.mitarifamitaxi.taximetrousuario.viewmodels.onboarding.ForgotPasswordViewModel
import com.mitarifamitaxi.taximetrousuario.viewmodels.onboarding.ForgotPasswordViewModelFactory
import kotlinx.coroutines.launch

class ForgotPasswordActivity : BaseActivity() {

    private val viewModel: ForgotPasswordViewModel by viewModels {
        ForgotPasswordViewModelFactory(this, appViewModel)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeViewModelEvents()
    }

    private fun observeViewModelEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationEvents.collect { event ->
                    when (event) {
                        is ForgotPasswordViewModel.NavigationEvent.GoBack -> {
                            finish()
                        }
                    }
                }
            }
        }
    }

    @Composable
    override fun Content() {
        MainView(
            onConfirmClicked = {
                viewModel.validateEmail()
            },
            onGoBackClicked = {
                finish()
            }
        )
    }

    @Composable
    private fun MainView(
        onConfirmClicked: () -> Unit,
        onGoBackClicked: () -> Unit,
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

                    Box(
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(colorResource(id = R.color.black))
                    ) {

                        Box(
                            modifier = Modifier.Companion
                                .fillMaxSize()
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.city_background2),
                                contentDescription = null,
                                modifier = Modifier.Companion
                                    .fillMaxSize()
                                    .align(Alignment.Companion.BottomCenter)
                                    .offset(y = 20.dp)
                            )

                            Image(
                                painter = painterResource(id = R.drawable.logo3),
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
                                .padding(top = 29.dp, bottom = 10.dp, start = 29.dp, end = 29.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = stringResource(id = R.string.recover_password),
                                fontFamily = MontserratFamily,
                                fontWeight = FontWeight.Companion.Bold,
                                fontSize = 24.sp,
                                color = colorResource(id = R.color.main),
                                modifier = Modifier.Companion
                                    .padding(bottom = 20.dp),
                            )

                            Text(
                                text = stringResource(id = R.string.input_email_message),
                                fontFamily = MontserratFamily,
                                fontWeight = FontWeight.Companion.Normal,
                                fontSize = 14.sp,
                                color = colorResource(id = R.color.gray),
                                textAlign = TextAlign.Companion.Center,
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.Companion
                                    .padding(top = 32.dp)
                            ) {

                                CustomTextField(
                                    value = viewModel.email,
                                    onValueChange = { viewModel.email = it },
                                    placeholder = stringResource(id = R.string.email),
                                    leadingIcon = Icons.Rounded.Mail,
                                    keyboardType = KeyboardType.Companion.Email,
                                    isError = viewModel.emailIsError,
                                    errorMessage = viewModel.emailErrorMessage
                                )

                            }

                            Spacer(modifier = Modifier.Companion.weight(1.0f))

                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.Companion
                                    .padding(bottom = 12.dp)
                            ) {
                                CustomButton(
                                    text = stringResource(id = R.string.confirm).uppercase(),
                                    onClick = { onConfirmClicked() }
                                )

                                CustomButton(
                                    text = stringResource(id = R.string.back).uppercase(),
                                    onClick = { onGoBackClicked() },
                                    color = colorResource(id = R.color.gray1),
                                )
                            }
                        }
                    }
                }


            }
        }
    }
}