package com.mitarifamitaxi.taximetrousuario.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomPopupDialog
import com.mitarifamitaxi.taximetrousuario.components.ui.DrawerContent
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModelFactory
import kotlinx.coroutines.launch

// Provide a CompositionLocal for opening the drawer
val LocalOpenDrawer = compositionLocalOf<() -> Unit> {
    error("LocalOpenDrawer not provided")
}

open class BaseActivity : ComponentActivity() {

    val appViewModel: AppViewModel by viewModels {
        AppViewModelFactory(this)
    }

    open fun isDrawerEnabled(): Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyTheme {
                BaseScreen(
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
                                    startActivity(Intent(this, RoutePlannerActivity::class.java))
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
                        }
                    }
                )
            }
        }
    }

    @Composable
    fun BaseScreen(onMenuSectionClicked: (String) -> Unit) {
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
                        appViewModel.userData?.let { userData ->
                            DrawerContent(
                                userData = userData,
                                onProfileClicked = { handleMenuClick("PROFILE") },
                                onSectionClicked = { handleMenuClick(it.id) }
                            )
                        }
                    }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Content()
                        LoadingOverlayCompose()
                        CustomPopUpDialogCompose()
                    }
                }
            }
        } else {
            // When the drawer is disabled, provide a no-op openDrawer lambda.
            CompositionLocalProvider(LocalOpenDrawer provides {}) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Content()
                    LoadingOverlayCompose()
                    CustomPopUpDialogCompose()
                }
            }
        }
    }

    @Composable
    fun LoadingOverlayCompose() {
        if (appViewModel.isLoading) {
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
    }

    @Composable
    fun CustomPopUpDialogCompose() {
        if (appViewModel.showDialog) {
            CustomPopupDialog(
                dialogType = appViewModel.dialogType,
                title = appViewModel.dialogTitle,
                message = appViewModel.dialogMessage,
                primaryActionButton = appViewModel.dialogButtonText,
                showCloseButton = appViewModel.dialogShowCloseButton,
                onDismiss = {
                    appViewModel.showDialog = false
                    appViewModel.dialogOnDismiss?.invoke()
                },
                onPrimaryActionClicked = {
                    appViewModel.showDialog = false
                    appViewModel.dialogOnPrimaryActionClicked?.invoke()
                }
            )
        }
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
