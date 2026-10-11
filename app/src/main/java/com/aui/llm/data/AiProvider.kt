package com.aui.llm.data
data class AiModel(val id: String, val label: String)
enum class AiProvider(val displayName: String, val emoji: String, val hint: String, val models: List<AiModel>) {
    GEMINI("GEMINI","💎","AIzaSy...", listOf(AiModel("gemini-2.5-flash","2.5 Flash"), AiModel("gemini-1.5-flash","1.5 Flash"))),
    GROQ("GROQ","⚡","gsk_...", listOf(AiModel("llama-3.3-70b-versatile","Llama 3.3"))),
    CLAUDE("CLAUDE","🧠","sk-ant-...", listOf(AiModel("claude-3-5-sonnet-20241022","3.5 Sonnet"))),
    OPENAI("OPENAI","🤖","sk-proj-...", listOf(AiModel("gpt-4o-mini","GPT-4o mini"))),
    DEEPSEEK("DEEPSEEK","🐋","sk-...", listOf(AiModel("deepseek-chat","Chat"))),
    GROK("GROK","🌌","xai-...", listOf(AiModel("grok-beta","Beta"))),
    QWEN("QWEN","🌐","sk-...", listOf(AiModel("qwen-turbo","Turbo"))),
    OPENROUTER("OPENROUTER","🚀","sk-or-v1-...", listOf(AiModel("google/gemini-2.0-flash-exp:free","Gemini Free"))),
    GITHUB("GITHUB","🐙","ghp_...", listOf(AiModel("gpt-4o","GPT-4o"))),
    ELEVENLABS("ELEVENLABS","🎙️","sk_...", listOf(AiModel("eleven_multilingual_v2","v2"))),
    HACKERAI("HACKER AI","💀","api-key", listOf(AiModel("hackerai-v1","v1")))
}
