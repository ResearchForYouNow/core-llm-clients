package io.github.researchforyounow.llm.providers.openai.request

import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.utils.io.ByteReadChannel

internal object AudioMultipartBuilder {

    fun buildTranscriptionContent(
        request: AudioTranscriptionRequest,
        stream: Boolean,
    ): MultiPartFormDataContent = MultiPartFormDataContent(buildTranscriptionParts(request, stream))

    fun buildTranslationContent(
        request: AudioTranslationRequest,
    ): MultiPartFormDataContent = MultiPartFormDataContent(buildTranslationParts(request))

    fun buildTranscriptionParts(
        request: AudioTranscriptionRequest,
        stream: Boolean,
    ): List<PartData> {
        val parts = mutableListOf<PartData>()
        parts += buildFilePart(request.file)

        parts += formData {
            append("model", request.model)
            append("response_format", request.responseFormat.apiValue)
            request.prompt?.let { append("prompt", it) }
            request.language?.let { append("language", it) }
            request.temperature?.let { append("temperature", it.toString()) }
            request.chunkingStrategy?.let { append("chunking_strategy", it) }
            request.timestampGranularities?.forEach { append("timestamp_granularities[]", it) }
            request.include?.forEach { append("include[]", it) }
            request.knownSpeakerNames?.forEach { append("known_speaker_names[]", it) }
            request.knownSpeakerReferences?.forEach { append("known_speaker_references[]", it) }
            if (stream) append("stream", "true")
        }

        return parts
    }

    fun buildTranslationParts(
        request: AudioTranslationRequest,
    ): List<PartData> {
        val parts = mutableListOf<PartData>()
        parts += buildFilePart(request.file)

        parts += formData {
            append("model", request.model)
            append("response_format", request.responseFormat.apiValue)
            request.prompt?.let { append("prompt", it) }
            request.temperature?.let { append("temperature", it.toString()) }
        }

        return parts
    }

    private fun buildFilePart(
        file: AudioFile,
    ): PartData.FileItem {
        val headers = Headers.build {
            append(
                HttpHeaders.ContentDisposition,
                "form-data; name=\"file\"; filename=\"${file.fileName}\"",
            )
            append(HttpHeaders.ContentType, file.contentType ?: "application/octet-stream")
        }
        return PartData.FileItem(
            provider = { ByteReadChannel(file.bytes) },
            dispose = {},
            partHeaders = headers,
        )
    }
}
