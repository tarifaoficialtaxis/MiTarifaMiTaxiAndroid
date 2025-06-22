package com.mitarifamitaxi.taximetrousuario.components.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily

@Composable
fun MainTitleText(
    title: String,
    titleSize: TextUnit = 24.sp,
    titleColor: Color = colorResource(id = R.color.main),
    text: String? = null,
    textColor: Color = colorResource(id = R.color.gray1),
    modifier: Modifier = Modifier.fillMaxWidth()
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
    ) {

        Text(
            text = title,
            color = titleColor,
            fontSize = titleSize,
            fontFamily = MontserratFamily,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
        )

        if (text != null) {
            Text(
                text = text,
                color = textColor,
                fontSize = 14.sp,
                fontFamily = MontserratFamily,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    }


}