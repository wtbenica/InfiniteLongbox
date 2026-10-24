/*
 * Copyright (c) 2023. Wesley T. Benica
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.benica.infinite_longbox.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.benica.infinite_longbox.R
import dev.benica.infinite_longbox.ui.theme.Archivo
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** A list to choose from. */
@Composable
fun ChoiceDialog(
    title: Int,
    options: List<String>,
    cancelLabel: Int?,
    onChoose: (Int?) -> Unit,
    dismiss: () -> Unit,
) {
    fun finish(choice: Int?) {
        onChoose(choice)
        dismiss()
    }
    AlertDialog(
        onDismissRequest = { finish(null) },
        title = { Text(stringResource(title)) },
        text = {
            Column {
                options.forEachIndexed { index, option ->
                    Text(
                        option,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { finish(index) }
                            .padding(vertical = 12.dp),
                        color = colorResource(R.color.text_primary),
                        fontFamily = Archivo,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = cancelLabel?.let { label ->
            { TextButton(onClick = { finish(null) }) { Text(stringResource(label)) } }
        },
    )
}

/** Asks for a number. [onResult] gets what was typed, or null when cancelled. */
@Composable
fun NumberDialog(
    title: Int,
    message: Int,
    hint: Int,
    onResult: (String?) -> Unit,
    dismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    fun finish(result: String?) {
        onResult(result)
        dismiss()
    }
    AlertDialog(
        onDismissRequest = { finish(null) },
        title = { Text(stringResource(title)) },
        text = {
            Column {
                Text(stringResource(message), Modifier.padding(bottom = 12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { finish(text) }) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = { finish(null) }) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

/** Picks a date between [min] and [max], starting at [current]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDialog(
    current: LocalDate,
    min: LocalDate,
    max: LocalDate,
    onPicked: (LocalDate) -> Unit,
    dismiss: () -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = current.coerceIn(min, max).millis(),
        yearRange = min.year..max.year,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis in min.millis()..max.millis()
            override fun isSelectableYear(year: Int) = year in min.year..max.year
        },
    )
    DatePickerDialog(
        onDismissRequest = dismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPicked(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                dismiss()
            }) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = dismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    ) { DatePicker(state) }
}

/** The credits and licences, over the whole screen. */
@Composable
fun AboutDialog(dismiss: () -> Unit) {
    Dialog(
        onDismissRequest = dismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) { AboutScreen(onDismiss = dismiss) }
}

private fun LocalDate.millis() = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun LocalDate.coerceIn(min: LocalDate, max: LocalDate) = when {
    isBefore(min) -> min
    isAfter(max) -> max
    else -> this
}
