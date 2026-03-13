package at.uastw.resourcesgamehelper

import com.google.gson.annotations.SerializedName

data class MineDto(
    @SerializedName("mineID") val mineID: Int,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
    @SerializedName("HQboost") val hqBoost: Double,
    @SerializedName("fullrate") val fullRate: Double,
    @SerializedName("rawrate") val rawRate: Double,
    @SerializedName("techfactor") val techFactor: Double,
    @SerializedName("name") val name: String,
    @SerializedName("builddate") val buildDate: Long,
    @SerializedName("lastmaintenance") val lastMaintenance: Long,
    @SerializedName("condition") val condition: Double,
    @SerializedName("resourceName") val resourceName: String,
    @SerializedName("resourceID") val resourceID: Int,
    @SerializedName("lastenemyaction") val lastEnemyAction: Long,
    @SerializedName("def1") val def1: Int,
    @SerializedName("def2") val def2: Int,
    @SerializedName("def3") val def3: Int,
    @SerializedName("attackpenalty") val attackPenalty: Double,
    @SerializedName("attackcount") val attackCount: Int,
    @SerializedName("attacklost") val attackLost: Int,
    @SerializedName("quality") val quality: Double,
    @SerializedName("qualityInclTU") val qualityInclTU: Double
)

data class ItemPriceDto(
    @SerializedName("itemID") val itemID: Int,
    @SerializedName("itemName") val itemName: String,
    @SerializedName("KIprice") val kiPrice: Double,
    @SerializedName("price") val marketPrice: Double,
    @SerializedName("unixts") val unixTs: Long
)
