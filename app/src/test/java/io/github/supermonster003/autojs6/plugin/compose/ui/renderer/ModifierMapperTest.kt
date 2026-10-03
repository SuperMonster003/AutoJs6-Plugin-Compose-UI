package io.github.supermonster003.autojs6.plugin.compose.ui.renderer

import org.autojs.plugin.compose.api.*
import org.autojs.plugin.compose.api.catalog.ComponentCatalog
import org.autojs.plugin.compose.api.model.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

class ModifierMapperTest {
    private fun dp(value: Int) = UiValue.Dp(value.toDouble())
    private fun num(value: Double) = UiValue.Num(value)
    private fun parse(name: String, vararg args: UiValue, parent: String? = null): ModifierInstruction {
        val scope = ComponentCatalog.V1.modifier(name)?.scope ?: ScopeKind.ANY
        return ModifierMapper.parse(listOf(ModifierOp(name, args.toList(), scope)), parent).single()
    }
    private fun failure(code: String, block: () -> Unit) {
        try { block(); fail("Expected $code") }
        catch (error: ComposeUiContractException) { assertEquals(code, error.code) }
    }

    @Test fun everyV1OperationResolvesTypedArgumentsAndOptionalDefaults() {
        val input = linkedMapOf(
            ComposeUiModifiers.PADDING to listOf(dp(2)), ComposeUiModifiers.SIZE to listOf(dp(3), dp(7)),
            ComposeUiModifiers.WIDTH to listOf(dp(4)), ComposeUiModifiers.HEIGHT to listOf(dp(5)),
            ComposeUiModifiers.FILL_MAX_WIDTH to emptyList(), ComposeUiModifiers.FILL_MAX_HEIGHT to listOf(num(0.5)),
            ComposeUiModifiers.FILL_MAX_SIZE to emptyList(), ComposeUiModifiers.WEIGHT to listOf(num(2.0)),
            ComposeUiModifiers.ALIGN to listOf(UiValue.Enum("bottomEnd")),
            ComposeUiModifiers.BACKGROUND to listOf(UiValue.Color(0xff123456.toInt())),
            ComposeUiModifiers.BORDER to listOf(dp(1), UiValue.Color(0xffabcdef.toInt())),
            ComposeUiModifiers.CLIP to listOf(UiValue.Shape(ShapeKind.CIRCLE)),
            ComposeUiModifiers.ALPHA to listOf(num(0.25)), ComposeUiModifiers.CLICKABLE to listOf(num(23.0)),
            ComposeUiModifiers.VERTICAL_SCROLL to emptyList(), ComposeUiModifiers.HORIZONTAL_SCROLL to listOf(UiValue.Bool(false)),
            ComposeUiModifiers.OFFSET to listOf(dp(-4), dp(8)), ComposeUiModifiers.ASPECT_RATIO to listOf(num(1.5)),
            ComposeUiModifiers.TEST_TAG to listOf(UiValue.Str("probe")), ComposeUiModifiers.SEMANTICS to listOf(UiValue.Null),
        )
        assertEquals(ComponentCatalog.V1.modifiers.map { it.name }.toSet(), input.keys)
        val mapped = input.mapValues { (name, values) ->
            parse(name, *values.toTypedArray(), parent = when (name) {
                ComposeUiModifiers.WEIGHT -> ComposeUiComponents.ROW
                ComposeUiModifiers.ALIGN -> ComposeUiComponents.BOX
                else -> null
            })
        }
        input.forEach { (name, values) -> assertEquals(name, values, mapped.getValue(name).args.take(values.size)) }
        assertEquals(num(1.0), mapped.getValue(ComposeUiModifiers.FILL_MAX_WIDTH).args.single())
        assertEquals(num(1.0), mapped.getValue(ComposeUiModifiers.FILL_MAX_SIZE).args.single())
        assertTrue(mapped.getValue(ComposeUiModifiers.WEIGHT).boolean(1))
        assertTrue(mapped.getValue(ComposeUiModifiers.CLICKABLE).boolean(1))
        assertTrue(mapped.getValue(ComposeUiModifiers.VERTICAL_SCROLL).boolean(0))
        assertFalse(mapped.getValue(ComposeUiModifiers.HORIZONTAL_SCROLL).boolean(0))
        assertEquals(UiValue.Shape(ShapeKind.RECTANGLE), mapped.getValue(ComposeUiModifiers.BACKGROUND).args[1])
        assertEquals(UiValue.Shape(ShapeKind.RECTANGLE), mapped.getValue(ComposeUiModifiers.BORDER).args[2])
        assertEquals(listOf(-4f, 8f), mapped.getValue(ComposeUiModifiers.OFFSET).args.indices.map(mapped.getValue(ComposeUiModifiers.OFFSET)::dimension))
        assertSame(UiValue.Null, mapped.getValue(ComposeUiModifiers.SEMANTICS).args.single())
    }

