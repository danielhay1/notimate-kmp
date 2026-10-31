package com.hayduck.notemate.domain.extraction

import com.hayduck.notemate.domain.notification.NotificationClassification
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class NotificationAnalysisParserTest {
    private val parser = NotificationAnalysisParser()
    private val calendar = """{"schema":"calendar.v1","title":"Synthetic demo",
        "date":"2028-03-01","time":"14:30","location":null,"ambiguousFields":[]}"""
    private val valid = """{"schema":"notification.v1","classification":"POSSIBLE_CALENDAR_EVENT",
        "confidence":0.9,"isSensitive":false,
        "suggestedAction":{"type":"calendar_event","calendar":$calendar}}"""
    private val validOtp = """{"schema":"notification.v1","classification":"OTP_CODE",
        "confidence":0.95,"isSensitive":false,
        "suggestedAction":{"type":"otp_code","otp":{"schema":"otp.v1","code":"482910"}}}"""

    @Test
    fun classificationAndTypedActionAreDecodedSeparately() {
        val analysis = assertIs<ExtractionResult.Analysis>(parser.parse(valid)).analysis
        assertEquals(NotificationClassification.POSSIBLE_CALENDAR_EVENT, analysis.classification)
        val action = assertIs<SuggestedNotificationAction.CalendarEvent>(analysis.suggestedAction)
        assertEquals("Synthetic demo", action.calendar.fields.title)
        assertEquals(0.9, analysis.confidence)
        assertFalse(analysis.toString().contains("Synthetic demo"))
        assertFalse(action.toString().contains("Synthetic demo"))

        val otpAnalysis = assertIs<ExtractionResult.Analysis>(parser.parse(validOtp)).analysis
        assertEquals(NotificationClassification.OTP_CODE, otpAnalysis.classification)
        val otpAction = assertIs<SuggestedNotificationAction.OtpCode>(otpAnalysis.suggestedAction)
        assertEquals(OtpExtraction("482910"), otpAction.otp)
        assertFalse(otpAnalysis.toString().contains("482910"))
        assertFalse(otpAction.toString().contains("482910"))
        assertFalse(otpAction.otp.toString().contains("482910"))
    }

    @Test
    fun promotionClassificationHasNoExecutableAction() {
        val analysis = assertIs<ExtractionResult.Analysis>(parser.parse(noAction("PROMOTIONAL")))
            .analysis
        assertEquals(NotificationClassification.PROMOTIONAL, analysis.classification)
        assertEquals(null, analysis.suggestedAction)
    }

    @Test
    fun unsupportedActionsNeverBecomeTypedSuggestions() {
        assertEquals(review(ExtractionReviewReason.UNSUPPORTED_ACTION),
            parser.parse(valid.replace("calendar_event", "filter")))
    }

    @Test
    fun unknownMissingAndDuplicateKeysRejectAtEveryObjectLevel() {
        listOf(
            valid.replace("\"confidence\":0.9,", ""),
            valid.replace("\"confidence\":0.9", "\"confidence\":0.9,\"confidence\":1"),
            valid.replace("\"confidence\":0.9", "\"extra\":0,\"confidence\":0.9"),
            valid.replace("\"type\":\"calendar_event\"",
                "\"type\":\"calendar_event\",\"type\":\"calendar_event\""),
            valid.replace("\"location\":null", "\"location\":null,\"location\":null"),
            valid.replace("\"location\":null", "\"extra\":null,\"location\":null"),
            valid.replace("\"confidence\":0.9", "\"confid\\u0065nce\":0.9,\"confidence\":1"),
        ).forEach(::assertInvalid)
    }

    @Test
    fun syntaxPrimitiveTypesAndOutputBoundsFailClosed() {
        listOf(
            valid.replace(",\"classification\"", "\"classification\""),
            valid.replace(",\"classification\"", " \"classification\""),
            valid.replace("0.9", "\"0.9\""), valid.replace("false", "\"false\""),
            valid.replace("0.9", "null"), valid.replace("false", "null"),
            valid.replace("0.9", "1.1"), valid.replace("0.9", "-0.1"),
            valid + " trailing", "[$valid]", "x".repeat(8193),
            valid.replace("notification.v1", "notification.v2"),
            valid.replace("POSSIBLE_CALENDAR_EVENT", "UNSUPPORTED_CATEGORY"),
        ).forEach(::assertInvalid)
    }

    @Test
    fun actionClassificationMismatchAndInvalidCalendarAreRejected() {
        listOf(
            noAction("POSSIBLE_CALENDAR_EVENT"),
            noAction("OTP_CODE"),
            valid.replace("POSSIBLE_CALENDAR_EVENT", "PROMOTIONAL"),
            valid.replace("POSSIBLE_CALENDAR_EVENT", "IGNORED"),
            valid.replace("POSSIBLE_CALENDAR_EVENT", "OTP_CODE"),
            validOtp.replace("OTP_CODE", "POSSIBLE_CALENDAR_EVENT"),
            validOtp.replace("OTP_CODE", "PROMOTIONAL"),
            validOtp.replace("otp.v1", "otp.v2"),
            validOtp.replace("\"482910\"", "482910"),
            validOtp.replace("\"482910\"", "null"),
            validOtp.replace("482910", "123"),
            validOtp.replace("482910", "123456789"),
            validOtp.replace("482910", "ABCD"),
            validOtp.replace("482910", "12 34"),
            validOtp.replace("482910", "12-34"),
            validOtp.replace("\"code\":\"482910\"", "\"code\":\"482910\",\"code\":\"482910\""),
            validOtp.replace("\"code\":\"482910\"", "\"extra\":\"x\",\"code\":\"482910\""),
            valid.replace("2028-03-01", "2028-02-30"),
            valid.replace("14:30", "24:00"),
            valid.replace("Synthetic demo", "Synthetic\\u0000demo"),
            valid.replace("\"ambiguousFields\":[]", "\"ambiguousFields\":[\"TIME\",\"TIME\"]"),
        ).forEach(::assertInvalid)
    }

    private fun noAction(classification: String): String =
        """{"schema":"notification.v1","classification":"$classification",
            "confidence":0.9,"isSensitive":false,"suggestedAction":null}"""

    private fun assertInvalid(output: String) {
        assertEquals(review(ExtractionReviewReason.INVALID_OUTPUT), parser.parse(output))
    }

    private fun review(reason: ExtractionReviewReason): ExtractionResult =
        ExtractionResult.ReviewNeeded(reason)
}
