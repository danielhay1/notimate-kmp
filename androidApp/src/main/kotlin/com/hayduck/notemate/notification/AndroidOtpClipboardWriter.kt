package com.hayduck.notemate.notification

import android.app.KeyguardManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import android.os.PowerManager
import com.hayduck.notemate.domain.extraction.OtpClipboardCopy
import com.hayduck.notemate.domain.results.LocalResultsRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Writes fresh, policy-validated OTPs while unlocked; codes never enter the audit store.
 * Caller cancellation cannot interrupt an already begun write/audit sequence. Audit failures
 * propagate after copying and must not cause a clipboard retry. Platform writes have no receipt;
 * success means the write call returned, not that Android confirmed a clipboard change.
 * Callers must recheck current source, Profile, pause, access, and opt-in before invoking this adapter.
 */
internal class AndroidOtpClipboardWriter(
    private val context: Context,
    private val resultsRepository: LocalResultsRepository,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val nowEpochMilliseconds: () -> Long = System::currentTimeMillis,
    private val isDeviceUnlocked: () -> Boolean = {
        context.getSystemService(KeyguardManager::class.java)?.isDeviceLocked == false &&
            context.getSystemService(PowerManager::class.java)?.isInteractive == true
    },
    private val writeClip: ((ClipboardManager, ClipData) -> Unit)? = null,
) {
    suspend fun copyAndRecord(copy: OtpClipboardCopy, activityId: String): Boolean {
        require(activityId.isNotBlank()) { "Activity identifier must not be blank." }
        currentCoroutineContext().ensureActive()
        return withContext(mainDispatcher) {
            currentCoroutineContext().ensureActive()
            val now = nowEpochMilliseconds()
            if (copy.postedAtEpochMilliseconds < 0 || now < copy.postedAtEpochMilliseconds ||
                now - copy.postedAtEpochMilliseconds > MAXIMUM_AGE_MILLISECONDS ||
                !isDeviceUnlocked()
            ) return@withContext false
            val clipboard = context.getSystemService(ClipboardManager::class.java)
                ?: return@withContext false
            val clip = ClipData.newPlainText(OTP_CLIP_LABEL, copy.code).apply {
                description.extras = PersistableBundle().apply {
                    putBoolean(EXTRA_IS_SENSITIVE, true)
                }
            }
            withContext(NonCancellable) {
                val copied = try {
                    if (writeClip != null) writeClip(clipboard, clip)
                    else clipboard.setPrimaryClip(clip)
                    true
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    false
                }
                if (copied) {
                    withTimeout(AUDIT_TIMEOUT_MILLISECONDS) {
                        resultsRepository.appendActivity(copy.activityRecord(activityId))
                    }
                }
                copied
            }
        }
    }

    internal companion object {
        const val OTP_CLIP_LABEL = "NotiMate verification code"
        const val EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE"
        const val MAXIMUM_AGE_MILLISECONDS = 60_000L
        const val AUDIT_TIMEOUT_MILLISECONDS = 5_000L
    }
}
