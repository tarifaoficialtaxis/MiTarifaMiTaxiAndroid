package com.mitarifamitaxi.taximetrousuario

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.mitarifamitaxi.taximetrousuario.helpers.adds.AppOpenAdManager

class MyApplication : Application() {
    internal lateinit var appOpenAdManager: AppOpenAdManager

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {}
        appOpenAdManager = AppOpenAdManager(this)
    }
}