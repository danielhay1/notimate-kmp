package com.hayduck.notemate.domain.extraction

import com.hayduck.notemate.domain.proposal.CalendarDate
import com.hayduck.notemate.domain.proposal.CalendarField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class CalendarExtractionParserTest {
    private val parser = CalendarExtractionParser()
    private val valid = """{"schema":"calendar.v1","title":"Synthetic demo", "date":"2028-02-29",
        "time":"14:30","location":null,"ambiguousFields":[]}"""

    @Test
    fun explicitSchemaProducesTypedAdvisoryFields() {
        val result = assertIs<ExtractionResult.Calendar>(parser.parse(valid))
        assertEquals(CalendarDate(2028, 2, 29), result.extraction.fields.date)
        assertFalse(result.toString().contains("Synthetic demo"))
    }

    @Test
    fun nullFieldsAndDeclaredAmbiguityRemainReviewable() {
        val result = assertIs<ExtractionResult.Calendar>(parser.parse(valid
            .replace("\"2028-02-29\"", "null")
            .replace("\"ambiguousFields\":[]", "\"ambiguousFields\":[\"DATE\"]")))
        assertEquals(null, result.extraction.fields.date)
        assertEquals(setOf(CalendarField.DATE), result.extraction.ambiguousFields)
    }

    @Test
    fun missingUnknownAndRepeatedKeysFailClosed() {
        listOf(
            valid.replace("\"location\":null,", ""),
            valid.replace("\"location\":null", "\"extra\":null,\"location\":null"),
            valid.replace("\"location\":null", "\"location\":null,\"location\":\"Synthetic\""),
            valid.replace("calendar.v1", "calendar.v2"),
        ).forEach(::assertInvalid)
    }

    @Test
    fun malformedWrongTypesInvalidFieldsAndUnknownEnumsAreRejected() {
        listOf(
            "```json\n$valid\n```", valid + " trailing", valid.replace("null", "false"),
            valid.replace("\"Synthetic demo\"", "123"),
            valid.replace("2028-02-29", "2027-02-29"), valid.replace("14:30", "24:00"),
            valid.replace("14:30", "14:30Z"), valid.replace("Synthetic demo", ""),
            valid.replace("\"ambiguousFields\":[]", "\"ambiguousFields\":[\"UNKNOWN\"]"),
            valid.replace("\"ambiguousFields\":[]", "\"ambiguousFields\":[\"TIME\",\"TIME\"]"),
            valid.replace("null", "{}"), valid.replace("null", "[]"),
            valid.replace("Synthetic demo", "Synthetic\\u0000demo"),
        ).forEach(::assertInvalid)
    }

    @Test
    fun boundedErrorsExposeNoInputOrExceptionDetail() {
        assertInvalid(valid.replace("Synthetic demo", "a".repeat(257)))
        assertInvalid("a".repeat(8193))
        val result = parser.parse("Synthetic invalid model output")
        assertIs<ExtractionResult.ReviewNeeded>(result)
        assertFalse(result.toString().contains("Synthetic invalid model output"))
    }

    private fun assertInvalid(output: String) {
        assertEquals(ExtractionResult.ReviewNeeded(ExtractionReviewReason.INVALID_OUTPUT),
            parser.parse(output))
    }
}
