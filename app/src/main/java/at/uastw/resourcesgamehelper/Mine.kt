package at.uastw.resourcesgamehelper

data class Mine(
    val mineID: Int,
    val lat: Double,
    val lon: Double,
    val HQboost: Double,
    val fullrate: Double,
    val rawrate: Double,      // Changed from Int to Double
    val techfactor: Double,   // Changed from Int to Double
    val name: String,
    val builddate: Long,
    val lastmaintenance: Long,
    val condition: Double,
    val resourceName: String,
    val resourceID: Int,
    val lastenemyaction: Long,
    val def1: Int,
    val def2: Int,
    val def3: Int,
    val attackpenalty: Double, // Changed from Int to Double based on "0.918" in logs
    val attackcount: Int,
    val attacklost: Int,
    val quality: Double,
    val qualityInclTU: Double
)
