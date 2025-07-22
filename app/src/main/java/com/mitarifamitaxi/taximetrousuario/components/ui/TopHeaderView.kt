package com.mitarifamitaxi.taximetrousuario.components.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily
import android.graphics.BlurMaskFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.TextUnit

@Composable
fun TopHeaderView(
    leadingIcon: ImageVector? = null,
    leadingColor: Color = colorResource(id = R.color.main),
    onClickLeading: () -> Unit = {},
    title: String,
    titleFontSize: TextUnit = 20.sp,
    upperCaseTitle: Boolean = true,
    trailingIcon: ImageVector? = null,
    trailingColor: Color = colorResource(id = R.color.main),
    onClickTrailing: () -> Unit = {},
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val shadowHeight = 10.dp.toPx()
                val paint = Paint().asFrameworkPaint().apply {
                    isAntiAlias = true
                    color = 0x55000000
                    maskFilter = BlurMaskFilter(
                        shadowHeight,
                        BlurMaskFilter.Blur.NORMAL
                    )
                }
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawRect(
                        0f,
                        size.height - shadowHeight,
                        size.width,
                        size.height,
                        paint
                    )
                }
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(id = R.color.white))
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {

            if (leadingIcon != null) {
                Button(
                    onClick = { onClickLeading() },
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    shape = RectangleShape,
                    modifier = Modifier
                        .width(40.dp)
                ) {

                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = "content description",
                        modifier = Modifier
                            .size(40.dp)
                            .padding(0.dp),
                        tint = leadingColor,
                    )

                }
            } else {
                Spacer(modifier = Modifier.width(40.dp))
            }


            Text(
                text = if (upperCaseTitle) title.uppercase() else title,
                color = colorResource(id = R.color.black),
                fontSize = titleFontSize,
                fontFamily = MontserratFamily,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
            )

            if (trailingIcon != null) {
                Button(
                    onClick = { onClickTrailing() },
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    shape = RectangleShape,
                    modifier = Modifier
                        .width(40.dp)
                ) {

                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = "content description",
                        modifier = Modifier
                            .size(30.dp)
                            .padding(0.dp),
                        tint = trailingColor,
                    )

                }
            } else {
                Spacer(modifier = Modifier.width(40.dp))
            }
        }
    }


}