package ru.nksk.parentsapp.core.report.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/** Malformed optional counts never invalidate an otherwise valid assessment. */
object OptionalObservationCountsSerializer : KSerializer<ObservationCountsDto?> {
    private val delegate = ObservationCountsDto.serializer().nullable
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): ObservationCountsDto? {
        if (decoder !is JsonDecoder) return decoder.decodeSerializableValue(delegate)?.takeIf { it.isValid() }
        val element = decoder.decodeJsonElement() as? JsonObject ?: return null
        val fields = listOf("observedEpisodes", "completedEpisodes", "supportedEpisodes",
            "difficultyEpisodes", "neutralEpisodes", "pendingEpisodes", "incompleteEpisodes",
            "supportedWithoutGameHints", "assistedEpisodes")
        if (fields.any { field ->
            val value = element[field] as? JsonPrimitive
            value == null || value.isString || value.intOrNull?.let { it >= 0 } != true
        }) return null
        return try {
            Json(decoder.json) { ignoreUnknownKeys = true }
                .decodeFromJsonElement(delegate, element)?.takeIf { it.isValid() }
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    override fun serialize(encoder: Encoder, value: ObservationCountsDto?) {
        encoder.encodeSerializableValue(delegate, value?.takeIf { it.isValid() })
    }
}
