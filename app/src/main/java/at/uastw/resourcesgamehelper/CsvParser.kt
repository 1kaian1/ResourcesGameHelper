package at.uastw.resourcesgamehelper

import android.util.Log

object CsvParser {
    private const val TAG = "CsvParser"

    fun parseMines(csvData: String): List<Mine> {
        if (csvData.isBlank()) return emptyList()
        val lines = csvData.trim().split("\n")
        if (lines.size <= 1) return emptyList()
        
        val delimiter = if (lines[0].contains(";")) ";" else if (lines[0].contains("\t")) "\t" else ","

        return lines.drop(1).mapNotNull { line ->
            val parts = line.split(delimiter)
            if (parts.size >= 22) {
                try {
                    Mine(
                        mineID = parts[0].trim().toDouble().toInt(),
                        lat = parts[1].trim().toDouble(),
                        lon = parts[2].trim().toDouble(),
                        HQboost = parts[3].trim().toDouble(),
                        fullrate = parts[4].trim().toDouble(),
                        rawrate = parts[5].trim().toDouble(),
                        techfactor = parts[6].trim().toDouble(),
                        name = parts[7].trim().removeSurrounding("\""),
                        builddate = parts[8].trim().toDouble().toLong(),
                        lastmaintenance = parts[9].trim().toDouble().toLong(),
                        condition = parts[10].trim().toDouble(),
                        resourceName = parts[11].trim().removeSurrounding("\""), // Clean quotes here
                        resourceID = parts[12].trim().toDouble().toInt(),
                        lastenemyaction = parts[13].trim().toDouble().toLong(),
                        def1 = parts[14].trim().toDouble().toInt(),
                        def2 = parts[15].trim().toDouble().toInt(),
                        def3 = parts[16].trim().toDouble().toInt(),
                        attackpenalty = parts[17].trim().toDouble(),
                        attackcount = parts[18].trim().toDouble().toInt(),
                        attacklost = parts[19].trim().toDouble().toInt(),
                        quality = parts[20].trim().toDouble(),
                        qualityInclTU = parts[21].trim().toDouble()
                    )
                } catch (e: Exception) { null }
            } else null
        }
    }

    fun parseItemPrices(csvData: String): Map<Int, Double> {
        val prices = mutableMapOf<Int, Double>()
        val lines = csvData.trim().split("\n")
        if (lines.size <= 1) return prices
        
        val delimiter = if (lines[0].contains(";")) ";" else if (lines[0].contains("\t")) "\t" else ","
        
        lines.drop(1).forEach { line ->
            val parts = line.split(delimiter)
            if (parts.size >= 4) {
                try {
                    val id = parts[0].trim().toInt()
                    val kiPrice = parts[2].trim().toDouble()
                    val marketPrice = parts[3].trim().toDouble()
                    prices[id] = if (marketPrice > 0) marketPrice else kiPrice
                } catch (e: Exception) {}
            }
        }
        return prices
    }

    fun parseProductionData(csvData: String): List<ProductionData> {
        val list = mutableListOf<ProductionData>()
        val lines = csvData.trim().split("\n")
        if (lines.size <= 1) return list
        
        val delimiter = if (lines[0].contains(";")) ";" else if (lines[0].contains("\t")) "\t" else ","
        
        lines.drop(1).forEach { line ->
            val parts = line.split(delimiter)
            if (parts.size >= 13) {
                try {
                    list.add(ProductionData(
                        outputItemId = parts[0].trim().toInt(),
                        itemName = parts[1].trim().removeSurrounding("\""),
                        factoryId = parts[2].trim().toInt(),
                        factoryName = parts[3].trim().removeSurrounding("\""),
                        baseOutputPerHour = parts[4].trim().toDouble(),
                        outputPerCycle = parts[5].trim().toDouble(),
                        creditsPerCycle = parts[6].trim().toDouble(),
                        input1Id = parts[7].trim().toIntOrNull(),
                        input1Qty = parts[8].trim().toDoubleOrNull() ?: 0.0,
                        input2Id = parts[9].trim().toIntOrNull(),
                        input2Qty = parts[10].trim().toDoubleOrNull() ?: 0.0,
                        input3Id = parts[11].trim().toIntOrNull(),
                        input3Qty = parts[12].trim().toDoubleOrNull() ?: 0.0
                    ))
                } catch (e: Exception) {}
            }
        }
        return list
    }
}
