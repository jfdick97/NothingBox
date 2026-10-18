package de.jfd.straingauge

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields

private val weekDateFormat =
    DateTimeFormatter.ofPattern("dd.MM.yyyy")

fun mondayOf(date: LocalDate): LocalDate =
    date.with(
        TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
    )

fun weekRangeLabel(start: LocalDate): String =
    "${start.format(weekDateFormat)} – " +
            start.plusDays(6).format(weekDateFormat)

private fun weekNumberLabel(start: LocalDate): String {
    val fields = WeekFields.ISO

    val number = start.get(fields.weekOfWeekBasedYear())
    val year = start.get(fields.weekBasedYear())

    return "KW $number · $year"
}

@Composable
fun WeekSelector(
    selectedMonday: LocalDate,
    onWeekSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var showDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val selectedWeek = mondayOf(selectedMonday)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIconButton(
            icon = Icons.Filled.ChevronLeft,
            label = "Vorherige Woche",
            onClick = {
                onWeekSelected(selectedWeek.minusWeeks(1))
            },
            enabled = enabled
        )

        Surface(
            onClick = { showDialog = true },
            enabled = enabled,
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 8.dp,
                    vertical = 10.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = "Woche auswählen"
                )

                Text(
                    text = weekNumberLabel(selectedWeek),
                    style = MaterialTheme.typography.labelLarge
                )

                Text(
                    text = weekRangeLabel(selectedWeek),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        AppIconButton(
            icon = Icons.Filled.ChevronRight,
            label = "Nächste Woche",
            onClick = {
                onWeekSelected(selectedWeek.plusWeeks(1))
            },
            enabled = enabled
        )
    }

    if (showDialog) {
        WeekPickerDialog(
            selectedMonday = selectedWeek,
            onSelected = {
                onWeekSelected(it)
                showDialog = false
            },
            onDismiss = {
                showDialog = false
            }
        )
    }
}

@Composable
private fun WeekPickerDialog(
    selectedMonday: LocalDate,
    onSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val fields = WeekFields.ISO
    val selectedYear = selectedMonday.get(fields.weekBasedYear())

    var displayedYear by rememberSaveable {
        mutableIntStateOf(selectedYear)
    }

    // Die Woche mit dem 4. Januar ist immer ISO-Kalenderwoche 1.
    val firstWeek = mondayOf(
        LocalDate.of(displayedYear, 1, 4)
    )

    val nextYearFirstWeek = mondayOf(
        LocalDate.of(displayedYear + 1, 1, 4)
    )

    val weeks = generateSequence(firstWeek) {
        it.plusWeeks(1)
    }.takeWhile {
        it < nextYearFirstWeek
    }.toList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Woche auswählen")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {
                    AppIconButton(
                        icon = Icons.Filled.ChevronLeft,
                        label = "Vorheriges Jahr",
                        onClick = { displayedYear-- }
                    )

                    Text(
                        text = displayedYear.toString(),
                        style = MaterialTheme.typography.titleLarge
                    )

                    AppIconButton(
                        icon = Icons.Filled.ChevronRight,
                        label = "Nächstes Jahr",
                        onClick = { displayedYear++ }
                    )
                }

                key(displayedYear) {
                    val initialIndex =
                        if (displayedYear == selectedYear) {
                            (
                                    selectedMonday.get(
                                        fields.weekOfWeekBasedYear()
                                    ) - 1
                                    ).coerceIn(0, weeks.lastIndex)
                        } else {
                            0
                        }

                    val listState = rememberLazyListState(
                        initialFirstVisibleItemIndex = initialIndex
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        state = listState,
                        verticalArrangement =
                            Arrangement.spacedBy(6.dp)
                    ) {
                        items(
                            count = weeks.size,
                            key = { weeks[it].toString() }
                        ) { index ->
                            val week = weeks[index]
                            val selected = week == selectedMonday

                            Surface(
                                onClick = {
                                    onSelected(week)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                color = if (selected) {
                                    MaterialTheme.colorScheme
                                        .secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                contentColor = if (selected) {
                                    MaterialTheme.colorScheme
                                        .onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = weekNumberLabel(week),
                                        style = MaterialTheme.typography
                                            .titleSmall
                                    )

                                    Text(
                                        text = weekRangeLabel(week),
                                        style = MaterialTheme.typography
                                            .bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            AppIconButton(
                icon = Icons.Filled.Close,
                label = "Schließen",
                onClick = onDismiss
            )
        },
        dismissButton = {
            AppIconButton(
                icon = Icons.Filled.Today,
                label = "Aktuelle Woche auswählen",
                onClick = {
                    onSelected(mondayOf(LocalDate.now()))
                }
            )
        }
    )
}