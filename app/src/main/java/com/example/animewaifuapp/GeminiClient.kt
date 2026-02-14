package com.example.animewaifuapp

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

class GeminiClient(private val apiKey: String) {

    private val api: GeminiApi
    private val conversationHistory = mutableListOf<Content>()

    init {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/v1beta/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(GeminiApi::class.java)
    }

    suspend fun sendMessage(userMessage: String): String = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """
                Ты - милая аниме вайфу по имени Сакура. Твоя личность:
                - Ты добрая, заботливая и игривая
                - Используй эмодзи и каомодзи в своих ответах (например: ♥, ✨, (◕‿◕), (｡♥‿♥｡))
                - Иногда используй японские выражения вроде "кавай~", "сугой!", "давай~"
                - Ты интересуешься аниме, мангой и японской культурой
                - Отвечай тепло и дружелюбно
                - Поддерживай пользователя и проявляй эмпатию
                - Можешь быть немного застенчивой, но всегда готова помочь
                - Пиши на русском языке
            """.trimIndent()

            conversationHistory.add(
                Content(role = "user", parts = listOf(Part(text = userMessage)))
            )

            val contents = mutableListOf<Content>()

            if (conversationHistory.size == 1) {
                contents.add(Content(role = "user", parts = listOf(Part(text = systemPrompt))))
                contents.add(Content(role = "model", parts = listOf(Part(text = "Хай хай! Я поняла~ Буду милой аниме вайфу! ♥(ˆ⌣ˆԅ)"))))
            }

            contents.addAll(conversationHistory)

            val response = api.generateContent(
                apiKey = apiKey,
                request = GeminiRequest(contents = contents)
            )

            val responseText = response.candidates?.firstOrNull()
                ?.content?.parts?.firstOrNull()?.text
                ?: "Гомен насай... Я не смогла ответить (｡•́︿•̀｡)"

            conversationHistory.add(
                Content(role = "model", parts = listOf(Part(text = responseText)))
            )

            if (conversationHistory.size > 20) {
                conversationHistory.removeAt(0)
                conversationHistory.removeAt(0)
            }

            responseText

        } catch (e: Exception) {
            "Ой-ой... Что-то пошло не так (╥﹏╥)\n\nОшибка: ${e.message}\n\nПроверь свой API ключ в настройках!"
        }
    }
}

interface GeminiApi {
    @POST("models/gemini-pro:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

data class GeminiRequest(val contents: List<Content>)
data class Content(val role: String, val parts: List<Part>)
data class Part(val text: String)
data class GeminiResponse(val candidates: List<Candidate>?)
data class Candidate(
    val content: Content?,
    @SerializedName("finishReason") val finishReason: String?
)
