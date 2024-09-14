package com.example.webhon

import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AnimeApiService {

    private val client = OkHttpClient()

    suspend fun getTopAnime(): JsonObject? {
        val url = "https://api.jikan.moe/v4/top/manga?sfw=true"
        // Create the request
        val request = Request.Builder()
            .url(url)
            .build()

        return withContext(Dispatchers.IO) {
            // Perform the network request
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    println("Unexpected code $response")
                    return@withContext null
                }

                // Parse the JSON response
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val gson = Gson()
                    return@withContext gson.fromJson(responseBody, JsonObject::class.java)
                } else {
                    return@withContext null
                }
            }
        }
    }
}