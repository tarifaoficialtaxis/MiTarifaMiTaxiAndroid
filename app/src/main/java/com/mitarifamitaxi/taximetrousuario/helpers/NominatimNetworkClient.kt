
package com.mitarifamitaxi.taximetrousuario.helpers

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request

object NominatimNetworkClient {

    val nominatimClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val newReq: Request = chain.request().newBuilder()
                    .header("User-Agent", "MiTarifaMiTaxi/1.0.0 (tarifaoficialtaxis@gmail.com)")
                    .header("Accept-Language", "es")
                    .build()
                chain.proceed(newReq)
            })
            .build()
    }
}