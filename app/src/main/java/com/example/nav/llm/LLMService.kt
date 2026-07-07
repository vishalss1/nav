package com.example.nav.llm

import android.util.Log
import com.example.nav.model.ScreenContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LLMService {
    private val TAG = "LLMService"

    private val api: GroqApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.groq.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(GroqApi::class.java)
    }

    suspend fun getNavigationSteps(apiKey: String, context: ScreenContext, userIntent: String): String {
        val prompt = buildPrompt(context, userIntent)
        
        return try {
            val response = api.getChatCompletion(
                authHeader = "Bearer $apiKey",
                request = GroqRequest(
                    model = "llama-3.3-70b-versatile",
                    messages = listOf(
                        Message(role = "system", content = "You are a phone navigation assistant helping an elderly user navigate Android apps. Give numbered steps using ONLY elements visible on screen. Keep responses under 5 steps."),
                        Message(role = "user", content = prompt)
                    )
                )
            )
            response.choices.firstOrNull()?.message?.content ?: "No response from LLM"
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Groq API", e)
            "Error: ${e.message}"
        }
    }

    private fun buildPrompt(context: ScreenContext, userIntent: String): String {
        return """
            User wants to: $userIntent
            
            Current screen:
            - App: ${context.appName} (${context.appPackage})
            - Visible text: ${context.visibleTexts.joinToString(", ")}
            - Tappable: ${context.clickableElements.map { it.text }.joinToString(", ")}
            
            Provide steps.
        """.trimIndent()
    }
}
