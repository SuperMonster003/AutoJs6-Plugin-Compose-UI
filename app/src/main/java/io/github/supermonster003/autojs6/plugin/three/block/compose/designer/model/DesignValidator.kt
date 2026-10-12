package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model

import org.autojs.plugin.compose.api.ComposeUiComponents
import org.autojs.plugin.compose.api.ComposeUiContractException
import org.autojs.plugin.compose.api.ComposeUiLimits
import org.autojs.plugin.compose.api.catalog.ChildrenPolicy
import org.autojs.plugin.compose.api.catalog.ComponentCatalog
import org.autojs.plugin.compose.api.catalog.ComponentInvocation
import org.autojs.plugin.compose.api.catalog.ValueType
import org.autojs.plugin.compose.api.model.ShapeKind
import org.autojs.plugin.compose.api.model.UiValue
import org.autojs.plugin.compose.api.model.ValueKind
import org.autojs.plugin.compose.api.v2.ComposeUiV2

enum class Severity { ERROR, WARNING }

/** [target] names the property, slot, event, modifier or `children` the message is about. */
data class Diagnostic(val nodeId: Int?, val target: String?, val severity: Severity, val message: String)

/**
 * Converts JavaScript literals the way the host's script bridge does, so the inspector and the validator
 * agree with the runtime on what a literal means for a given catalog type.
 */
object JsValueConverter {
    sealed class Conversion {
        data class Ok(val value: UiValue) : Conversion()
        /** The runtime may accept it (named colors, image paths) but the designer cannot decide offline. */
        data class Unverifiable(val reason: String) : Conversion()
        data class Invalid(val reason: String) : Conversion()
    }

    private val dimension = Regex("""^\s*(-?\d+(?:\.\d+)?)\s*(dp|sp)\s*$""")
    private val hexColor = Regex("""^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{4}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$""")

    fun convert(literal: JsLiteral, type: ValueType): Conversion {
        val kinds = type.kinds
        val converted: UiValue = try {
            when {
                literal is JsLiteral.Null -> if (ValueKind.NULL in kinds) UiValue.Null else return Conversion.Invalid("A value is required")
                ValueKind.COLOR in kinds -> when (literal) {
                    is JsLiteral.Str -> hexColor.matchEntire(literal.value)?.let { UiValue.Color(parseHex(it.groupValues[1])) }
                        ?: return Conversion.Unverifiable("Named colors are resolved by the host")
                    is JsLiteral.Num -> UiValue.Color(literal.value.toLong().toInt())
                    else -> return Conversion.Invalid("Expected a color")
                }
                ValueKind.BITMAP in kinds -> if (literal is JsLiteral.Str) return Conversion.Unverifiable("Images are resolved by the host") else return Conversion.Invalid("Expected an image path or object")
                ValueKind.TEXT_STYLE in kinds && literal !is JsLiteral.Str -> textStyle(literal) ?: return Conversion.Invalid("Expected a typography name or a text style object")
                ValueKind.SHAPE in kinds -> shape(literal) ?: return Conversion.Invalid("Expected rectangle, rounded, circle or {shape, radius}")
                ValueKind.LIST in kinds && literal is JsLiteral.Arr -> {
                    if (literal.items.size !in type.listMinimum..type.listMaximum) return Conversion.Invalid("Invalid list length")
                    val itemType = ValueType(type.listKinds.ifEmpty { setOf(ValueKind.NUMBER) })
                    val items = literal.items.map { item ->
                        when (val inner = convert(item, itemType)) {
                            is Conversion.Ok -> inner.value
                            is Conversion.Unverifiable -> return inner
                            is Conversion.Invalid -> return inner
                        }
                    }
                    UiValue.ListOf(items)
                }
                ValueKind.DP in kinds -> UiValue.Dp(dimension(literal, "dp") ?: return Conversion.Invalid("Expected a number or a dp dimension"))
                ValueKind.SP in kinds -> UiValue.Sp(dimension(literal, "sp") ?: return Conversion.Invalid("Expected a number or an sp dimension"))
                ValueKind.ENUM in kinds -> UiValue.Enum((literal as? JsLiteral.Str)?.value ?: return Conversion.Invalid("Expected one of ${type.enumValues}"))
                ValueKind.ICON_NAME in kinds -> UiValue.IconName((literal as? JsLiteral.Str)?.value ?: return Conversion.Invalid("Expected an icon name"))
                ValueKind.STRING in kinds -> UiValue.Str((literal as? JsLiteral.Str)?.value ?: return Conversion.Invalid("Expected a string"))
                ValueKind.BOOLEAN in kinds -> UiValue.Bool((literal as? JsLiteral.Bool)?.value ?: return Conversion.Invalid("Expected true or false"))
                ValueKind.NUMBER in kinds -> UiValue.Num((literal as? JsLiteral.Num)?.value ?: return Conversion.Invalid("Expected a number"))
                else -> return Conversion.Invalid("Unsupported value type")
            }
        } catch (error: ComposeUiContractException) {
            return Conversion.Invalid(error.message ?: "Invalid value")
        } catch (error: IllegalArgumentException) {
            return Conversion.Invalid(error.message ?: "Invalid value")
        }
        if (!type.accepts(converted)) {
            return Conversion.Invalid(if (converted is UiValue.Enum && type.enumValues.isNotEmpty()) "Expected one of ${type.enumValues}" else "Value is outside the allowed range")
        }
        return Conversion.Ok(converted)
    }

