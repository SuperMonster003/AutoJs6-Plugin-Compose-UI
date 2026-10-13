package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.codegen.TsxWriter
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.DesignerParseError
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse.TsxReader
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.storage.ProjectStore
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Writes the generated TSX snapshot sources (roadmap P9.2); runs only from `.python/generate_tsx_snapshots.py`,
 * which sets TSX_SNAPSHOTS_RECORD to the snapshot root and parses the written files with the TypeScript Engine
 * parser Worker afterwards. `gallery/` and `examples/` hold the TSX form of the JavaScript scripts, `templates/` the
 * new-project template; `canonical/` holds the TSX writer's form of every hand-written source (`host/`, `cases/`)
 * whose current tree maps.
 */
class TsxSnapshotRecorder {

    @Test
    fun record() {
        val target = System.getenv("TSX_SNAPSHOTS_RECORD")
        assumeTrue("set by .python/generate_tsx_snapshots.py", !target.isNullOrBlank())
        val snapshots = TsxSnapshots(Paths.get(target!!))
        GENERATED.forEach { clear(snapshots.root.resolve(it)) }
        TsxSnapshots.gallery().forEach { (component, script) -> write(snapshots.root.resolve("gallery/$component.tsx"), TsxSnapshots.tsxForm(script)) }
        TsxSnapshots.examples().forEach { (name, script) -> write(snapshots.root.resolve("examples/" + name.removeSuffix(".js") + ".tsx"), TsxSnapshots.tsxForm(script)) }
        write(snapshots.root.resolve(TEMPLATE), ProjectStore.TSX_TEMPLATE)
        HAND_WRITTEN.flatMap { snapshots.sources(it) }.filter { snapshots.isCurrent(it) }.forEach { name ->
            val document = try {
                TsxReader().read(snapshots.source(name), snapshots.tree(name))
            } catch (error: DesignerParseError) {
                return@forEach
            }
            write(snapshots.root.resolve("canonical/$name"), TsxWriter.write(document))
        }
    }

    private fun clear(directory: Path) {
        if (!Files.isDirectory(directory)) return
        Files.walk(directory).use { stream -> stream.sorted(Comparator.reverseOrder()).forEach { Files.delete(it) } }
    }

    private fun write(path: Path, text: String) {
        Files.createDirectories(path.parent)
        Files.writeString(path, text)
    }

    companion object {
        val GENERATED = listOf("gallery", "examples", "canonical", "templates")
        const val TEMPLATE = "templates/new-project.tsx"
        val HAND_WRITTEN = listOf("host", "cases")
    }
}
