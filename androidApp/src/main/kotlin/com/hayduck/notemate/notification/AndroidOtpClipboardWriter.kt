package com.hayduck.notemate.notification

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import com.hayduck.notemate.domain.extraction.OtpClipboardCopy
import com.hayduck.notemate.domain.results.LocalResultsRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Copies a policy-validated [OtpClipboardCopy] to the Android clipboard as sensitive content and
 * records a content-free audit event only after the platform write succeeds.
 */
internal class AndroidOtpClipboardWriter(
    private val context: Context,
    private val resultsRepository: LocalResultsRepository,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val writeClip: ((ClipboardManager, ClipData) -> Unit)? = null,
) {
    suspend fun copyAndRecord(copy: OtpClipboardCopy, activityId: String): Boolean {
        require(activityId.isNotBlank()) { "Activity identifier must not be blank." }
        val copied = try {
            withContext(mainDispatcher) {
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                    ?: return@withContext false
                val clip = ClipData.newPlainText(OTP_CLIP_LABEL, copy.code).apply {
                    description.extras = PersistableBundle().apply {
                        putBoolean(EXTRA_IS_SENSITIVE, true)
                    }
                }
                if (writeClip != null) {
                    writeClip(clipboard, clip)
                } else {
                    clipboard.setPrimaryClip(clip)
                }
                true
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            false
        }
        if (!copied) return false
        resultsRepository.appendActivity(copy.activityRecord(activityId))
        return true
    }

    internal companion object {
        const val OTP_CLIP_LABEL = "NotiMate verification code"
        const val EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE"
    }
}
