package com.trajectory.vault.data.ai

/** How a provider's HTTP API is shaped. The Ask AI client picks its request format from this. */
enum class ApiStyle(val label: String) {
    OPENAI_COMPATIBLE("OpenAI-compatible"),
    ANTHROPIC("Anthropic"),
    GEMINI("Google Gemini"),
}

/**
 * The AI services Ask AI can be pointed at. [defaultBaseUrl] is only a starting point that fills the
 * URL field; the user can edit it, and [CUSTOM] has none. [keyRequired] is false for servers
 * that run on your own network and usually need no key.
 */
enum class AiProvider(
    val label: String,
    val defaultBaseUrl: String,
    val style: ApiStyle,
    val keyRequired: Boolean,
) {
    OPENAI("OpenAI", "https://api.openai.com/v1", ApiStyle.OPENAI_COMPATIBLE, true),
    ANTHROPIC("Anthropic Claude", "https://api.anthropic.com/v1", ApiStyle.ANTHROPIC, true),
    GEMINI("Google Gemini", "https://generativelanguage.googleapis.com/v1beta", ApiStyle.GEMINI, true),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1", ApiStyle.OPENAI_COMPATIBLE, true),
    GROQ("Groq", "https://api.groq.com/openai/v1", ApiStyle.OPENAI_COMPATIBLE, true),
    OLLAMA("Ollama (your own server)", "http://localhost:11434/v1", ApiStyle.OPENAI_COMPATIBLE, false),
    CUSTOM("Custom endpoint", "", ApiStyle.OPENAI_COMPATIBLE, false),
}
