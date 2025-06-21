package com.mitarifamitaxi.taximetrousuario.components.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mitarifamitaxi.taximetrousuario.R


@Composable
fun RegisterHeaderBox() {
    Box(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .height(LocalConfiguration.current.screenHeightDp.dp * 0.27f)
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
                    .fillMaxWidth()
                    .align(Alignment.Companion.BottomCenter)
                    .padding(bottom = 30.dp)
            )

            Image(
                painter = painterResource(id = R.drawable.logo3),
                contentDescription = null,
                modifier = Modifier.Companion
                    .height(91.dp)
                    .align(Alignment.Companion.Center)
            )
        }
    }
}

@Preview
@Composable
fun RegisterHeaderBoxPreview() {
    RegisterHeaderBox()
}