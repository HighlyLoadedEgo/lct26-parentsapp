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

/** An unsupported optional explanation must not hide otherwise valid server assessments. */
object OptionalSkillExplanationSerializer : KSerializer<SkillExplanationDto?> {
    private val delegate = SkillExplanationDto.serializer().nullable
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): SkillExplanationDto? {
        if (decoder !is JsonDecoder) return decoder.decodeSerializableValue(delegate)?.takeIf { it.isSupported() }
        val element = decoder.decodeJsonElement()
        val version = (element as? JsonObject)?.get("version") as? JsonPrimitive ?: return null
        if (version.isString || version.intOrNull != 1) return null
        return try {
            Json(decoder.json) { ignoreUnknownKeys = true }
                .decodeFromJsonElement(delegate, element)?.takeIf { it.isSupported() }
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    override fun serialize(encoder: Encoder, value: SkillExplanationDto?) {
        encoder.encodeSerializableValue(delegate, value?.takeIf { it.isSupported() })
    }
}
