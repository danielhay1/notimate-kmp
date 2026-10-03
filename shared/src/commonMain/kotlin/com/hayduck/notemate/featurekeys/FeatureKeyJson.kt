package com.hayduck.notemate.featurekeys

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal object FeatureKeyJson {
    fun defaults(source: String): List<FeatureKey> {
        val rows = Json.parseToJsonElement(source) as? JsonArray
            ?: error("Feature defaults must be an array")
        val keys = rows.map { row ->
            val fields = row as? JsonObject ?: error("Invalid feature definition")
            require(fields.keys == setOf("key", "type", "default"))
            val key = (fields["key"] as? JsonPrimitive)?.takeIf { it.isString }?.content
                ?: error("Missing feature key")
            require(key.matches(Regex("[A-Za-z][A-Za-z0-9]*")))
            val type = type(fields["type"]) ?: error("Invalid feature type")
            val value = value(type, fields["default"]) ?: error("Invalid feature default")
            FeatureKey(key, value, value)
        }
        require(keys.map { it.key }.distinct().size == keys.size)
        return keys
    }

    fun overrides(source: String?): JsonObject? {
        if (source == null) return null
        return runCatching { Json.parseToJsonElement(source) as? JsonObject }.getOrNull()
    }

    fun overrideValue(entry: JsonElement?, expectedType: FeatureKeyType): FeatureKeyValue? {
        val fields = entry as? JsonObject ?: return null
        if (fields.keys != setOf("type", "value") || type(fields["type"]) != expectedType) {
            return null
        }
        return value(expectedType, fields["value"])
    }

    fun encodeOverrides(keys: List<FeatureKey>): String = buildJsonObject {
        keys.filter { it.value != it.defaultValue }.forEach { feature ->
            put(feature.key, buildJsonObject {
                put("type", feature.type.jsonName)
                put("value", primitive(feature.value))
            })
        }
    }.toString()

    private fun type(element: JsonElement?): FeatureKeyType? {
        val name = (element as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return null
        return FeatureKeyType.entries.firstOrNull { it.jsonName == name }
    }

    private fun value(type: FeatureKeyType, element: JsonElement?): FeatureKeyValue? {
        val primitive = element as? JsonPrimitive ?: return null
        if (primitive == JsonNull || primitive.isString != (type == FeatureKeyType.STRING)) {
            return null
        }
        return parseFeatureKeyValue(type, primitive.content)
    }

    private fun primitive(value: FeatureKeyValue): JsonPrimitive = when (value) {
        is FeatureKeyValue.BooleanValue -> JsonPrimitive(value.value)
        is FeatureKeyValue.IntValue -> JsonPrimitive(value.value)
        is FeatureKeyValue.FloatValue -> JsonPrimitive(value.value)
        is FeatureKeyValue.DoubleValue -> JsonPrimitive(value.value)
        is FeatureKeyValue.StringValue -> JsonPrimitive(value.value)
    }
}
