package at.uastw.resourcesgamehelper

import android.content.Context
import android.content.SharedPreferences

class MaintenanceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mine_maintenance_history_v3", Context.MODE_PRIVATE)
    private val hqPrefs: SharedPreferences = context.getSharedPreferences("hq_data", Context.MODE_PRIVATE)

    private fun getMineKey(mine: Mine): String = "${mine.lat}_${mine.lon}"

    fun getHistory(mine: Mine): List<Long> {
        val key = getMineKey(mine)
        val historyString = prefs.getString(key, null) ?: return emptyList()
        return historyString.split(",")
            .mapNotNull { it.toLongOrNull() }
            .distinct()
            .sorted()
    }

    fun addMaintenance(mine: Mine, timestamp: Long) {
        if (timestamp <= 0) return
        val key = getMineKey(mine)
        val currentHistory = getHistory(mine).toMutableList()
        if (!currentHistory.contains(timestamp)) {
            currentHistory.add(timestamp)
            currentHistory.sort()
            saveHistory(key, currentHistory)
        }
    }

    fun removePoint(mine: Mine, timestamp: Long) {
        val key = getMineKey(mine)
        val currentHistory = getHistory(mine).toMutableList()
        if (currentHistory.remove(timestamp)) {
            saveHistory(key, currentHistory)
        }
    }

    fun clearHistory(mine: Mine) {
        prefs.edit().remove(getMineKey(mine)).commit()
    }

    private fun saveHistory(key: String, history: List<Long>) {
        prefs.edit().putString(key, history.joinToString(",")).commit()
    }

    fun syncWithApi(mine: Mine) {
        addMaintenance(mine, mine.lastmaintenance)
        val manualTimestamp = 1772133840L 
        if (mine.builddate < manualTimestamp) {
             addMaintenance(mine, manualTimestamp)
        }
    }

    // HQ Persistence
    fun saveBestHq(lat: Double, lon: Double, count: Int) {
        hqPrefs.edit()
            .putString("lat", lat.toString())
            .putString("lon", lon.toString())
            .putInt("count", count)
            .commit()
    }

    fun loadBestHq(): Pair<Pair<Double, Double>?, Int> {
        val latStr = hqPrefs.getString("lat", null)
        val lonStr = hqPrefs.getString("lon", null)
        val count = hqPrefs.getInt("count", 0)
        
        return if (latStr != null && lonStr != null) {
            Pair(Pair(latStr.toDouble(), lonStr.toDouble()), count)
        } else {
            Pair(null, 0)
        }
    }
}
