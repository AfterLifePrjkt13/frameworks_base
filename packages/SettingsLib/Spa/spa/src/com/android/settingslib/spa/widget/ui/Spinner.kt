/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settingslib.spa.widget.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.framework.theme.SettingsTheme

data class SpinnerOption(
    val id: Int,
    val text: String,
)

@Composable
fun Spinner(options: List<SpinnerOption>, selectedId: Int?, setId: (id: Int) -> Unit) {
    if (options.isEmpty()) {
        return
    }

    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .padding(SettingsDimension.itemPadding)
            .selectableGroup(),
    ) {
        val contentPadding = PaddingValues(horizontal = SettingsDimension.itemPaddingEnd)
        Button(
            onClick = { expanded = true },
            modifier = Modifier.height(36.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SettingsTheme.colorScheme.spinnerHeaderContainer,
                contentColor = SettingsTheme.colorScheme.onSpinnerHeaderContainer,
            ),
            contentPadding = contentPadding,
        ) {
            SpinnerText(options.find { it.id == selectedId })
            Icon(
                imageVector = when {
                    expanded -> Icons.Outlined.ArrowDropUp
                    else -> Icons.Outlined.ArrowDropDown
                },
                contentDescription = null,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(SettingsTheme.colorScheme.spinnerItemContainer),
            offset = DpOffset(x = 0.dp, y = 4.dp),
        ) {
            for (option in options) {
                DropdownMenuItem(
                    text = {
                        SpinnerText(
                            option = option,
                            modifier = Modifier.padding(end = 24.dp),
                            color = SettingsTheme.colorScheme.onSpinnerItemContainer,
                        )
                    },
                    onClick = {
                        expanded = false
                        setId(option.id)
                    },
                    contentPadding = contentPadding,
                )
            }
        }
    }
}

@Composable
private fun SpinnerText(
    option: SpinnerOption?,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    Text(
        text = option?.text ?: "",
        modifier = modifier.padding(end = SettingsDimension.itemPaddingEnd),
        color = color,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Preview(showBackground = true)
@Composable
private fun SpinnerPreview() {
    SettingsTheme {
        var selectedId by rememberSaveable { mutableStateOf(1) }
        Spinner(
            options = (1..3).map { SpinnerOption(id = it, text = "Option $it") },
            selectedId = selectedId,
            setId = { selectedId = it },
        )
    }
}
