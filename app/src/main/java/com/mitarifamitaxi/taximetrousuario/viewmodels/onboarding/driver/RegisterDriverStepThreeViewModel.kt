package com.mitarifamitaxi.taximetrousuario.viewmodels.onboarding.driver

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mitarifamitaxi.taximetrousuario.viewmodels.AppViewModel
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.mitarifamitaxi.taximetrousuario.R
import com.mitarifamitaxi.taximetrousuario.helpers.LocalUserManager
import com.mitarifamitaxi.taximetrousuario.models.DialogType
import com.mitarifamitaxi.taximetrousuario.models.LocalUser
import com.mitarifamitaxi.taximetrousuario.models.VehicleBrand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar

class RegisterDriverStepThreeViewModel(context: Context, private val appViewModel: AppViewModel) :
    ViewModel() {

    private val appContext = context.applicationContext
    private val vehicleBrandsObj = mutableStateOf(listOf<VehicleBrand>())

    val vehicleBrandNames: List<String>
        get() = vehicleBrandsObj.value.map { it.name ?: "" }.sorted()
    var selectedBrand by mutableStateOf<String?>(null)
        private set

    var vehicleModelsNames by mutableStateOf(listOf<String>())
    var selectedModel by mutableStateOf<String?>(null)
        private set

    var vehicleYears by mutableStateOf(listOf<String>())
    var selectedYear by mutableStateOf<String?>(null)
        private set

    var plate by mutableStateOf("")

    init {
        getVehicleBrands()
        loadListOfYears()
    }

    private fun getVehicleBrands() {
        viewModelScope.launch {
            try {

                appViewModel.isLoading = true

                val firestore = FirebaseFirestore.getInstance()
                val brandsQuerySnapshot = withContext(Dispatchers.IO) {
                    firestore.collection("vehicleBrands")
                        .get()
                        .await()
                }

                if (!brandsQuerySnapshot.isEmpty) {
                    try {
                        vehicleBrandsObj.value =
                            brandsQuerySnapshot.documents.mapNotNull { it.toObject(VehicleBrand::class.java) }
                        appViewModel.isLoading = false
                    } catch (e: Exception) {
                        Log.e(
                            "RegisterDriverStepThreeViewModel",
                            "Error converting brands: ${e.message}"
                        )
                        appViewModel.isLoading = false
                        appViewModel.showMessage(
                            type = DialogType.ERROR,
                            title = appContext.getString(R.string.something_went_wrong),
                            message = appContext.getString(R.string.general_error)
                        )

                    }
                } else {
                    appViewModel.isLoading = false
                    appViewModel.showMessage(
                        type = DialogType.ERROR,
                        title = appContext.getString(R.string.something_went_wrong),
                        message = appContext.getString(R.string.general_error)
                    )
                }
            } catch (e: Exception) {
                Log.e("RegisterDriverStepThreeViewModel", "Error fetching brands: ${e.message}")
                appViewModel.isLoading = false
                appViewModel.showMessage(
                    type = DialogType.ERROR,
                    title = appContext.getString(R.string.something_went_wrong),
                    message = appContext.getString(R.string.general_error)
                )
            }
        }

    }

    fun loadListOfYears() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        vehicleYears = (currentYear + 1 downTo (currentYear - 30)).map { it.toString() }
        selectedYear = null
    }

    fun onBrandSelected(brand: String) {
        selectedBrand = brand
        val selectedBrandObj = vehicleBrandsObj.value.find { it.name == brand }
        vehicleModelsNames = (selectedBrandObj?.models)?.sorted() ?: emptyList()
        selectedModel = null
    }

    fun onModelSelected(model: String) {
        selectedModel = model
    }

    fun onYearSelected(year: String) {
        selectedYear = year
    }

    fun onNext() {

        if (selectedBrand.isNullOrEmpty() ||
            selectedModel.isNullOrEmpty() ||
            selectedYear.isNullOrEmpty() ||
            plate.isEmpty()
        ) {
            appViewModel.showMessage(
                type = DialogType.ERROR,
                title = appContext.getString(R.string.attention),
                message = appContext.getString(R.string.all_fields_required)
            )
            return
        }

        updateUserData(
            brand = selectedBrand ?: "",
            model = selectedModel ?: "",
            year = selectedYear ?: "",
            plate = plate.uppercase()
        )
    }

    private fun updateUserData(
        brand: String,
        model: String,
        year: String,
        plate: String
    ) {

        appViewModel.isLoading = true

        val userData = LocalUserManager(appContext).getUserState()

        val userDataUpdated = userData?.copy(
            vehicleBrand = brand,
            vehicleModel = model,
            vehicleYear = year,
            vehiclePlate = plate,
        )

        userDataUpdated?.let {
            LocalUserManager(appContext).saveUserState(it)
            appViewModel.updateUserDataOnFirebase(it)
        }

    }

}

class RegisterDriverStepThreeViewModelFactory(
    private val context: Context,
    private val appViewModel: AppViewModel
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterDriverStepThreeViewModel::class.java)) {
            return RegisterDriverStepThreeViewModel(context, appViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}