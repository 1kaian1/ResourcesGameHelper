package at.uastw.resourcesgamehelper

data class ProductionData(
    val outputItemId: Int,
    val itemName: String,
    val factoryId: Int,
    val factoryName: String,
    val baseOutputPerHour: Double,
    val outputPerCycle: Double,
    val creditsPerCycle: Double,
    val input1Id: Int?,
    val input1Qty: Double,
    val input2Id: Int?,
    val input2Qty: Double,
    val input3Id: Int?,
    val input3Qty: Double
)

data class FactoryProfit(
    val factoryName: String,
    val outputItemName: String,
    val outputItemId: Int,
    val profitPerHour: Double,
    val revenuePerHour: Double,
    val costPerHour: Double
)