    @Test fun logicalPaddingOverloadsAndOrderArePreservedIncludingRepeatedOps() {
        assertEquals(listOf(2f, 2f, 2f, 2f), parse(ComposeUiModifiers.PADDING, dp(2)).padding())
        assertEquals(listOf(2f, 7f, 2f, 7f), parse(ComposeUiModifiers.PADDING, dp(2), dp(7)).padding())
        assertEquals(listOf(1f, 2f, 3f, 4f), parse(ComposeUiModifiers.PADDING, dp(1), dp(2), dp(3), dp(4)).padding())
        val ops = listOf(ModifierOp(ComposeUiModifiers.PADDING, listOf(dp(2))),
            ModifierOp(ComposeUiModifiers.BACKGROUND, listOf(UiValue.Color(-1))), ModifierOp(ComposeUiModifiers.PADDING, listOf(dp(7))))
        val forward = ModifierMapper.parse(ops)
        val backward = ModifierMapper.parse(ops.reversed())
        assertEquals(ops.map { it.name }, forward.map { it.name })
        assertEquals(listOf(2f, 2f, 2f, 2f), forward.first().padding())
        assertEquals(listOf(7f, 7f, 7f, 7f), backward.first().padding())
        assertNotEquals(forward, backward)
    }

    @Test fun boundariesRejectWrongKindsArityRangesAndScopeBeforeComposition() {
        val invalid = listOf(
            ModifierOp(ComposeUiModifiers.PADDING, listOf(dp(1), dp(2), dp(3))),
            ModifierOp(ComposeUiModifiers.SIZE, listOf(dp(-1))), ModifierOp(ComposeUiModifiers.ALPHA, listOf(num(1.01))),
            ModifierOp(ComposeUiModifiers.FILL_MAX_SIZE, listOf(num(-0.1))), ModifierOp(ComposeUiModifiers.ASPECT_RATIO, listOf(num(0.0))),
            ModifierOp(ComposeUiModifiers.CLICKABLE, listOf(num(2.5))), ModifierOp(ComposeUiModifiers.CLICKABLE, listOf(num(0.0))),
            ModifierOp(ComposeUiModifiers.SEMANTICS, listOf(UiValue.Bool(false))),
            ModifierOp(ComposeUiModifiers.OFFSET, listOf(num(1.0), num(2.0))), ModifierOp("missing"),
            ModifierOp(ComposeUiModifiers.WEIGHT, listOf(num(1.0))),
        )
        invalid.forEach { operation -> failure(ComposeUiErrorCodes.INVALID_MODIFIER) { ModifierMapper.parse(listOf(operation)) } }
        for (parent in listOf(null, ComposeUiComponents.BOX, ComposeUiComponents.BUTTON)) {
            failure(ComposeUiErrorCodes.SCOPE_MISMATCH) { parse(ComposeUiModifiers.WEIGHT, num(1.0), parent = parent) }
        }
        failure(ComposeUiErrorCodes.SCOPE_MISMATCH) { parse(ComposeUiModifiers.ALIGN, UiValue.Enum("center"), parent = ComposeUiComponents.COLUMN) }
        for (parent in listOf(ComposeUiComponents.ROW, ComposeUiComponents.COLUMN)) {
            assertEquals(1f, parse(ComposeUiModifiers.WEIGHT, num(1.0), parent = parent).number(0), 0f)
        }
        failure(ComposeUiErrorCodes.LIMIT_EXCEEDED) {
            ModifierMapper.parse(List(ComposeUiLimits.MAX_MODIFIER_OPS + 1) { ModifierOp(ComposeUiModifiers.FILL_MAX_SIZE) })
        }
    }

