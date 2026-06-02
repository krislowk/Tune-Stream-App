package com.vynce.music.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

object GeminiService {
    private val client =
        OkHttpClient
            .Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    suspend fun translate(
        text: String,
        targetLanguage: String,
        apiKey: String,
        model: String,
        mode: String,
        maxRetries: Int = 3,
        sourceLanguage: String? = null,
        customSystemPrompt: String = "",
    ): Result<List<String>> =
        withContext(Dispatchers.IO) {
            var currentAttempt = 0

            // Validate input
            if (text.isBlank()) {
                return@withContext Result.failure(Exception("Input text is empty"))
            }

            val lines = text.lines()
            val lineCount = lines.size

            while (currentAttempt < maxRetries) {
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

                    val effectiveModel = if (model.isNotBlank()) model else "gemini-3.5-flash"
                    val request =
                        Request
                            .Builder()
                            .url("https://generativelanguage.googleapis.com/v1beta/models/$effectiveModel:generateContent")
                            .apply {
                                if (apiKey.isNotBlank()) {
                                    addHeader("x-goog-api-key", apiKey.trim())
                                }
                            }.addHeader("Content-Type", "application/json")
                            .post(jsonBody.toString().toRequestBody(JSON))
                            .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()

                    if (!response.isSuccessful) {
                        // Retry on server errors (5xx)
                        if (response.code >= 500) {
                            currentAttempt++
                            delay((1000L * currentAttempt).milliseconds)
                            continue
                        }

                        val errorMsg =
                            try {
                                JSONObject(responseBody ?: "").optJSONObject("error")?.optString("message")
                                    ?: "HTTP ${response.code}: ${response.message}"
                            } catch (_: Exception) {
                                "HTTP ${response.code}: ${response.message}"
                            }
                        return@withContext Result.failure(Exception("Translation failed: $errorMsg"))
                    }

                    if (responseBody == null) {
                        currentAttempt++
                        continue
                    }

                    val jsonResponse = JSONObject(responseBody)
                    val candidates = jsonResponse.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val contentObj = candidates.getJSONObject(0).optJSONObject("content")
                        val parts = contentObj?.optJSONArray("parts")
                        var content = parts?.optJSONObject(0)?.optString("text")?.trim()

                        if (!content.isNullOrBlank()) {
                            // Enhanced JSON extraction with multiple fallback strategies
                            var translatedLines: List<String>? = null

                            // Strategy 1: Try direct JSON parsing
                            try {
                                val jsonArray = JSONArray(content)
                                translatedLines = (0 until jsonArray.length()).map { jsonArray.optString(it) }
                            } catch (_: Exception) {
                                // Strategy 2: Extract JSON from Markdown code blocks
                                content = content.replace("```json", "").replace("```", "").trim()

                                try {
                                    val jsonArray = JSONArray(content)
                                    translatedLines = (0 until jsonArray.length()).map { jsonArray.optString(it) }
                                } catch (_: Exception) {
                                    // Strategy 3: Find first [ and last ]
                                    val startIdx = content.indexOf('[')
                                    val endIdx = content.lastIndexOf(']')

                                    if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                                        val jsonString = content.substring(startIdx, endIdx + 1)
                                        try {
                                            val jsonArray = JSONArray(jsonString)
                                            translatedLines = (0 until jsonArray.length()).map { jsonArray.optString(it) }
                                        } catch (_: Exception) {
                                            // Strategy 4: Manual line-by-line parsing as last resort
                                            translatedLines =
                                                content
                                                    .lines()
                                                    .filter { it.trim().isNotEmpty() }
                                                    .map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
                                        }
                                    }
                                }
                            }

                            if (translatedLines != null) {
                                // Validate line count matches
                                if (translatedLines.size == lineCount) {
                                    return@withContext Result.success(translatedLines)
                                } else if (translatedLines.size > lineCount) {
                                    // If we got more lines, take first N
                                    return@withContext Result.success(translatedLines.take(lineCount))
                                } else {
                                    // If we got fewer lines, pad with empty strings
                                    val paddedLines = translatedLines.toMutableList()
                                    while (paddedLines.size < lineCount) {
                                        paddedLines.add("")
                                    }
                                    return@withContext Result.success(paddedLines)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (currentAttempt == maxRetries - 1) {
                        return@withContext Result.failure(e)
                    }
                }
                currentAttempt++
                delay((1000L * currentAttempt).milliseconds)
            }
            return@withContext Result.failure(Exception("Max retries exceeded"))
        }

    suspend fun generateTimedLyrics(
        lyrics: String,
        title: String,
        artist: String,
        durationSeconds: Int,
        apiKey: String,
        model: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """You are a master Lyric Synchronizer. Convert static lyrics into a high-precision LRC [mm:ss.xx] file.

<OBJECTIVE>
Assign accurate timestamps to each line based on the provided song duration.
</OBJECTIVE>

<RULES>
- FORMAT: Raw LRC text only. No [ar:], [ti:], etc. unless requested.
- TIMING: Must be realistic for a $durationSeconds second song. 
- MUSICALITY: Align timestamps with rhythmic "downbeats" or vocal entries.
- GENRE: For Funk/Phonk, pay close attention to syncopated flows and repetitive loops.
- NO_CHAT: No introductory or concluding remarks.
</RULES>"""

            val userPrompt = """<SONG_INFO>
Title: $title
Artist: $artist
Duration: $durationSeconds seconds
</SONG_INFO>

<LYRICS>
$lyrics
</LYRICS>"""

            val jsonBody = JSONObject().apply {
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
                    put("temperature", 0.1)
                })
            }

            val effectiveModel = if (model.isNotBlank()) model else "gemini-3.5-flash-lite"
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/$effectiveModel:generateContent")
                .apply {
                    if (apiKey.isNotBlank()) {
                        addHeader("x-goog-api-key", apiKey.trim())
                    }
                }
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody(JSON))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Sync failed: ${response.code}"))
            }

            val jsonResponse = JSONObject(responseBody ?: "")
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val contentObj = candidates.getJSONObject(0).optJSONObject("content")
                val parts = contentObj?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text")?.trim() ?: ""
                
                // Clean up any potential markdown code blocks
                val cleanText = text.replace("```lrc", "").replace("```", "").trim()
                Result.success(cleanText)
            } else {
                Result.failure(Exception("No content in response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateLyrics(
        title: String,
        artist: String,
        apiKey: String,
        model: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """You are a lyrics retrieval expert. Your task is to provide the full, accurate lyrics for the requested song.

<RULES>
- Output ONLY the lyrics text.
- NO metadata (artist, title, [Verse 1], etc.) unless they are part of the original song structure.
- NO explanations or conversational text.
- If the song is instrumental or has no lyrics, output: [Instrumental]
</RULES>"""

            val userPrompt = """Provide the lyrics for:
Song: $title
Artist: $artist"""

            val jsonBody = JSONObject().apply {
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
                    put("temperature", 0.1)
                })
            }

            val effectiveModel = if (model.isNotBlank()) model else "gemini-3.5-flash"
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/$effectiveModel:generateContent")
                .apply {
                    if (apiKey.isNotBlank()) {
                        addHeader("x-goog-api-key", apiKey.trim())
                    }
                }
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody(JSON))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Generation failed: ${response.code}"))
            }

            val jsonResponse = JSONObject(responseBody ?: "")
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val contentObj = candidates.getJSONObject(0).optJSONObject("content")
                val parts = contentObj?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text")?.trim() ?: ""
                Result.success(text)
            } else {
                Result.failure(Exception("No content in response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
