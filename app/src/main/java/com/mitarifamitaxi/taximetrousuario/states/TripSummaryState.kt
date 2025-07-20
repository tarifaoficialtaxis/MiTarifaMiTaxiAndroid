package com.mitarifamitaxi.taximetrousuario.states

import com.mitarifamitaxi.taximetrousuario.models.Trip

import android.graphics.Bitmap

data class TripSummaryState(
    val isDetails: Boolean = false,
    val tripData: Trip = Trip(),
    val routeImageLocal: Bitmap? = null,
    val showShareDialog: Boolean = false,
    val shareNumber: String = "",
    val isShareNumberError: Boolean = false,
    val isDetailsOpen: Boolean = false
)