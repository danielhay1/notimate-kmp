package com.hayduck.notemate.featurekeys

enum class FeatureKeyType(val jsonName: String) {
    BOOLEAN("boolean"),
    INT("int"),
    FLOAT("float"),
    DOUBLE("double"),
    STRING("string"),
}

/** Typed, finite feature configuration values; these must never contain secrets. */
sealed interface FeatureKeyValue {
    val type: FeatureKeyType
    val text: String

    data class BooleanValue(val value: Boolean) : FeatureKeyValue {
        override val type = FeatureKeyType.BOOLEAN
        override val text get() = value.toString()
    }

    data class IntValue(val value: Int) : FeatureKeyValue {
        override val type = FeatureKeyType.INT
        override val text get() = value.toString()
    }

    data class FloatValue(val value: Float) : FeatureKeyValue {
        init { require(value.isFinite()) }
        override val type = FeatureKeyType.FLOAT
        override val text get() = value.toString()
    }

    data class DoubleValue(val value: Double) : FeatureKeyValue {
        init { require(value.isFinite()) }
        override val type = FeatureKeyType.DOUBLE
        override val text get() = value.toString()
    }

    data class StringValue(val value: String) : FeatureKeyValue {
        override val type = FeatureKeyType.STRING
        override val text get() = value
    }
}

/** Returns null for invalid input, numeric overflow, NaN, or infinity. */
fun parseFeatureKeyValue(type: FeatureKeyType, text: String): FeatureKeyValue? = when (type) {
    FeatureKeyType.BOOLEAN -> text.toBooleanStrictOrNull()?.let(FeatureKeyValue::BooleanValue)
    FeatureKeyType.INT -> text.trim().toIntOrNull()?.let(FeatureKeyValue::IntValue)
    FeatureKeyType.FLOAT -> text.trim().toFloatOrNull()?.takeIf { it.isFinite() }
        ?.let(FeatureKeyValue::FloatValue)
    FeatureKeyType.DOUBLE -> text.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
        ?.let(FeatureKeyValue::DoubleValue)
    FeatureKeyType.STRING -> FeatureKeyValue.StringValue(text)
}

data class FeatureKey(val key: String, val defaultValue: FeatureKeyValue, val value: FeatureKeyValue) {
    val type: FeatureKeyType get() = defaultValue.type
}

object FeatureKeys {
    const val NOTIFICATION_TESTING_ENABLED = "notificationTestingEnabled"
}
