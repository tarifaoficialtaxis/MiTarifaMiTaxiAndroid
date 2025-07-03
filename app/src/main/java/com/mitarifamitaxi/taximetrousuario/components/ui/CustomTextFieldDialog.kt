package com.mitarifamitaxi.taximetrousuario.components.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily

@Composable
fun CustomTextFieldDialog(
    title: String,
    message: String,
    textFieldValue: String,
    onValueChange: (String) -> Unit,
    isTextFieldError: Boolean,
    textButton: String,
    onDismiss: () -> Unit,
    onButtonClicked: () -> Unit
) {

    colorResource(id = R.color.main)
    colorResource(id = R.color.yellow2)

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
                .padding(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .border(8.dp, colorResource(id = R.color.yellow2), CircleShape)
                        .background(colorResource(id = R.color.main), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PriorityHigh,
                        contentDescription = "Error Icon",
                        tint = Color.White,
                        modifier = Modifier.size(55.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    fontFamily = MontserratFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = colorResource(id = R.color.main)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontFamily = MontserratFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    CustomTextField(
                        value = textFieldValue,
                        onValueChange = onValueChange,
                        placeholder = stringResource(id = R.string.mobilePhone),
                        leadingIcon = Icons.Rounded.PhoneIphone,
                        keyboardType = KeyboardType.Phone,
                        isError = isTextFieldError,
                        errorMessage = stringResource(id = R.string.error_missing_phone_number)
                    )
                }


                Spacer(modifier = Modifier.height(15.dp))

                CustomButton(
                    text = textButton.uppercase(),
                    onClick = onButtonClicked,
                    color = colorResource(id = R.color.main),
                )

                Spacer(modifier = Modifier.height(15.dp))

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

