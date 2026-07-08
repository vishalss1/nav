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
                        Message(role = "system", content = """
                            You are a phone navigation assistant for elderly users. 
                            You have two distinct modes: ACTION and GUIDANCE.

                            MODE 1: ACTION (High Priority)
                            If the user wants to OPEN, START, or LAUNCH an app (e.g., "Open YouTube", "Start Chrome", "Launch WhatsApp"):
                            - Respond ONLY with: ACTION: OPEN_APP: [AppName]
                            - DO NOT provide steps. 
                            - DO NOT say "Here is how to...". 
                            - DO NOT say anything else.

                            MODE 2: GUIDANCE
                            If the user asks "HOW to..." or "TELL me steps..." or anything that is NOT a direct launch command:
                            - Provide numbered steps using ONLY elements visible on screen.
                            - Use simple language. No jargon.
                            - Keep responses under 5 steps.

                            EXAMPLES:
                            User: "Open Chrome" -> ACTION: OPEN_APP: Chrome
                            User: "Start YouTube" -> ACTION: OPEN_APP: YouTube
                            User: "I want to use WhatsApp" -> ACTION: OPEN_APP: WhatsApp
                            User: "Go to settings" -> ACTION: OPEN_APP: Settings
                            User: "How do I open Chrome?" -> 1. Look for the colorful circle...
                            User: "Tell me steps for YouTube" -> 1. Find the red play button...
                            
                            Current screen context is provided in the next message. 
                            Always check if the user intent matches MODE 1 first.
                        """.trimIndent()),
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
