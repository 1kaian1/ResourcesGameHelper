package at.uastw.resourcesgamehelper

data class GameItem(
    val id: Int,
    val name: String,
    val iconUrl: String?,
    val baseEarningsPerHour: Double = 0.0, // €/h at Q100% and C100%
    val baseOutputPerHour: Double = 0.0    // m3/h at Q100% and C100%
)
