package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import org.autojs.plugin.ui.preview.UiPreviewContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The packaged `ui-preview-api.aar` is the v2 the designer's client is written against (roadmap P10.6): the frozen
 * v1 base the handshake checks first, the v2 identity announced through the preview info, the script-run keys
 * and exit reasons the client and the console rely on. A stale or foreign AAR in `libs/` fails here before a
 * device does.
 */
class UiPreviewContractGuardTest {
    @Test
    fun `the contract is v2 over the frozen v1 base`() {
        assertEquals(2, UiPreviewContract.VERSION)
        assertEquals(1, UiPreviewContract.BASE_VERSION)
        // The hash of contract v1 as shipped with 1.6.0: a v1 host answers it from getContractHash().
        assertEquals("680f41b5459648ff07bd1d3d9684c5279d9398b78f61c0307862f50f8dcac0b6", UiPreviewContract.HASH_V1)
        assertTrue(Regex("[0-9a-f]{64}").matches(UiPreviewContract.HASH))
        assertTrue(UiPreviewContract.HASH != UiPreviewContract.HASH_V1)
        // v2 keeps every v1 transaction in its place and appends after it (the ordinal order is frozen).
        val v1Service = UiPreviewContract.HASH_INPUT_V1.substringAfter("v1|").substringBefore("|callback:")
        val v1Callbacks = UiPreviewContract.HASH_INPUT_V1.substringAfter("|callback:").substringBefore("|bundle:")
        assertTrue(UiPreviewContract.HASH_INPUT.startsWith("autojs6.ui.preview.v2|$v1Service|runScript("))
        assertTrue(UiPreviewContract.HASH_INPUT.contains("|callback:$v1Callbacks|onConsole("))
        assertTrue(UiPreviewContract.HASH_INPUT.endsWith("|bounds:json-inline|script:text-inline"))
        assertTrue(UiPreviewContract.HASH_INPUT.contains("runScript(String,Bundle)->Bundle|stopScript(String)->Bundle"))
        assertTrue(UiPreviewContract.HASH_INPUT.contains("onConsole(String,Bundle)|onScriptError(String,Bundle)|onScriptExit(String,Bundle)"))
    }

    @Test
    fun `script runs have their keys gates and exit reasons`() {
        assertEquals("scriptRunSupported", UiPreviewContract.KEY_SCRIPT_RUN_SUPPORTED)
        assertEquals("scriptText", UiPreviewContract.KEY_SCRIPT_TEXT)
        assertEquals("scriptName", UiPreviewContract.KEY_SCRIPT_NAME)
        assertEquals("runId", UiPreviewContract.KEY_RUN_ID)
        assertEquals("level", UiPreviewContract.KEY_LEVEL)
        assertEquals("text", UiPreviewContract.KEY_TEXT)
        assertEquals(listOf("exit", "stopped", "error", "replaced", "released"), UiPreviewContract.EXIT_REASONS.toList())
        assertEquals("ERR_AUTOJS6_UI_PREVIEW_SCRIPT_FAILED", UiPreviewContract.ERROR_SCRIPT_FAILED)
        assertEquals(512 * 1024, UiPreviewContract.MAX_SCRIPT_CHARS)
        assertTrue(UiPreviewContract.SCRIPT_START_TIMEOUT_MS >= UiPreviewContract.LAYOUT_TIMEOUT_MS)
        // D50: the pointer reaches the embedded tree on API 33+, the keyboard on API 35+; the embedding itself needs API 30.
        assertEquals(30, UiPreviewContract.MIN_API_EMBEDDED)
        assertEquals(33, UiPreviewContract.MIN_API_INTERACTIVE)
        assertEquals(35, UiPreviewContract.MIN_API_IME)
    }
}
