package com.mitarifamitaxi.taximetrousuario.components.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.K

@Composable
fun TwoOptionSelectorDialog(
    title: String,
    primaryTitle: String,
    secondaryTitle: String,
    primaryIcon: ImageVector,
    secondaryIcon: ImageVector,
    onDismiss: () -> Unit,
    onPrimaryActionClicked: () -> Unit,
    onSecondaryActionClicked: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {}
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .align(Alignment.BottomCenter)
                .background(
                    Color.White,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
                .padding(K.GENERAL_PADDING)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                MainSubtitle(
                    subtitle = title,
                    subtitleColor = colorResource(id = R.color.main)
                )


                Spacer(modifier = Modifier.height(29.dp))


                Row(
                    horizontalArrangement = Arrangement.spacedBy(29.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
                {

                    CustomContactBoxView(
                        icon = primaryIcon,
                        text = primaryTitle,
                        onClick = onPrimaryActionClicked,
                        modifier = Modifier
                            .weight(1f)
                    )

                    CustomContactBoxView(
                        icon = secondaryIcon,
                        text = secondaryTitle,
                        onClick = onSecondaryActionClicked,
                        modifier = Modifier
                            .weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(29.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    border = BorderStroke(0.dp, Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = colorResource(id = R.color.gray7),
                        contentColor = colorResource(id = R.color.white)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "content description"
                    )
                }

            }
        }
    }
}

