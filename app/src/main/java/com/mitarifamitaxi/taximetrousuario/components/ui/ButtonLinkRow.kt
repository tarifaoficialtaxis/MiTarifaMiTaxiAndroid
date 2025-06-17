package com.mitarifamitaxi.taximetrousuario.components.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily

@Composable
fun ButtonLinkRow(
    text: String,
    onClick: () -> Unit,
) {

    Button(
        onClick = onClick,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 12.dp)
        ) {

            Text(
                text = text,
                modifier = Modifier
                    .weight(1f),
                fontFamily = MontserratFamily,
                fontSize = 14.sp,
                color = colorResource(id = R.color.black),
                fontWeight = FontWeight.Bold
            )

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