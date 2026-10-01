package com.hayduck.notemate.domain.extraction

import com.hayduck.notemate.domain.proposal.CalendarField
import com.hayduck.notemate.domain.proposal.CalendarFields
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.json.Json

/** Strict calendar.v1 JSON boundary; errors expose only enums, never input or parser exceptions. */
@OptIn(ExperimentalSerializationApi::class)
class CalendarExtractionParser {
    private val json = Json { exceptionsWithDebugInfo = false }

    fun parse(output: String): ExtractionResult {
        if (output.length > MAXIMUM_MODEL_OUTPUT_LENGTH) return invalid()
        return try {
            val decoded = json.decodeFromString(CalendarOutputDecoder, output)
            if (decoded.schema != "calendar.v1") return invalid()
            val date = decoded.date?.let(::parseCalendarDate)
            val time = decoded.time?.let(::parseCalendarTime)
            if ((decoded.date != null && date == null) || (decoded.time != null && time == null)) {
                return invalid()
            }
            val ambiguity = decoded.ambiguousFields.map { name ->
                CalendarField.entries.find { it.name == name } ?: return invalid()
            }
            if (ambiguity.size != ambiguity.distinct().size) return invalid()
            val extraction = CalendarExtraction(
                CalendarFields(decoded.title, date, time, decoded.location), ambiguity.toSet(),
            )
            if (extraction.isSchemaValid()) ExtractionResult.Calendar(extraction) else invalid()
        } catch (_: SerializationException) {
            invalid()
        } catch (_: IllegalArgumentException) {
            invalid()
        }
    }

    private fun invalid(): ExtractionResult =
        ExtractionResult.ReviewNeeded(ExtractionReviewReason.INVALID_OUTPUT)
}

private class CalendarOutput(
    val schema: String,
    val title: String?,
    val date: String?,
    val time: String?,
    val location: String?,
    val ambiguousFields: List<String>,
)

// Streaming decoding preserves duplicate keys so they cannot silently replace earlier fields.
@OptIn(ExperimentalSerializationApi::class)
private object CalendarOutputDecoder : DeserializationStrategy<CalendarOutput> {
    private val nullableString = String.serializer().nullable
    private val strings = ListSerializer(String.serializer())
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CalendarOutput") {
        element<String>("schema")
        element("title", nullableString.descriptor)
        element("date", nullableString.descriptor)
        element("time", nullableString.descriptor)
        element("location", nullableString.descriptor)
        element("ambiguousFields", strings.descriptor)
    }

    override fun deserialize(decoder: Decoder): CalendarOutput {
        val composite = decoder.beginStructure(descriptor)
        val seen = mutableSetOf<Int>()
        var schema = ""
        val fields = arrayOfNulls<String>(4)
        var ambiguity = emptyList<String>()
        while (true) {
            val index = composite.decodeElementIndex(descriptor)
            if (index == CompositeDecoder.DECODE_DONE) break
            if (index !in 0..5 || !seen.add(index)) throw SerializationException("Invalid fields.")
            when (index) {
                0 -> schema = composite.decodeStringElement(descriptor, index)
                in 1..4 -> fields[index - 1] =
                    composite.decodeSerializableElement(descriptor, index, nullableString)
                5 -> ambiguity = composite.decodeSerializableElement(descriptor, index, strings)
            }
        }
        composite.endStructure(descriptor)
        if (seen.size != 6) throw SerializationException("Missing fields.")
        return CalendarOutput(schema, fields[0], fields[1], fields[2], fields[3], ambiguity)
    }
}
