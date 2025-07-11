package com.mitarifamitaxi.taximetrousuario.states

import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.models.LocalUser
import com.mitarifamitaxi.taximetrousuario.models.UserLocation

data class AppState(
    val isLoading: Boolean = false,
    val isGettingLocation: Boolean = false,
    val userData: LocalUser? = null,
    val userLocation: UserLocation? = null,
    val dialogState: DialogState = DialogState()
)

data class DialogState(
    val show: Boolean = false,
    val type: DialogType = DialogType.SUCCESS,
    val title: String = "",
    val message: String = "",
    val buttonText: String? = null,
    val secondaryButtonText: String? = null,
    val showCloseButton: Boolean = true,
    val onDismiss: (() -> Unit)? = null,
    val onPrimaryActionClicked: (() -> Unit)? = null,
    val onSecondaryActionClicked: (() -> Unit)? = null
)