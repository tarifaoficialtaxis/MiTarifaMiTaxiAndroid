package com.mitarifamitaxi.taximetrousuario.viewmodels.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.models.Trip
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.states.HomeState
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class HomeViewModel(context: Context, private val appViewModel: AppViewModel) : ViewModel() {

    private val appContext = context.applicationContext

    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState

    init {
        getTripsByUserId()
    }

    private fun getTripsByUserId() {
        val db = FirebaseFirestore.getInstance()
        val tripsRef = db.collection("trips")
            .whereEqualTo("userId", appViewModel.uiState.value.userData?.id)
            .orderBy("endHour", Query.Direction.DESCENDING)
            .limit(3)

        tripsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = error.message ?: appContext.getString(R.string.error_fetching_trips)
                )
                return@addSnapshotListener
            }

            try {
                if (snapshot != null && !snapshot.isEmpty) {
                    val trips = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Trip::class.java)?.copy(id = doc.id)
                    }
                    _uiState.update { it.copy(trips = trips) }
                } else {
                    _uiState.update { it.copy(trips = emptyList()) }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Unexpected error: ${e.message}")
            }

        }
    }
}

class HomeViewModelFactory(private val context: Context, private val appViewModel: AppViewModel) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(context, appViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}