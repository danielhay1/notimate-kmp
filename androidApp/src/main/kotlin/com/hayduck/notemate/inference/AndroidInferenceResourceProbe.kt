package com.hayduck.notemate.inference

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.hayduck.notemate.domain.extraction.DeviceResourceState
import com.hayduck.notemate.domain.extraction.ExtractionDeviceConditions

/** Admission thresholds must be qualified for the chosen model and target device. */
class InferenceResourcePolicy(
    val minimumAvailableMemoryBytes: Long,
    val minimumBatteryPercent: Int,
    val maximumThermalStatus: Int,
) {
    init {
        require(minimumAvailableMemoryBytes > 0 && minimumBatteryPercent in 1..100 &&
            maximumThermalStatus in 0..6) { "Invalid resource policy." }
    }

    internal fun evaluate(readings: InferenceResourceReadings): ExtractionDeviceConditions =
        ExtractionDeviceConditions(
            memory = available(readings.availableMemoryBytes?.let { bytes ->
                readings.lowMemory?.let { bytes >= minimumAvailableMemoryBytes && !it }
            }),
            battery = available(readings.batteryPercent?.let { it >= minimumBatteryPercent }),
            thermal = available(readings.thermalStatus?.let { it <= maximumThermalStatus }),
        )

    private fun available(value: Boolean?): DeviceResourceState = when (value) {
        true -> DeviceResourceState.AVAILABLE
        false -> DeviceResourceState.CONSTRAINED
        null -> DeviceResourceState.UNKNOWN
    }
}

internal data class InferenceResourceReadings(
    val availableMemoryBytes: Long?,
    val lowMemory: Boolean?,
    val batteryPercent: Int?,
    val thermalStatus: Int?,
)

/** Unknown signals, including thermal status before Android 10, deny heavy inference. */
class AndroidInferenceResourceProbe(context: Context, private val policy: InferenceResourcePolicy) {
    private val applicationContext = context.applicationContext

    fun read(): ExtractionDeviceConditions {
        val memory = runCatching {
            val manager = applicationContext.getSystemService(ActivityManager::class.java)
                ?: return@runCatching null
            ActivityManager.MemoryInfo().also(manager::getMemoryInfo)
        }.getOrNull()
        val batteryPercent = runCatching {
            val battery = applicationContext.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ) ?: return@runCatching null
            val level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (scale <= 0 || level !in 0..scale) null
            else (level.toLong() * 100 / scale).toInt()
        }.getOrNull()
        val thermal = runCatching {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) null
            else applicationContext.getSystemService(PowerManager::class.java)
                ?.currentThermalStatus?.takeIf { it in 0..6 }
        }.getOrNull()
        return policy.evaluate(
            InferenceResourceReadings(
                memory?.availMem?.takeIf { it >= 0 }, memory?.lowMemory, batteryPercent, thermal,
            )
        )
    }
}
