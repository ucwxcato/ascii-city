package com.asciicity.content

import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue

internal object JsonContentReader {
    fun parseObject(text: String, source: String): JsonObjectView {
        val root = try {
            JsonReader().parse(text)
        } catch (error: Exception) {
            throw ContentValidationException("$source:$: malformed JSON: ${error.message}", error)
        }
        return JsonObjectView(root, source, "$")
    }
}

internal class JsonObjectView(
    private val value: JsonValue,
    private val source: String,
    private val path: String,
) {
    val location: String
        get() = "$source:$path"

    private val fields: Map<String, JsonValue>

    init {
        if (!value.isObject) {
            throw ContentValidationException("$source:$path: expected a JSON object")
        }
        val parsed = linkedMapOf<String, JsonValue>()
        var child = value.child
        while (child != null) {
            val name = child.name ?: throw ContentValidationException("$source:$path: object field has no name")
            if (name in parsed) {
                throw ContentValidationException("$source:$path.$name: duplicate field")
            }
            parsed[name] = child
            child = child.next
        }
        fields = parsed
    }

    fun rejectUnknown(allowed: Set<String>) {
        val unknown = fields.keys.firstOrNull { it !in allowed } ?: return
        throw ContentValidationException("$source:$path.$unknown: unknown field")
    }

    fun requiredString(name: String): String {
        val child = required(name)
        if (!child.isString) {
            throw ContentValidationException("$source:$path.$name: expected a string")
        }
        return child.asString()
    }

    fun optionalString(name: String): String? {
        val child = fields[name] ?: return null
        if (!child.isString) {
            throw ContentValidationException("$source:$path.$name: expected a string")
        }
        return child.asString()
    }

    fun requiredBoolean(name: String): Boolean {
        val child = required(name)
        if (!child.isBoolean) {
            throw ContentValidationException("$source:$path.$name: expected a boolean")
        }
        return child.asBoolean()
    }

    fun optionalBoolean(name: String, default: Boolean): Boolean {
        val child = fields[name] ?: return default
        if (!child.isBoolean) {
            throw ContentValidationException("$source:$path.$name: expected a boolean")
        }
        return child.asBoolean()
    }

    fun requiredInt(name: String): Int {
        val number = requiredNumber(name)
        if (number % 1.0 != 0.0 || number < Int.MIN_VALUE || number > Int.MAX_VALUE) {
            throw ContentValidationException("$source:$path.$name: expected an integer")
        }
        return number.toInt()
    }

    fun requiredDouble(name: String): Double = requiredNumber(name)

    fun optionalDouble(name: String, default: Double): Double = if (name in fields) requiredNumber(name) else default

    fun requiredObject(name: String): JsonObjectView = JsonObjectView(required(name), source, "$path.$name")

    fun optionalObject(name: String): JsonObjectView? {
        val child = fields[name] ?: return null
        return JsonObjectView(child, source, "$path.$name")
    }

    fun requiredArray(name: String): List<JsonValueView> {
        val array = required(name)
        if (!array.isArray) {
            throw ContentValidationException("$source:$path.$name: expected an array")
        }
        val values = mutableListOf<JsonValueView>()
        var child = array.child
        var index = 0
        while (child != null) {
            values += JsonValueView(child, source, "$path.$name[$index]")
            child = child.next
            index += 1
        }
        return values
    }

    fun optionalArray(name: String): List<JsonValueView> = if (name in fields) requiredArray(name) else emptyList()

    fun optionalStringArray(name: String): Set<String> {
        if (name !in fields) {
            return emptySet()
        }
        val values = requiredArray(name)
        val result = linkedSetOf<String>()
        for (value in values) {
            val text = value.asString()
            if (!result.add(text)) {
                throw ContentValidationException("${value.location}: duplicate value '$text'")
            }
        }
        return result
    }

    fun optionalStringList(name: String): List<String> {
        if (name !in fields) {
            return emptyList()
        }
        val values = requiredArray(name)
        val seen = mutableSetOf<String>()
        return values.map { value ->
            val text = value.asString()
            if (!seen.add(text)) {
                throw ContentValidationException("${value.location}: duplicate value '$text'")
            }
            text
        }
    }

    private fun required(name: String): JsonValue = fields[name]
        ?: throw ContentValidationException("$source:$path.$name: required field is missing")

    private fun requiredNumber(name: String): Double {
        val child = required(name)
        if (!child.isNumber) {
            throw ContentValidationException("$source:$path.$name: expected a number")
        }
        val number = child.asDouble()
        if (!number.isFinite()) {
            throw ContentValidationException("$source:$path.$name: number must be finite")
        }
        return number
    }
}

internal class JsonValueView(
    private val value: JsonValue,
    private val source: String,
    private val path: String,
) {
    val location: String
        get() = "$source:$path"

    fun asObject(): JsonObjectView = JsonObjectView(value, source, path)

    fun asString(): String {
        if (!value.isString) {
            throw ContentValidationException("$source:$path: expected a string")
        }
        return value.asString()
    }
}
