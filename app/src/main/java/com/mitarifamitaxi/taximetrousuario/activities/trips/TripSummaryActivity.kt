package com.mitarifamitaxi.taximetrousuario.activities.trips

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.activities.BaseActivity
import com.mitarifamitaxi.taximetrousuario.activities.home.HomeActivity
import com.mitarifamitaxi.taximetrousuario.activities.pqrs.PqrsActivity
import com.mitarifamitaxi.taximetrousuario.activities.sos.SosActivity
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomButton
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomTextFieldDialog
import com.mitarifamitaxi.taximetrousuario.components.ui.TopHeaderView
import com.mitarifamitaxi.taximetrousuario.components.ui.TripInfoRow
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily
import com.mitarifamitaxi.taximetrousuario.helpers.formatDigits
import com.mitarifamitaxi.taximetrousuario.helpers.formatElapsed
import com.mitarifamitaxi.taximetrousuario.helpers.formatNumberWithDots
import com.mitarifamitaxi.taximetrousuario.helpers.getShortAddress
import com.mitarifamitaxi.taximetrousuario.helpers.hourFormatDate
import com.mitarifamitaxi.taximetrousuario.helpers.tripSummaryFormatDate
import com.mitarifamitaxi.taximetrousuario.models.Trip
import com.mitarifamitaxi.taximetrousuario.models.UserLocation
import com.mitarifamitaxi.taximetrousuario.states.TripSummaryState
import com.mitarifamitaxi.taximetrousuario.viewmodels.trips.TripSummaryViewModel
import com.mitarifamitaxi.taximetrousuario.viewmodels.trips.TripSummaryViewModelFactory
import kotlinx.coroutines.launch

class TripSummaryActivity : BaseActivity() {

    private val viewModel: TripSummaryViewModel by viewModels {
        TripSummaryViewModelFactory(this, appViewModel)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeViewModelEvents()

        val isDetails = intent.getBooleanExtra("is_details", false)
        viewModel.onChangeDetails(isDetails)
        val tripJson = intent.getStringExtra("trip_data")
        tripJson?.let {
            viewModel.setTrip(Gson().fromJson(it, Trip::class.java))
        }

        val imageBytes = intent.getByteArrayExtra("trip_image_bytes")
        imageBytes?.let {
            viewModel.setRouteImageLocal(BitmapFactory.decodeByteArray(it, 0, it.size))
        }
    }


