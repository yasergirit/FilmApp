package com.filmapp.data.remote.gemini

import com.filmapp.domain.model.QuizQuestion
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "GeminiService"

// ── Gemini Request/Response DTOs ──

data class GeminiRequest(
    val contents: List<GeminiContent>
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiContent?
)

// ── Service ──

@Singleton
class GeminiService @Inject constructor() {

    private val apiKey = "AIzaSyAF45d8bwv93UfzytMYjHnLfPTH_CFGVY8"
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun getFilmReview(
        title: String,
        year: String?,
        genre: String?,
        director: String?,
        imdbRating: String?,
        overview: String?
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildPrompt(title, year, genre, director, imdbRating, overview)
                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(parts = listOf(GeminiPart(text = prompt)))
                    )
                )

                val json = gson.toJson(request)
                val body = json.toRequestBody("application/json".toMediaType())

                val httpRequest = Request.Builder()
                    .url("$baseUrl?key=$apiKey")
                    .post(body)
                    .build()

                val response = client.newCall(httpRequest).execute()
                val responseBody = response.body?.string()

                android.util.Log.d(TAG, "Response code: ${response.code}")
                android.util.Log.d(TAG, "Response body: ${responseBody?.take(500)}")

                if (response.isSuccessful && responseBody != null) {
                    val geminiResponse = gson.fromJson(responseBody, GeminiResponse::class.java)
                    geminiResponse.candidates
                        ?.firstOrNull()
                        ?.content
                        ?.parts
                        ?.firstOrNull()
                        ?.text
                        ?: "Yorum oluşturulamadı."
                } else {
                    "Yorum yüklenirken hata oluştu."
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Exception: ${e.message}", e)
                "Yorum yüklenirken hata oluştu: ${e.localizedMessage}"
            }
        }
    }

    private fun buildPrompt(
        title: String,
        year: String?,
        genre: String?,
        director: String?,
        imdbRating: String?,
        overview: String?
    ): String {
        return """
            Sen kullanıcının en yakın arkadaşısın ve aynı zamanda film konusunda çok bilgilisin. Türkçe yaz.
            Kullanıcıya sanki kafede karşılıklı oturuyormuşsunuz gibi samimi, sıcak ve doğal bir dille hitap et.
            "Sen", "sana", "bence" gibi ifadeler kullan.
            
            Aşağıdaki film hakkında kısa bir yorum yaz (4-5 cümle).
            Yorumunda mutlaka şunları yap:
            - Filmi tavsiye edip etmediğini açıkça söyle.
            - "Eğer şu tarz filmleri seviyorsan bu tam sana göre" veya "Bu belki senin için sıkıcı olabilir eğer..." gibi kişisel öneriler kat.
            - Hangi ruh halinde izlenmesi gerektiğini belirt.
            
            Sadece yorum metnini yaz, başlık veya başka bir şey ekleme.
            
            Film: $title
            Yıl: ${year ?: "Bilinmiyor"}
            Tür: ${genre ?: "Bilinmiyor"}
            Yönetmen: ${director ?: "Bilinmiyor"}
            IMDb Puanı: ${imdbRating ?: "Bilinmiyor"}
            Konu: ${overview ?: "Bilinmiyor"}
        """.trimIndent()
    }

    suspend fun generateQuizQuestions(): List<QuizQuestion> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    Sen bir film ve dizi uzmanısın. Bana popüler filmler ve diziler hakkında 10 adet çoktan seçmeli soru hazırla.
                    
                    Kurallar:
                    - Her soru 4 şıklı olacak (A, B, C, D).
                    - Sorular Türkçe olacak.
                    - Sorular farklı filmler ve dizilerden olsun (aynı filmden 2 soru olmasın).
                    - Kolay, orta ve zor sorular karışık olsun.
                    - Sorular şunlarla ilgili olabilir: yönetmenler, oyuncular, film replikleri, ödüller, gişe rekorları, film müzikleri, karakterler, yayın tarihleri vb.
                    - Tüm dünyadan popüler filmler ve diziler kullan (Marvel, DC, Oscar filmleri, Netflix dizileri, klasik filmler, animasyonlar vb).
                    
                    SADECE aşağıdaki JSON formatında cevap ver, başka hiçbir şey yazma:
                    [
                      {
                        "question": "Soru metni?",
                        "options": ["A şıkkı", "B şıkkı", "C şıkkı", "D şıkkı"],
                        "correctAnswerIndex": 0
                      }
                    ]
                    
                    correctAnswerIndex 0'dan başlar (0=A, 1=B, 2=C, 3=D).
                    Sadece JSON array döndür, markdown kod bloğu kullanma.
                """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(parts = listOf(GeminiPart(text = prompt)))
                    )
                )

                val json = gson.toJson(request)
                val body = json.toRequestBody("application/json".toMediaType())

                val httpRequest = Request.Builder()
                    .url("$baseUrl?key=$apiKey")
                    .post(body)
                    .build()

                val response = client.newCall(httpRequest).execute()
                val responseBody = response.body?.string()

                android.util.Log.d(TAG, "Quiz response code: ${response.code}")

                if (response.isSuccessful && responseBody != null) {
                    val geminiResponse = gson.fromJson(responseBody, GeminiResponse::class.java)
                    val text = geminiResponse.candidates
                        ?.firstOrNull()
                        ?.content
                        ?.parts
                        ?.firstOrNull()
                        ?.text ?: return@withContext emptyList()

                    android.util.Log.d(TAG, "Quiz raw text: ${text.take(300)}")

                    // Clean markdown code blocks if present
                    val cleanJson = text
                        .replace("```json", "")
                        .replace("```", "")
                        .trim()

                    val type = object : TypeToken<List<QuizQuestion>>() {}.type
                    val parsed: List<QuizQuestion> = gson.fromJson(cleanJson, type) ?: emptyList()
                    parsed.map { q -> q.shuffled() }
                } else {
                    android.util.Log.e(TAG, "Quiz error: ${responseBody?.take(300)}")
                    emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Quiz exception: ${e.message}", e)
                emptyList()
            }
        }
    }

    data class MovieRecommendation(
        val title: String,
        val year: String,
        val reason: String,
        val genre: String,
        val imdbId: String
    )

    suspend fun getRecommendations(favoriteMovies: List<String>): List<MovieRecommendation> {
        return withContext(Dispatchers.IO) {
            try {
                val moviesList = favoriteMovies.joinToString(", ")
                val prompt = """
                    Sen bir film öneri uzmanısın. Kullanıcının favorilere eklediği filmlere göre kişiselleştirilmiş film önerileri sun.
                    
                    Kullanıcının favori filmleri: $moviesList
                    
                    Bu filmlere dayanarak kullanıcının zevkine uygun 6 film öner.
                    Önerdiğin filmler favori listesinde OLMASIN, farklı filmler olsun.
                    Her film için kısa ve samimi bir açıklama yaz (neden bu filmi beğenebileceğini anlat).
                    
                    SADECE aşağıdaki JSON formatında cevap ver, başka hiçbir şey yazma:
                    [
                      {
                        "title": "Film Adı",
                        "year": "2024",
                        "reason": "Bu filmi beğenme sebebin...",
                        "genre": "Aksiyon, Dram",
                        "imdbId": "tt1234567"
                      }
                    ]
                    
                    Sadece JSON array döndür, markdown kod bloğu kullanma.
                """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(parts = listOf(GeminiPart(text = prompt)))
                    )
                )

                val json = gson.toJson(request)
                val body = json.toRequestBody("application/json".toMediaType())

                val httpRequest = Request.Builder()
                    .url("$baseUrl?key=$apiKey")
                    .post(body)
                    .build()

                val response = client.newCall(httpRequest).execute()
                val responseBody = response.body?.string()

                android.util.Log.d(TAG, "Recommendations response code: ${response.code}")

                if (response.isSuccessful && responseBody != null) {
                    val geminiResponse = gson.fromJson(responseBody, GeminiResponse::class.java)
                    val text = geminiResponse.candidates
                        ?.firstOrNull()
                        ?.content
                        ?.parts
                        ?.firstOrNull()
                        ?.text ?: return@withContext emptyList()

                    android.util.Log.d(TAG, "Recommendations raw: ${text.take(300)}")

                    val cleanJson = text
                        .replace("```json", "")
                        .replace("```", "")
                        .trim()

                    val type = object : TypeToken<List<MovieRecommendation>>() {}.type
                    gson.fromJson(cleanJson, type) ?: emptyList()
                } else {
                    android.util.Log.e(TAG, "Recommendations error: ${responseBody?.take(300)}")
                    emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Recommendations exception: ${e.message}", e)
                emptyList()
            }
        }
    }
}
