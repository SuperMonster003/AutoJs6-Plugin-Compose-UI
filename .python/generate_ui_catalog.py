#!/usr/bin/env python3
"""Generate the designer's ui (XML) catalog from the AutoJs6 documentation site (roadmap P10.1, appendix C).

Reads api/ui.md (the built-in tag table) and api/uiAttributes.md (the attribute tables per view class) of an
explicitly named AutoJs6-Documentation checkout and writes app/src/main/resources/designer-ui/catalog.json.
The view class inheritance chains and the palette categories are kept in this script; the documentation is
the only source of tags, attributes, aliases, value types and the unsupported lists. --check reports drift.
"""

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

OUTPUT = Path(__file__).resolve().parents[1] / "app/src/main/resources/designer-ui/catalog.json"

# View class -> parent chain (nearest first). Js* classes are the host's widgets; the rest are Android / AndroidX.
CHAINS = {
    "View": [],
    "ViewGroup": ["View"],
    "Space": ["View"],
    "TextView": ["View"],
    "Button": ["TextView"],
    "EditText": ["TextView"],
    "CheckedTextView": ["TextView"],
    "Chronometer": ["TextView"],
    "TextClock": ["TextView"],
    "CompoundButton": ["Button"],
    "CheckBox": ["CompoundButton"],
    "RadioButton": ["CompoundButton"],
    "Switch": ["CompoundButton"],
    "ToggleButton": ["CompoundButton"],
    "ImageView": ["View"],
    "ImageButton": ["ImageView"],
    "QuickContactBadge": ["ImageView"],
    "FloatingActionButton": ["ImageView"],
    "ProgressBar": ["View"],
    "SeekBar": ["ProgressBar"],
    "RatingBar": ["ProgressBar"],
    "SurfaceView": ["View"],
    "VideoView": ["SurfaceView"],
    "CanvasView": ["View"],
    "WebView": ["ViewGroup"],
    "LinearLayout": ["ViewGroup"],
    "FrameLayout": ["ViewGroup"],
    "RelativeLayout": ["ViewGroup"],
    "ScrollView": ["FrameLayout"],
    "HorizontalScrollView": ["FrameLayout"],
    "DrawerLayout": ["ViewGroup"],
    "AppBarLayout": ["LinearLayout"],
    "Toolbar": ["ViewGroup"],
    "ActionMenuView": ["LinearLayout"],
    "CardView": ["FrameLayout"],
    "RadioGroup": ["LinearLayout"],
    "NumberPicker": ["LinearLayout"],
    "DatePicker": ["FrameLayout"],
    "TimePicker": ["FrameLayout"],
    "CalendarView": ["FrameLayout"],
    "SearchView": ["LinearLayout"],
    "Spinner": ["ViewGroup"],
    "ConsoleView": ["FrameLayout"],
    "RecyclerView": ["ViewGroup"],
    "ViewPager": ["ViewGroup"],
    "TabLayout": ["FrameLayout"],
    "ViewAnimator": ["FrameLayout"],
    "ViewFlipper": ["ViewAnimator"],
    "ViewSwitcher": ["ViewAnimator"],
    "TextSwitcher": ["ViewSwitcher"],
    "ImageSwitcher": ["ViewSwitcher"],
    "JsTextView": ["TextView"],
    "JsButton": ["Button"],
    "JsEditText": ["EditText"],
    "JsCheckedTextView": ["CheckedTextView"],
    "JsChronometer": ["Chronometer"],
    "JsTextClock": ["TextClock"],
    "JsTextSwitcher": ["TextSwitcher"],
    "JsCheckBox": ["CheckBox"],
    "JsRadioButton": ["RadioButton"],
    "JsRadioGroup": ["RadioGroup"],
    "JsSwitch": ["Switch"],
    "JsToggleButton": ["ToggleButton"],
    "JsSpinner": ["Spinner"],
    "JsNumberPicker": ["NumberPicker"],
    "JsDatePicker": ["DatePicker"],
    "JsTimePicker": ["TimePicker"],
    "JsCalendarView": ["CalendarView"],
    "JsSearchView": ["SearchView"],
    "JsProgressBar": ["ProgressBar"],
    "JsSeekBar": ["SeekBar"],
    "JsRatingBar": ["RatingBar"],
    "JsImageView": ["ImageView"],
    "JsImageButton": ["ImageButton"],
    "JsQuickContactBadge": ["QuickContactBadge"],
    "JsFloatingActionButton": ["FloatingActionButton"],
    "JsVideoView": ["VideoView"],
    "JsWebView": ["WebView"],
    "JsCanvasView": ["CanvasView"],
    "JsConsoleView": ["ConsoleView"],
    "JsGlobalConsoleView": ["ConsoleView"],
    "JsListView": ["RecyclerView"],
    "JsGridView": ["RecyclerView"],
    "JsLinearLayout": ["LinearLayout"],
    "JsFrameLayout": ["FrameLayout"],
    "JsRelativeLayout": ["RelativeLayout"],
    "JsScrollView": ["ScrollView"],
    "JsDrawerLayout": ["DrawerLayout"],
    "JsAppBarLayout": ["AppBarLayout"],
    "JsToolbar": ["Toolbar"],
    "JsActionMenuView": ["ActionMenuView"],
    "JsCardView": ["CardView"],
    "JsViewPager": ["ViewPager"],
    "JsTabLayout": ["TabLayout"],
    "JsViewFlipper": ["ViewFlipper"],
    "JsViewSwitcher": ["ViewSwitcher"],
    "ComposeContainerView": ["FrameLayout"],
}

