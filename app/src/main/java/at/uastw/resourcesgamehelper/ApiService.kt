package at.uastw.resourcesgamehelper

import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("/")
    suspend fun getMines(
        @Query("q") queryType: String = "5",
        @Query("f") f: String = "1",
        @Query("k") apiKey: String = Config.API_KEY,
        @Query("l") language: String = "en"
    ): List<MineDto>

    @GET("/")
    suspend fun getItemPrices(
        @Query("q") queryType: String = "1006",
        @Query("f") f: String = "1",
        @Query("k") apiKey: String = Config.API_KEY,
        @Query("l") language: String = "en"
    ): List<ItemPriceDto>
}
