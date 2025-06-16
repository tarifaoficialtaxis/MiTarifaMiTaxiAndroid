package com.mitarifamitaxi.taximetrousuario.activities.onboarding

import android.content.Intent
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
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneIphone
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
import com.google.gson.Gson
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.activities.BaseActivity
import com.mitarifamitaxi.taximetrousuario.activities.home.HomeActivity
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomButton
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomTextField
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily
import com.mitarifamitaxi.taximetrousuario.models.AuthProvider
import com.mitarifamitaxi.taximetrousuario.models.LocalUser
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
            viewModel.userId = userData.id ?: ""
            viewModel.firstName = userData.firstName ?: ""
            viewModel.lastName = userData.lastName ?: ""
            viewModel.email = userData.email ?: ""
            viewModel.mobilePhone = userData.mobilePhone ?: ""
            viewModel.authProvider = userData.authProvider ?: AuthProvider.google
        }
    }

    @Composable
    override fun Content() {
        MainView(
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
    private fun MainView(
        onCompleteProfile: () -> Unit
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
                                .padding(
                                    top = 29.dp,
                                    bottom = 10.dp,
                                    start = 29.dp,
                                    end = 29.dp
                                )
                                .verticalScroll(rememberScrollState())

                        ) {
                            Text(
                                text = stringResource(id = R.string.complete_profile),
                                fontFamily = MontserratFamily,
                                fontWeight = FontWeight.Companion.Bold,
                                fontSize = 24.sp,
                                color = colorResource(id = R.color.main),
                                modifier = Modifier.Companion
                                    .padding(bottom = 25.dp),
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.Companion
                                    .padding(bottom = 10.dp)
                            ) {
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

                            }

                            Spacer(modifier = Modifier.Companion.height(20.dp))

                            CustomButton(
                                text = stringResource(id = R.string.complete_profile_action).uppercase(),
                                onClick = { onCompleteProfile() }
                            )


                        }
                    }
                }


            }

        }
    }
}