# Primary tag -> (palette category, may hold element children, child axis).
TAG_META = {
    "view": ("other", False, None),
    "space": ("other", False, None),
    "linear": ("layout", True, "horizontal"),
    "vertical": ("layout", True, "vertical"),
    "frame": ("layout", True, "stack"),
    "compose": ("other", False, None),
    "relative": ("layout", True, "stack"),
    "scroll": ("layout", True, "vertical"),
    "drawer": ("layout", True, "stack"),
    "appbar": ("layout", True, "vertical"),
    "toolbar": ("layout", True, "horizontal"),
    "actionmenu": ("layout", True, "horizontal"),
    "card": ("layout", True, "stack"),
    "text": ("text", False, None),
    "button": ("button", False, None),
    "input": ("input", False, None),
    "checkedtext": ("choice", False, None),
    "chronometer": ("text", False, None),
    "textclock": ("text", False, None),
    "textswitcher": ("text", True, "stack"),
    "checkbox": ("choice", False, None),
    "radio": ("choice", False, None),
    "radiogroup": ("choice", True, "vertical"),
    "switch": ("choice", False, None),
    "togglebutton": ("choice", False, None),
    "spinner": ("choice", False, None),
    "numberpicker": ("choice", False, None),
    "datepicker": ("choice", False, None),
    "timepicker": ("choice", False, None),
    "calendar": ("choice", False, None),
    "search": ("input", False, None),
    "progressbar": ("progress", False, None),
    "seekbar": ("progress", False, None),
    "ratingbar": ("progress", False, None),
    "image": ("image", False, None),
    "imagebutton": ("button", False, None),
    "quickcontactbadge": ("image", False, None),
    "fab": ("button", False, None),
    "video": ("media", False, None),
    "webview": ("media", False, None),
    "canvas": ("media", False, None),
    "console": ("other", False, None),
    "globalconsole": ("other", False, None),
    "list": ("list", True, "vertical"),
    "grid": ("list", True, "vertical"),
    "viewpager": ("layout", True, "horizontal"),
    "tabs": ("layout", False, None),
    "viewflipper": ("layout", True, "stack"),
    "viewswitcher": ("layout", True, "stack"),
}

