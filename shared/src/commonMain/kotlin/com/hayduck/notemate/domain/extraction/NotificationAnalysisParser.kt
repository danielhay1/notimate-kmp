package com.hayduck.notemate.domain.extraction

import com.hayduck.notemate.domain.notification.NotificationClassification
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull

/** Strict notification.v1 envelope with action decoding separate from classification. */
@OptIn(ExperimentalSerializationApi::class)
class NotificationAnalysisParser {
    private val json = Json { exceptionsWithDebugInfo = false }

    fun parse(output: String): ExtractionResult {
        if (output.length > MAXIMUM_MODEL_OUTPUT_LENGTH) return invalid()
        return try {
            json.parseToJsonElement(output)
            val analysis = json.decodeFromString(NotificationAnalysisDecoder, output)
            if (analysis.isSchemaValid()) ExtractionResult.Analysis(analysis) else invalid()
        } catch (_: UnsupportedSuggestedAction) {
            ExtractionResult.ReviewNeeded(ExtractionReviewReason.UNSUPPORTED_ACTION)
        } catch (_: SerializationException) {
            invalid()
        } catch (_: IllegalArgumentException) {
            invalid()
        }
    }

    private fun invalid(): ExtractionResult =
        ExtractionResult.ReviewNeeded(ExtractionReviewReason.INVALID_OUTPUT)
}

private class UnsupportedSuggestedAction : SerializationException("Unsupported suggested action.")

@OptIn(ExperimentalSerializationApi::class)
private object NotificationAnalysisDecoder : DeserializationStrategy<NotificationAnalysis> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("NotificationAnalysis") {
        element<String>("schema")
        element<String>("classification")
        element("confidence", JsonPrimitive.serializer().descriptor)
        element("isSensitive", JsonPrimitive.serializer().descriptor)
        element("suggestedAction", SuggestedActionDecoder.descriptor.nullable)
    }

    override fun deserialize(decoder: Decoder): NotificationAnalysis {
        val composite = decoder.beginStructure(descriptor)
        val seen = mutableSetOf<Int>()
        var schema = ""
        var classificationName = ""
        var confidence: Double? = null
        var isSensitive: Boolean? = null
        var action: SuggestedNotificationAction? = null
        while (true) {
            val index = composite.decodeElementIndex(descriptor)
            if (index == CompositeDecoder.DECODE_DONE) break
            if (index !in 0..4 || !seen.add(index)) throw SerializationException("Invalid fields.")
            when (index) {
                0 -> schema = composite.decodeStringElement(descriptor, index)
                1 -> classificationName = composite.decodeStringElement(descriptor, index)
                2 -> {
                    val value = composite.decodeSerializableElement(
                        descriptor, index, JsonPrimitive.serializer(),
                    )
                    if (!value.isString) confidence = value.doubleOrNull
                }
                3 -> {
                    val value = composite.decodeSerializableElement(
                        descriptor, index, JsonPrimitive.serializer(),
                    )
                    if (!value.isString) isSensitive = value.booleanOrNull
                }
                4 -> action = composite.decodeNullableSerializableElement(
                    descriptor, index, SuggestedActionDecoder,
                )
            }
        }
        composite.endStructure(descriptor)
        val classification = NotificationClassification.entries.find {
            it.name == classificationName
        }
        if (seen.size != 5 || schema != "notification.v1" || classification == null ||
            confidence == null || isSensitive == null
        ) throw SerializationException("Invalid analysis.")
        return NotificationAnalysis(classification, action, confidence, isSensitive)
    }
}

// Each discriminator has a typed payload decoder; unsupported future actions never reach policy.
@OptIn(ExperimentalSerializationApi::class)
private object SuggestedActionDecoder : DeserializationStrategy<SuggestedNotificationAction> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SuggestedAction") {
        element<String>("type")
        element("calendar", CalendarOutputDecoder.descriptor)
    }

    override fun deserialize(decoder: Decoder): SuggestedNotificationAction {
        val composite = decoder.beginStructure(descriptor)
        val seen = mutableSetOf<Int>()
        var type = ""
        var calendar: CalendarOutput? = null
        while (true) {
            val index = composite.decodeElementIndex(descriptor)
            if (index == CompositeDecoder.DECODE_DONE) break
            if (index !in 0..1 || !seen.add(index)) throw SerializationException("Invalid action.")
            when (index) {
                0 -> type = composite.decodeStringElement(descriptor, index)
                1 -> calendar = composite.decodeSerializableElement(
                    descriptor, index, CalendarOutputDecoder,
                )
            }
        }
        composite.endStructure(descriptor)
        if (type.isNotEmpty() && type != "calendar_event") throw UnsupportedSuggestedAction()
        if (seen.size != 2) throw SerializationException("Missing action fields.")
        val extraction = calendar?.toExtraction()
            ?: throw SerializationException("Invalid calendar.")
        return SuggestedNotificationAction.CalendarEvent(extraction)
    }
}
