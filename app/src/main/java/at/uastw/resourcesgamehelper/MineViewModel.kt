package at.uastw.resourcesgamehelper

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

class MineViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MineViewModel"
    private val maintenanceManager = MaintenanceManager(application)
    
    private val _mines = MutableStateFlow<List<Mine>>(emptyList())
    val mines: StateFlow<List<Mine>> = _mines

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _itemPrices = MutableStateFlow<Map<Int, Double>>(emptyMap())
    val itemPrices: StateFlow<Map<Int, Double>> = _itemPrices

    private val _factoryProfits = MutableStateFlow<List<FactoryProfit>>(emptyList())
    val factoryProfits: StateFlow<List<FactoryProfit>> = _factoryProfits

    // HQ State - Initialized with loaded data
    private val _bestHqLocation = MutableStateFlow<Pair<Pair<Double, Double>?, Int>>(maintenanceManager.loadBestHq())
    val bestHqLocation: StateFlow<Pair<Pair<Double, Double>?, Int>> = _bestHqLocation

    private val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(Config.BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    fun fetchMines() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = apiService.getMines()
                _mines.value = CsvParser.parseMines(response)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching mines", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchFactories() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val pricesResponse = apiService.getItemPrices()
                val prices = CsvParser.parseItemPrices(pricesResponse)
                _itemPrices.value = prices

                val prodData = FactoryData.productionList

                val profits = prodData.map { data ->
                    val unitPrice = prices[data.outputItemId] ?: 0.0
                    val revenuePerHour = unitPrice * data.baseOutputPerHour
                    val inputCostPerCycle = ((prices[data.input1Id] ?: 0.0) * data.input1Qty) +
                                            ((prices[data.input2Id] ?: 0.0) * data.input2Qty) +
                                            ((prices[data.input3Id] ?: 0.0) * data.input3Qty)
                    val totalCostPerCycle = inputCostPerCycle + data.creditsPerCycle
                    val cyclesPerHour = if (data.outputPerCycle > 0) data.baseOutputPerHour / data.outputPerCycle else 0.0
                    val costPerHour = totalCostPerCycle * cyclesPerHour
                    FactoryProfit(data.factoryName, data.itemName, data.outputItemId, revenuePerHour - costPerHour, revenuePerHour, costPerHour)
                }.sortedByDescending { it.profitPerHour }
                _factoryProfits.value = profits
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating factory profits", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun calculateBestHq() {
        val currentMines = _mines.value
        if (currentMines.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = withContext(Dispatchers.Default) {
                    HqCalculator.findBestHqLocation(currentMines)
                }
                _bestHqLocation.value = result
                // Save the result for persistence
                result.first?.let { pos ->
                    maintenanceManager.saveBestHq(pos.first, pos.second, result.second)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating HQ", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
