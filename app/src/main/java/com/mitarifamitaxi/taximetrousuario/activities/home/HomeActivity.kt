package com.mitarifamitaxi.taximetrousuario.activities.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.gson.Gson
import com.mitarifamitaxi.taximetrousuario.BuildConfig
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.activities.BaseActivity
import com.mitarifamitaxi.taximetrousuario.activities.LocalOpenDrawer
import com.mitarifamitaxi.taximetrousuario.activities.pqrs.PqrsActivity
import com.mitarifamitaxi.taximetrousuario.activities.sos.SosActivity
import com.mitarifamitaxi.taximetrousuario.activities.taximeter.TaximeterActivity
import com.mitarifamitaxi.taximetrousuario.activities.trips.MyTripsActivity
import com.mitarifamitaxi.taximetrousuario.activities.trips.TripSummaryActivity
import com.mitarifamitaxi.taximetrousuario.components.adds.BottomBannerAd
import com.mitarifamitaxi.taximetrousuario.components.ui.NoTripsView
import com.mitarifamitaxi.taximetrousuario.components.ui.ProfilePictureBox
import com.mitarifamitaxi.taximetrousuario.components.ui.TripItem
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily
import com.mitarifamitaxi.taximetrousuario.models.Trip
import com.mitarifamitaxi.taximetrousuario.states.AppState
import com.mitarifamitaxi.taximetrousuario.states.HomeState
import com.mitarifamitaxi.taximetrousuario.viewmodels.home.HomeViewModel
import com.mitarifamitaxi.taximetrousuario.viewmodels.home.HomeViewModelFactory

class HomeActivity : BaseActivity() {

    override fun isDrawerEnabled(): Boolean = true

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(this, appViewModel)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appViewModel.requestLocationPermission(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        appViewModel.stopLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        appViewModel.reloadUserData()
    }

    private fun ensureLocationAndProceed(action: () -> Unit) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            action()
        } else {
            appViewModel.requestLocationPermission(this)
        }
    }

    @Composable
    override fun Content() {

        val uiState by viewModel.uiState.collectAsState()
        val appState by appViewModel.uiState.collectAsState()

        HomeScreen(
            uiState = uiState,
            appState = appState,
            onTaximeterClick = {
                ensureLocationAndProceed {
                    appState.userData?.city?.let {
                        viewModel.getCityRates(userCity = it, goNext = {
                            startActivity(Intent(this, TaximeterActivity::class.java))
                        })
                    }
                }
            },
            onSosClick = {
                startActivity(Intent(this, SosActivity::class.java))
            },
            onPqrsClick = {
                startActivity(Intent(this, PqrsActivity::class.java))
            },
            onMyTripsClick = {
                startActivity(Intent(this, MyTripsActivity::class.java))
            },
            onTripClicked = { trip ->
                val tripJson = Gson().toJson(trip)
                val intent = Intent(this, TripSummaryActivity::class.java)
                intent.putExtra("is_details", true)
                intent.putExtra("trip_data", tripJson)
                startActivity(intent)
            }
        )

    }

    @Composable
    private fun HomeScreen(
        uiState: HomeState,
        appState: AppState,
        onTaximeterClick: () -> Unit,
        onSosClick: () -> Unit,
        onPqrsClick: () -> Unit,
        onMyTripsClick: () -> Unit,
        onTripClicked: (Trip) -> Unit
    ) {
        val openDrawer = LocalOpenDrawer.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(id = R.color.white)),
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        colorResource(id = R.color.black),
                        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                    )

            ) {
                Image(
                    painter = painterResource(id = R.drawable.city_background3),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(95.dp)
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))

                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {

                    OutlinedButton(
                        onClick = { openDrawer() },
                        modifier = Modifier
                            .size(45.dp)
                            .border(2.dp, colorResource(id = R.color.white), CircleShape),
                        shape = CircleShape,
                        border = BorderStroke(0.dp, Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = colorResource(id = R.color.main),
                            contentColor = colorResource(id = R.color.white)
                        ),
                    ) {

                        ProfilePictureBox(
                            imageUri = appState.userData?.profilePicture?.toUri(),
                            editable = false,
                            boxSize = 45,
                            iconSize = 30
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = stringResource(id = R.string.welcome_home),
                        color = colorResource(id = R.color.white),
                        fontSize = 20.sp,
                        fontFamily = MontserratFamily,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .align(Alignment.Start)
                    )

                    Text(
                        text = "${appState.userData?.firstName}!",
                        color = colorResource(id = R.color.main),
                        fontSize = 20.sp,
                        fontFamily = MontserratFamily,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = appState.userData?.city ?: "",
                        color = colorResource(id = R.color.white),
                        fontSize = 14.sp,
                        fontFamily = MontserratFamily,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier
                            .align(Alignment.Start)
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.home_taxi),
                    contentDescription = null,
                    modifier = Modifier
                        .width(220.dp)
                        .height(100.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 30.dp, y = 15.dp)
                )
            }


            Column(
                modifier = Modifier
                    .weight(1f)
            ) {

                if (appState.isGettingLocation) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(modifier = Modifier.height(60.dp))
                        Text(
                            text = stringResource(id = R.string.getting_location),
                            color = colorResource(id = R.color.black),
                            fontSize = 20.sp,
                            fontFamily = MontserratFamily,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp)
                        )
                        CircularProgressIndicator(
                            color = colorResource(id = R.color.black)
                        )
                    }
                } else {

                    UserView(
                        uiState = uiState,
                        onTaximeterClick = onTaximeterClick,
                        onSosClick = onSosClick,
                        onPqrsClick = onPqrsClick,
                        onMyTripsClick = onMyTripsClick,
                        onTripClicked = onTripClicked
                    )
                }
            }

