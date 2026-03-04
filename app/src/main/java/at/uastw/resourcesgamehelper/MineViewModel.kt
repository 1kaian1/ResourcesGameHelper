package at.uastw.resourcesgamehelper

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

class MineViewModel : ViewModel() {
    private val TAG = "MineViewModel"
    private val _mines = MutableStateFlow<List<Mine>>(emptyList())
    val mines: StateFlow<List<Mine>> = _mines

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

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
                Log.d(TAG, "Fetching mines from API...")
                val response = apiService.getMines()
                Log.d(TAG, "API Response received, length: ${response.length}")
                // Log first 100 characters of response to see format
                Log.d(TAG, "Response preview: ${response.take(200)}")
                
                _mines.value = CsvParser.parseMines(response)
                Log.d(TAG, "Mines updated in state: ${_mines.value.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching mines", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
