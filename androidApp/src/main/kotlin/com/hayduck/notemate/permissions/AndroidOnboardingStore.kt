package com.hayduck.notemate.permissions

import android.content.Context
import android.util.AtomicFile
import com.hayduck.notemate.today.OnboardingPreferences
import com.hayduck.notemate.today.OnboardingStep
import com.hayduck.notemate.today.OnboardingStore
import java.io.DataInputStream
import java.io.File
import java.io.FileNotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Stores only onboarding flags outside platform backup; malformed existing data fails closed. */
internal class AndroidOnboardingStore(context: Context) : OnboardingStore {
    private val file = AtomicFile(File(context.noBackupFilesDir, "onboarding-state"))
    private val mutex = Mutex()

    override suspend fun load(): OnboardingPreferences = withContext(Dispatchers.IO) {
        mutex.withLock { readPreferences() }
    }

    override suspend fun save(preferences: OnboardingPreferences) = withContext(Dispatchers.IO) {
        mutex.withLock {
            readPreferences()
            val output = file.startWrite()
            try {
                output.write(byteArrayOf(
                    VERSION.toByte(),
                    preferences.step.storageByte(),
                    preferences.hasHadAccess.flagByte(),
                    preferences.hasRequestedAlerts.flagByte(),
                ))
                file.finishWrite(output)
                check(readPreferences() == preferences) { "Onboarding changes were not saved." }
            } catch (failure: Throwable) {
                file.failWrite(output)
                throw failure
            }
        }
    }

    private fun readPreferences(): OnboardingPreferences {
        val input = try {
            file.openRead()
        } catch (missing: FileNotFoundException) {
            if (file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()) throw missing
            return OnboardingPreferences()
        }
        return DataInputStream(input).use {
            check(it.readUnsignedByte() == VERSION) { "Onboarding version is unsupported." }
            val preferences = OnboardingPreferences(
                step = it.readStep(),
                hasHadAccess = it.readFlag(),
                hasRequestedAlerts = it.readFlag(),
            )
            check(it.read() == -1) { "Onboarding data is invalid." }
            preferences
        }
    }

    private fun DataInputStream.readFlag(): Boolean {
        val value = readUnsignedByte()
        check(value == 0 || value == 1) { "Onboarding flag is invalid." }
        return value == 1
    }

    private fun Boolean.flagByte(): Byte = if (this) 1 else 0

    private fun DataInputStream.readStep(): OnboardingStep = when (readUnsignedByte()) {
        0 -> OnboardingStep.WELCOME
        1 -> OnboardingStep.PRIVACY
        2 -> OnboardingStep.ACCESS
        3 -> OnboardingStep.ALERTS
        4 -> OnboardingStep.COMPLETE
        else -> error("Onboarding step is invalid.")
    }

    private fun OnboardingStep.storageByte(): Byte = when (this) {
        OnboardingStep.WELCOME -> 0
        OnboardingStep.PRIVACY -> 1
        OnboardingStep.ACCESS -> 2
        OnboardingStep.ALERTS -> 3
        OnboardingStep.COMPLETE -> 4
    }

    companion object {
        private const val VERSION = 1
    }
}
