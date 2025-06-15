package com.mitarifamitaxi.taximetrousuario.components.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import com.mitarifamitaxi.taximetrousuario.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDropDown(
    leadingIcon: ImageVector,
    label: String,
    options: List<String>,
    selectedOptionText: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
            .background(colorResource(id = R.color.white))
    ) {
        TextField(
            value = selectedOptionText,
            onValueChange = {},
            readOnly = true,
            label = {
                Text(
                    text = label,
                    fontFamily = MontserratFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = "Car icon",
                    tint = Color(0xFFFFB300)
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable, true)
                .fillMaxWidth(),
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
                errorPlaceholderColor = colorResource(id = R.color.red)
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = colorResource(id = R.color.white),
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = selectionOption,
                            fontFamily = MontserratFamily,
                            fontWeight = FontWeight.Companion.Medium,
                            fontSize = 14.sp,
                            color = colorResource(id = R.color.black),
                        )
                    },
                    onClick = {
                        onOptionSelected(selectionOption)
                        expanded = false
                        focusManager.clearFocus()
                    }
                )
            }
        }
    }
}
