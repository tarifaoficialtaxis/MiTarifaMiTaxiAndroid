package com.mitarifamitaxi.taximetrousuario.components.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mitarifamitaxi.taximetrousuario.R


@Composable
fun RegisterHeaderBox() {
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
}