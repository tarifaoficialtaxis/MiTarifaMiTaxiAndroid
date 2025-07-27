package com.mitarifamitaxi.taximetrousuario.components.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily
import com.mitarifamitaxi.taximetrousuario.helpers.formatNumberWithDots
import com.mitarifamitaxi.taximetrousuario.helpers.getShortAddress
import com.mitarifamitaxi.taximetrousuario.helpers.tripCardFormatDate
import com.mitarifamitaxi.taximetrousuario.models.Trip

@Composable
fun TripItem(
    trip: Trip,
    onTripClicked: () -> Unit,
    isTripSelected: Boolean? = null,
    onTripSelected: (Trip) -> Unit = {}
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(1.dp),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(id = R.color.white)
        )
    ) {

        Row {

            if (isTripSelected != null) {
                CustomCheckBox(
                    checked = isTripSelected,
                    onValueChange = { isChecked ->
                        onTripSelected(trip)
                    },
                    modifier = Modifier
                        .padding(
                            start = 15.dp,
                            top = 15.dp,
                        )
                )
            }

            Button(
                onClick = onTripClicked,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                shape = RectangleShape
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(15.dp),
                    modifier = Modifier
                        .padding(15.dp)
                        .fillMaxWidth()
                ) {

                    Column(
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        trip.startHour?.let {
                            Text(
                                text = if (it.isNotEmpty()) tripCardFormatDate(it) else "-",
                                fontFamily = MontserratFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = colorResource(id = R.color.gray1),
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(15.dp)
                                    .border(2.dp, colorResource(id = R.color.yellow2), CircleShape)
                                    .background(
                                        colorResource(id = R.color.main),
                                        shape = CircleShape
                                    ),
                            )

                            Text(
                                text = trip.startAddress?.getShortAddress() ?: "",
                                fontFamily = MontserratFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = colorResource(id = R.color.gray1),
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {

                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = colorResource(id = R.color.main),
                                modifier = Modifier.size(15.dp)
                            )

                            Text(
                                text = trip.endAddress?.getShortAddress() ?: "",
                                fontFamily = MontserratFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = colorResource(id = R.color.gray1),
                            )
                        }

                        Text(
                            text = "$ ${
                                trip.total?.toInt()?.formatNumberWithDots()
                            } ${trip.currency ?: ""}",
                            fontFamily = MontserratFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = colorResource(id = R.color.main),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(colorResource(id = R.color.main), shape = CircleShape)
                    ) {

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "content description",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(25.dp),
                            tint = colorResource(id = R.color.black),
                        )
                    }


                }
            }
        }


    }
}