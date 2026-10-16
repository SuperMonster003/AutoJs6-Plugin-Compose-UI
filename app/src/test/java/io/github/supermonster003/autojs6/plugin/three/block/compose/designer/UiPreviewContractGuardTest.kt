package io.github.supermonster003.autojs6.plugin.three.block.compose.designer

import org.autojs.plugin.ui.preview.UiPreviewContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The packaged `ui-preview-api.aar` is the v4 the designer's client is written against (roadmap P10.6, P11.H1, P11.5):
 * the frozen v1 base the handshake checks first, the frozen v2 identity the preview info still answers, the frozen v3
 * and current v4 identities listed with the served versions, the script-run keys and exit reasons the client and the
 * console rely on, the project-resource keys and the v4 lifecycle and busy-wait keys. A stale or foreign AAR in
 * `libs/` fails here before a device does.
 */
class UiPreviewContractGuardTest {
    @Test
    fun `the contract is v4 over the frozen v1 base and v2 and v3 identities`() {
        assertEquals(4, UiPreviewContract.VERSION)
        assertEquals(3, UiPreviewContract.VERSION_V3)
        assertEquals(2, UiPreviewContract.VERSION_V2)
        assertEquals(1, UiPreviewContract.BASE_VERSION)
        // The hash of contract v1 as shipped with 1.6.0: a v1 host answers it from getContractHash().
        assertEquals("680f41b5459648ff07bd1d3d9684c5279d9398b78f61c0307862f50f8dcac0b6", UiPreviewContract.HASH_V1)
        // The hash of contract v2 as shipped with 1.7.0: the preview info's identity keys keep answering it.
        assertEquals("81f7fe8d2338eed197dcc21e3cff7007a681ea89d080b42d1ae132c94688f1bb", UiPreviewContract.HASH_V2)
        // The hash of contract v3 as shipped with 1.8.0: the version list keeps answering it.
        assertEquals("196e52cd66ff871dca8351a0e9208679ed2bf64af5f733d05312239b1f03a44b", UiPreviewContract.HASH_V3)
        assertEquals("b7591e9ee2fe24f475db79a0186c87fd6c36d8de632c3074cc491155d2e881b2", UiPreviewContract.HASH)
        // v2 keeps every v1 transaction in its place and appends after it; v3 adds no transaction; v4 appends one.
        val v1Service = UiPreviewContract.HASH_INPUT_V1.substringAfter("v1|").substringBefore("|callback:")
        val v1Callbacks = UiPreviewContract.HASH_INPUT_V1.substringAfter("|callback:").substringBefore("|bundle:")
        assertTrue(UiPreviewContract.HASH_INPUT_V2.startsWith("autojs6.ui.preview.v2|$v1Service|runScript("))
        assertTrue(UiPreviewContract.HASH_INPUT_V2.contains("|callback:$v1Callbacks|onConsole("))
        assertTrue(UiPreviewContract.HASH_INPUT_V2.contains("runScript(String,Bundle)->Bundle|stopScript(String)->Bundle"))
        val v2Service = UiPreviewContract.HASH_INPUT_V2.substringAfter("v2|").substringBefore("|callback:")
        assertTrue(UiPreviewContract.HASH_INPUT_V3.startsWith("autojs6.ui.preview.v3|$v2Service|callback:"))
        assertTrue(UiPreviewContract.HASH_INPUT.startsWith("autojs6.ui.preview.v4|$v2Service|sendLifecycle(String,Bundle)->Bundle|callback:"))
        assertTrue(UiPreviewContract.HASH_INPUT.contains("|onScriptExit(String,Bundle)|onLifecycleHandled(String,Bundle)|bundle:"))
    }

    @Test
    fun `lifecycle events busy waits and the v4 flags have their keys`() {
        assertEquals("dialogsInCanvas", UiPreviewContract.KEY_DIALOGS_IN_CANVAS)
        assertEquals("lifecycleEvents", UiPreviewContract.KEY_LIFECYCLE_EVENTS)
        assertEquals("busyWaitLimitSupported", UiPreviewContract.KEY_BUSY_WAIT_LIMIT_SUPPORTED)
        assertEquals("windowFunctionsVisible", UiPreviewContract.KEY_WINDOW_FUNCTIONS_VISIBLE)
        assertEquals("busyWaitLimitMs", UiPreviewContract.KEY_BUSY_WAIT_LIMIT_MS)
        assertEquals("event", UiPreviewContract.KEY_LIFECYCLE_EVENT)
        assertEquals("consumed", UiPreviewContract.KEY_CONSUMED)
        assertEquals(listOf("pause", "resume", "back_pressed"), UiPreviewContract.LIFECYCLE_EVENTS.toList())
        assertEquals("ERR_AUTOJS6_UI_PREVIEW_BUSY_WAIT", UiPreviewContract.ERROR_BUSY_WAIT)
        assertEquals("ERR_AUTOJS6_UI_PREVIEW_NOT_RUNNING", UiPreviewContract.ERROR_NOT_RUNNING)
        // The designer's setting offers whole seconds within the host's limits.
        assertEquals(1_000L, UiPreviewContract.MIN_BUSY_WAIT_LIMIT_MS)
        assertEquals(60_000L, UiPreviewContract.MAX_BUSY_WAIT_LIMIT_MS)
        assertEquals(5_000L, UiPreviewContract.DEFAULT_BUSY_WAIT_LIMIT_MS)
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
