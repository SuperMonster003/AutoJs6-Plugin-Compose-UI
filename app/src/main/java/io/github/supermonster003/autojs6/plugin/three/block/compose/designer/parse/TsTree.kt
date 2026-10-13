package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse

/**
 * A TypeScript syntax tree as the parser contract v1 delivers it (schema revision 1): node kinds, UTF-16
 * positions into the parsed source, child nodes under TypeScript's own property names, and the
 * syntactic diagnostics. Unknown members are ignored, as the contract asks of clients.
 */
class TsTree(
    val schemaRevision: Int,
    val compilerVersion: String,
    val fileName: String,
    val languageVariant: String,
    val diagnostics: List<TsDiagnostic>,
    val root: TsNode,
)

class TsDiagnostic(val start: Int, val length: Int, val code: Int, val category: String, val message: String)

class TsNode(
    val kind: String,
    val start: Int,
    val end: Int,
    val text: String?,
    val declarationKind: String?,
    val operator: String?,
    val triviaOnly: Boolean,
    private val fields: Map<String, Any>,
) {
    fun node(name: String): TsNode? = fields[name] as? TsNode

    @Suppress("UNCHECKED_CAST")
    fun nodes(name: String): List<TsNode> = fields[name] as? List<TsNode> ?: emptyList()

    fun has(name: String): Boolean = name in fields

    override fun toString(): String = "$kind[$start, $end)"
}

class TsTreeFormatException(message: String) : RuntimeException(message)

object TsTreeJson {

    fun parse(json: String): TsTree {
        val value = Reader(json).document()
        val top = value as? Map<*, *> ?: throw TsTreeFormatException("The tree is not a JSON object")
        val revision = (top["schemaRevision"] as? Number)?.toInt() ?: throw TsTreeFormatException("schemaRevision is missing")
        if (revision != SCHEMA_REVISION) throw TsTreeFormatException("Tree schema revision $revision is not supported")
        val diagnostics = (top["diagnostics"] as? List<*>).orEmpty().map { item ->
            val diagnostic = item as? Map<*, *> ?: throw TsTreeFormatException("A diagnostic is not an object")
            TsDiagnostic(
                start = (diagnostic["start"] as? Number)?.toInt() ?: 0,
                length = (diagnostic["length"] as? Number)?.toInt() ?: 0,
                code = (diagnostic["code"] as? Number)?.toInt() ?: 0,
                category = diagnostic["category"] as? String ?: "error",
                message = diagnostic["message"] as? String ?: "",
            )
        }
        return TsTree(
            schemaRevision = revision,
            compilerVersion = top["compilerVersion"] as? String ?: "",
            fileName = top["fileName"] as? String ?: "",
            languageVariant = top["languageVariant"] as? String ?: "",
            diagnostics = diagnostics,
            root = node(top["root"] ?: throw TsTreeFormatException("root is missing")),
        )
    }

    private val SCALARS = setOf("kind", "start", "end", "text", "declarationKind", "operator", "triviaOnly")

    private fun node(value: Any): TsNode {
        val members = value as? Map<*, *> ?: throw TsTreeFormatException("A node is not an object")
        val fields = LinkedHashMap<String, Any>()
        members.forEach { (key, member) ->
            if (key !is String || key in SCALARS || member == null) return@forEach
            when (member) {
                is Map<*, *> -> if ("kind" in member) fields[key] = node(member)
                is List<*> -> if (member.all { it is Map<*, *> && "kind" in it }) fields[key] = member.map { node(it!!) }
            }
        }
        return TsNode(
            kind = members["kind"] as? String ?: throw TsTreeFormatException("A node has no kind"),
            start = (members["start"] as? Number)?.toInt() ?: throw TsTreeFormatException("A node has no start"),
            end = (members["end"] as? Number)?.toInt() ?: throw TsTreeFormatException("A node has no end"),
            text = members["text"] as? String,
            declarationKind = members["declarationKind"] as? String,
            operator = members["operator"] as? String,
            triviaOnly = members["triviaOnly"] == true,
            fields = fields,
        )
    }

    private const val SCHEMA_REVISION = 1

    /** A small strict JSON reader: objects keep member order, numbers become Long or Double. */
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

        private fun fail(message: String): Nothing = throw TsTreeFormatException("$message at $index")
    }
}