    private fun parseHex(digits: String): Int {
        val expanded = when (digits.length) {
            3 -> "FF" + digits.map { "$it$it" }.joinToString("")
            4 -> digits.map { "$it$it" }.joinToString("")
            6 -> "FF$digits"
            else -> digits
        }
        return java.lang.Long.parseLong(expanded, 16).toInt()
    }

    private fun dimension(literal: JsLiteral, unit: String): Double? = when (literal) {
        is JsLiteral.Num -> literal.value
        is JsLiteral.Str -> dimension.matchEntire(literal.value)?.takeIf { it.groupValues[2] == unit }?.groupValues?.get(1)?.toDoubleOrNull()
        else -> null
    }

    private fun shape(literal: JsLiteral): UiValue.Shape? {
        val fields: Map<String, JsLiteral> = when (literal) {
            is JsLiteral.Str -> mapOf("shape" to literal)
            is JsLiteral.Obj -> literal.fields.toMap()
            else -> return null
        }
        if (fields.keys.any { it != "shape" && it != "radius" }) return null
        val kind = when ((fields["shape"] as? JsLiteral.Str)?.value) {
            "rectangle" -> ShapeKind.RECTANGLE
            "rounded" -> ShapeKind.ROUNDED
            "circle" -> ShapeKind.CIRCLE
            else -> return null
        }
        val radius = fields["radius"]?.let { dimension(it, "dp") ?: return null } ?: 0.0
        return UiValue.Shape(kind, radius)
    }

    private fun textStyle(literal: JsLiteral): UiValue.TextStyle? {
        val fields = (literal as? JsLiteral.Obj)?.fields?.toMap() ?: return null
        if (fields.keys.any { it !in setOf("fontFamily", "fontSize", "fontWeight", "italic", "lineHeight", "letterSpacing") }) return null
        return UiValue.TextStyle(
            fontFamily = fields["fontFamily"]?.let { (it as? JsLiteral.Str)?.value ?: return null },
            fontSize = fields["fontSize"]?.let { dimension(it, "sp") ?: return null },
            fontWeight = fields["fontWeight"]?.let { (it as? JsLiteral.Num)?.value?.toInt() ?: return null },
            italic = fields["italic"]?.let { (it as? JsLiteral.Bool)?.value ?: return null },
            lineHeight = fields["lineHeight"]?.let { dimension(it, "sp") ?: return null },
            letterSpacing = fields["letterSpacing"]?.let { dimension(it, "sp") ?: return null },
        )
    }
}

/**
 * Catalog-driven validation of a document (roadmap D36): property names, aliases, shortcuts, slots,
 * events, children policies, modifier signatures and scopes, the V2 parent rules and the contract limits.
 * Diagnostics never block saving; the export summarizes them.
 */
class DesignValidator(private val catalog: ComponentCatalog = ComposeUiV2.interopCatalog) {

    fun validate(document: DesignDocument): List<Diagnostic> {
        val diagnostics = ArrayList<Diagnostic>()
        val root = document.root ?: return listOf(Diagnostic(null, null, Severity.ERROR, "The document has no mount, floaty or dialog"))
        val total = root.descendants().count()
        if (total > ComposeUiLimits.MAX_NODES_PER_SESSION) diagnostics += Diagnostic(root.id, null, Severity.ERROR, "The tree has $total nodes; the contract allows ${ComposeUiLimits.MAX_NODES_PER_SESSION}")
        validateNode(root, null, 1, diagnostics)
        return diagnostics
    }

