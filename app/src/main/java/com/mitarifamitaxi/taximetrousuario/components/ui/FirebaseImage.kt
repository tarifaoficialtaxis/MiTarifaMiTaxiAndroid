package com.mitarifamitaxi.taximetrousuario.components.ui

import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.storage.FirebaseStorage
import com.bumptech.glide.signature.ObjectKey

@Composable
fun FirebaseImage(
    storagePath: String,
    scaleTypeProp: ImageView.ScaleType? = ImageView.ScaleType.CENTER_CROP,
    modifier: Modifier = Modifier
) {
    val storageRef = FirebaseStorage.getInstance()
        .reference
        .child(storagePath)

    AndroidView(
        factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = scaleTypeProp
                Glide.with(ctx)
                    .load(storageRef)
                    //.diskCacheStrategy(DiskCacheStrategy.NONE)
                    //.skipMemoryCache(true)
                    .signature(ObjectKey(System.currentTimeMillis().toString()))
                    .into(this)
            }
        },
        modifier = modifier
    )
}