# Attribute section heading -> view class the tables and bullets of that section belong to.
SECTION_CLASSES = {
    "View 属性": "View",
    "ViewGroup 属性": "ViewGroup",
    "TextView 属性": "TextView",
    "输入框附加属性": "EditText",
    "ImageView 属性": "ImageView",
    "圆角图片属性": "JsImageView",
    "ProgressBar 与 SeekBar 属性": "ProgressBar",
    "LinearLayout": "LinearLayout",
    "FrameLayout": "FrameLayout",
    "RelativeLayout": "RelativeLayout",
    "ScrollView": "ScrollView",
    "CardView": "CardView",
    "AppBarLayout": "AppBarLayout",
    "Toolbar": "Toolbar",
    "ViewPager 与 TabLayout": "TabLayout",
    "ViewAnimator": "ViewAnimator",
    "CompoundButton": "CompoundButton",
    "Spinner": "Spinner",
    "NumberPicker": "NumberPicker",
    "Calendar 与 DatePicker": "CalendarView",
    "TimePicker": "TimePicker",
    "SearchView": "SearchView",
    "FloatingActionButton": "FloatingActionButton",
    "QuickContactBadge": "QuickContactBadge",
    "Chronometer 与 TextClock": "Chronometer",
    "ConsoleView": "ConsoleView",
    "CanvasView": "CanvasView",
    "SurfaceView 与 VideoView": "SurfaceView",
    "WebView": "WebView",
    "List 与 Grid 属性": "JsListView",
}

# "`tag` 另外支持" lines inside a section switch the target class.
TAG_SWITCH = {
    "viewpager": "ViewPager", "tabs": "TabLayout", "tab": "TabLayout", "calendar": "CalendarView", "datepicker": "DatePicker",
    "timepicker": "TimePicker", "chronometer": "Chronometer", "textclock": "TextClock", "video": "VideoView", "list": "JsListView",
    "grid": "JsGridView", "<SurfaceView>": "SurfaceView",
}

UNSUPPORTED_HEADINGS = {
    "View": ["View"], "ViewGroup": ["ViewGroup"], "TextView 与 TextSwitcher": ["TextView", "TextSwitcher"],
    "ProgressBar": ["ProgressBar"], "DatePicker": ["DatePicker"], "Spinner 不支持属性": ["Spinner"],
}

BACKTICK = re.compile(r"`([^`]+)`")


def cells(line: str) -> list[str]:
    return [cell.strip() for cell in line.strip().strip("|").split("|")]


def names_of(cell: str) -> list[tuple[str, list[str]]]:
    """`a` (`b`, `c`) -> [(a, [b, c])]; `a`, `b` -> [(a, []), (b, [])]."""
    if "(" in cell:
        head, tail = cell.split("(", 1)
        primaries = BACKTICK.findall(head)
        aliases = BACKTICK.findall(tail)
        if not primaries:
            return []
        return [(primaries[0], aliases)] + [(name, []) for name in primaries[1:]]
    return [(name, []) for name in BACKTICK.findall(cell)]


def type_of(cell: str) -> tuple[str, list[str]]:
    text = cell.strip()
    lowered = text.lower()
    values = BACKTICK.findall(text)
    if "boolean" in lowered:
        return "boolean", []
    if text.startswith("整数") or "整数" in text and "尺寸" not in text:
        return "int", []
    if text.startswith("浮点数"):
        return "float", []
    if text.startswith("尺寸"):
        return "dimension", []
    if text.startswith("颜色"):
        return "color", []
    if text.startswith("Drawable"):
        return "drawable", []
    if text.startswith("字符串"):
        return "string", []
    if text.startswith("ID"):
        return "id", []
    if text.startswith("Gravity"):
        return "gravity", []
    if values and re.fullmatch(r"(`[^`]+`\s*,?\s*)+", text):
        return "enum", values
    return "other", values


