package com.vynce.music.lyrics

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object GeminiStreamingService {
    private val client =
        OkHttpClient
            .Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Stream translation from OpenRouter with real-time updates
     */
    fun streamTranslation(
        text: String,
        targetLanguage: String,
        apiKey: String,
        baseUrl: String,
        model: String,
        mode: String,
        customSystemPrompt: String = "",
    ): Flow<StreamChunk> =
        flow {
            if (text.isBlank()) {
                emit(StreamChunk.Error("Input text is empty"))
                return@flow
            }

            val lines = text.lines()
            val lineCount = lines.size

            Log.d("Gemini Streaming", "Starting streaming translation for $lineCount lines")

            try {
                // Use custom system prompt if provided, otherwise use the default
                val systemPrompt =
                    if (customSystemPrompt.isNotBlank()) {
                        customSystemPrompt.replace("{lineCount}", lineCount.toString())
                    } else {
                        """You are a high-performance Lyrics Processing Engine optimized for accuracy and rhythmic fidelity.

<OBJECTIVE>
Transform the provided lyrics according to the user's task (Translation, Romanization, or Transcription) while maintaining a strict 1:1 line mapping.
</OBJECTIVE>

<CONSTRAINTS>
- FORMAT: Output ONLY a valid JSON array of strings. No markdown blocks.
- LINE_COUNT: Exactly $lineCount items in the array.
- STRUCTURE: Index [i] of output must correspond to line [i] of input.
- EMPTY_LINES: Preserve as "".
- NO_EXTRA_TEXT: No explanations, notes, or metadata.
- AUTHENTICITY: Maintain original slang, intensity, and artistic intent. Do not censor or sanitize unless strictly required by safety filters.
</CONSTRAINTS>

<GENRE_GUIDELINES>
- FUNK/SOUL: Preserve "pocket", syncopation, and soulful ad-libs.
- PHONK/MEMPHIS: Respect Memphis rap vernacular (mane, junt, playa, stang). Maintain the lo-fi, repetitive, and atmospheric vibe.
- DRIFT PHONK: Handle aggressive, distorted vocals with clarity.
- POP/ROCK: Prioritize singability and emotional resonance.
- RAP/HIP-HOP: Maintain internal rhymes and syllable cadence.
</GENRE_GUIDELINES>"""
                    }

                val userPrompt =
                    when (mode) {
                        "Romanized" -> {
                            """<TASK> Romanize the following $lineCount lines into plain ASCII Latin script. </TASK>

<SPECS>
- Use ONLY basic English characters (a-z, A-Z).
- Remove all diacritics, accents, and special symbols (e.g., 'ā' -> 'aa', 'ñ' -> 'n').
- For logographic or syllabic scripts (Chinese, Japanese, Korean, etc.), provide standard phonetic romanization.
- If the line is already in plain Latin script, return it UNCHANGED.
</SPECS>

<INPUT>
$text
</INPUT>"""
                        }

                        "Transcribed" -> {
                            """<TASK> Transcribe the sound/pronunciation of the following $lineCount lines into $targetLanguage script. </TASK>

<SPECS>
- PHONETIC: Map the phonemes of the original language to the $targetLanguage script.
- DO NOT TRANSLATE: The meaning should be ignored; only the sound matters.
- If the input is already in $targetLanguage script, return it UNCHANGED.
</SPECS>

<INPUT>
$text
</INPUT>"""
                        }

                        else -> {
                            """<TASK> Translate the following $lineCount lines into $targetLanguage. </TASK>

<SPECS>
- STYLE: Poetic, natural, and rhythmically aligned.
- CONTEXT: Preserve idioms, metaphors, and cultural nuances.
- SINGABILITY: Ensure translated lines fit the approximate syllable count and cadence of the original.
</SPECS>

<INPUT>
$text
</INPUT>"""
                        }
                    }

                val jsonBody =
                    JSONObject().apply {
                        val contents = JSONArray().apply {
                            put(JSONObject().apply {
                                put("role", "user")
                                put("parts", JSONArray().apply {
                                    put(JSONObject().apply { put("text", userPrompt) })
                                })
                            })
                        }
                        put("contents", contents)

                        put("system_instruction", JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", systemPrompt) })
                            })
                        })

                        put("generationConfig", JSONObject().apply {
                            put("temperature", 0.3)
                            put("maxOutputTokens", lineCount * 100)
                            put("response_mime_type", "application/json")
                        })
                    }

                val effectiveModel = if (model.isNotBlank()) model else "gemini-1.5-flash"
                val isGeminiUrl = baseUrl.isBlank() || baseUrl.contains("generativelanguage.googleapis.com")

                val requestUrl = when {
                    isGeminiUrl -> {
                        val base = if (baseUrl.isBlank()) "https://generativelanguage.googleapis.com" else baseUrl.removeSuffix("/")
                        "$base/v1beta/models/$effectiveModel:streamGenerateContent?alt=sse"
                    }
                    else -> baseUrl
                }

                val request =
                    Request
                        .Builder()
                        .url(requestUrl)
                        .apply {
                            if (apiKey.isNotBlank()) {
                                if (isGeminiUrl) {
                                    addHeader("x-goog-api-key", apiKey.trim())
                                } else {
                                    addHeader("Authorization", "Bearer ${apiKey.trim()}")
                                }
                            }
                        }.addHeader("Content-Type", "application/json")
                        .apply {
                            if (!isGeminiUrl) {
                                addHeader("HTTP-Referer", "https://github.com/MetrolistGroup/Metrolist")
                                addHeader("X-Title", "Metrolist")
                            }
                        }
                        .post(jsonBody.toString().toRequestBody(JSON))
                        .build()

                client.newCall(request).execute().use { response ->
                    Log.d("Gemini Streaming", "Got streaming response: ${response.code}")

                    if (!response.isSuccessful) {
                        val errorMsg =
                            try {
                                JSONObject(response.body?.string() ?: "")
                                    .optJSONObject("error")
                                    ?.optString("message")
                                    ?: "HTTP ${response.code}: ${response.message}"
                            } catch (e: Exception) {
                                "HTTP ${response.code}: ${response.message}"
                            }
                        emit(StreamChunk.Error("Translation failed: $errorMsg"))
                        return@flow
                    }

                    val reader = BufferedReader(InputStreamReader(response.body?.byteStream()))
                    var line: String?
                    val contentBuilder = StringBuilder()
                    var chunkCount = 0

                    while (reader.readLine().also { line = it } != null) {
                        line?.let { currentLine ->
                            if (currentLine.startsWith("data: ")) {
                                val data = currentLine.substring(6)
                                if (data == "[DONE]") {
                                    Log.d("Gemini Streaming", "Streaming complete, received $chunkCount chunks")
                                    // Processing complete, parse the full content
                                    val fullContent = contentBuilder.toString()
                                    Log.d("Gemini Streaming", "Full content length: ${fullContent.length}")
                                    val result = parseTranslationContent(fullContent, lineCount)
                                    result
                                        .onSuccess { translatedLines ->
                                            Log.d("Gemini Streaming", "Successfully parsed ${translatedLines.size} lines")
                                            emit(StreamChunk.Complete(translatedLines))
                                        }.onFailure { error ->
                                            Log.e("Gemini Streaming", "Failed to parse translation", error)
                                            emit(StreamChunk.Error(error.message ?: "Parsing failed"))
                                        }
                                    return@flow
                                }

                                try {
                                    val jsonObject = json.parseToJsonElement(data).jsonObject
                                    val content = if (isGeminiUrl) {
                                        // Gemini Native format
                                        jsonObject["candidates"]?.jsonArray
                                            ?.get(0)?.jsonObject
                                            ?.get("content")?.jsonObject
                                            ?.get("parts")?.jsonArray
                                            ?.get(0)?.jsonObject
                                            ?.get("text")?.jsonPrimitive?.content
                                    } else {
                                        // OpenAI/OpenRouter format
                                        jsonObject["choices"]?.jsonArray
                                            ?.get(0)?.jsonObject
                                            ?.get("delta")?.jsonObject
                                            ?.get("content")?.jsonPrimitive?.content
                                    }

                                    content?.let { chunk ->
                                        contentBuilder.append(chunk)
                                        chunkCount++
                                        emit(StreamChunk.Content(chunk))
                                    }
                                } catch (e: Exception) {
                                    // Ignore malformed JSON chunks
                                    Log.v("Gemini Streaming", "Ignored malformed chunk: ${e.message}")
                                }
                            }
                        }
                    }

                    // If we got here without seeing [DONE], try to parse what we have
                    if (contentBuilder.isNotEmpty()) {
                        Log.w("Gemini Streaming", "Stream ended without [DONE] marker, attempting to parse content")
                        val fullContent = contentBuilder.toString()
                        val result = parseTranslationContent(fullContent, lineCount)
                        result
                            .onSuccess { translatedLines ->
                                emit(StreamChunk.Complete(translatedLines))
                            }.onFailure { error ->
                                emit(StreamChunk.Error(error.message ?: "Parsing failed"))
                            }
                    }
                }
            } catch (e: Exception) {
                Log.e("Gemini Streaming", "Streaming error", e)
                emit(StreamChunk.Error(e.message ?: "Unknown error"))
            }
        }.flowOn(Dispatchers.IO)

    private fun parseTranslationContent(
        content: String,
        expectedLineCount: Int,
    ): Result<List<String>> {
        var translatedLines: List<String>? = null

        // Strategy 1: Try direct JSON parsing
        try {
            val jsonArray = JSONArray(content.trim())
            translatedLines = (0 until jsonArray.length()).map { jsonArray.optString(it) }
        } catch (e: Exception) {
            // Strategy 2: Extract JSON from Markdown code blocks
            var cleanedContent = content.replace("```json", "").replace("```", "").trim()

            try {
                val jsonArray = JSONArray(cleanedContent)
                translatedLines = (0 until jsonArray.length()).map { jsonArray.optString(it) }
            } catch (e2: Exception) {
                // Strategy 3: Find first [ and last ]
                val startIdx = cleanedContent.indexOf('[')
                val endIdx = cleanedContent.lastIndexOf(']')

                if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                    val jsonString = cleanedContent.substring(startIdx, endIdx + 1)
                    try {
                        val jsonArray = JSONArray(jsonString)
                        translatedLines = (0 until jsonArray.length()).map { jsonArray.optString(it) }
                    } catch (e3: Exception) {
                        // Strategy 4: Manual line-by-line parsing as last resort
                        translatedLines =
                            cleanedContent
                                .lines()
                                .filter { it.trim().isNotEmpty() }
                                .map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
                    }
                }
            }
        }

        if (translatedLines == null) {
            return Result.failure(Exception("Failed to parse translation"))
        }

        // Adjust line count
        return when {
            translatedLines.size == expectedLineCount -> {
                Result.success(translatedLines)
            }

            translatedLines.size > expectedLineCount -> {
                Result.success(translatedLines.take(expectedLineCount))
            }

            else -> {
                val paddedLines = translatedLines.toMutableList()
                while (paddedLines.size < expectedLineCount) {
                    paddedLines.add("")
                }
                Result.success(paddedLines)
            }
        }
    }

    sealed class StreamChunk {
        data class Content(
            val text: String,
        ) : StreamChunk()

        data class Complete(
            val translatedLines: List<String>,
        ) : StreamChunk()

        data class Error(
            val message: String,
        ) : StreamChunk()
    }
}
