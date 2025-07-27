package com.mitarifamitaxi.taximetrousuario.components.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.MontserratFamily


@Composable
fun CustomCheckBox(
    text: String? = null,
    checked: Boolean,
    isEnabled: Boolean = true,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier? = Modifier
) {

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier ?: Modifier
    ) {

        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            Checkbox(
                checked = checked,
                enabled = isEnabled,
                onCheckedChange = onValueChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = colorResource(id = R.color.main),
                    uncheckedColor = colorResource(id = R.color.gray6),

                    disabledCheckedColor = colorResource(id = R.color.yellow1),
                    disabledUncheckedColor = colorResource(id = R.color.gray2)
                )
            )
        }

        if (text != null) {
            Text(
                text = text,
                fontFamily = MontserratFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = colorResource(id = R.color.gray1),
            )
        }
    }
}