package com.mitarifamitaxi.taximetrousuario

import android.app.Application

import com.bumptech.glide.Glide
import com.firebase.ui.storage.images.FirebaseImageLoader
import com.google.firebase.storage.StorageReference
import java.io.InputStream

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Glide.get(this).registry.append(
            StorageReference::class.java,
            InputStream::class.java,
            FirebaseImageLoader.Factory()
        )
    }

}
