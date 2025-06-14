package com.mitarifamitaxi.taximetrousuario.activities.onboarding

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.activities.BaseActivity
import com.mitarifamitaxi.taximetrousuario.activities.onboarding.driver.RegisterDriverStepOneActivity
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomContactBoxView
import com.mitarifamitaxi.taximetrousuario.components.ui.OnboardingBottomLink
import com.mitarifamitaxi.taximetrousuario.components.ui.RegisterHeaderBox
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily
import com.mitarifamitaxi.taximetrousuario.models.UserRole

class SelectRoleActivity : BaseActivity() {


    @Composable
    override fun Content() {
        MainView(
            onLoginClicked = {
                finish()
            },
            onOptionClicked = { option ->
                if (option == UserRole.USER) {
                    startActivity(Intent(this, RegisterActivity::class.java))
                } else if (option == UserRole.DRIVER) {
                    startActivity(Intent(this, RegisterDriverStepOneActivity::class.java))
                }
            }
        )
    }

    @Composable
    private fun MainView(
        onLoginClicked: () -> Unit,
        onOptionClicked: (option: UserRole) -> Unit,
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
                                color = colorResource(id = R.color.main),
                                modifier = Modifier.Companion
                                    .padding(bottom = 58.dp),
                            )

                            Text(
                                text = stringResource(id = R.string.select_one_option),
                                fontFamily = MontserratFamily,
                                fontWeight = FontWeight.Companion.Bold,
                                fontSize = 16.sp,
                                color = colorResource(id = R.color.black),
                                modifier = Modifier.Companion
                                    .padding(bottom = 29.dp),
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(29.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            )
                            {

                                CustomContactBoxView(
                                    icon = Icons.Default.Person,
                                    text = stringResource(id = R.string.user),
                                    onClick = { onOptionClicked(UserRole.USER) },
                                    modifier = Modifier
                                        .weight(1f)
                                )

                                CustomContactBoxView(
                                    icon = Icons.Default.LocalTaxi,
                                    text = stringResource(id = R.string.driver),
                                    onClick = { onOptionClicked(UserRole.DRIVER) },
                                    modifier = Modifier
                                        .weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.Companion.weight(1.0f))

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