package com.mitarifamitaxi.taximetrousuario.components.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily

@Composable
fun CustomMultilineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isError: Boolean = false,
    errorMessage: String? = null
) {

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(
                    text = placeholder,
                    fontFamily = MontserratFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                )
            },
            minLines = 3,
            maxLines = 10,
            isError = isError,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = colorResource(id = R.color.transparent),
                unfocusedContainerColor = colorResource(id = R.color.transparent),
                errorContainerColor = colorResource(id = R.color.transparent),

                focusedLabelColor = colorResource(id = R.color.gray1),
                focusedPlaceholderColor = colorResource(id = R.color.gray1),
                unfocusedPlaceholderColor = colorResource(id = R.color.gray1),

                focusedTextColor = colorResource(id = R.color.gray1),

                focusedIndicatorColor = colorResource(id = R.color.main),
                unfocusedIndicatorColor = colorResource(id = R.color.gray2),

                errorLabelColor = colorResource(id = R.color.red),
                errorCursorColor = colorResource(id = R.color.main),
                errorIndicatorColor = colorResource(id = R.color.red),
                errorPlaceholderColor = colorResource(id = R.color.red),
            ),
            textStyle = TextStyle(
                fontSize = 14.sp,
                fontFamily = MontserratFamily,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.fillMaxWidth()
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
