package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import org.autojs.plugin.ui.preview.UiPreviewContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The packaged `ui-preview-api.aar` is the v3 the designer's client is written against (roadmap P10.6, P11.H1):
 * the frozen v1 base the handshake checks first, the frozen v2 identity the preview info still answers, the v3
 * identity listed with the served versions, the script-run keys and exit reasons the client and the console rely
 * on, and the project-resource keys. A stale or foreign AAR in `libs/` fails here before a device does.
 */
class UiPreviewContractGuardTest {
    @Test
    fun `the contract is v3 over the frozen v1 base and v2 identity`() {
        assertEquals(3, UiPreviewContract.VERSION)
        assertEquals(2, UiPreviewContract.VERSION_V2)
        assertEquals(1, UiPreviewContract.BASE_VERSION)
        // The hash of contract v1 as shipped with 1.6.0: a v1 host answers it from getContractHash().
        assertEquals("680f41b5459648ff07bd1d3d9684c5279d9398b78f61c0307862f50f8dcac0b6", UiPreviewContract.HASH_V1)
        // The hash of contract v2 as shipped with 1.7.0: the preview info's identity keys keep answering it.
        assertEquals("81f7fe8d2338eed197dcc21e3cff7007a681ea89d080b42d1ae132c94688f1bb", UiPreviewContract.HASH_V2)
        assertEquals("196e52cd66ff871dca8351a0e9208679ed2bf64af5f733d05312239b1f03a44b", UiPreviewContract.HASH)
        // v2 keeps every v1 transaction in its place and appends after it; v3 adds no transaction.
        val v1Service = UiPreviewContract.HASH_INPUT_V1.substringAfter("v1|").substringBefore("|callback:")
        val v1Callbacks = UiPreviewContract.HASH_INPUT_V1.substringAfter("|callback:").substringBefore("|bundle:")
        assertTrue(UiPreviewContract.HASH_INPUT_V2.startsWith("autojs6.ui.preview.v2|$v1Service|runScript("))
        assertTrue(UiPreviewContract.HASH_INPUT_V2.contains("|callback:$v1Callbacks|onConsole("))
        assertTrue(UiPreviewContract.HASH_INPUT_V2.contains("runScript(String,Bundle)->Bundle|stopScript(String)->Bundle"))
        val v2Service = UiPreviewContract.HASH_INPUT_V2.substringAfter("v2|").substringBefore("|callback:")
        assertTrue(UiPreviewContract.HASH_INPUT.startsWith("autojs6.ui.preview.v3|$v2Service|callback:"))
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

    @Test
    fun `project resources have their keys limits and diagnostic`() {
        assertEquals("supportedContractVersions", UiPreviewContract.KEY_SUPPORTED_CONTRACT_VERSIONS)
        assertEquals("contractHashes", UiPreviewContract.KEY_CONTRACT_HASHES)
        assertEquals("resourcesSupported", UiPreviewContract.KEY_RESOURCES_SUPPORTED)
        assertEquals("resourcesFd", UiPreviewContract.KEY_RESOURCES_FD)
        assertEquals("resourcesSha256", UiPreviewContract.KEY_RESOURCES_SHA256)
        assertEquals("layoutPath", UiPreviewContract.KEY_LAYOUT_PATH)
        assertEquals("androidLayout", UiPreviewContract.KEY_ANDROID_LAYOUT)
        assertEquals("projectFd", UiPreviewContract.KEY_PROJECT_FD)
        assertEquals("projectSha256", UiPreviewContract.KEY_PROJECT_SHA256)
        assertEquals("scriptPath", UiPreviewContract.KEY_SCRIPT_PATH)
        assertEquals("ERR_AUTOJS6_UI_PREVIEW_ARCHIVE_INVALID", UiPreviewContract.ERROR_ARCHIVE_INVALID)
        assertEquals("ERR_AUTOJS6_UI_PREVIEW_ATTRIBUTE_IGNORED", UiPreviewContract.ERROR_ATTRIBUTE_IGNORED)
        assertEquals("o", UiPreviewContract.Diagnostic.OUTCOME)
        assertEquals(listOf("namespace", "unsupported", "unknown"), UiPreviewContract.ATTRIBUTE_OUTCOMES.toList())
        assertEquals(32L * 1024 * 1024, UiPreviewContract.MAX_ARCHIVE_BYTES)
        assertEquals(4_096, UiPreviewContract.MAX_ARCHIVE_ENTRIES)
    }
}