def parse_tags(ui_md: str) -> list[dict]:
    section = ui_md.split("### 内置标签", 1)[1].split("\n### ", 1)[0]
    tags = []
    for line in section.splitlines():
        if not line.startswith("| `"):
            continue
        tag_cell, class_cell, summary = cells(line)[:3]
        aliases = BACKTICK.findall(tag_cell)
        view_class = BACKTICK.findall(class_cell)[0].split(".")[-1]
        primary = aliases[0]
        if primary == "linear":
            meta = TAG_META["linear"]
        else:
            meta = TAG_META.get(primary) or TAG_META.get(next((a for a in aliases if a in TAG_META), primary))
        if meta is None:
            raise ValueError(f"No palette metadata for tag {primary}")
        category, container, axis = meta
        tags.append({
            "tag": primary, "aliases": aliases, "viewClass": view_class, "category": category,
            "container": container, "axis": axis, "summary": summary,
        })
    return tags


def switch_class(token: str, tag_classes: dict[str, str]) -> str:
    """The class a `tag` 另外支持 line targets: the explicit map, else the tag's Android class (the host's Js* wrapper's parent)."""
    if token in TAG_SWITCH:
        return TAG_SWITCH[token]
    if token.startswith("<") and token.strip("<>") in CHAINS:
        return token.strip("<>")
    view_class = tag_classes.get(token)
    if view_class is None:
        raise ValueError(f"Unknown tag in an attribute section: {token}")
    if view_class.startswith("Js") and CHAINS.get(view_class):
        return CHAINS[view_class][0]
    return view_class


def parse_attributes(doc: str, tag_classes: dict[str, str]) -> tuple[dict, list, dict, dict]:
    classes: dict[str, dict] = {}
    creation_only = []
    unsupported: dict[str, list[str]] = {}
    shorthands: dict[str, str] = {}
    current_class = None
    mode = None
    group = ""
    for raw in doc.splitlines():
        line = raw.rstrip()
        if line.startswith("## ") or line.startswith("### "):
            title = line.lstrip("#").strip()
            group = title if line.startswith("### ") else ""
            if title == "AutoJs6 简写转换":
                mode = "shorthand"
                current_class = None
            elif title == "创建阶段专用属性":
                mode = "creation"
                current_class = None
            elif title == "明确不支持的属性":
                mode = "unsupported"
                current_class = None
            elif mode == "unsupported" and title in UNSUPPORTED_HEADINGS:
                current_class = title
            elif title in SECTION_CLASSES:
                mode = "attrs"
                current_class = SECTION_CLASSES[title]
            elif line.startswith("## "):
                mode = None
                current_class = None
            continue
        if mode == "attrs":
            switch = re.match(r"^(?:`([a-z]+)`(?: 和 `[a-z]+`)?|完整类名或短类名 `(<\w+>)`) (?:另外|继续)?支持", line)
            if switch:
                current_class = switch_class(switch.group(1) or switch.group(2), tag_classes)
                continue
            entry = None
            if line.startswith("| `"):
                parts = cells(line)
                entry = (parts[0], parts[1] if len(parts) > 1 else "", parts[2] if len(parts) > 2 else "")
            elif line.startswith("- `"):
                body = line[2:]
                name_part, _, rest = body.partition(" - ")
                entry = (name_part, rest.rstrip("."), "")
            if entry and current_class:
                kind, values = type_of(entry[1])
                for name, aliases in names_of(entry[0]):
                    classes.setdefault(current_class, {"attrs": []})["attrs"].append({
                        "name": name, "aliases": aliases, "type": kind, "values": values,
                        "typeText": entry[1], "summary": entry[2], "group": group,
                    })
        elif mode == "shorthand" and line.startswith("| `"):
            source_cell, target_cell = cells(line)[:2]
            sources = BACKTICK.findall(source_cell)
            targets = BACKTICK.findall(target_cell)
            if any("..." in target for target in targets):
                # "对应 layout_..." rows: the full name is layout_ plus the shorthand.
                for source in sources:
                    if "=" not in source:
                        shorthands[source] = "layout_" + source
            elif len(sources) == len(targets):
                for source, target in zip(sources, targets):
                    if "=" not in source:
                        shorthands[source] = target
            elif len(targets) == 1 and "=" not in sources[0]:
                for source in sources:
                    shorthands[source] = targets[0]
        elif mode == "creation" and line.startswith("| "):
            parts = cells(line)
            if len(parts) < 3 or parts[0].startswith("---") or parts[0] == "视图":
                continue
            tags = BACKTICK.findall(parts[0]) or ["*"]
            kind, values = type_of(parts[2])
            creation_only.append({"tags": tags, "attrs": BACKTICK.findall(parts[1]), "type": kind, "values": values, "summary": parts[2]})
        elif mode == "unsupported" and current_class and line.startswith("`"):
            for class_name in UNSUPPORTED_HEADINGS[current_class]:
                unsupported.setdefault(class_name, []).extend(BACKTICK.findall(line))
    return classes, creation_only, unsupported, shorthands


