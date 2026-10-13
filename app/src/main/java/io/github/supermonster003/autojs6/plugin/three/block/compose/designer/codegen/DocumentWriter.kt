package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.DesignDocument
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.Dialect

/** Writes a document in the language it was read from (roadmap P9.2). */
object DocumentWriter {
    fun write(document: DesignDocument): String = when (document.dialect) {
        Dialect.JS -> ScriptWriter.write(document)
        Dialect.TSX -> TsxWriter.write(document)
    }
}
