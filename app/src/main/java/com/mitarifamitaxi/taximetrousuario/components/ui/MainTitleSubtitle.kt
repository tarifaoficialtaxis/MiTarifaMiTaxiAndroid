package com.mitarifamitaxi.taximetrousuario.components.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.mitarifamitaxi.taximetrousuario.R

@Composable
fun MainTitleSubtitle(
    title: String,
    titleColor: Color = colorResource(id = R.color.main),
    subtitle: String,
    subtitleColor: Color = colorResource(id = R.color.black),
    modifier: Modifier = Modifier.fillMaxWidth()
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
    ) {

        MainTitleText(
            title = title,
            titleColor = titleColor,
            text = subtitle,
            textColor = subtitleColor
        )
        MainSubtitle(
            subtitle = subtitle,
            subtitleColor = subtitleColor
        )
    }
}