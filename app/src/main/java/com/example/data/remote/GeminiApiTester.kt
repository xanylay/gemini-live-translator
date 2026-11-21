package com.example.data.remote

import com.example.domain.model.ApiKeyTestResult
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class GeminiApiTester(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()
) {
    suspend fun testApiKey(apiKey: String): ApiKeyTestResult = withContext(Dispatchers.IO) {
        val cleaned = apiKey.trim()
        if (cleaned.isEmpty()) {
            return@withContext ApiKeyTestResult.Error("Please enter a Gemini API key first.")
        }

        val startMs = System.currentTimeMillis()
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models?pageSize=100&key=$cleaned")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val latency = (System.currentTimeMillis() - startMs).coerceAtLeast(1L)
                val body = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    val apiMessage = runCatching {
                        JSONObject(body).optJSONObject("error")?.optString("message")
                    }.getOrNull()
                    val friendly = when (response.code) {
                        400, 401, 403 -> "Invalid or unauthorized API key (${response.code}). ${apiMessage.orEmpty()}"
                        429 -> "Rate limit or quota exceeded (429). ${apiMessage.orEmpty()}"
                        else -> "Gemini API error (${response.code}): ${apiMessage ?: response.message}"
                    }.trim()
                    return@withContext ApiKeyTestResult.Error(friendly)
                }

                val json = JSONObject(body)
                val modelsArray = json.optJSONArray("models")
                val totalModels = modelsArray?.length() ?: 0
                val liveModels = mutableListOf<String>()

                if (modelsArray != null) {
                    for (i in 0 until modelsArray.length()) {
                        val obj = modelsArray.optJSONObject(i) ?: continue
                        val fullName = obj.optString("name").removePrefix("models/")
                        val methods = obj.optJSONArray("supportedGenerationMethods")
                        if (methods != null) {
                            for (j in 0 until methods.length()) {
                                if (methods.optString(j) == "bidiGenerateContent") {
                                    liveModels.add(fullName)
                                    break
                                }
                            }
                        }
                    }
                }

                ApiKeyTestResult.Success(
                    modelCount = totalModels,
                    liveModels = liveModels,
                    latencyMs = latency
                )
            }
        } catch (e: IOException) {
            ApiKeyTestResult.Error("Network error: ${e.localizedMessage ?: "Unable to reach Gemini API"}")
        } catch (e: Exception) {
            ApiKeyTestResult.Error("Verification failed: ${e.localizedMessage ?: "Unexpected error"}")
        }
    }
}
