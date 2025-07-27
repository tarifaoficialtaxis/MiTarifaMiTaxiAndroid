package com.mitarifamitaxi.taximetrousuario.viewmodels.trips

import android.content.Context
import android.util.Log
import com.mitarifamitaxi.taximetrousuario.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mitarifamitaxi.taximetrousuario.helpers.FirebaseStorageUtils
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.models.Trip
import com.mitarifamitaxi.taximetrousuario.states.MyTripsState
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


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

    fun toggleTripSelection(trip: Trip) {
        _uiState.update { currentState ->
            val isAlreadySelected = currentState.tripsSelected.any { it.id == trip.id }

            val updatedSelection = if (isAlreadySelected) {
                currentState.tripsSelected.filterNot { it.id == trip.id }
            } else {
                currentState.tripsSelected + trip
            }

            currentState.copy(tripsSelected = updatedSelection)
        }
    }


    fun onDeleteAction() {
        appViewModel.showMessage(
            type = DialogType.WARNING,
            title = appContext.getString(R.string.delete_trips_question),
            message = appContext.getString(R.string.wont_be_able_to_recover_trips),
            buttonText = appContext.getString(R.string.delete),
            onButtonClicked = {
                deleteTripsSelected()
            }
        )
    }

    fun deleteTripsSelected() {
        viewModelScope.launch {
            appViewModel.setLoading(true)

            val tripsToDelete = _uiState.value.tripsSelected
            val db = FirebaseFirestore.getInstance()
            val batch = db.batch()

            // Primero eliminamos las imágenes en paralelo
            tripsToDelete.forEach { trip ->
                trip.routeImage?.let { imageUrl ->
                    FirebaseStorageUtils.deleteImage(imageUrl)
                }
            }

            // Luego eliminamos los documentos en Firestore
            tripsToDelete.forEach { trip ->
                trip.id?.let { id ->
                    val docRef = db.collection("trips").document(id)
                    batch.delete(docRef)
                }
            }

            batch.commit()
                .addOnSuccessListener {
                    appViewModel.setLoading(false)
                    _uiState.update { it.copy(tripsSelected = emptyList()) }
                    appViewModel.showMessage(
                        type = DialogType.SUCCESS,
                        title = appContext.getString(R.string.success),
                        message = appContext.getString(R.string.trips_deleted_successfully)
                    )
                }
                .addOnFailureListener { e ->
                    appViewModel.setLoading(false)
                    _uiState.update { it.copy(tripsSelected = emptyList()) }
                    appViewModel.showMessage(
                        type = DialogType.ERROR,
                        title = appContext.getString(R.string.error),
                        message = appContext.getString(R.string.trips_deleted_error)
                    )
                    Log.e("MyTripsViewModel", "Error al eliminar viajes: ${e.message}")
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