    @Test fun explicitClickPrecedenceAndEnablementAreSharedWithNativeControls() {
        val base = UiNode(1, ComposeUiComponents.BUTTON, callbacks = mapOf(ComposeUiEvents.CLICK to 10))
        assertEquals(10, ModifierMapper.clickCallback(base)); assertTrue(ModifierMapper.interactionEnabled(base))
        val nested = UiNode(1, ComposeUiComponents.BUTTON, callbacks = base.callbacks, modifier = listOf(
            ModifierOp(ComposeUiModifiers.CLICKABLE, listOf(num(20.0))),
            ModifierOp(ComposeUiModifiers.CLICKABLE, listOf(num(30.0), UiValue.Bool(false)))))
        assertEquals(10, ModifierMapper.clickCallback(nested)); assertTrue(ModifierMapper.interactionEnabled(nested))
        val fallback = UiNode(1, ComposeUiComponents.BUTTON, modifier = nested.modifier)
        assertEquals(20, ModifierMapper.clickCallback(fallback))
        val disabled = UiNode(1, ComposeUiComponents.BUTTON, props = mapOf(ComposeUiProps.ENABLED to UiValue.Bool(false)),
            modifier = listOf(ModifierOp(ComposeUiModifiers.CLICKABLE, listOf(num(20.0), UiValue.Bool(true)))))
        assertFalse(ModifierMapper.interactionEnabled(disabled))
    }

    @Test fun commandRegistryRejectsUnsupportedOrStaleHandlesAndOnlyDisposesItsOwnRegistration() {
        val registry = NodeCommandRegistry()
        val calls = mutableListOf<String>()
        val first = registry.register(1, NodeCommandRegistry.Handle(focus = { calls += "old focus" }))
        val queued = registry.resolve(UiCommand.Focus(1))
        val second = registry.register(1, NodeCommandRegistry.Handle(focus = { calls += "focus" }, blur = { calls += "blur" },
            scroll = { index, offset -> calls += "$index:$offset" }))
        first()
        complete { registry.execute(UiCommand.Focus(1)); registry.execute(UiCommand.Blur(1)); registry.execute(UiCommand.ScrollTo(1, offset = 9)) }
        assertEquals(listOf("focus", "blur", "null:9"), calls)
        failure(ComposeUiErrorCodes.NODE_DETACHED) { complete(queued) }
        failure(ComposeUiErrorCodes.INVALID_ARGUMENT) { registry.validate(UiCommand.ScrollTo(1, index = 2)) }
        val unmounted = registry.resolve(UiCommand.Focus(1))
        second()
        failure(ComposeUiErrorCodes.NODE_DETACHED) { complete(unmounted) }
        failure(ComposeUiErrorCodes.NODE_DETACHED) { registry.validate(UiCommand.Focus(1)) }
        registry.register(2, NodeCommandRegistry.Handle())
        failure(ComposeUiErrorCodes.INVALID_ARGUMENT) { registry.validate(UiCommand.Focus(2)) }
        registry.register(3, NodeCommandRegistry.Handle(scroll = { index, offset -> calls += "$index:$offset" }, indexedScroll = true))
        complete { registry.execute(UiCommand.ScrollTo(3, index = 4, offset = -7)) }
        assertEquals("4:-7", calls.last())
        registry.clear()
        failure(ComposeUiErrorCodes.NODE_DETACHED) { registry.validate(UiCommand.ScrollTo(3, index = 4)) }
    }

    private fun complete(action: suspend () -> Unit) {
        var result: Result<Unit>? = null
        action.startCoroutine(object : Continuation<Unit> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(outcome: Result<Unit>) { result = outcome }
        })
        checkNotNull(result) { "A pure registry test must not suspend" }.getOrThrow()
    }
}
