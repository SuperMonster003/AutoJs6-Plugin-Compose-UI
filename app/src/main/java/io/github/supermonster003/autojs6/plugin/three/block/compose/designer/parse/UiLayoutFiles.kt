package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen.UiScriptWriter
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.AttrPart
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.FragmentUsage
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiDocument
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiSegment
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlFragment
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.XmlNode
import org.mozilla.javascript.CompilerEnvirons
import org.mozilla.javascript.Context
import org.mozilla.javascript.Parser
import org.mozilla.javascript.ast.AstRoot
import org.mozilla.javascript.ast.ErrorCollector
import org.mozilla.javascript.ast.FunctionCall
import org.mozilla.javascript.ast.Name
import org.mozilla.javascript.ast.PropertyGet
import org.mozilla.javascript.ast.XmlExpression
import org.mozilla.javascript.ast.XmlLiteral
import org.mozilla.javascript.ast.XmlString

/**
 * Layout files made in the designer (roadmap P11.3 / D66, D67): moving an XML literal of the script into a layout
 * file, and adding a new layout file with one empty root container. Both rewrite the script text and read it back,
 * so the result is an ordinary [UiDocument] (with fresh element ids) that the editor applies as one undoable step;
 * nothing is written until the document is saved.
 */
object UiLayoutFiles {

    /** Why a fragment cannot be moved into a file. */
    enum class Refusal {
        /** The fragment is a layout file already. */
        NOT_A_LITERAL,
        /** The literal is not written directly in `ui.layout(...)` or as the first argument of `ui.inflate(...)`. */
        NOT_INLINE,
        /** The literal holds E4X `{...}` expressions, which a file cannot evaluate. */
        EXPRESSIONS,
    }

    /** Why a path cannot name a new layout file. */
    enum class PathProblem {
        /** Not a relative path of a `.xml` file inside the script's folder. */
        INVALID,
        /** The script refers to that file already. */
        TAKEN,
    }

    const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"

    /** The new file of [addFile]: one empty vertical container, AutoJs6 XML or native Android XML. */
    fun blankFile(native: Boolean): String = if (native) {
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<LinearLayout xmlns:android=\"$ANDROID_NAMESPACE\"\n" +
            "    android:layout_width=\"match_parent\"\n" +
            "    android:layout_height=\"wrap_content\"\n" +
            "    android:orientation=\"vertical\">\n" +
            "</LinearLayout>\n"
    } else {
        "<vertical>\n</vertical>\n"
    }

    /** Why the fragment at [index] (of [UiDocument.fragments]) cannot be moved into a file, or null when it can. */
    fun refusalOf(document: UiDocument, index: Int): Refusal? {
        val literal = literalAt(document, index) ?: return Refusal.NOT_A_LITERAL
        if (hasExpressions(literal)) return Refusal.EXPRESSIONS
        return if (rewrite(document, index, "layout.xml") == null) Refusal.NOT_INLINE else null
    }

    /** Why [path] cannot name a new layout file of [document], or null when it can. */
    fun pathProblem(document: UiDocument, path: String): PathProblem? = when {
        !isValidPath(path) -> PathProblem.INVALID
        document.files.any { it.file?.path == path } || path in document.missingFiles -> PathProblem.TAKEN
        else -> null
    }

    /** A relative path of a `.xml` file that stays inside the script's folder (`layout.xml`, `res/layout/main.xml`). */
    fun isValidPath(path: String): Boolean {
        if (path.length <= ".xml".length || !path.endsWith(".xml", ignoreCase = true)) return false
        if (path.startsWith("/") || ':' in path || '\\' in path) return false
        return path.split('/').all { part -> part.isNotEmpty() && part != "." && part != ".." && part.none { it < ' ' || it in "\"'`*?<>|" } }
    }

    /** [base] when [document] does not refer to it yet, else `-2`, `-3`, ... before its `.xml`. */
    fun freePath(document: UiDocument, base: String): String {
        if (pathProblem(document, base) == null) return base
        val stem = base.removeSuffix(".xml")
        var counter = 2
        while (pathProblem(document, "$stem-$counter.xml") != null) counter++
        return "$stem-$counter.xml"
    }

    /** The path offered when the fragment at [index] is moved into a file. */
    fun suggestedPath(document: UiDocument, index: Int): String {
        val fragment = document.fragments[index]
        val layout = fragment.usage == FragmentUsage.LAYOUT
        val base = when {
            document.isNative(fragment) -> if (layout) "res/layout/activity_main.xml" else "res/layout/item.xml"
            else -> if (layout) "layout.xml" else "item.xml"
        }
        return freePath(document, base)
    }

    /** The path offered for a new file of [addFile]. */
    fun suggestedNewPath(document: UiDocument, native: Boolean): String = freePath(document, if (native) "res/layout/item.xml" else "item.xml")

    /**
     * Whether a new file of [document] is native Android XML: as the script's `ui.useAndroidLayout(true / false)` says,
     * or null when the script leaves it automatic and the user picks.
     */
    fun fixedDialect(document: UiDocument): Boolean? = document.androidLayout