    private fun observeViewModelEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationEvents.collect { event ->
                    when (event) {
                        is TripSummaryViewModel.NavigationEvent.GoBack -> {
                            finishAction()
                        }
                    }
                }
            }
        }
    }

    private fun finishAction() {
        //interstitialAdManager.showAd(this) {
        val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        //}
    }

    @Composable
    override fun Content() {

        val uiState by viewModel.uiState.collectAsState()

        TripSummaryScreen(
            uiState = uiState,
            onDeleteAction = {
                viewModel.onDeleteAction()
            },
            onSosAction = {
                val intent = Intent(this, SosActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            },
            onPqrsAction = {
                val intent = Intent(this, PqrsActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            },
            onShareAction = {
                viewModel.onShowShareDialog(true)
            },
            onFinishAction = {
                finishAction()
            }
        )

    }

    @Composable
    private fun TripSummaryScreen(
        uiState: TripSummaryState,
        onDeleteAction: () -> Unit,
        onSosAction: () -> Unit,
        onPqrsAction: () -> Unit,
        onShareAction: () -> Unit,
        onFinishAction: () -> Unit
    ) {

        Column(
            modifier = Modifier
                .background(colorResource(id = R.color.gray4))
                .fillMaxSize()
        ) {
            TopHeaderView(
                title = stringResource(id = R.string.trip_summary),
                leadingIcon = Icons.Filled.ChevronLeft,
                onClickLeading = {
                    if (uiState.isDetails) {
                        finish()
                    } else {
                        onFinishAction()
                    }
                },
                trailingIcon = if (uiState.isDetails) Icons.Filled.Delete else null,
                onClickTrailing = onDeleteAction
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                if (uiState.isDetails) {
                    AsyncImage(
                        model = uiState.tripData.routeImage,
                        contentDescription = "Trip route map image",
                        contentScale = ContentScale.Companion.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                } else {
                    uiState.routeImageLocal?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Companion.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 29.dp)
                        .padding(horizontal = 29.dp)
                ) {

                    Row {
                        Column {
                            uiState.tripData.startHour?.let {
                                Text(
                                    text = if (it.isNotEmpty()) tripSummaryFormatDate(it) else "-",
                                    fontFamily = MontserratFamily,
                                    fontWeight = FontWeight.Companion.Medium,
                                    fontSize = 20.sp,
                                    color = colorResource(id = R.color.blue1),
                                )
                            }

                            Text(
                                text = "$ ${
                                    uiState.tripData.total?.toInt()?.formatNumberWithDots()
                                } ${uiState.tripData.currency}",
                                fontFamily = MontserratFamily,
                                fontWeight = FontWeight.Companion.Bold,
                                fontSize = 24.sp,
                                color = colorResource(id = R.color.main),
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        if (uiState.tripData.companyImage != null) {
                            AsyncImage(
                                model = uiState.tripData.companyImage,
                                contentDescription = "Company logo",
                                contentScale = ContentScale.Companion.Crop,
                                modifier = Modifier
                                    .size(70.dp)
                            )
                        }
                    }



                    Row(
                        verticalAlignment = Alignment.Companion.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(top = 10.dp),
                    ) {

                        Box(
                            modifier = Modifier
                                .size(15.dp)
                                .border(2.dp, colorResource(id = R.color.yellow2), CircleShape)
                                .background(colorResource(id = R.color.main), shape = CircleShape),
                        )

                        Text(
                            text = uiState.tripData.startAddress ?: "",
                            fontFamily = MontserratFamily,
                            fontWeight = FontWeight.Companion.Normal,
                            fontSize = 12.sp,
                            color = colorResource(id = R.color.black),
                            modifier = Modifier.weight(0.8f)
                        )

                        Spacer(modifier = Modifier.weight(0.2f))

                        uiState.tripData.startHour?.let {
                            Text(
                                text = if (it.isNotEmpty()) hourFormatDate(it) else "-",
                                fontFamily = MontserratFamily,
                                fontWeight = FontWeight.Companion.Medium,
                                fontSize = 14.sp,
                                color = colorResource(id = R.color.gray1),
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Companion.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(bottom = 10.dp),
                    ) {

                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = colorResource(id = R.color.main),
                            modifier = Modifier.size(15.dp)
                        )

                        Text(
                            text = uiState.tripData.endAddress?.getShortAddress() ?: "",
                            fontFamily = MontserratFamily,
                            fontWeight = FontWeight.Companion.Normal,
                            fontSize = 12.sp,
                            color = colorResource(id = R.color.black),
                            modifier = Modifier.weight(0.8f)
                        )

                        Spacer(modifier = Modifier.weight(0.2f))

                        uiState.tripData.endHour?.let {
                            Text(
                                text = if (it.isNotEmpty()) hourFormatDate(it) else "-",
                                fontFamily = MontserratFamily,
                                fontWeight = FontWeight.Companion.Medium,
                                fontSize = 14.sp,
                                color = colorResource(id = R.color.gray1),
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(colorResource(id = R.color.gray2))
                    )

                    if (uiState.tripData.units != null && uiState.tripData.showUnits == true) {
                        TripInfoRow(
                            title = stringResource(id = R.string.units),
                            value = uiState.tripData.units.toInt().toString()
                        )
                    }

                    if (uiState.tripData.startHour != null && uiState.tripData.endHour != null) {
                        TripInfoRow(
                            title = stringResource(id = R.string.time_trip),
                            value = formatElapsed(
                                uiState.tripData.startHour,
                                uiState.tripData.endHour
                            )
                        )
                    }

                    if (uiState.tripData.total != null) {
                        TripInfoRow(
                            title = stringResource(id = R.string.total),
                            value = "$${
                                uiState.tripData.total.toInt()
                                    .formatNumberWithDots()
                            } ${uiState.tripData.currency}"
                        )
                    }

                    Row {

                        Spacer(
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                viewModel.onChangeIsDetailsOpen(!uiState.isDetailsOpen)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorResource(id = R.color.transparent)
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {

                                Text(
                                    text = stringResource(id = R.string.see_details),
                                    fontFamily = MontserratFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = colorResource(id = R.color.main),
                                )

                                Icon(
                                    imageVector = if (uiState.isDetailsOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = colorResource(id = R.color.main),
                                    modifier = Modifier
                                        .size(32.dp)
                                )

                            }


                        }
                    }

                    if (uiState.isDetailsOpen) {
                        TripInfoRow(
                            title = stringResource(id = R.string.distance_made),
                            value = "${((uiState.tripData.distance ?: 0.0) / 1000).formatDigits(1)} KM"
                        )

                        TripInfoRow(
                            title = stringResource(id = R.string.base),
                            value = ""
                        )

                        Column(
                            modifier = Modifier
                                .padding(start = 29.dp)
                        ) {
                            if (uiState.tripData.baseUnits != null && uiState.tripData.showUnits == true) {
                                TripInfoRow(
                                    title = stringResource(id = R.string.units_base),
                                    value = uiState.tripData.baseUnits.toInt().toString()
                                )
                            }

                            TripInfoRow(
                                title = stringResource(id = R.string.fare_base),
                                value = "$${
                                    uiState.tripData.baseRate?.toInt()?.formatNumberWithDots()
                                } ${uiState.tripData.currency}"
                            )
                        }

                        if (uiState.tripData.recharges.isNotEmpty()) {

                            TripInfoRow(
                                title = stringResource(id = R.string.recharges),
                                value = ""
                            )

                            Column(
                                modifier = Modifier
                                    .padding(start = 29.dp)
                            ) {

                                if (uiState.tripData.showUnits == true) {

                                    uiState.tripData.rechargeUnits?.takeIf { it > 0.0 }?.let {
                                        TripInfoRow(
                                            title = stringResource(id = R.string.units_recharge),
                                            value = it.toInt().toString()
                                        )
                                    }
                                }

                                uiState.tripData.recharges.forEach { recharge ->
                                    TripInfoRow(
                                        title = recharge.name ?: "",
                                        value = "+$${
                                            ((recharge.units ?: 0.0) * (uiState.tripData.unitPrice ?: 0.0)).formatNumberWithDots()
                                        } ${uiState.tripData.currency}"
                                    )
                                }
                            }
                        }
                    }


                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 20.dp)
                    ) {

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                CustomButton(
                                    text = stringResource(id = R.string.sos).uppercase(),
                                    onClick = { onSosAction() },
                                    color = colorResource(id = R.color.red1),
                                    leadingIcon = Icons.Rounded.WarningAmber
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                CustomButton(
                                    text = stringResource(id = R.string.pqrs).uppercase(),
                                    onClick = { onPqrsAction() },
                                    color = colorResource(id = R.color.blue2),
                                    leadingIcon = Icons.AutoMirrored.Outlined.Chat
                                )
                            }
                        }

                        CustomButton(
                            text = stringResource(id = R.string.share).uppercase(),
                            onClick = onShareAction,
                            leadingIcon = Icons.Rounded.Share
                        )

                        if (!uiState.isDetails) {
                            CustomButton(
                                text = stringResource(id = R.string.back_home).uppercase(),
                                onClick = onFinishAction,
                                color = colorResource(id = R.color.gray1)
                            )
                        }
                    }

                }

            }


        }

        if (uiState.showShareDialog) {
            CustomTextFieldDialog(
                title = getString(R.string.share_trip),
                message = getString(R.string.share_trip_message),
                textButton = getString(R.string.send),
                textFieldValue = uiState.shareNumber,
                onValueChange = { newNumber ->
                    viewModel.onShareNumberChange(newNumber)
                },
                isTextFieldError = uiState.isShareNumberError,
                onDismiss = { viewModel.onShowShareDialog(false) },
                onButtonClicked = {
                    viewModel.sendWatsAppMessage(
                        onIntentReady = { intent ->
                            startActivity(intent)
                        }
                    )
                }
            )
        }
    }

    @Preview
    @Composable
    fun ScreenPreview() {
        TripSummaryScreen(
            uiState = TripSummaryState(
                tripData = Trip(
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
                )
            ),
            onDeleteAction = {},
            onSosAction = {},
            onPqrsAction = {},
            onShareAction = {},
            onFinishAction = {}
        )
    }
}
