package com.mitarifamitaxi.taximetrousuario.viewmodels.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.models.Trip
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.CityRatesManager
import com.mitarifamitaxi.taximetrousuario.helpers.LocalUserManager
import com.mitarifamitaxi.taximetrousuario.models.LocalUser
import com.mitarifamitaxi.taximetrousuario.models.Rates
import com.mitarifamitaxi.taximetrousuario.states.HomeState
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class HomeViewModel(context: Context, private val appViewModel: AppViewModel) : ViewModel() {

    private val appContext = context.applicationContext

    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState

    init {
        getTripsByUserId()
        observeCurrentUser()
    }

    fun observeCurrentUser() {
        val userId = appViewModel.uiState.value.userData?.id
        if (userId.isNullOrEmpty()) {
            Log.e("HomeViewModel", "User ID es nulo o vacío al intentar observar usuario.")
            return
        }

        val db = FirebaseFirestore.getInstance()

        val userRef = db.collection("users")
            .document(userId)


        userRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("HomeViewModel", "Error escuchando usuario: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                try {
                    val updatedUser = snapshot.toObject(LocalUser::class.java)
                    updatedUser?.let { user ->
                        viewModelScope.launch(Dispatchers.Main) {
                            appViewModel.updateLocalUser(user)
                            LocalUserManager(appContext).saveUserState(user)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Error parseando User: ${e.message}")
                }
            } else {
                Log.w("HomeViewModel", "Snapshot de usuario vacío o no existe.")
            }
        }
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

    fun getCityRates(userCity: String, goNext: () -> Unit) {
        viewModelScope.launch {
            val firestore = FirebaseFirestore.getInstance()
            val ratesQuerySnapshot = withContext(Dispatchers.IO) {
                firestore.collection("dynamicRates").whereEqualTo("city", userCity).get()
                    .await()
            }

            if (!ratesQuerySnapshot.isEmpty) {
                val cityRatesDoc = ratesQuerySnapshot.documents[0]
                try {
                    val rates = cityRatesDoc.toObject(Rates::class.java) ?: Rates()
                    CityRatesManager(appContext).saveRatesState(rates)
                    goNext()

                } catch (e: Exception) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                    appViewModel.showMessage(
                        type = DialogType.ERROR,
                        title = appContext.getString(R.string.something_went_wrong),
                        message = appContext.getString(R.string.general_error)
                    )
                }
            } else {
                FirebaseCrashlytics.getInstance()
                    .recordException(Exception("TaximeterViewModel ratesQuerySnapshot empty for city: $userCity"))
                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.we_sorry),
                    message = appContext.getString(R.string.feature_not_available_on_city)
                )
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