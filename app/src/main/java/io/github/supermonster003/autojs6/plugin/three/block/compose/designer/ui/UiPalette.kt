package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.supermonster003.autojs6.plugin.three.block.compose.R
import io.github.supermonster003.autojs6.plugin.three.block.compose.app.StandalonePalette
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiCatalog
import io.github.supermonster003.autojs6.plugin.three.block.compose.designer.model.UiTemplates

/** The component palette of a ui (XML) project (roadmap P10.4): the catalog's tags by the documentation's categories. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UiPalette(catalog: UiCatalog, palette: StandalonePalette, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val categories = remember(catalog) { UiTemplates.categories(catalog) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(palette.surface)) {
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).padding(horizontal = 16.dp).testTag("designer-palette")) {
            categories.forEach { category ->
                item(key = "category-" + category.id) {
                    Text(stringResource(categoryTitle(category.id)), color = Color(palette.muted), fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                }
                items(category.tags, key = { it }) { tag ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onPick(tag) }.padding(vertical = 10.dp).testTag("designer-palette-$tag")) {
                        Text(tag, color = Color(palette.text), fontSize = 15.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                        Text(catalog.tag(tag)?.viewClass.orEmpty(), color = Color(palette.muted), fontSize = 12.sp)
                    }
                    HorizontalDivider(color = Color(palette.divider))
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

internal fun categoryTitle(id: String): Int = when (id) {
    "layout" -> R.string.designer_ui_category_layout
    "text" -> R.string.designer_ui_category_text
    "button" -> R.string.designer_ui_category_button
    "input" -> R.string.designer_ui_category_input
    "choice" -> R.string.designer_ui_category_choice
    "progress" -> R.string.designer_ui_category_progress
    "image" -> R.string.designer_ui_category_image
    "media" -> R.string.designer_ui_category_media
    "list" -> R.string.designer_ui_category_list
    else -> R.string.designer_ui_category_other
}
