package com.mitarifamitaxi.taximetrousuario.components.ui

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect

@Composable
fun SpeedLimitBox(
    speed: Int,
    speedLimit: Int,
    units: String,
    modifier: Modifier = Modifier
) {

    val speedColor = when {
        speed > speedLimit + 5 -> colorResource(id = R.color.red1)
        speed > speedLimit -> colorResource(id = R.color.mainYellow)
        else -> colorResource(id = R.color.black)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(
                color = colorResource(id = R.color.white),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 5.dp)

    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .border(
                    width = 2.dp,
                    color = colorResource(id = R.color.black),
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            Text(
                text = speedLimit.toString(),
                fontFamily = MontserratFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 25.sp,
                modifier = Modifier
                    .padding(vertical = 10.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((-7).dp),
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = speed.toString(),
                fontFamily = MontserratFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 25.sp,
                color = speedColor,
            )

            Text(
                text = units,
                fontFamily = MontserratFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                color = colorResource(id = R.color.black),
            )

        }
    }
}