    fun validateNode(node: DesignNode, parentType: String?, depth: Int, diagnostics: MutableList<Diagnostic>) {
        fun error(target: String?, message: String) { diagnostics += Diagnostic(node.id, target, Severity.ERROR, message) }
        fun warning(target: String?, message: String) { diagnostics += Diagnostic(node.id, target, Severity.WARNING, message) }
        val spec = catalog.component(node.type)
        if (spec == null) { error(null, "Unknown component ${node.type}"); return }
        if (spec.invocation != ComponentInvocation.NODE) error(null, "${node.type} is a session command, not a node")
        if (depth > ComposeUiLimits.MAX_DEPTH) error(null, "The tree is nested deeper than ${ComposeUiLimits.MAX_DEPTH} levels")
        val shortcuts = catalog.shortcuts.associateBy { it.name }
        val slotNames = spec.slots.map { it.name }.toSet()
        val eventNames = spec.events.map { "on" + it.name.replaceFirstChar(Char::uppercaseChar) }.toSet()
        val seen = HashSet<String>()
        var propCount = 0
        node.entries.forEach { entry ->
            when (entry) {
                is NodeEntry.Key -> {
                    val key = entry.value
                    if (key is PropValue.Literal && key.value !is JsLiteral.Str && key.value !is JsLiteral.Num) error("key", "key must be a string or a number")
                    if (!seen.add("key")) error("key", "key is given twice")
                }
                is NodeEntry.Prop -> {
                    propCount++
                    val canonical = spec.props.firstOrNull { it.name == entry.name || entry.name in it.aliases }
                    when {
                        entry.name == "ref" -> Unit
                        canonical != null -> {
                            if (!seen.add(canonical.name)) error(entry.name, "${canonical.name} is given twice (aliases count)")
                            checkValue(entry.name, entry.value, canonical.type, ::error, ::warning)
                        }
                        entry.name in shortcuts -> {
                            if (!seen.add(entry.name)) error(entry.name, "${entry.name} is given twice")
                            val shortcut = shortcuts.getValue(entry.name)
                            val modifier = catalog.modifier(shortcut.modifierName)!!
                            if (modifier.allowedParents.isNotEmpty() && parentType !in modifier.allowedParents) {
                                error(entry.name, "${entry.name} only applies inside ${modifier.allowedParents.joinToString(" / ")}")
                            }
                            val value = entry.value
                            if (value is PropValue.Literal) {
                                val literal = value.value
                                if (shortcut.expandList && literal is JsLiteral.Arr) {
                                    val signature = modifier.signatures.firstOrNull { literal.items.size in it.requiredCount..it.args.size }
                                    if (signature == null) error(entry.name, "${entry.name} accepts ${modifier.signatures.joinToString(" or ") { it.args.size.toString() }} values")
                                    else literal.items.forEachIndexed { index, item -> checkValue(entry.name, PropValue.Literal(item), signature.args[index].type, ::error, ::warning) }
                                } else checkValue(entry.name, value, modifier.signatures.first().args.first().type, ::error, ::warning)
                            }
                        }
                        entry.name in slotNames -> error(entry.name, "${entry.name} is a slot of ${node.type}; give it a node or a string")
                        entry.name in eventNames -> error(entry.name, "${entry.name} expects a function")
                        entry.name == "children" || entry.name == "slots" -> error(entry.name, "${entry.name} is not a property")
                        else -> error(entry.name, "${node.type} has no property ${entry.name}")
                    }
                }
                is NodeEntry.Slot -> {
                    if (entry.name !in slotNames) error(entry.name, "${node.type} has no slot ${entry.name}")
                    if (!seen.add("slot:" + entry.name)) error(entry.name, "slot ${entry.name} is given twice")
                }
                is NodeEntry.Handler -> {
                    if (entry.name !in eventNames) error(entry.name, "${node.type} has no event ${entry.name}")
                    if (!seen.add(entry.name)) error(entry.name, "${entry.name} is given twice")
                }
                is NodeEntry.Modifier -> {
                    if (!seen.add("modifier")) error("modifier", "modifier is given twice")
                    if (entry.chain.size > ComposeUiLimits.MAX_MODIFIER_OPS) error("modifier", "The modifier chain exceeds ${ComposeUiLimits.MAX_MODIFIER_OPS} operations")
                    entry.chain.forEach { operation -> checkModifier(operation, parentType, ::error, ::warning) }
                }
                is NodeEntry.ModifierExpression -> if (!seen.add("modifier")) error("modifier", "modifier is given twice")
            }
        }
        if (propCount > ComposeUiLimits.MAX_PROPERTIES_PER_NODE) error(null, "More than ${ComposeUiLimits.MAX_PROPERTIES_PER_NODE} properties")
        spec.props.filter { it.required }.forEach { prop ->
            if (node.props.none { it.name == prop.name || it.name in prop.aliases }) error(prop.name, "${node.type} requires ${prop.name}")
        }
        spec.slots.filter { it.required }.forEach { slot ->
            if (node.slots.none { it.name == slot.name }) error(slot.name, "${node.type} requires the ${slot.name} slot")
        }
        val childNodes = node.children.mapNotNull { (it as? DesignChild.Node)?.node }
        val limit = if (spec.childrenPolicy == ChildrenPolicy.LAZY_ITEMS || node.type == ComposeUiComponents.LAZY_COLUMN || node.type == ComposeUiComponents.LAZY_ROW)
            ComposeUiLimits.MAX_LAZY_CHILDREN_PER_NODE else ComposeUiLimits.MAX_CHILDREN_PER_NODE
        when (spec.childrenPolicy) {
            ChildrenPolicy.NONE -> if (node.children.isNotEmpty()) error("children", "${node.type} takes no children")
            ChildrenPolicy.SINGLE -> if (node.children.size > 1) error("children", "${node.type} takes a single child")
            else -> Unit
        }
        if (node.children.size > limit) error("children", "${node.type} has more than $limit children")
        if (node.children.isNotEmpty() && node.slots.any { it.name == "content" }) error("children", "Both children and the content slot are given")
        val expectedParent = EXPECTED_PARENT[node.type]
        if (expectedParent != null && parentType != expectedParent) error(null, "${node.type} must be a direct child of $expectedParent")
        val expectedChild = EXPECTED_CHILD[node.type]
        if (expectedChild != null) {
            childNodes.filter { it.type != expectedChild }.forEach { error("children", "${node.type} accepts only $expectedChild children, not ${it.type}") }
            if (node.children.isEmpty() && node.type != ComposeUiV2.Components.TAB_ROW) error("children", "${node.type} needs at least one ${expectedChild}")
        }
        val keys = HashSet<String>()
        childNodes.forEach { child ->
            val key = child.key
            if (key is PropValue.Literal) {
                val text = when (val literal = key.value) { is JsLiteral.Str -> literal.value; is JsLiteral.Num -> literal.text; else -> null }
                if (text != null && !keys.add(text)) error("children", "Two children share the key '$text'")
            }
        }
        node.slots.forEach { slot -> (slot.child as? DesignChild.Node)?.let { validateNode(it.node, node.type, depth + 1, diagnostics) } }
        childNodes.forEach { validateNode(it, node.type, depth + 1, diagnostics) }
    }

