package at.uastw.resourcesgamehelper

import android.util.Log

object CsvParser {
    private const val TAG = "CsvParser"

    fun parseMines(csvData: String): List<Mine> {
        if (csvData.isBlank()) {
            Log.w(TAG, "Received empty CSV data")
            return emptyList()
        }

        val lines = csvData.trim().split("\n")
        Log.d(TAG, "Total lines received: ${lines.size}")
        
        if (lines.size <= 1) {
            Log.w(TAG, "CSV contains only header or is empty: $csvData")
            return emptyList()
        }

        // Detect delimiter (comma or semicolon)
        val firstLine = lines[0]
        val delimiter = if (firstLine.contains(";")) ";" else ","
        Log.d(TAG, "Detected delimiter: '$delimiter'")

        // Skip header
        val parsedMines = lines.drop(1).mapNotNull { line ->
            // Use regex or a more sophisticated split to handle quoted commas if necessary, 
            // but for this API simple split is usually enough if names don't contain delimiter.
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
                        resourceName = parts[11].trim(),
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
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing line: $line", e)
                    null
                }
            } else {
                if (line.isNotBlank()) {
                    Log.w(TAG, "Line has insufficient parts (${parts.size}): $line")
                }
                null
            }
        }
        
        Log.d(TAG, "Successfully parsed ${parsedMines.size} mines")
        return parsedMines
    }
}
