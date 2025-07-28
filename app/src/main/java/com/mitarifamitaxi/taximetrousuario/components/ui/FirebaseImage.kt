package com.mitarifamitaxi.taximetrousuario.components.ui

import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage

@Composable
fun FirebaseImage(
    storagePath: String,
    modifier: Modifier = Modifier
) {
    val storageRef = FirebaseStorage.getInstance()
        .reference
        .child(storagePath)

    AndroidView(
        factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                Glide.with(ctx)
                    .load(storageRef)
                    .into(this)
            }
        },
        modifier = modifier
    )
}
