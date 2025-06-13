package com.mitarifamitaxi.taximetrousuario.components.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CustomButton(
    text: String,
    onClick: () -> Unit,
    color: Color = colorResource(id = R.color.main),
    leadingIcon: ImageVector? = null,
    modifier: Modifier? = Modifier.fillMaxWidth(),
) {

    var isClicked by remember { mutableStateOf(false) }

    Button(
        onClick = {
            if (!isClicked) {
                isClicked = true
                onClick()
                CoroutineScope(Dispatchers.Main).launch {
                    delay(1000L)
                    isClicked = false
                }
            }
        },
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(50),
        enabled = !isClicked,
        modifier = modifier ?: Modifier.fillMaxWidth()
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
        ) {

            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = colorResource(id = R.color.white),
                    modifier = Modifier
                        .padding(end = 10.dp)
                )
            }

            Text(
                text = text,
                fontFamily = MontserratFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier
                    .padding(vertical = 3.dp),
                textAlign = TextAlign.Center,
            )
        }


    }

}