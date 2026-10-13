package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse

/** Text helpers shared by the JavaScript and the TSX readers. */
internal object SourceText {

    /** [text] without leading and trailing blank lines and without the indentation common to its lines. */
    fun dedent(text: String): String {
        val lines = text.split('\n').dropWhile { it.isBlank() }.dropLastWhile { it.isBlank() }
        if (lines.isEmpty()) return ""
        val indent = lines.filter { it.isNotBlank() }.minOf { line -> line.takeWhile { it == ' ' || it == '\t' }.length }
        return lines.joinToString("\n") { line -> if (line.isBlank()) "" else line.substring(minOf(indent, line.takeWhile { it == ' ' || it == '\t' }.length)).trimEnd() }
    }

    /** 1-based line of [offset] in [source]. */
    fun lineOf(source: String, offset: Int): Int = 1 + source.substring(0, offset.coerceIn(0, source.length)).count { it == '\n' }

    /** 1-based column of [offset] in [source]. */
    fun columnOf(source: String, offset: Int): Int {
        val bounded = offset.coerceIn(0, source.length)
        return bounded - source.lastIndexOf('\n', bounded - 1)
    }
}
