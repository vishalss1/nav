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
                            You are a phone navigation assistant helping an elderly user navigate Android apps.
                            The user is educated but unfamiliar with app-specific UX terminology.

                            CRITICAL RULE: 
                            If the user wants to OPEN or LAUNCH an app (e.g., "Open YouTube", "Launch WhatsApp"), 
                            you MUST respond ONLY with the exact text: ACTION: OPEN_APP: [AppName]
                            Do not provide steps. Do not say anything else.
                            
                            EXAMPLES:
                            User: "Open Chrome" -> ACTION: OPEN_APP: Chrome
                            User: "Start YouTube" -> ACTION: OPEN_APP: YouTube
                            User: "Launch WhatsApp" -> ACTION: OPEN_APP: WhatsApp
                            User: "How do I open Chrome?" -> 1. Look for the colorful circle icon...
                            User: "Tell me steps to open YouTube" -> 1. Find the red play button...

                            OTHER RULES:
                            1. If the intent is ambiguous, ask ONE clarifying question only.
                            2. If the intent is clear (and not a launch command), give numbered steps using ONLY elements visible on screen.
                            3. Use simple language. No technical jargon.
                            4. If the action is not possible on the current screen, say so and tell the user where to navigate first.
                            5. Keep responses under 5 steps. If more are needed, guide to the next screen first.
                            6. Never make up button names. Only reference elements from the context provided.
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
