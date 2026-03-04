package at.uastw.resourcesgamehelper

import android.content.Context
import android.content.SharedPreferences

class MaintenanceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("maintenance_history", Context.MODE_PRIVATE)

    fun getHistory(mineID: Int): List<Long> {
        val historyString = prefs.getString(mineID.toString(), null) ?: return emptyList()
        return historyString.split(",").mapNotNull { it.toLongOrNull() }.sorted()
    }

    fun addMaintenance(mineID: Int, timestamp: Long) {
        val currentHistory = getHistory(mineID).toMutableList()
        if (!currentHistory.contains(timestamp)) {
            currentHistory.add(timestamp)
            currentHistory.sort()
            prefs.edit().putString(mineID.toString(), currentHistory.joinToString(",")).apply()
        }
    }

    /**
     * Ensures that the maintenance from API and the manual one are recorded.
     */
    fun syncWithApi(mine: Mine) {
        addMaintenance(mine.mineID, mine.lastmaintenance)
        
        // Add the specific requested maintenance: 26.2.2026 20:24
        // 26.2.2026 20:24:00 is approx 1772133840 seconds
        val manualTimestamp = 1772133840L 
        if (mine.builddate < manualTimestamp && manualTimestamp <= System.currentTimeMillis() / 1000) {
             // Only add if it makes sense for this mine (built before this date)
             // For simplicity, we'll just add it as requested for testing
             addMaintenance(mine.mineID, manualTimestamp)
        }
    }
}