    /**
     * Moves the literal fragment at [index] into the layout file [path]: `ui.layout(<...>)` becomes
     * `ui.layoutFile("path")`, `ui.inflate(<...>, ...)` becomes `ui.inflate(files.read("path"), ...)`, and the file
     * holds the literal as it is (its common indentation removed).
     */
    fun extract(document: UiDocument, index: Int, path: String, reader: UiScriptReader = UiScriptReader()): UiDocument {
        require(pathProblem(document, path) == null) { "Unusable layout file path: $path" }
        val refusal = refusalOf(document, index)
        require(refusal == null) { "The fragment cannot be moved into a file: $refusal" }
        val script = rewrite(document, index, path)!!
        val text = dedent(UiScriptWriter.write(literalAt(document, index)!!.root)) + "\n"
        val files = UiScriptWriter.writeFiles(document) + (path to text)
        return reader.read(script) { files[it] }
    }

    /** Adds the layout file [path] holding [blankFile] and a `let <name>View = ui.inflate(files.read("path"));` at the end of the script. */
    fun addFile(document: UiDocument, path: String, native: Boolean, reader: UiScriptReader = UiScriptReader()): UiDocument {
        require(pathProblem(document, path) == null) { "Unusable layout file path: $path" }
        val script = UiScriptWriter.write(document)
        val name = freeName(script, variableNameOf(path))
        val statement = "let $name = ui.inflate(files.read(${quoted(script, path)}));"
        val files = UiScriptWriter.writeFiles(document) + (path to blankFile(native))
        return reader.read(script.trimEnd() + "\n\n" + statement + "\n") { files[it] }
    }

    private fun literalAt(document: UiDocument, index: Int) =
        document.segments.filterIsInstance<UiSegment.Layout>().getOrNull(index)?.fragment

    private fun hasExpressions(fragment: XmlFragment): Boolean =
        fragment.root.descendants().any { element ->
            element.children.any { it is XmlNode.Expr } || element.attrs.any { attr -> attr.parts?.any { it is AttrPart.Expr } == true }
        }

    /** The script with the literal at [index] replaced by a reference to [path], or null when it is not written inline. */
    private fun rewrite(document: UiDocument, index: Int, path: String): String? {
        val script = UiScriptWriter.write(document)
        val literals = ArrayList<XmlLiteral>()
        val root = try {
            parse(script)
        } catch (error: RuntimeException) {
            return null
        }
        root.visitAll { node ->
            if (node is XmlLiteral) literals += node
            true
        }
        literals.sortBy { start(it) }
        val literal = literals.getOrNull(index) ?: return null
        val call = literal.parent as? FunctionCall ?: return null
        val reference = quoted(script, path)
        return when {
            isUiCall(call, "layout") && call.arguments.size == 1 && call.arguments[0] === literal ->
                script.substring(0, call.absolutePosition) + "ui.layoutFile($reference)" + script.substring(call.absolutePosition + call.length)
            isUiCall(call, "inflate") && call.arguments.firstOrNull() === literal ->
                script.substring(0, start(literal)) + "files.read($reference)" + script.substring(end(literal))
            else -> null
        }
    }

    private fun start(literal: XmlLiteral): Int = literal.fragments.first().absolutePosition

    private fun end(literal: XmlLiteral): Int = literal.fragments.maxOf { fragment ->
        when (fragment) {
            is XmlString -> fragment.absolutePosition + fragment.xml.length
            is XmlExpression -> fragment.absolutePosition + fragment.length
            else -> fragment.absolutePosition + fragment.length
        }
    }

    private fun isUiCall(call: FunctionCall, method: String): Boolean {
        val target = call.target as? PropertyGet ?: return false
        return (target.target as? Name)?.identifier == "ui" && target.property.identifier == method
    }

    private fun parse(script: String): AstRoot {
        val environment = CompilerEnvirons().apply {
            languageVersion = Context.VERSION_ES6
            isIdeMode = true
            setRecordingComments(false)
            setRecordingLocalJsDocComments(false)
        }
        val collector = ErrorCollector()
        environment.errorReporter = collector
        return Parser(environment, collector).parse(script, "designer.js", 1)
    }

    /** A string literal of [path] in the quote style of the script's `"ui"` directive. */
    private fun quoted(script: String, path: String): String {
        val quote = script.trimStart().firstOrNull()?.takeIf { it == '\'' || it == '"' } ?: '"'
        return "$quote$path$quote"
    }

    /** Removes the indentation every line after the first shares (the literal's first line starts at its `<`). */
    internal fun dedent(text: String): String {
        val lines = text.split('\n')
        if (lines.size < 2) return text
        fun leading(line: String) = line.takeWhile { it == ' ' || it == '\t' }.length
        val indent = lines.drop(1).filter { it.isNotBlank() }.minOfOrNull(::leading) ?: 0
        return (listOf(lines[0]) + lines.drop(1).map { it.drop(minOf(indent, leading(it))) }).joinToString("\n")
    }

    /** `res/layout/item_row.xml` -> `itemRowView`. */
    private fun variableNameOf(path: String): String {
        val words = path.substringAfterLast('/').removeSuffix(".xml").split(Regex("[^A-Za-z0-9]+")).filter { it.isNotEmpty() }
        val base = words.mapIndexed { i, word -> if (i == 0) word.replaceFirstChar { it.lowercaseChar() } else word.replaceFirstChar { it.uppercaseChar() } }.joinToString("")
        return (if (base.isEmpty() || base[0].isDigit()) "layout" else base) + "View"
    }

    private fun freeName(script: String, name: String): String {
        fun used(candidate: String) = Regex("(?<![A-Za-z0-9_$])${Regex.escape(candidate)}(?![A-Za-z0-9_$])").containsMatchIn(script)
        if (!used(name)) return name
        var counter = 2
        while (used("$name$counter")) counter++
        return "$name$counter"
    }
}
