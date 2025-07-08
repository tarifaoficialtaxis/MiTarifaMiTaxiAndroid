package com.mitarifamitaxi.taximetrousuario.activities

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.activities.home.HomeActivity
import com.mitarifamitaxi.taximetrousuario.activities.onboarding.LoginActivity
import com.mitarifamitaxi.taximetrousuario.activities.pqrs.PqrsActivity
import com.mitarifamitaxi.taximetrousuario.activities.profile.ProfileActivity
import com.mitarifamitaxi.taximetrousuario.activities.routeplanner.RoutePlannerActivity
import com.mitarifamitaxi.taximetrousuario.activities.sos.SosActivity
import com.mitarifamitaxi.taximetrousuario.activities.taximeter.TaximeterActivity
import com.mitarifamitaxi.taximetrousuario.activities.trips.MyTripsActivity
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomPopupDialog
import com.mitarifamitaxi.taximetrousuario.components.ui.SideMenu
import com.mitarifamitaxi.taximetrousuario.helpers.ContactsCatalogManager
import com.mitarifamitaxi.taximetrousuario.helpers.LocalUserManager
import com.mitarifamitaxi.taximetrousuario.helpers.NominatimNetworkClient
import com.mitarifamitaxi.taximetrousuario.helpers.UserLocationManager
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.states.AppState
import com.mitarifamitaxi.taximetrousuario.states.DialogState
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModelFactory
import kotlinx.coroutines.launch

val LocalOpenDrawer = compositionLocalOf<() -> Unit> { {} }

open class BaseActivity : ComponentActivity() {

    val appViewModel: AppViewModel by viewModels {
        AppViewModelFactory(this)
    }

    val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (fineLocationGranted || coarseLocationGranted) {
                appViewModel.getCurrentLocation()
            } else {
                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    getString(R.string.permission_required),
                    getString(R.string.location_permission_required)
                )
            }
        }

    open fun isDrawerEnabled(): Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NominatimNetworkClient.init(this)
        setContent {
            val appState by appViewModel.uiState.collectAsState()

            MyTheme {
                BaseScreen(
                    appState = appState,
                    onMenuSectionClicked = { sectionId ->
                        when (sectionId) {
                            "PROFILE" -> {

                                if (this !is ProfileActivity) {
                                    startActivity(Intent(this, ProfileActivity::class.java))
                                }


                            }

                            "HOME" -> {
                                if (this !is HomeActivity) {
                                    startActivity(Intent(this, HomeActivity::class.java))
                                }
                            }

                            "TAXIMETER" -> {
                                if (this !is TaximeterActivity) {
                                    startActivity(
                                        Intent(
                                            this,
                                            RoutePlannerActivity::class.java
                                        )
                                    )
                                }
                            }

                            "SOS" -> {
                                if (this !is SosActivity) {
                                    startActivity(Intent(this, SosActivity::class.java))
                                }
                            }

                            "PQRS" -> {
                                if (this !is PqrsActivity) {
                                    startActivity(Intent(this, PqrsActivity::class.java))
                                }
                            }

                            "MY_TRIPS" -> {
                                if (this !is MyTripsActivity) {
                                    startActivity(Intent(this, MyTripsActivity::class.java))
                                }
                            }

                            "LOGOUT" -> {
                                LocalUserManager(this).deleteUserState()
                                UserLocationManager(this).deleteUserLocationState()
                                ContactsCatalogManager(this).deleteContactsState()
                                val intent = Intent(this, LoginActivity::class.java).apply {
                                    flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                startActivity(intent)
                                finish()
                            }

                        }
                    }
                )
            }
        }
    }

    @Composable
    fun BaseScreen(
        appState: AppState,
        onMenuSectionClicked: (String) -> Unit
    ) {
        if (isDrawerEnabled()) {
            // Remember the drawer state and provide a lambda to open the drawer.
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            val openDrawer: () -> Unit = {
                scope.launch { drawerState.open() }
            }

            // Lambda that closes the drawer before handling the menu click.
            val handleMenuClick: (String) -> Unit = { sectionId ->
                scope.launch {
                    drawerState.close() // Close the drawer first.
                    onMenuSectionClicked(sectionId) // Then handle the menu click.
                }
            }

            CompositionLocalProvider(LocalOpenDrawer provides openDrawer) {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        appState.userData?.let { userData ->
                            SideMenu(
                                userData = userData,
                                onProfileClicked = { handleMenuClick("PROFILE") },
                                onSectionClicked = { handleMenuClick(it.id) }
                            )
                        }
                    }
                ) {
                    BaseMainBox(appState = appState)
                }
            }
        } else {
            // When the drawer is disabled, provide a no-op openDrawer lambda.
            CompositionLocalProvider(LocalOpenDrawer provides {}) {
                BaseMainBox(appState = appState)
            }
        }
    }

    @Composable
    fun BaseMainBox(appState: AppState) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            Content()
            if (appState.isLoading) {
                LoadingOverlayCompose()
            }
            if (appState.dialogState.show) {
                CustomPopUpDialogCompose(
                    dialogState = appState.dialogState
                )
            }
        }
    }


    @Composable
    fun LoadingOverlayCompose() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = colorResource(id = R.color.main)
            )
        }
    }

    @Composable
    fun CustomPopUpDialogCompose(dialogState: DialogState) {
        CustomPopupDialog(
            dialogType = dialogState.type,
            title = dialogState.title,
            message = dialogState.message,
            primaryActionButton = dialogState.buttonText,
            showCloseButton = dialogState.showCloseButton,
            onDismiss = {
                appViewModel.hideMessage()
                dialogState.onDismiss?.invoke()
            },
            onPrimaryActionClicked = {
                appViewModel.hideMessage()
                dialogState.onPrimaryActionClicked?.invoke()
            }
        )

    }

    /**
     * MyTheme sets up a custom MaterialTheme.
     */
    @Composable
    fun MyTheme(content: @Composable () -> Unit) {
        val customColorScheme = lightColorScheme(
            primary = colorResource(id = R.color.main),
            onPrimary = Color.White,
            secondary = colorResource(id = R.color.yellow1)
        )

        MaterialTheme(
            colorScheme = customColorScheme,
            content = content
        )
    }

    /**
     * Content is the main screen content that child activities override.
     */
    @Composable
    open fun Content() {
        // Default empty content; override in child activities.
    }
}
