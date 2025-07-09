package com.mitarifamitaxi.taximetrousuario.activities.trips

import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.mitarifamitaxi.taximetrousuario.BuildConfig
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.activities.BaseActivity
import com.mitarifamitaxi.taximetrousuario.components.adds.BottomBannerAd
import com.mitarifamitaxi.taximetrousuario.components.ui.NoTripsView
import com.mitarifamitaxi.taximetrousuario.components.ui.TopHeaderView
import com.mitarifamitaxi.taximetrousuario.components.ui.TripItem
import com.mitarifamitaxi.taximetrousuario.models.Trip
import com.mitarifamitaxi.taximetrousuario.models.UserLocation
import com.mitarifamitaxi.taximetrousuario.states.MyTripsState
import com.mitarifamitaxi.taximetrousuario.viewmodels.trips.MyTripsViewModel
import com.mitarifamitaxi.taximetrousuario.viewmodels.trips.MyTripsViewModelFactory

class MyTripsActivity : BaseActivity() {

    override fun isDrawerEnabled(): Boolean = true

    private val viewModel: MyTripsViewModel by viewModels {
        MyTripsViewModelFactory(this, appViewModel)
    }

    @Composable
    override fun Content() {
        val uiState by viewModel.uiState.collectAsState()
        MyTripsScreen(
            uiState = uiState,
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
    private fun MyTripsScreen(
        uiState: MyTripsState,
        onTripClicked: (Trip) -> Unit
    ) {

        Column(
            modifier = Modifier
                .background(colorResource(id = R.color.gray4))
                .fillMaxSize(),
        ) {
            TopHeaderView(
                title = stringResource(id = R.string.my_trips),
                leadingIcon = Icons.Filled.ChevronLeft,
                onClickLeading = {
                    finish()
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 29.dp)
                    //.padding(bottom = 40.dp)
                    .padding(horizontal = 29.dp)
            ) {
                if (uiState.trips.isEmpty()) {
                    NoTripsView()
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(11.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        uiState.trips.forEach { trip ->
                            TripItem(
                                trip,
                                onTripClicked = { onTripClicked(trip) }
                            )
                        }
                    }
                }
            }

            BottomBannerAd(adId = BuildConfig.MY_TRIPS_AD_UNIT_ID)
        }
    }

    @Preview
    @Composable
    fun ScreenPreview() {
        MyTripsScreen(
            uiState = MyTripsState(
                trips = listOf(
                    Trip(
                        endAddress = "Welland Ave + Bunting Rd, St. Catharines, ON L2M 5V7, Canada",
                        startCoords = UserLocation(43.158396629424381, -79.223706008781051),
                        currency = "CAD",
                        total = 12675.266674339222,
                        routeImage = "https://firebasestorage.googleapis.com:443/v0/b/mitarifamitaxi-4a0e2.appspot.com/o/images%2F1749259927386.186.png?alt=media&token=0f39484a-7217-4760-83cd-32a7ca7decf3",
                        endHour = "2025-06-07T01:32:06.798000Z",
                        startHour = "2025-06-07T01:24:58.292000Z",
                        endCoords = UserLocation(43.176251155140861, -79.212042830449874),
                        units = 86.226303907069536,
                        distance = 3041.6303907069505,
                        baseRate = 9175.1966743392222,
                        startAddress = "50 Herrick Ave, St. Catharines, ON L2P 0G3, Canada",
                        userId = "uIeFGe937wd0d0r5ItGK7RfFKut2"
                    ),
                    Trip(
                        startHour = "2025-06-07T16:31:24.506000Z",
                        startAddress = "Welland Ave + Bunting Rd, St. Catharines, ON L2M 5V7, Canada",
                        endAddress = "270 Colborne St, Welland, ON L3B 3P1, Canada",
                        distance = 30567.82467867462,
                        endHour = "2025-06-07T17:12:37.079000Z",
                        units = 374.67824678674646,
                        endCoords = UserLocation(42.95842187247974, -79.253060534018289),
                        baseRate = 55077.702277651726,
                        currency = "CAD",
                        startCoords = UserLocation(43.176251155140861, -79.212042830449874),
                        total = 55077.702277651726,
                        routeImage = "https://firebasestorage.googleapis.com:443/v0/b/mitarifamitaxi-4a0e2.appspot.com/o/images%2F1749316357640.529.png?alt=media&token=95ceff02-110d-4086-b5e9-0b1dc2fec521",
                        userId = "uIeFGe937wd0d0r5ItGK7RfFKut2"
                    )
                )
            ),
            onTripClicked = {}
        )
    }


}