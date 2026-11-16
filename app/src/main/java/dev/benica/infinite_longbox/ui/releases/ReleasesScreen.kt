package dev.benica.infinite_longbox.ui.releases

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.database.daos.ReleaseRow
import dev.benica.infinite_longbox.viewmodels.PublisherOption
import dev.benica.infinite_longbox.viewmodels.ReleaseSection
import dev.benica.infinite_longbox.viewmodels.ReleasesView
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/** The first year the picker offers. */
private const val FIRST_YEAR = 1930

/** A month's releases: a bar to move between months, a publisher filter, then the issues under their weeks. [view] is null while loading. */
@Composable
fun ReleasesScreen(
    month: YearMonth,
    view: ReleasesView?,
    picked: Set<Int>,
    loading: Boolean,
    onPublishers: (Set<Int>) -> Unit,
    onStep: (Long) -> Unit,
    onPick: (YearMonth) -> Unit,
    onIssue: (Int) -> Unit,
) {
    var picking by remember { mutableStateOf(false) }
    var choosingPublishers by remember { mutableStateOf(false) }
    val sections = view?.sections
    val margin = dimensionResource(R.dimen.margin_wide)
    val list = rememberLazyListState()
    LaunchedEffect(month) { list.scrollToItem(0) }

    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onStep(-1) }) {
                Text("‹", style = MaterialTheme.typography.headlineMedium)
            }
            Text(
                monthLabel(month),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).clickable { picking = true }.padding(vertical = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconButton(onClick = { onStep(1) }) {
                Text("›", style = MaterialTheme.typography.headlineMedium)
            }
        }
        if (view != null) {
            FilterChip(
                selected = picked.isNotEmpty(),
                onClick = { choosingPublishers = true },
                label = { Text(publisherLabel(view.publishers, picked)) },
                modifier = Modifier.padding(horizontal = margin),
            )
        }
        // The bar under the toolbar shows the fetching
        HorizontalDivider()
        when {
            sections == null || (loading && sections.isEmpty()) -> Unit
            sections.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(stringResource(R.string.releases_empty), color = colorResource(R.color.text_secondary))
            }
            else -> LazyColumn(state = list, modifier = Modifier.fillMaxSize()) {
                sections.forEach { section ->
                    item(key = "h${section.weekStart}") { SectionHeader(section, margin) }
                    items(section.rows, key = { it.issueId }) { row -> ReleaseItem(row, onIssue, margin) }
                }
            }
        }
    }
    }
    if (choosingPublishers && view != null) {
        PublisherPicker(view.publishers, picked, onDone = { onPublishers(it); choosingPublishers = false }, onDismiss = { choosingPublishers = false })
    }
    if (picking) MonthPicker(month, onPick = { onPick(it); picking = false }, onDismiss = { picking = false })
}

private fun monthLabel(month: YearMonth) =
    "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}"

@Composable
private fun SectionHeader(section: ReleaseSection, margin: androidx.compose.ui.unit.Dp) {
    Column(Modifier.fillMaxWidth().background(colorResource(R.color.surface_sunken)).padding(horizontal = margin, vertical = 8.dp)) {
        val start = section.weekStart
        Text(
            if (start != null) stringResource(R.string.releases_week_of, start.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
            else stringResource(R.string.releases_elsewhere),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        if (start == null) {
            Text(
                stringResource(R.string.releases_elsewhere_hint),
                style = MaterialTheme.typography.bodySmall,
                color = colorResource(R.color.text_secondary),
            )
        }
    }
}

@Composable
private fun ReleaseItem(row: ReleaseRow, onIssue: (Int) -> Unit, margin: androidx.compose.ui.unit.Dp) {
    Row(
        Modifier.fillMaxWidth().clickable { onIssue(row.issueId) }.padding(horizontal = margin, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text("${row.series} #${row.number}", maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
            row.publisher?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = colorResource(R.color.text_secondary), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun MonthPicker(current: YearMonth, onPick: (YearMonth) -> Unit, onDismiss: () -> Unit) {
    var year by remember { mutableStateOf(current.year) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.releases_pick)) },
        text = {
            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { year -= 1 }) { Text("‹", style = MaterialTheme.typography.headlineSmall) }
                    Box(Modifier.weight(1f), Alignment.Center) {
                        var open by remember { mutableStateOf(false) }
                        TextButton(onClick = { open = true }) {
                            Text("$year ▾", style = MaterialTheme.typography.titleMedium)
                        }
                        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                            val years = (YearMonth.now().year + 1 downTo FIRST_YEAR).toList()
                            val yearList = rememberLazyListState(years.indexOf(year).coerceAtLeast(0))
                            LazyColumn(state = yearList, modifier = Modifier.height(280.dp).width(120.dp)) {
                                items(years) { y ->
                                    DropdownMenuItem(
                                        text = {
                                            Text("$y", fontWeight = if (y == year) FontWeight.Bold else FontWeight.Normal)
                                        },
                                        onClick = { year = y; open = false },
                                    )
                                }
                            }
                        }
                    }
                    IconButton(onClick = { year += 1 }) { Text("›", style = MaterialTheme.typography.headlineSmall) }
                }
                (1..12).chunked(3).forEach { months ->
                    Row(Modifier.fillMaxWidth()) {
                        months.forEach { m ->
                            TextButton(onClick = { onPick(YearMonth.of(year, m)) }, modifier = Modifier.weight(1f)) {
                                Text(java.time.Month.of(m).getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                    fontWeight = if (YearMonth.of(year, m) == current) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.releases_close)) } },
    )
}

@Composable
private fun publisherLabel(options: List<PublisherOption>, picked: Set<Int>): String {
    val names = options.filter { it.id in picked }.map { it.name }
    return when {
        names.isEmpty() -> stringResource(R.string.releases_all_publishers)
        names.size == 1 -> names.first()
        else -> "${names.first()} +${names.size - 1}"
    }
}

@Composable
private fun PublisherPicker(options: List<PublisherOption>, picked: Set<Int>, onDone: (Set<Int>) -> Unit, onDismiss: () -> Unit) {
    var chosen by remember { mutableStateOf(picked) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.releases_publishers)) },
        text = {
            LazyColumn {
                items(options, key = { it.id }) { option ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            chosen = if (option.id in chosen) chosen - option.id else chosen + option.id
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = option.id in chosen, onCheckedChange = null, modifier = Modifier.padding(12.dp))
                        Text(option.name.ifEmpty { "?" }, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${option.count}", color = colorResource(R.color.text_secondary), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onDone(chosen) }) { Text(stringResource(R.string.releases_done)) } },
        dismissButton = { TextButton(onClick = { onDone(emptySet()) }) { Text(stringResource(R.string.releases_show_all)) } },
    )
}
