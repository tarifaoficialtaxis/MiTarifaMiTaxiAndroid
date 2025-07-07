package com.mitarifamitaxi.taximetrousuario.components.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isSecure: Boolean = false,
    isEnabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    leadingIcon: ImageVector,
    trailingIcon: ImageVector? = null,
    onClickTrailingIcon: () -> Unit = {},
    onFocusChanged: (Boolean) -> Unit = {},
    isError: Boolean = false,
    errorMessage: String? = null,
    focusedIndicatorColor: Color = colorResource(id = R.color.main),
    unfocusedIndicatorColor: Color = colorResource(id = R.color.gray2),
) {
    val isPasswordVisible = remember { mutableStateOf(isSecure) }

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val labelFontSize by mutableFloatStateOf(if (isFocused || value.isNotEmpty()) 12f else 14f)

    Column(modifier = Modifier.fillMaxWidth()) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            enabled = isEnabled,
            interactionSource = interactionSource,
            label = {
                Text(
                    text = placeholder,
                    fontFamily = MontserratFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = labelFontSize.sp
                )
            },
            isError = isError,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isSecure) KeyboardType.Password else keyboardType,
                capitalization = capitalization
            ),
            singleLine = true,
            visualTransformation = if (isPasswordVisible.value) PasswordVisualTransformation() else VisualTransformation.None,
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = colorResource(id = R.color.main)
                )
            },
            trailingIcon = {
                if (isSecure) {
                    Icon(
                        imageVector = if (isPasswordVisible.value) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = null,
                        tint = colorResource(id = R.color.main),
                        modifier = Modifier.clickable {
                            isPasswordVisible.value = !isPasswordVisible.value
                        }
                    )
                } else {
                    trailingIcon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = colorResource(id = R.color.main),
                            modifier = Modifier.clickable {
                                onClickTrailingIcon()
                            }
                        )
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = colorResource(id = R.color.transparent),
                unfocusedContainerColor = colorResource(id = R.color.transparent),
                errorContainerColor = colorResource(id = R.color.transparent),

                focusedLabelColor = colorResource(id = R.color.gray1),
                focusedPlaceholderColor = colorResource(id = R.color.gray1),
                unfocusedPlaceholderColor = colorResource(id = R.color.gray1),

                focusedTextColor = colorResource(id = R.color.gray1),

                focusedIndicatorColor = focusedIndicatorColor,
                unfocusedIndicatorColor = unfocusedIndicatorColor,

                errorLabelColor = colorResource(id = R.color.red),
                errorCursorColor = colorResource(id = R.color.main),
                errorIndicatorColor = colorResource(id = R.color.red),
                errorPlaceholderColor = colorResource(id = R.color.red),

                disabledContainerColor = colorResource(id = R.color.transparent),
                disabledIndicatorColor = colorResource(id = R.color.transparent),
                disabledTextColor = colorResource(id = R.color.gray1),
                disabledLabelColor = colorResource(id = R.color.gray1),

                ),
            textStyle = TextStyle(
                fontSize = 15.sp,
                fontFamily = MontserratFamily,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .onFocusChanged { focusState ->
                    onFocusChanged(focusState.isFocused)
                }
        )

        if (isError && !errorMessage.isNullOrEmpty()) {
            Row(
                modifier = Modifier.padding(start = 5.dp, top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Error,
                    contentDescription = "Error Icon",
                    modifier = Modifier.size(16.dp),
                    tint = colorResource(id = R.color.red)
                )
                Text(
                    text = errorMessage,
                    color = colorResource(id = R.color.red),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
