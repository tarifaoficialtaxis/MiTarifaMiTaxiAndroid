package com.mitarifamitaxi.taximetrousuario.viewmodels.trips

import android.content.Context
import android.util.Log
import com.mitarifamitaxi.taximetrousuario.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.models.Trip
import com.mitarifamitaxi.taximetrousuario.states.MyTripsState
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update


class MyTripsViewModel(context: Context, private val appViewModel: AppViewModel) : ViewModel() {

    private val appContext = context.applicationContext

    private val _uiState = MutableStateFlow(MyTripsState())
    val uiState: StateFlow<MyTripsState> = _uiState


    init {
        getTripsByUserId()
    }

    private fun getTripsByUserId() {
        appViewModel.setLoading(true)
        val db = FirebaseFirestore.getInstance()
        val tripsRef = db.collection("trips")
            .whereEqualTo("userId", appViewModel.uiState.value.userData?.id)
            .orderBy("endHour", Query.Direction.DESCENDING)

        tripsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                appViewModel.setLoading(false)
                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = appContext.getString(R.string.error_fetching_trips),
                )

                return@addSnapshotListener
            }
            try {
                appViewModel.setLoading(false)
                if (snapshot != null && !snapshot.isEmpty) {
                    val trips = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Trip::class.java)?.copy(id = doc.id)
                    }
                    _uiState.update { it.copy(trips = trips) }
                } else {
                    _uiState.update { it.copy(trips = emptyList()) }
                }
            } catch (e: Exception) {
                appViewModel.setLoading(false)
                Log.e("MyTripsViewModel", "Unexpected error: ${e.message}")
            }

        }
    }

}

class MyTripsViewModelFactory(
    private val context: Context,
    private val appViewModel: AppViewModel
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(cls: Class<T>): T {
        return MyTripsViewModel(context, appViewModel) as T
    }
}