package com.aui.llm.api
import com.aui.llm.data.AiProvider
import com.aui.llm.data.db.entities.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ApiResult(val success: Boolean, val message: String)

class AiClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS).build()
    private val JSON = "application/json".toMediaType()

    suspend fun test(p: AiProvider, k: String): ApiResult = withContext(Dispatchers.IO) {
        try {
            val m = p.models.firstOrNull()?.id ?: return@withContext ApiResult(false, "no model")
            val r = send(p, k, m, emptyList())
            if (r.startsWith("❌") || r.startsWith("⚠️")) ApiResult(false, r) else ApiResult(true, "✅ شغال")
        } catch (e: Exception) { ApiResult(false, "❌ ${e.message}") }
    }

    suspend fun send(p: AiProvider, key: String, model: String, history: List<MessageEntity>, prompt: String? = null): String =
        withContext(Dispatchers.IO) {
            try {
                val messages = history.map { JSONObject().apply { put("role", it.role); put("content", it.content) } }.toMutableList()
                if (prompt != null) messages.add(JSONObject().apply { put("role", "user"); put("content", prompt) })

                when (p) {
                    AiProvider.GEMINI -> gemini(key, model, messages)
                    AiProvider.CLAUDE -> claude(key, model, messages)
                    else -> {
                        val url = when (p) {
                            AiProvider.OPENAI -> "https://api.openai.com/v1/chat/completions"
                            AiProvider.GROQ -> "https://api.groq.com/openai/v1/chat/completions"
                            AiProvider.DEEPSEEK -> "https://api.deepseek.com/v1/chat/completions"
                            AiProvider.GROK -> "https://api.x.ai/v1/chat/completions"
                            AiProvider.QWEN -> "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
                            AiProvider.OPENROUTER -> "https://openrouter.ai/api/v1/chat/completions"
                            AiProvider.GITHUB -> "https://models.inference.ai.azure.com/chat/completions"
                            AiProvider.HACKERAI -> "https://api.hackerai.co/v1/chat/completions"
                            else -> ""
                        }
                        if (url.isEmpty()) "⚠️ غير مدعوم" else openai(url, key, model, messages)
                    }
                }
            } catch (e: Exception) { "❌ ${e.message}" }
        }

    private fun openai(url: String, key: String, model: String, msgs: List<JSONObject>): String {
        val body = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray(msgs))
        }.toString()
        val req = Request.Builder().url(url)
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody(JSON)).build()
        http.newCall(req).execute().use { r ->
            val s = r.body?.string() ?: ""
            if (!r.isSuccessful) return "❌ HTTP ${r.code}: $s"
            return JSONObject(s).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        }
    }

    private fun gemini(key: String, model: String, msgs: List<JSONObject>): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key"
        val contents = JSONArray()
        for (m in msgs) {
            val role = if (m.getString("role") == "assistant") "model" else "user"
            contents.put(JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().apply { put(JSONObject().apply { put("text", m.getString("content")) }) })
            })
        }
        val body = JSONObject().apply { put("contents", contents) }.toString()
        val req = Request.Builder().url(url).addHeader("Content-Type", "application/json")
            .post(body.toRequestBody(JSON)).build()
        http.newCall(req).execute().use { r ->
            val s = r.body?.string() ?: ""
            if (!r.isSuccessful) return "❌ HTTP ${r.code}: $s"
            return JSONObject(s).getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
        }
    }

    private fun claude(key: String, model: String, msgs: List<JSONObject>): String {
        val body = JSONObject().apply {
            put("model", model); put("max_tokens", 4096)
            put("messages", JSONArray(msgs))
        }.toString()
        val req = Request.Builder().url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", key)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody(JSON)).build()
        http.newCall(req).execute().use { r ->
            val s = r.body?.string() ?: ""
            if (!r.isSuccessful) return "❌ HTTP ${r.code}: $s"
            return JSONObject(s).getJSONArray("content").getJSONObject(0).getString("text")
        }
    }
}