    private fun checkModifier(operation: ModifierEntry, parentType: String?, error: (String?, String) -> Unit, warning: (String?, String) -> Unit) {
        val spec = catalog.modifier(operation.name)
        if (spec == null) { error("modifier", "Unknown modifier ${operation.name}"); return }
        val signature = spec.signatures.firstOrNull { operation.args.size in it.requiredCount..it.args.size }
        if (signature == null) { error("modifier", "${operation.name} accepts ${spec.signatures.joinToString(" or ") { "${it.requiredCount}..${it.args.size}" }} arguments"); return }
        if (spec.allowedParents.isNotEmpty() && parentType !in spec.allowedParents) {
            error("modifier", "${operation.name} only applies inside ${spec.allowedParents.joinToString(" / ")}")
        }
        operation.args.forEachIndexed { index, argument ->
            val argSpec = signature.args[index]
            when (argument) {
                is ModifierArg.Callback -> if (!argSpec.callback) error("modifier", "${operation.name} argument ${index + 1} is not a callback")
                is ModifierArg.Value -> if (argSpec.callback) {
                    if (argument.value !is PropValue.Expression) error("modifier", "${operation.name} needs a callback function")
                } else checkValue("modifier ${operation.name}", argument.value, argSpec.type, error, warning)
            }
        }
    }

    private fun checkValue(target: String, value: PropValue, type: ValueType, error: (String?, String) -> Unit, warning: (String?, String) -> Unit) {
        val literal = (value as? PropValue.Literal)?.value ?: return
        when (val conversion = JsValueConverter.convert(literal, type)) {
            is JsValueConverter.Conversion.Invalid -> error(target, conversion.reason)
            is JsValueConverter.Conversion.Unverifiable -> warning(target, conversion.reason)
            is JsValueConverter.Conversion.Ok -> if (conversion.value is UiValue.Str && conversion.value.value.length > ComposeUiLimits.MAX_STRING_CHARS) {
                error(target, "Text exceeds ${ComposeUiLimits.MAX_STRING_CHARS} characters")
            }
        }
    }

    companion object {
        /** Direct-parent rules of the V2 item components (mirrors ComposeUiV2.validateNode). */
        val EXPECTED_PARENT: Map<String, String> = mapOf(
            ComposeUiV2.Components.NAVIGATION_BAR_ITEM to ComposeUiV2.Components.NAVIGATION_BAR,
            ComposeUiV2.Components.NAVIGATION_RAIL_ITEM to ComposeUiV2.Components.NAVIGATION_RAIL,
            ComposeUiV2.Components.TAB to ComposeUiV2.Components.TAB_ROW,
            ComposeUiV2.Components.SEGMENTED_BUTTON_ITEM to ComposeUiV2.Components.SEGMENTED_BUTTON,
        )
        val EXPECTED_CHILD: Map<String, String> = EXPECTED_PARENT.entries.associate { (child, parent) -> parent to child }
    }
}
