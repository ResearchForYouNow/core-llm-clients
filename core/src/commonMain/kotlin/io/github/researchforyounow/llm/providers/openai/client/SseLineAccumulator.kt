package io.github.researchforyounow.llm.providers.openai.client

internal class SseLineAccumulator {
    private val dataLines = mutableListOf<String>()

    fun onLine(
        line: String,
    ): String? {
        if (line.isEmpty()) {
            if (dataLines.isEmpty()) return null
            val payload = dataLines.joinToString("\n")
            dataLines.clear()
            return payload
        }
        if (line.startsWith("data:")) {
            dataLines += line.removePrefix("data:").trimStart()
        }
        return null
    }
}
