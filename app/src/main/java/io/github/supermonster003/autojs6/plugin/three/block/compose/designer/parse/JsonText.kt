package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse

/** A JSON text the designer cannot read; [offset] is the UTF-16 position of the problem. */
class JsonTextException(message: String, val offset: Int) : RuntimeException("$message at $offset")

/**
 * A small strict JSON reader shared by the TSX syntax trees and the preview sandbox bridge: objects become
 * ordered maps, arrays lists, numbers Long when integral and Double otherwise, `null` Kotlin null. [write]
 * is the inverse for the values the sandbox bridge sends.
 */
internal object JsonText {

    fun parse(text: String): Any? = Reader(text).document()

    /** Maps with string keys, lists, strings, numbers, booleans and null; non-finite numbers become null. */
    fun write(value: Any?): String = StringBuilder().also { write(it, value) }.toString()

    /** [text] as a JSON string literal, which is also a JavaScript string literal (U+2028 / U+2029 are escaped). */
    fun quote(text: String): String = StringBuilder().also { quote(it, text) }.toString()

    private fun write(out: StringBuilder, value: Any?) {
        when (value) {
            null -> out.append("null")
            is String -> quote(out, value)
            is Boolean -> out.append(value)
            is Int, is Long, is Short, is Byte -> out.append(value)
            is Number -> {
                val number = value.toDouble()
                when {
                    !number.isFinite() -> out.append("null")
                    number == Math.floor(number) && Math.abs(number) < 1e15 -> out.append(number.toLong())
                    else -> out.append(number)
                }
            }
            is Map<*, *> -> {
                out.append('{')
                var first = true
                value.forEach { (key, item) ->
                    if (!first) out.append(',')
                    first = false
                    quote(out, key as String)
                    out.append(':')
                    write(out, item)
                }
                out.append('}')
            }
            is Iterable<*> -> {
                out.append('[')
                value.forEachIndexed { index, item -> if (index > 0) out.append(','); write(out, item) }
                out.append(']')
            }
            else -> throw IllegalArgumentException("Not a JSON value: ${value::class.java.name}")
        }
    }

    private fun quote(out: StringBuilder, text: String) {
        out.append('"')
        for (char in text) {
            when {
                char == '"' -> out.append("\\\"")
                char == '\\' -> out.append("\\\\")
                char == '\n' -> out.append("\\n")
                char == '\r' -> out.append("\\r")
                char == '\t' -> out.append("\\t")
                char.code < 0x20 || char == ' ' || char == ' ' -> out.append("\\u").append(String.format("%04x", char.code))
                else -> out.append(char)
            }
        }
        out.append('"')
    }

    private class Reader(private val text: String) {
        private var index = 0

        fun document(): Any? {
            val value = value()
            skipWhitespace()
            if (index != text.length) fail("Unexpected trailing content")
            return value
        }

        private fun value(): Any? {
            skipWhitespace()
            if (index >= text.length) fail("Unexpected end of JSON")
            return when (text[index]) {
                '{' -> obj()
                '[' -> array()
                '"' -> string()
                't' -> literal("true", true)
                'f' -> literal("false", false)
                'n' -> literal("null", null)
                else -> number()
            }
        }

        private fun obj(): Map<String, Any?> {
            val members = LinkedHashMap<String, Any?>()
            index++
            skipWhitespace()
            if (peek() == '}') { index++; return members }
            while (true) {
                skipWhitespace()
                if (peek() != '"') fail("Expected a member name")
                val name = string()
                skipWhitespace()
                if (peek() != ':') fail("Expected ':'")
                index++
                members[name] = value()
                skipWhitespace()
                when (peek()) {
                    ',' -> index++
                    '}' -> { index++; return members }
                    else -> fail("Expected ',' or '}'")
                }
            }
        }

        private fun array(): List<Any?> {
            val items = ArrayList<Any?>()
            index++
            skipWhitespace()
            if (peek() == ']') { index++; return items }
            while (true) {
                items += value()
                skipWhitespace()
                when (peek()) {
                    ',' -> index++
                    ']' -> { index++; return items }
                    else -> fail("Expected ',' or ']'")
                }
            }
        }

        private fun string(): String {
            val out = StringBuilder()
            index++
            while (true) {
                if (index >= text.length) fail("Unterminated string")
                val char = text[index++]
                when {
                    char == '"' -> return out.toString()
                    char == '\\' -> {
                        if (index >= text.length) fail("Unterminated escape")
                        when (val escape = text[index++]) {
                            '"' -> out.append('"')
                            '\\' -> out.append('\\')
                            '/' -> out.append('/')
                            'b' -> out.append('\b')
                            'f' -> out.append('\u000C')
                            'n' -> out.append('\n')
                            'r' -> out.append('\r')
                            't' -> out.append('\t')
                            'u' -> {
                                if (index + 4 > text.length) fail("Truncated unicode escape")
                                out.append(text.substring(index, index + 4).toIntOrNull(16)?.toChar() ?: fail("Bad unicode escape"))
                                index += 4
                            }
                            else -> fail("Bad escape \\$escape")
                        }
                    }
                    char.code < 0x20 -> fail("Control character in string")
                    else -> out.append(char)
                }
            }
        }

        private fun number(): Number {
            val start = index
            if (peek() == '-') index++
            while (index < text.length && (text[index].isDigit() || text[index] in ".eE+-")) index++
            val token = text.substring(start, index)
            if (token.isEmpty() || token == "-") fail("Unexpected character")
            return token.toLongOrNull() ?: token.toDoubleOrNull() ?: fail("Bad number $token")
        }

        private fun literal(word: String, value: Any?): Any? {
            if (!text.startsWith(word, index)) fail("Unexpected token")
            index += word.length
            return value
        }

        private fun peek(): Char = if (index < text.length) text[index] else '\u0000'

        private fun skipWhitespace() {
            while (index < text.length && text[index] in " \t\r\n") index++
        }

        private fun fail(message: String): Nothing = throw JsonTextException(message, index)
    }
}
