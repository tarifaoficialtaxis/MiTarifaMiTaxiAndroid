package com.mitarifamitaxi.taximetrousuario.activities.home

import android.content.Intent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.core.net.toUri
import com.google.gson.Gson
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.activities.BaseActivity
import com.mitarifamitaxi.taximetrousuario.activities.LocalOpenDrawer
import com.mitarifamitaxi.taximetrousuario.activities.pqrs.PqrsActivity
import com.mitarifamitaxi.taximetrousuario.activities.routeplanner.RoutePlannerActivity
import com.mitarifamitaxi.taximetrousuario.activities.sos.SosActivity
import com.mitarifamitaxi.taximetrousuario.activities.trips.MyTripsActivity
import com.mitarifamitaxi.taximetrousuario.activities.trips.TripSummaryActivity
import com.mitarifamitaxi.taximetrousuario.components.ui.NoTripsView
import com.mitarifamitaxi.taximetrousuario.components.ui.ProfilePictureBox
import com.mitarifamitaxi.taximetrousuario.components.ui.TripItem
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily
import com.mitarifamitaxi.taximetrousuario.models.DriverStatus
import com.mitarifamitaxi.taximetrousuario.models.Trip
import com.mitarifamitaxi.taximetrousuario.models.UserRole
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

    @Composable
    override fun Content() {

        val uiState by viewModel.uiState.collectAsState()
        val appState by appViewModel.uiState.collectAsState()

        HomeScreen(
            uiState = uiState,
            appState = appState,
            onRequestServiceClick = {

            },
            onTaximeterClick = {
                startActivity(Intent(this, RoutePlannerActivity::class.java))
                //startActivity(Intent(this, TaximeterActivity::class.java))
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
        onRequestServiceClick: () -> Unit,
        onTaximeterClick: () -> Unit,
        onSosClick: () -> Unit,
        onPqrsClick: () -> Unit,
        onMyTripsClick: () -> Unit,
        onTripClicked: (Trip) -> Unit
    ) {
        val openDrawer = LocalOpenDrawer.current

        Column(
            modifier = Modifier.Companion
                .fillMaxSize()
                .background(colorResource(id = R.color.white)),
        ) {

            Box(
                modifier = Modifier.Companion
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
                    contentScale = ContentScale.Companion.FillWidth,
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                        .height(95.dp)
                        .align(Alignment.Companion.BottomCenter)
                        .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))

                )

                Column(
                    modifier = Modifier.Companion
                        .fillMaxSize()
                        .padding(20.dp)
                ) {

                    OutlinedButton(
                        onClick = { openDrawer() },
                        modifier = Modifier.Companion
                            .size(45.dp)
                            .border(2.dp, colorResource(id = R.color.white), CircleShape),
                        shape = CircleShape,
                        border = BorderStroke(0.dp, Color.Companion.Transparent),
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

                    Spacer(modifier = Modifier.Companion.height(18.dp))
                    Text(
                        text = stringResource(id = R.string.welcome_home),
                        color = colorResource(id = R.color.white),
                        fontSize = 20.sp,
                        fontFamily = MontserratFamily,
                        fontWeight = FontWeight.Companion.Medium,
                        modifier = Modifier.Companion
                            .align(Alignment.Companion.Start)
                    )

                    Text(
                        text = appState.userData?.firstName ?: "",
                        color = colorResource(id = R.color.main),
                        fontSize = 20.sp,
                        fontFamily = MontserratFamily,
                        fontWeight = FontWeight.Companion.Bold,
                        modifier = Modifier.Companion
                            .align(Alignment.Companion.Start)
                    )
                    Spacer(modifier = Modifier.Companion.height(5.dp))

                    Text(
                        text = appState.userData?.city ?: "",
                        color = colorResource(id = R.color.white),
                        fontSize = 14.sp,
                        fontFamily = MontserratFamily,
                        fontWeight = FontWeight.Companion.Normal,
                        modifier = Modifier.Companion
                            .align(Alignment.Companion.Start)
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.home_taxi),
                    contentDescription = null,
                    modifier = Modifier.Companion
                        .width(220.dp)
                        .height(100.dp)
                        .align(Alignment.Companion.BottomEnd)
                        .offset(x = 30.dp, y = 15.dp)
                )
            }

            if (appState.isGettingLocation) {
                Column(
                    horizontalAlignment = Alignment.Companion.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.Companion.height(60.dp))
                    Text(
                        text = stringResource(id = R.string.getting_location),
                        color = colorResource(id = R.color.black),
                        fontSize = 20.sp,
                        fontFamily = MontserratFamily,
                        fontWeight = FontWeight.Companion.Medium,
                        textAlign = TextAlign.Companion.Center,
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    )
                    CircularProgressIndicator(
                        color = colorResource(id = R.color.black)
                    )
                }
            } else {

                if (appState.userData?.role == UserRole.USER) {
                    UserView(
                        uiState = uiState,
                        onRequestServiceClick = onRequestServiceClick,
                        onTaximeterClick = onTaximeterClick,
                        onSosClick = onSosClick,
                        onPqrsClick = onPqrsClick,
                        onMyTripsClick = onMyTripsClick,
                        onTripClicked = onTripClicked
                    )
                } else {
                    DriverView(
                        appState = appState,
                        onTaximeterClick = onTaximeterClick,
                        onSosClick = onSosClick
                    )
                }

            }
        }
    }

    @Composable
    private fun UserView(
        uiState: HomeState,
        onRequestServiceClick: () -> Unit,
        onTaximeterClick: () -> Unit,
        onSosClick: () -> Unit,
        onPqrsClick: () -> Unit,
        onMyTripsClick: () -> Unit,
        onTripClicked: (Trip) -> Unit
    ) {

        Column(
            Modifier.Companion
                .padding(horizontal = 29.dp)
                .padding(top = 30.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {

            OutlinedButton(
                onClick = onRequestServiceClick,
                modifier = Modifier.Companion
                    .fillMaxWidth(),
                border = null,
                contentPadding = PaddingValues(0.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.home_request_service),
                    contentDescription = null,
                    contentScale = ContentScale.Companion.FillWidth,
                    modifier = Modifier.Companion
                        .fillMaxSize()
                )
            }

            OutlinedButton(
                onClick = onTaximeterClick,
                modifier = Modifier.Companion
                    .fillMaxWidth(),
                border = null,
                contentPadding = PaddingValues(0.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.home_taximetro_button),
                    contentDescription = null,
                    contentScale = ContentScale.Companion.FillWidth,
                    modifier = Modifier.Companion
                        .fillMaxSize()
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(11.dp),
                modifier = Modifier.Companion
                    .fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onSosClick,
                    modifier = Modifier.Companion
                        .weight(1.0f),
                    border = null,
                    contentPadding = PaddingValues(0.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.home_sos_button),
                        contentDescription = null,
                        contentScale = ContentScale.Companion.FillWidth,
                        modifier = Modifier.Companion
                            .fillMaxSize()
                    )
                }

                OutlinedButton(
                    onClick = onPqrsClick,
                    modifier = Modifier.Companion
                        .weight(1.0f),
                    border = null,
                    contentPadding = PaddingValues(0.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.home_pqrs_button),
                        contentDescription = null,
                        contentScale = ContentScale.Companion.FillWidth,
                        modifier = Modifier.Companion
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
                        fontWeight = FontWeight.Companion.Bold,
                        modifier = Modifier.Companion
                            .padding(top = 15.dp)
                    )

                    Spacer(modifier = Modifier.Companion.weight(1.0f))

                    if (uiState.trips.isNotEmpty()) {
                        TextButton(onClick = onMyTripsClick) {
                            Text(
                                text = stringResource(id = R.string.see_all),
                                color = colorResource(id = R.color.main),
                                textDecoration = TextDecoration.Companion.Underline,
                                fontSize = 14.sp,
                                fontFamily = MontserratFamily,
                                fontWeight = FontWeight.Companion.Bold,
                            )
                        }
                    }
                }


                if (uiState.trips.isEmpty()) {
                    NoTripsView()
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(11.dp),
                        modifier = Modifier.Companion
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

    @Composable
    private fun DriverView(
        appState: AppState,
        onTaximeterClick: () -> Unit,
        onSosClick: () -> Unit
    ) {
        Column(
            Modifier.Companion
                .padding(horizontal = 29.dp)
                .padding(top = 30.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            OutlinedButton(
                onClick = onTaximeterClick,
                modifier = Modifier.Companion
                    .fillMaxWidth(),
                border = null,
                contentPadding = PaddingValues(0.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.home_taximetro_button),
                    contentDescription = null,
                    contentScale = ContentScale.Companion.Fit,
                    modifier = Modifier.Companion
                        .fillMaxSize()
                )
            }

            OutlinedButton(
                onClick = onSosClick,
                modifier = Modifier.Companion
                    .fillMaxWidth(),
                border = null,
                contentPadding = PaddingValues(0.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.home_sos_driver_button),
                    contentDescription = null,
                    contentScale = ContentScale.Companion.Fit,
                    modifier = Modifier.Companion
                        .fillMaxSize()
                )
            }

            if (appState.userData?.driverStatus == DriverStatus.PENDING) {
                DriverPendingReviewView()
            }
        }
    }

    @Composable
    private fun DriverPendingReviewView() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .border(8.dp, colorResource(id = R.color.yellow2), CircleShape)
                    .background(colorResource(id = R.color.main), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PriorityHigh,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                text = stringResource(id = R.string.account_pending_review),
                color = colorResource(id = R.color.main),
                fontFamily = MontserratFamily,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 5.dp),
            )

            Text(
                text = stringResource(id = R.string.let_you_know),
                color = colorResource(id = R.color.gray1),
                fontFamily = MontserratFamily,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp
            )


        }
    }


}