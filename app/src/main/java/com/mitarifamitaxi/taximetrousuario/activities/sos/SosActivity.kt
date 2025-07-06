package com.mitarifamitaxi.taximetrousuario.activities.sos

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.activities.BaseActivity
import com.mitarifamitaxi.taximetrousuario.activities.profile.ProfileActivity
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomAsyncImageButton
import com.mitarifamitaxi.taximetrousuario.components.ui.CustomContactActionDialog
import com.mitarifamitaxi.taximetrousuario.components.ui.TopHeaderView
import com.mitarifamitaxi.taximetrousuario.models.ContactCatalog
import com.mitarifamitaxi.taximetrousuario.states.SosState
import com.mitarifamitaxi.taximetrousuario.viewmodels.sos.SosViewModel
import com.mitarifamitaxi.taximetrousuario.viewmodels.sos.SosViewModelFactory
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.grid.items
import com.mitarifamitaxi.taximetrousuario.helpers.K

class SosActivity : BaseActivity() {

    private val viewModel: SosViewModel by viewModels {
        SosViewModelFactory(this, appViewModel)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appViewModel.requestLocationPermission(this)
        observeViewModelEvents()
    }

    override fun onDestroy() {
        super.onDestroy()
        appViewModel.stopLocationUpdates()
    }

    private fun observeViewModelEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationEvents.collect { event ->
                    when (event) {
                        is SosViewModel.NavigationEvent.GoToProfile -> {
                            startActivity(
                                Intent(this@SosActivity, ProfileActivity::class.java)
                            )
                        }

                        is SosViewModel.NavigationEvent.GoBack -> {
                            finish()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appViewModel.reloadUserData()
    }

    @Composable
    override fun Content() {
        val uiState by viewModel.uiState.collectAsState()
        SosScreen(
            uiState = uiState
        )
    }

    @Composable
    private fun SosScreen(
        uiState: SosState
    ) {

        Column(
            modifier = Modifier.Companion
                .fillMaxSize()
                .background(colorResource(id = R.color.gray4)),
        ) {
            TopHeaderView(
                title = stringResource(id = R.string.sos),
                leadingIcon = Icons.Filled.ChevronLeft,
                onClickLeading = {
                    finish()
                }
            )

            Column(
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .padding(K.GENERAL_PADDING)
                    .verticalScroll(rememberScrollState())
            ) {

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 40.dp)
                ) {
                    items(uiState.contact.lines) { item ->
                        CustomAsyncImageButton(
                            image = item.image ?: "",
                            onClick = {
                                viewModel.showContactDialog(item)
                            }
                        )
                    }
                }


            }

        }

        if (uiState.showContactDialog) {
            CustomContactActionDialog(
                title = stringResource(id = R.string.select_one_action),
                contactCatalog = uiState.contactCatalogSelected ?: ContactCatalog(),
                onDismiss = { viewModel.hideContactDialog() },
                onCallAction = { number ->
                    viewModel.hideContactDialog()
                    viewModel.validateSosAction(
                        isCall = true,
                        contactNumber = number,
                        onIntentReady = {
                            startActivity(it)
                        })
                },
                onMessageAction = { number ->
                    viewModel.hideContactDialog()
                    viewModel.validateSosAction(
                        isCall = false,
                        contactNumber = number,
                        onIntentReady = {
                            startActivity(it)
                        })
                },
            )
        }
    }

    @Preview
    @Composable
    fun ScreenPreview() {
        SosScreen(
            uiState = SosState()
        )
    }
}