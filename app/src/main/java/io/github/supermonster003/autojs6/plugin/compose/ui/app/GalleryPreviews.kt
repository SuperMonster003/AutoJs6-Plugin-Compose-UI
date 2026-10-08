package io.github.supermonster003.autojs6.plugin.compose.ui.app

import io.github.supermonster003.autojs6.plugin.compose.ui.R
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Live Material 3 showcase of one catalog component, rendered by this process. These are plain
 * Compose demonstrations of the same components the renderer uses; the script path is only executed
 * by the host. Every branch keeps its own remembered state so the preview is interactive.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GalleryPreview(component: String) {
    Box(modifier = Modifier.fillMaxWidth().testTag("preview-$component")) {
        when (component) {
            "Column" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("First"); Text("Second"); Text("Third")
            }
            "Row" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Star, null); Text("Starred"); Spacer(Modifier.weight(1f)); Text("End")
            }
            "Box" -> Box(modifier = Modifier.size(160.dp, 120.dp).background(Color(0xFFE3F2FD), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Text("Centered", color = Color(0xFF1D1B20)) }
            "Spacer" -> Column { Text("Above the spacer"); Spacer(Modifier.height(24.dp)); Text("Below the spacer") }
            "LazyColumn" -> LazyColumn(modifier = Modifier.height(160.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(100) { Text("Row $it") } }
            "LazyRow" -> LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(20) { Card { Text("Card $it", modifier = Modifier.padding(16.dp)) } } }
            "LazyVerticalGrid" -> LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.height(160.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(30) { Card { Text("$it", modifier = Modifier.padding(16.dp)) } }
            }
            "HorizontalPager" -> {
                val state = rememberPagerState { 3 }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Page ${state.currentPage + 1} of 3")
                    HorizontalPager(state = state, modifier = Modifier.height(120.dp)) { page ->
                        Card(modifier = Modifier.fillMaxSize()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(listOf("First", "Second", "Third")[page] + " page") } }
                    }
                }
            }
            "Surface" -> Surface(color = Color(0xFFFFF8E1), shape = RoundedCornerShape(12.dp), tonalElevation = 2.dp) { Text("A tinted surface", color = Color(0xFF1D1B20), modifier = Modifier.padding(16.dp)) }
            "Card" -> Card(modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Card title", style = MaterialTheme.typography.titleMedium); Text("Supporting text inside the card") } }
            "HorizontalDivider" -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Above"); HorizontalDivider(); Text("Below") }
            "Scaffold" -> Scaffold(modifier = Modifier.height(200.dp), topBar = { TopAppBar(title = { Text("Scaffold") }) }) { padding -> Text("Content below the app bar", modifier = Modifier.padding(padding).padding(16.dp)) }
            "TopAppBar" -> TopAppBar(
                title = { Text("Title") },
                navigationIcon = { IconButton(onClick = {}) { Icon(Icons.Filled.Menu, null) } },
                actions = { IconButton(onClick = {}) { Icon(Icons.Filled.MoreVert, null) } },
            )
            "AndroidView" -> {
                val color = MaterialTheme.colorScheme.onSurface
                AndroidView(factory = { context -> TextView(context).apply { text = context.getString(R.string.gallery_android_view_sample); textSize = 18f } }, update = { it.setTextColor(android.graphics.Color.argb((color.alpha * 255).toInt(), (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())) })
            }
            "Text" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Headline", style = MaterialTheme.typography.headlineSmall)
                Text("Body text with a custom color", color = Color(0xFF1565C0))
                Text("A very long single line that is clipped with an ellipsis at the end", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            "Icon" -> Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { Icon(Icons.Filled.Home, null); Icon(Icons.Filled.Favorite, null, tint = Color(0xFFE53935)); Icon(Icons.Filled.Settings, null) }
            "Image" -> Image(painter = rememberVectorPainter(Icons.Filled.Star), contentDescription = "Sample image", modifier = Modifier.size(96.dp))
            "Badge" -> Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                BadgedBox(badge = { Badge { Text("3") } }) { Icon(Icons.Filled.Email, null) }
                BadgedBox(badge = { Badge() }) { Icon(Icons.Filled.Notifications, null) }
            }
            "Tooltip" -> TooltipBox(positionProvider = TooltipDefaults.rememberTooltipPositionProvider(), tooltip = { PlainTooltip { Text("Long-press to show this tooltip") } }, state = rememberTooltipState()) {
                Button(onClick = {}) { Text("Hover me") }
            }
            "Button" -> { var count by remember { mutableIntStateOf(0) }; Button(onClick = { count++ }) { Text("Clicked $count times") } }
            "ElevatedButton" -> ElevatedButton(onClick = {}) { Text("Elevated") }
            "FilledTonalButton" -> FilledTonalButton(onClick = {}) { Text("Filled tonal") }
            "OutlinedButton" -> OutlinedButton(onClick = {}) { Text("Outlined") }
            "TextButton" -> TextButton(onClick = {}) { Text("Text button") }
            "IconButton" -> IconButton(onClick = {}) { Icon(Icons.Filled.Favorite, null) }
            "FloatingActionButton" -> FloatingActionButton(onClick = {}) { Icon(Icons.Filled.Add, null) }
            "SegmentedButton", "SegmentedButtonItem" -> {
                var selected by remember { mutableIntStateOf(0) }
                val labels = listOf("Day", "Week", "Month")
                SingleChoiceSegmentedButtonRow {
                    labels.forEachIndexed { index, label ->
                        SegmentedButton(selected = selected == index, onClick = { selected = index }, shape = SegmentedButtonDefaults.itemShape(index, labels.size)) { Text(label) }
                    }
                }
            }
            "Switch" -> { var enabled by remember { mutableStateOf(true) }; Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { Text(if (enabled) "Enabled" else "Disabled", modifier = Modifier.weight(1f)); Switch(checked = enabled, onCheckedChange = { enabled = it }) } }
            "Checkbox" -> { var agreed by remember { mutableStateOf(false) }; Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = agreed, onCheckedChange = { agreed = it }); Text("I agree to the terms") } }
            "RadioButton" -> {
                var size by remember { mutableStateOf("small") }
                Column { listOf("small" to "Small", "large" to "Large").forEach { (value, label) -> Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = size == value, onClick = { size = value }); Text(label) } } }
            }
            "Slider" -> { var volume by remember { mutableFloatStateOf(40f) }; Column { Text("Volume: ${volume.toInt()}"); Slider(value = volume, onValueChange = { volume = it }, valueRange = 0f..100f, steps = 9) } }
            "AssistChip" -> AssistChip(onClick = {}, label = { Text("Set a reminder") }, leadingIcon = { Icon(Icons.Filled.DateRange, null, modifier = Modifier.size(18.dp)) })
            "FilterChip" -> { var done by remember { mutableStateOf(true) }; var open by remember { mutableStateOf(false) }; Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(selected = done, onClick = { done = !done }, label = { Text("Done") }); FilterChip(selected = open, onClick = { open = !open }, label = { Text("Open") }) } }
            "InputChip" -> { var selected by remember { mutableStateOf(false) }; InputChip(selected = selected, onClick = { selected = !selected }, label = { Text("Android") }, leadingIcon = { Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp)) }) }
            "TextField" -> { var name by remember { mutableStateOf("") }; Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { TextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, placeholder = { Text("Your name") }, singleLine = true, modifier = Modifier.fillMaxWidth()); Text("Hello, ${name.ifEmpty { "stranger" }}") } }
            "OutlinedTextField" -> { var email by remember { mutableStateOf("") }; OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, isError = email.isNotEmpty() && '@' !in email, supportingText = { Text("Enter a valid address") }, modifier = Modifier.fillMaxWidth()) }
            "SearchBar" -> {
                var query by remember { mutableStateOf("") }
                var expanded by remember { mutableStateOf(false) }
                SearchBar(
                    inputField = { SearchBarDefaults.InputField(query = query, onQueryChange = { query = it }, onSearch = { expanded = false }, expanded = expanded, onExpandedChange = { expanded = it }, placeholder = { Text("Search") }) },
                    expanded = expanded, onExpandedChange = { expanded = it },
                ) { Text("Suggestions appear here", modifier = Modifier.padding(16.dp)) }
            }
            "DatePicker" -> DatePicker(state = rememberDatePickerState(), modifier = Modifier.fillMaxWidth())
            "TimePicker" -> TimePicker(state = rememberTimePickerState(initialHour = 9, initialMinute = 30, is24Hour = true))
            "CircularProgressIndicator" -> Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) { CircularProgressIndicator(progress = { 0.6f }); CircularProgressIndicator() }
            "LinearProgressIndicator" -> Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) { LinearProgressIndicator(progress = { 0.4f }, modifier = Modifier.fillMaxWidth()); LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            "PullToRefresh" -> {
                var refreshing by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                PullToRefreshBox(isRefreshing = refreshing, onRefresh = { scope.launch { refreshing = true; delay(1500); refreshing = false } }, modifier = Modifier.height(160.dp)) {
                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(30) { Text("Pull down to refresh $it") } }
                }
            }
            "NavigationBar", "NavigationBarItem" -> {
                var tab by remember { mutableIntStateOf(0) }
                NavigationBar {
                    listOf(Icons.Filled.Home to "Home", Icons.Filled.Search to "Search", Icons.Filled.Person to "Profile").forEachIndexed { index, (icon, label) ->
                        NavigationBarItem(selected = tab == index, onClick = { tab = index }, icon = { Icon(icon, null) }, label = { Text(label) })
                    }
                }
            }
            "NavigationRail", "NavigationRailItem" -> {
                var page by remember { mutableIntStateOf(0) }
                Row { NavigationRail(modifier = Modifier.height(200.dp), header = { Icon(Icons.Filled.Menu, null) }) {
                    NavigationRailItem(selected = page == 0, onClick = { page = 0 }, icon = { Icon(Icons.Filled.Home, null) }, label = { Text("Home") })
                    NavigationRailItem(selected = page == 1, onClick = { page = 1 }, icon = { Icon(Icons.Filled.Settings, null) }, label = { Text("Settings") })
                }; Text("Page: ${if (page == 0) "home" else "settings"}", modifier = Modifier.padding(16.dp)) }
            }
            "NavigationDrawer", "NavigationDrawerItem" -> {
                var item by remember { mutableIntStateOf(0) }
                ModalDrawerSheet(modifier = Modifier.height(180.dp).fillMaxWidth()) {
                    NavigationDrawerItem(label = { Text("Inbox") }, icon = { Icon(Icons.Filled.Email, null) }, badge = { Text("12") }, selected = item == 0, onClick = { item = 0 })
                    NavigationDrawerItem(label = { Text("Sent") }, icon = { Icon(Icons.Filled.Share, null) }, selected = item == 1, onClick = { item = 1 })
                }
            }
            "TabRow", "Tab" -> {
                var index by remember { mutableIntStateOf(0) }
                Column { TabRow(selectedTabIndex = index) { listOf("Overview", "Details", "History").forEachIndexed { i, label -> Tab(selected = index == i, onClick = { index = i }, text = { Text(label) }) } }; Text("Tab ${index + 1}", modifier = Modifier.padding(16.dp)) }
            }
            "AlertDialog" -> {
                var open by remember { mutableStateOf(false) }
                Button(onClick = { open = true }) { Text("Show dialog") }
                if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text("Delete file?") }, text = { Text("This cannot be undone.") },
                    confirmButton = { TextButton(onClick = { open = false }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } })
            }
            "ModalBottomSheet" -> {
                var open by remember { mutableStateOf(false) }
                Button(onClick = { open = true }) { Text("Show bottom sheet") }
                if (open) ModalBottomSheet(onDismissRequest = { open = false }) { Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Sheet title", style = MaterialTheme.typography.titleMedium); Button(onClick = { open = false }) { Text("Close") } } }
            }
            "DropdownMenu", "DropdownMenuItem" -> {
                var open by remember { mutableStateOf(false) }
                Box { Button(onClick = { open = true }) { Text("Open menu") }
                    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                        DropdownMenuItem(text = { Text("Edit") }, onClick = { open = false })
                        DropdownMenuItem(text = { Text("Share") }, leadingIcon = { Icon(Icons.Filled.Share, null) }, onClick = { open = false })
                        DropdownMenuItem(text = { Text("Delete") }, leadingIcon = { Icon(Icons.Filled.Delete, null) }, onClick = {}, enabled = false)
                    } }
            }
            "Snackbar" -> Snackbar(action = { TextButton(onClick = {}) { Text("Undo") } }) { Text("Saved") }
            else -> Text(component, fontWeight = FontWeight.Medium, fontSize = 16.sp)
        }
    }
}