def build(docs: Path) -> dict:
    ui_md = (docs / "api/ui.md").read_text(encoding="utf-8")
    attrs_md = (docs / "api/uiAttributes.md").read_text(encoding="utf-8")
    tags = parse_tags(ui_md)
    tag_classes = {alias: tag["viewClass"] for tag in tags for alias in tag["aliases"]}
    classes, creation_only, unsupported, shorthands = parse_attributes(attrs_md, tag_classes)
    for tag in tags:
        if tag["viewClass"] not in CHAINS:
            raise ValueError(f"No inheritance chain for {tag['viewClass']} (tag {tag['tag']})")
    for class_name in classes:
        if class_name not in CHAINS:
            raise ValueError(f"No inheritance chain for attribute class {class_name}")
    chains = {name: full_chain(name) for name in CHAINS}
    try:
        commit = subprocess.run(["git", "-C", str(docs), "rev-parse", "--short", "HEAD"], capture_output=True, text=True, check=True).stdout.strip()
    except (OSError, subprocess.CalledProcessError):
        commit = "unknown"
    return {
        "schemaVersion": 1,
        "docsCommit": commit,
        "tags": tags,
        "chains": chains,
        "classes": {name: classes[name] for name in sorted(classes)},
        "shorthands": dict(sorted(shorthands.items())),
        "creationOnly": creation_only,
        "unsupported": {name: sorted(set(values)) for name, values in sorted(unsupported.items())},
        "gravity": ["left", "start", "textStart", "top", "right", "end", "textEnd", "bottom", "center_horizontal", "center_vertical", "center"],
        "dimensionUnits": ["px", "dp", "dip", "sp", "pt", "in", "mm"],
    }


def full_chain(name: str) -> list[str]:
    chain = []
    cursor = name
    while CHAINS.get(cursor):
        parent = CHAINS[cursor][0]
        chain.append(parent)
        cursor = parent
    return chain


def render(catalog: dict) -> str:
    return json.dumps(catalog, ensure_ascii=False, indent=2) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--docs", required=True, type=Path, help="Explicit AutoJs6-Documentation checkout path")
    parser.add_argument("--check", action="store_true", help="Compare the generated catalog with the committed file")
    args = parser.parse_args()
    try:
        text = render(build(args.docs.resolve()))
    except (OSError, ValueError, KeyError, IndexError) as error:
        print(f"ui catalog: {error}", file=sys.stderr)
        return 1
    if args.check:
        current = OUTPUT.read_text(encoding="utf-8") if OUTPUT.is_file() else ""
        if current != text:
            print(f"ui catalog: {OUTPUT} differs from the documentation", file=sys.stderr)
            return 1
        print("ui catalog: checked")
        return 0
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(text, encoding="utf-8")
    catalog = json.loads(text)
    attrs = sum(len(entry["attrs"]) for entry in catalog["classes"].values())
    print(f"ui catalog: {len(catalog['tags'])} tags, {attrs} attributes in {len(catalog['classes'])} classes, docs {catalog['docsCommit']}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
