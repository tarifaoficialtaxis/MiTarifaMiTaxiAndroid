package com.mitarifamitaxi.taximetrousuario

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.google.android.gms.ads.MobileAds
import com.mitarifamitaxi.taximetrousuario.helpers.adds.AppOpenAdManager

class MyApplication : Application() {

    internal lateinit var appOpenAdManager: AppOpenAdManager

    private var startedActivities = 0
    private var isChangingConfig = false

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) { }
        appOpenAdManager = AppOpenAdManager(this)

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                if (startedActivities == 0 && !isChangingConfig) {
                    appOpenAdManager.showAdIfAvailable(activity)
                }
                startedActivities++
            }

            override fun onActivityStopped(activity: Activity) {
                isChangingConfig = activity.isChangingConfigurations
                startedActivities--
            }

            override fun onActivityCreated(a: Activity, b: Bundle?) {}
            override fun onActivityResumed(a: Activity) {}
            override fun onActivityPaused(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
            override fun onActivityDestroyed(a: Activity) {}
        })
    }
}
