package com.example.nav.llm

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GroqApi {
    @POST("openai/v1/chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") authHeader: String,
        @Body request: GroqRequest
    ): GroqResponse
}

data class GroqRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.5,
    @SerializedName("max_tokens") val maxTokens: Int = 1024
)

data class Message(
    val role: String,
    val content: String
)

data class GroqResponse(
    val id: String,
    val choices: List<Choice>
)

data class Choice(
    val message: Message,
    @SerializedName("finish_reason") val finishReason: String
)
