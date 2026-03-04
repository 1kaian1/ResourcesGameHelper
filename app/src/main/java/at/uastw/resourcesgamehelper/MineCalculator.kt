package at.uastw.resourcesgamehelper

object MineCalculator {
    // Precise decay rate provided by user: 0.000000461677333333 per second
    const val DECAY_RATE_PER_SECOND = 0.000000461677333333

    fun calculateCurrentCondition(lastMaintenanceSeconds: Long): Double {
        val nowSeconds = System.currentTimeMillis() / 1000
        val elapsedSeconds = (nowSeconds - lastMaintenanceSeconds).coerceAtLeast(0)
        val current = 1.0 - (elapsedSeconds * DECAY_RATE_PER_SECOND)
        return current.coerceAtLeast(0.0)
    }

    fun calculateMaxHourlyEarnings(mine: Mine): Double {
        val baseValue = GameData.getBaseEarningsPerHour(mine.resourceID)
        return baseValue * mine.quality * mine.HQboost
    }

    fun calculateCurrentHourlyEarnings(mine: Mine): Double {
        val maxEarnings = calculateMaxHourlyEarnings(mine)
        val currentCondition = calculateCurrentCondition(mine.lastmaintenance)
        return maxEarnings * currentCondition
    }

    fun calculateCurrentHourlyLoss(mine: Mine): Double {
        val maxEarnings = calculateMaxHourlyEarnings(mine)
        return maxEarnings * (1.0 - calculateCurrentCondition(mine.lastmaintenance))
    }

    fun calculateCurrentHourlyOutput(mine: Mine): Double {
        val baseOutput = GameData.getBaseOutputPerHour(mine.resourceID)
        val currentCondition = calculateCurrentCondition(mine.lastmaintenance)
        return baseOutput * mine.quality * currentCondition * mine.HQboost
    }

    private fun calculateEarningsBetween(baseValue: Double, quality: Double, hqBoost: Double, start: Long, end: Long): Double {
        val durationSeconds = (end - start).coerceAtLeast(0)
        if (durationSeconds <= 0) return 0.0
        val durationHours = durationSeconds / 3600.0
        
        // Integration of (1 - kt) from 0 to T: T - (k * T^2)/2
        // Then divide by T to get average condition: 1 - (k * T)/2
        val avgCondition = (1.0 + (1.0 - durationSeconds * DECAY_RATE_PER_SECOND).coerceAtLeast(0.0)) / 2.0
        return baseValue * quality * hqBoost * avgCondition * durationHours
    }

    fun calculateTotalEarningsWithHistory(mine: Mine, history: List<Long>): Double {
        val baseValue = GameData.getBaseEarningsPerHour(mine.resourceID)
        val nowSeconds = System.currentTimeMillis() / 1000
        
        // Sort and filter history to include build date as the very first point
        val points = (history + mine.builddate).filter { it <= nowSeconds }.distinct().sorted()
        
        var total = 0.0
        for (i in 0 until points.size - 1) {
            val start = points[i]
            val end = points[i+1]
            total += calculateEarningsBetween(baseValue, mine.quality, mine.HQboost, start, end)
        }
        
        // From the last history point to now
        val lastPoint = points.lastOrNull() ?: mine.builddate
        total += calculateEarningsBetween(baseValue, mine.quality, mine.HQboost, lastPoint, nowSeconds)
        
        return total
    }
}
