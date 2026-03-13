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
import retrofit2.converter.gson.GsonConverterFactory

class MineViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MineViewModel"
    private val maintenanceManager = MaintenanceManager(application)
    
    private val _mines = MutableStateFlow<List<Mine>>(emptyList())
    val mines: StateFlow<List<Mine>> = _mines

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _factoryProfits = MutableStateFlow<List<FactoryProfit>>(emptyList())
    val factoryProfits: StateFlow<List<FactoryProfit>> = _factoryProfits

    private val _bestHqLocation = MutableStateFlow<Pair<Pair<Double, Double>?, Int>>(maintenanceManager.loadBestHq())
    val bestHqLocation: StateFlow<Pair<Pair<Double, Double>?, Int>> = _bestHqLocation

    private val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(Config.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    fun fetchMines() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val dtos = apiService.getMines()
                _mines.value = dtos.map { dto ->
                    Mine(
                        mineID = dto.mineID,
                        lat = dto.lat,
                        lon = dto.lon,
                        HQboost = dto.hqBoost,
                        fullrate = dto.fullRate,
                        rawrate = dto.rawRate,
                        techfactor = dto.techFactor,
                        name = dto.name,
                        builddate = dto.buildDate,
                        lastmaintenance = dto.lastMaintenance,
                        condition = dto.condition,
                        resourceName = dto.resourceName,
                        resourceID = dto.resourceID,
                        lastenemyaction = dto.lastEnemyAction,
                        def1 = dto.def1,
                        def2 = dto.def2,
                        def3 = dto.def3,
                        attackpenalty = dto.attackPenalty,
                        attackcount = dto.attackCount,
                        attacklost = dto.attackLost,
                        quality = dto.quality,
                        qualityInclTU = dto.qualityInclTU
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching mines (JSON)", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchFactories() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val priceDtos = apiService.getItemPrices()
                val prices = priceDtos.associate { it.itemID to (if (it.marketPrice > 0) it.marketPrice else it.kiPrice) }

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
                    
                    FactoryProfit(
                        factoryName = data.factoryName,
                        outputItemName = data.itemName,
                        outputItemId = data.outputItemId,
                        profitPerHour = revenuePerHour - costPerHour,
                        revenuePerHour = revenuePerHour,
                        costPerHour = costPerHour
                    )
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
