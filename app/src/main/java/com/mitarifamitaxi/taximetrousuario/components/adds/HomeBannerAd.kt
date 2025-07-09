package com.mitarifamitaxi.taximetrousuario.components.adds

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.mitarifamitaxi.taximetrousuario.BuildConfig
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun HomeBannerAdd(modifier: Modifier = Modifier) {

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(LocalContext.current, screenWidthDp)

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(adSize)
                adUnitId = BuildConfig.HOME_AD_UNIT_ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}