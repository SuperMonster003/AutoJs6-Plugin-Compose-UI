package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignDocument
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Dialect
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiDocument

/** Syntax checks the inspector runs before it applies an edited handler. */
internal interface SyntaxCheck {
    /** The first syntax error of a handler with [params] and [body], positioned in the body; null when it parses. */
    suspend fun checkHandler(params: List<String>, body: String): DesignerParseError?
}

/** What reading a document gave. */
internal sealed class ReadResult {
    data class Parsed(val document: DesignDocument) : ReadResult()
    /** A `"ui";` mode script (roadmap P10): XML literals edited losslessly and previewed by the host. */
    data class ParsedUi(val document: UiDocument) : ReadResult()
    /** The text parses but the designer cannot map it, or it has a syntax error. */
    data class Rejected(val error: DesignerParseError) : ReadResult()
    /**
     * TSX only (roadmap D42): the TypeScript Engine parser is missing, too old or failing, so the text can only
     * be shown read-only. [reason] is null for a parser error ([detail] carries its code and message).
     */
    data class Unavailable(val reason: TsxParserClient.Reason?, val installedVersion: String?, val detail: String?) : ReadResult()
}

/**
 * Reads documents of one [Dialect]: JavaScript with the Rhino [ScriptReader] on the calling thread, TSX through
 * the TypeScript Engine parser service ([TsxParserClient]) and [TsxReader]. Owns the parser binding.
 */
internal class DocumentReader(
    val dialect: Dialect,
    private val client: TsxParserClient?,
    /** The message shown when a handler cannot be checked because the parser is unavailable, given the cause. */
    private val checkUnavailable: (String) -> String = { it },
    /** The text of a layout file a `"ui"` script refers to by a literal path (roadmap P11.3), or null when unreadable. */
    private val layoutFile: (String) -> String? = { null },
) : SyntaxCheck, AutoCloseable {

    init {
        require(dialect == Dialect.JS || client != null) { "TSX documents need a parser client" }
    }

    /** The result right away for JavaScript; null for TSX, which must be read with [read]. */
    fun readNow(text: String): ReadResult? {
        if (dialect != Dialect.JS) return null
        return try {
            // A script that starts with the "ui" directive and lays out XML is a ui-mode project (roadmap P10).
            if (UiScriptReader.looksLikeUiScript(text)) ReadResult.ParsedUi(UiScriptReader().read(text, layoutFile))
            else ReadResult.Parsed(ScriptReader().read(text))
        } catch (error: DesignerParseError) {
            ReadResult.Rejected(error)
        }
    }

    suspend fun read(text: String): ReadResult {
        readNow(text)?.let { return it }
        val source = TsxReader.normalize(text)
        return when (val outcome = client!!.parse(TsxReader.FILE_NAME, source)) {
            is TsxParserClient.Outcome.Parsed -> try {
                ReadResult.Parsed(TsxReader().read(source, outcome.tree))
            } catch (error: DesignerParseError) {
                ReadResult.Rejected(error)
            }
            is TsxParserClient.Outcome.Failed -> ReadResult.Unavailable(null, client.installedVersion(), outcome.code.removePrefix(ERROR_PREFIX) + ": " + outcome.message)
            is TsxParserClient.Outcome.Unavailable -> ReadResult.Unavailable(outcome.reason, outcome.installedVersion, null)
        }
    }

    override suspend fun checkHandler(params: List<String>, body: String): DesignerParseError? {
        if (dialect == Dialect.JS) return ScriptReader.checkHandler(params, body)
        val probe = TsxReader.handlerProbe(params, body)
        return when (val outcome = client!!.parse(TsxReader.FILE_NAME, probe)) {
            is TsxParserClient.Outcome.Parsed -> TsxReader.handlerError(probe, outcome.tree)
            is TsxParserClient.Outcome.Failed -> DesignerParseError(1, 1, checkUnavailable(outcome.code.removePrefix(ERROR_PREFIX)))
            is TsxParserClient.Outcome.Unavailable -> DesignerParseError(1, 1, checkUnavailable(outcome.reason.name))
        }
    }

    override fun close() {
        client?.close()
    }

    private companion object {
        const val ERROR_PREFIX = "ERR_AUTOJS6_TYPESCRIPT_PARSER_"
    }
}
