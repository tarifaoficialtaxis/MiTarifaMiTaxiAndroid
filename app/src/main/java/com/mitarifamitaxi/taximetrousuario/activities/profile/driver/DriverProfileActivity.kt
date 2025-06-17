package com.mitarifamitaxi.taximetrousuario.activities.profile.driver

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.activities.BaseActivity
import com.mitarifamitaxi.taximetrousuario.activities.onboarding.LoginActivity
import com.mitarifamitaxi.taximetrousuario.components.ui.ButtonLinkRow
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomButton
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomPasswordPopupDialog
import com.mitarifamitaxi.taximetrousuario.components.ui.ProfilePictureBox
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily
import com.mitarifamitaxi.taximetrousuario.viewmodels.profile.driver.DriverProfileViewModel
import com.mitarifamitaxi.taximetrousuario.viewmodels.profile.driver.DriverProfileViewModelFactory
import kotlinx.coroutines.launch

class DriverProfileActivity : BaseActivity() {

    override fun isDrawerEnabled(): Boolean = true

    private val viewModel: DriverProfileViewModel by viewModels {
        DriverProfileViewModelFactory(this, appViewModel)
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
                        is DriverProfileViewModel.NavigationEvent.LogOutComplete -> {
                            logOutAction()
                        }

                        is DriverProfileViewModel.NavigationEvent.LaunchGoogleSignIn -> {
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
        MainView(
            onClickBack = {
                finish()
            },
            onDeleteAccountClicked = {
                viewModel.onDeleteAccountClicked()
            },
            onLogOutClicked = {
                viewModel.logOut()
            }
        )

        if (viewModel.showPasswordPopUp) {
            CustomPasswordPopupDialog(
                title = stringResource(id = R.string.warning),
                message = stringResource(id = R.string.re_auth_message),
                buttonText = stringResource(id = R.string.delete_account),
                onDismiss = { viewModel.showPasswordPopUp = false },
                onPasswordValid = { password ->
                    viewModel.showPasswordPopUp = false
                    viewModel.authenticateUserByEmailAndPassword(password)
                }

            )
        }

    }

    @Composable
    private fun MainView(
        onClickBack: () -> Unit,
        onDeleteAccountClicked: () -> Unit,
        onLogOutClicked: () -> Unit,
    ) {

        Column(
            modifier = Modifier.Companion
                .fillMaxSize()
                .background(colorResource(id = R.color.white)),
        ) {
            Box(
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .height(230.dp)
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
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                        .height(112.dp)
                        .align(Alignment.Companion.BottomCenter)
                        .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                )

                Column(
                    horizontalAlignment = Alignment.Companion.CenterHorizontally,
                    modifier = Modifier.Companion
                        .fillMaxSize()
                ) {
                    Row(
                        verticalAlignment = Alignment.Companion.CenterVertically,
                        modifier = Modifier.Companion
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
                            modifier = Modifier.Companion
                                .width(40.dp)
                        ) {

                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "content description",
                                modifier = Modifier.Companion
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
                            modifier = Modifier.Companion
                                .weight(1f)
                        )

                        Spacer(modifier = Modifier.Companion.width(40.dp))

                    }

                    ProfilePictureBox(
                        imageUri = appViewModel.userData?.profilePicture?.toUri(),
                        editable = false
                    )

                    Text(
                        text = appViewModel.userData?.firstName + " " + appViewModel.userData?.lastName,
                        color = colorResource(id = R.color.white),
                        fontSize = 18.sp,
                        fontFamily = MontserratFamily,
                        fontWeight = FontWeight.Companion.Bold,
                        textAlign = TextAlign.Companion.Center,
                        modifier = Modifier.Companion
                            .padding(top = 5.dp)
                            .fillMaxWidth()
                    )

                    Text(
                        text = stringResource(
                            id = R.string.city_param,
                            appViewModel.userData?.city ?: ""
                        ),
                        color = colorResource(id = R.color.white),
                        fontSize = 14.sp,
                        fontFamily = MontserratFamily,
                        fontWeight = FontWeight.Companion.Normal,
                        textAlign = TextAlign.Companion.Center,
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                    )


                }

            }

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.Companion
                    .padding(top = 20.dp)
                    .padding(horizontal = 29.dp)
            ) {


                Column(
                    modifier = Modifier.Companion
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                ) {

                    Column(
                        verticalArrangement = Arrangement.spacedBy(30.dp),
                    ) {
                        ButtonLinkRow(
                            text = stringResource(id = R.string.personal_information).uppercase(),
                            onClick = {
                                startActivity(
                                    Intent(
                                        this@DriverProfileActivity,
                                        DriverProfilePersonalInfoActivity::class.java
                                    )
                                )
                            }
                        )

                        ButtonLinkRow(
                            text = stringResource(id = R.string.vehicle_information).uppercase(),
                            onClick = {
                                startActivity(
                                    Intent(
                                        this@DriverProfileActivity,
                                        DriverProfileVehicleInfoActivity::class.java
                                    )
                                )
                            }
                        )
                    }



                    Spacer(modifier = Modifier.Companion.weight(1f))

                    Button(
                        onClick = { onDeleteAccountClicked() },
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.red2)
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                        modifier =
                            Modifier.Companion
                                .fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(id = R.string.delete_account).uppercase(),
                            color = colorResource(id = R.color.red1),
                            fontSize = 16.sp,
                            fontFamily = MontserratFamily,
                            fontWeight = FontWeight.Companion.Bold,
                            textAlign = TextAlign.Companion.Center,
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.Companion
                            .padding(top = 20.dp, bottom = 30.dp)
                            .fillMaxWidth()
                    ) {
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
    }
}