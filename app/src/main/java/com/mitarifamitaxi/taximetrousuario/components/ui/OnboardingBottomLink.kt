package com.mitarifamitaxi.taximetrousuario.components.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily

@Composable
fun OnboardingBottomLink(
    text: String,
    linkText: String,
    onClick: () -> Unit
) {

    Button(
        onClick = { onClick() },
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(id = R.color.transparent),
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Companion.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                10.dp,
                Alignment.Companion.CenterHorizontally
            ),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = text,
                fontFamily = MontserratFamily,
                fontWeight = FontWeight.Companion.Medium,
                fontSize = 14.sp,
                color = colorResource(id = R.color.gray1),
            )

            Text(
                text = linkText,
                fontFamily = MontserratFamily,
                fontWeight = FontWeight.Companion.Bold,
                fontSize = 14.sp,
                color = colorResource(id = R.color.main),
            )
        }
    }
}