package com.mitarifamitaxi.taximetrousuario.components.adds

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@Composable
fun BottomBannerAd(
    adId: String
) {

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
        LocalContext.current,
        screenWidthDp
    )

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        factory = { context ->
            AdView(context).apply {
                setAdSize(adSize)
                adUnitId = adId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}