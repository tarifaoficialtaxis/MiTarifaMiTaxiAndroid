package com.mitarifamitaxi.taximetrousuario.components.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily

@Composable
fun TaximeterInfoRow(
    title: String,
    value: String
) {
    Column {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 5.dp)

        ) {

            Text(
                text = title,
                fontFamily = MontserratFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = colorResource(id = R.color.gray1),
                modifier = Modifier.weight(0.7f)
            )

            Text(
                text = value,
                fontFamily = MontserratFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = colorResource(id = R.color.main),
                modifier = Modifier.weight(0.3f),
                textAlign = TextAlign.End
            )

        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(colorResource(id = R.color.gray2))
        )

    }


}