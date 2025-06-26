package com.mitarifamitaxi.taximetrousuario.viewmodels.trips

import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import com.mitarifamitaxi.taximetrousuario.R
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.models.Trip
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel


class MyTripsViewModel(context: Context, private val appViewModel: AppViewModel) : ViewModel() {
    private val appContext = context.applicationContext

    private val _trips = mutableStateOf<List<Trip>>(emptyList())
    val trips: State<List<Trip>> = _trips

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
                    _trips.value = trips
                } else {
                    _trips.value = emptyList()
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
) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MyTripsViewModel::class.java)) {
            return MyTripsViewModel(context, appViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}