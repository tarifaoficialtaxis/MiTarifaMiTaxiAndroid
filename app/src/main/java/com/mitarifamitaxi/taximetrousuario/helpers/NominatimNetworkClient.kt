package com.mitarifamitaxi.taximetrousuario.helpers

import android.annotation.SuppressLint
import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request

object NominatimNetworkClient {

    @SuppressLint("StaticFieldLeak")
    private lateinit var userManager: LocalUserManager

    fun init(appContext: Context) {
        userManager = LocalUserManager(appContext)
    }

    val nominatimClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->

                val user = userManager.getUserState()
                val userAgent = user?.let {
                    "MiTarifaMiTaxi/1.0.0 (id=${it.id}; usr=${it.email})"
                } ?: "MiTarifaMiTaxi/1.0.0 (anon)"

                val newReq: Request = chain.request().newBuilder()
                    .header("User-Agent", userAgent)
                    .header("Accept-Language", "es")
                    .build()
                chain.proceed(newReq)
            })
            .build()
    }
}