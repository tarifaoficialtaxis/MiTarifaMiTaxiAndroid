package com.mitarifamitaxi.taximetrousuario.components.ui

import android.widget.ImageView
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CustomAsyncImageButton(
    image: String,
    onClick: () -> Unit,
) {

    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(),
        border = null,
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(20.dp)
    ) {

        FirebaseImage(
            storagePath = image,
            scaleTypeProp = ImageView.ScaleType.FIT_XY,
            modifier = Modifier
                .fillMaxSize(),
        )
    }

}