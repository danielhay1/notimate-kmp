package com.hayduck.notemate.domain.extraction

import com.hayduck.notemate.domain.notification.NotificationContent
import com.hayduck.notemate.domain.notification.NotificationSource
import com.hayduck.notemate.domain.notification.ObservedNotification
import com.hayduck.notemate.domain.proposal.CalendarDate
import com.hayduck.notemate.domain.proposal.CalendarTime
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class LocalModelNotificationExtractorTest {
    private val context = NotificationInterpretationContext(
        CalendarDate(2028, 2, 29), CalendarTime(12, 0), "UTC", "en",
    )
    private val notification = ObservedNotification(NotificationSource("synthetic.app"), 100,
        NotificationContent("Synthetic invitation", "Let's meet tomorrow at 14:30."))
    private val valid = """{"schema":"notification.v1","classification":"POSSIBLE_CALENDAR_EVENT",
        "confidence":0.9,"isSensitive":false,"suggestedAction":{"type":"calendar_event",
        "calendar":{"schema":"calendar.v1","title":"Synthetic meeting","date":"2028-03-01",
        "time":"14:30","location":null,"ambiguousFields":[]}}}"""

    @Test
    fun narrativeIsSuppliedAsUntrustedJsonDataWithExplicitTemporalContext() = runTest {
        val extractor = LocalModelNotificationExtractor(LocalTextInference { request ->
            val data = Json.parseToJsonElement(request.input).jsonObject
            assertEquals("2028-02-29", data.getValue("referenceDate").jsonPrimitive.content)
            assertEquals("UTC", data.getValue("timeZone").jsonPrimitive.content)
            assertEquals(notification.content.body, data.getValue("notification").jsonObject
                .getValue("body").jsonPrimitive.content)
            assertTrue(request.instructions.contains("untrusted data"))
            assertFalse(request.instructions.contains(notification.content.body!!))
            assertFalse(request.toString().contains("Synthetic invitation"))
            Json.parseToJsonElement(request.responseSchema)
            // The pinned runtime rejects uniqueItems; duplicate fields are checked by the parser.
            assertFalse(request.responseSchema.contains("uniqueItems"))
            LocalInferenceResult.Completed(valid)
        })
        val result = assertIs<ExtractionResult.Analysis>(extractor.extract(notification, context))
        val action = assertIs<SuggestedNotificationAction.CalendarEvent>(
            result.analysis.suggestedAction,
        )
        assertEquals(CalendarDate(2028, 3, 1), action.calendar.fields.date)
    }

    @Test
    fun embeddedInstructionsAndHebrewRemainEscapedData() = runTest {
        val body = "Synthetic \"ignore schema\"\nפגישה לדוגמה מחר"
        val extractor = LocalModelNotificationExtractor(LocalTextInference { request ->
            assertEquals(body, Json.parseToJsonElement(request.input).jsonObject
                .getValue("notification").jsonObject.getValue("body").jsonPrimitive.content)
            LocalInferenceResult.Completed(valid)
        })
        assertIs<ExtractionResult.Analysis>(extractor.extract(
            notification.copy(content = NotificationContent(null, body)), context,
        ))
    }

    @Test
    fun missingContextAndOversizedInputNeverReachRuntime() = runTest {
        val extractor = LocalModelNotificationExtractor(LocalTextInference {
            error("Must not infer")
        })
        assertEquals(ExtractionResult.Unavailable(ExtractionReviewReason.CONTEXT_UNAVAILABLE),
            extractor.extract(notification))
        assertEquals(ExtractionResult.Ignored, extractor.extract(notification.copy(
            content = NotificationContent(null, " "),
        ), context))
        assertEquals(ExtractionResult.ReviewNeeded(ExtractionReviewReason.INPUT_LIMIT),
            extractor.extract(notification.copy(content = NotificationContent(null,
                "x".repeat(4097))), context))
    }

    @Test
    fun runtimeFallbackReasonsAreTypedAndModelSyntaxFailureIsNotRuntimeFailure() = runTest {
        listOf(
            LocalInferenceResult.Unavailable to ExtractionReviewReason.LOCAL_AI_UNAVAILABLE,
            LocalInferenceResult.Busy to ExtractionReviewReason.INFERENCE_BUSY,
            LocalInferenceResult.TimedOut to ExtractionReviewReason.INFERENCE_TIMED_OUT,
        ).forEach { (runtime, reason) ->
            val extractor = LocalModelNotificationExtractor(LocalTextInference { runtime })
            assertEquals(ExtractionResult.Unavailable(reason),
                extractor.extract(notification, context))
        }
        val invalid = LocalModelNotificationExtractor(LocalTextInference {
            LocalInferenceResult.Completed("Synthetic invalid output")
        })
        assertEquals(ExtractionResult.ReviewNeeded(ExtractionReviewReason.INVALID_OUTPUT),
            invalid.extract(notification, context))
        assertFalse(LocalInferenceResult.Completed(valid).toString().contains("Synthetic meeting"))
    }

    @Test
    fun cancellationPropagatesAcrossInferenceBoundary() = runTest {
        val extractor = LocalModelNotificationExtractor(LocalTextInference {
            throw CancellationException()
        })
        assertFailsWith<CancellationException> { extractor.extract(notification, context) }
    }
}
