package com.mitarifamitaxi.taximetrousuario.components.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Whatsapp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily
import com.mitarifamitaxi.taximetrousuario.models.ContactCatalog
import com.mitarifamitaxi.taximetrousuario.states.SosState

@Composable
fun CustomContactActionDialog(
    title: String,
    contactCatalog: ContactCatalog,
    onDismiss: () -> Unit,
    onCallAction: () -> Unit,
    onMessageAction: () -> Unit
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
                .padding(29.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    text = title,
                    fontFamily = MontserratFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = colorResource(id = R.color.main)
                )

                Spacer(modifier = Modifier.height(29.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(29.dp)
                ) {

                    if (contactCatalog.line1 != null) {
                        CustomCardCallAction(
                            icon = Icons.Default.Call,
                            text = stringResource(id = R.string.emergency_line),
                            number = contactCatalog.line1,
                            onClick = onCallAction,
                        )
                    }

                    if (contactCatalog.line2 != null) {
                        CustomCardCallAction(
                            icon = Icons.Default.Call,
                            text = stringResource(id = R.string.phone),
                            number = contactCatalog.line2,
                            onClick = onCallAction,
                        )
                    }

                    if (contactCatalog.whatsapp != null) {
                        CustomCardCallAction(
                            icon = Icons.Default.Whatsapp,
                            text = stringResource(id = R.string.whatsapp),
                            number = contactCatalog.whatsapp,
                            onClick = onMessageAction,
                        )
                    }


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
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "content description"
                    )
                }



                Spacer(modifier = Modifier.height(16.dp))

            }
        }
    }


}

@Preview
@Composable
fun CustomContactActionDialogPreview() {
    CustomContactActionDialog(
        title = "Contact Us",
        contactCatalog = ContactCatalog(
            line1 = "1234567890",
            line2 = "0987654321",
            whatsapp = "1234567890"
        ),
        onDismiss = {},
        onCallAction = {},
        onMessageAction = {}
    )
}