//            BottomBannerAd(
//                adId = BuildConfig.HOME_AD_UNIT_ID
//            )
        }
    }

    @Composable
    private fun UserView(
        uiState: HomeState,
        onTaximeterClick: () -> Unit,
        onSosClick: () -> Unit,
        onPqrsClick: () -> Unit,
        onMyTripsClick: () -> Unit,
        onTripClicked: (Trip) -> Unit
    ) {

        Column(
            Modifier
                .padding(horizontal = 29.dp)
                .padding(top = 30.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {

            OutlinedButton(
                onClick = onTaximeterClick,
                modifier = Modifier
                    .fillMaxWidth(),
                border = null,
                contentPadding = PaddingValues(0.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.home_taximetro_button),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxSize()
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(11.dp),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onSosClick,
                    modifier = Modifier
                        .weight(1.0f),
                    border = null,
                    contentPadding = PaddingValues(0.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.home_sos_button),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxSize()
                    )
                }

                OutlinedButton(
                    onClick = onPqrsClick,
                    modifier = Modifier
                        .weight(1.0f),
                    border = null,
                    contentPadding = PaddingValues(0.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.home_pqrs_button),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxSize()
                    )
                }
            }

            Column {
                Row {
                    Text(
                        text = stringResource(id = R.string.my_trips),
                        color = colorResource(id = R.color.black),
                        fontSize = 16.sp,
                        fontFamily = MontserratFamily,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(top = 15.dp)
                    )

                    Spacer(modifier = Modifier.weight(1.0f))

                    if (uiState.trips.isNotEmpty()) {
                        TextButton(onClick = onMyTripsClick) {
                            Text(
                                text = stringResource(id = R.string.see_all),
                                color = colorResource(id = R.color.main),
                                textDecoration = TextDecoration.Underline,
                                fontSize = 14.sp,
                                fontFamily = MontserratFamily,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }


                if (uiState.trips.isEmpty()) {
                    NoTripsView()
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(11.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colorResource(id = R.color.white))
                            .padding(top = 10.dp)
                            .padding(bottom = 40.dp)
                    ) {
                        uiState.trips.forEach { trip ->
                            TripItem(
                                trip, onTripClicked = {
                                    onTripClicked(trip)
                                }
                            )
                        }
                    }
                }

            }

        }
    }

    @Preview
    @Composable
    fun ScreenPreview() {
        HomeScreen(
            uiState = HomeState(),
            appState = AppState(),
            onTaximeterClick = {},
            onSosClick = {},
            onPqrsClick = {},
            onMyTripsClick = {},
            onTripClicked = {}
        )
    }

}