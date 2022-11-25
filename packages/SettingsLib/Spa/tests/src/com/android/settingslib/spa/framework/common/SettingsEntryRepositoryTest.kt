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

package com.android.settingslib.spa.framework.common

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.spa.tests.testutils.SpaEnvironmentForTest
import com.android.settingslib.spa.tests.testutils.SppHome
import com.android.settingslib.spa.tests.testutils.SppLayer1
import com.android.settingslib.spa.tests.testutils.SppLayer2
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsEntryRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val spaEnvironment = SpaEnvironmentForTest(context)
    private val entryRepository by spaEnvironment.entryRepository

    @Test
    fun testGetPageWithEntry() {
        val pageWithEntry = entryRepository.getAllPageWithEntry()
        assertThat(pageWithEntry.size).isEqualTo(3)
        assertThat(
            entryRepository.getPageWithEntry(getUniquePageId("SppHome"))
                ?.entries?.size
        ).isEqualTo(1)
        assertThat(
            entryRepository.getPageWithEntry(getUniquePageId("SppLayer1"))
                ?.entries?.size
        ).isEqualTo(3)
        assertThat(
            entryRepository.getPageWithEntry(getUniquePageId("SppLayer2"))
                ?.entries?.size
        ).isEqualTo(2)
        assertThat(entryRepository.getPageWithEntry(getUniquePageId("SppWithParam"))).isNull()
    }

    @Test
    fun testGetEntry() {
        val entry = entryRepository.getAllEntries()
        assertThat(entry.size).isEqualTo(7)
        assertThat(
            entryRepository.getEntry(
                getUniqueEntryId(
                    "ROOT",
                    SppHome.createSettingsPage(),
                    SettingsPage.createNull(),
                    SppHome.createSettingsPage(),
                )
            )
        ).isNotNull()
        assertThat(
            entryRepository.getEntry(
                getUniqueEntryId(
                    "INJECT",
                    SppLayer1.createSettingsPage(),
                    SppHome.createSettingsPage(),
                    SppLayer1.createSettingsPage(),
                )
            )
        ).isNotNull()
        assertThat(
            entryRepository.getEntry(
                getUniqueEntryId(
                    "INJECT",
                    SppLayer2.createSettingsPage(),
                    SppLayer1.createSettingsPage(),
                    SppLayer2.createSettingsPage(),
                )
            )
        ).isNotNull()
        assertThat(
            entryRepository.getEntry(
                getUniqueEntryId("Layer1Entry1", SppLayer1.createSettingsPage())
            )
        ).isNotNull()
        assertThat(
            entryRepository.getEntry(
                getUniqueEntryId("Layer1Entry2", SppLayer1.createSettingsPage())
            )
        ).isNotNull()
        assertThat(
            entryRepository.getEntry(
                getUniqueEntryId("Layer2Entry1", SppLayer2.createSettingsPage())
            )
        ).isNotNull()
        assertThat(
            entryRepository.getEntry(
                getUniqueEntryId("Layer2Entry2", SppLayer2.createSettingsPage())
            )
        ).isNotNull()
    }

    @Test
    fun testGetEntryPath() {
        assertThat(
            entryRepository.getEntryPathWithDisplayName(
                getUniqueEntryId("Layer2Entry1", SppLayer2.createSettingsPage())
            )
        ).containsExactly("Layer2Entry1", "INJECT_SppLayer2", "INJECT_SppLayer1", "ROOT_SppHome")
            .inOrder()

        assertThat(
            entryRepository.getEntryPathWithTitle(
                getUniqueEntryId("Layer2Entry2", SppLayer2.createSettingsPage()),
                "entryTitle"
            )
        ).containsExactly("entryTitle", "SppLayer2", "TitleLayer1", "TitleHome").inOrder()

        assertThat(
            entryRepository.getEntryPathWithDisplayName(
                getUniqueEntryId(
                    "INJECT",
                    SppLayer1.createSettingsPage(),
                    SppHome.createSettingsPage(),
                    SppLayer1.createSettingsPage(),
                )
            )
        ).containsExactly("INJECT_SppLayer1", "ROOT_SppHome").inOrder()

        assertThat(
            entryRepository.getEntryPathWithTitle(
                getUniqueEntryId(
                    "INJECT",
                    SppLayer2.createSettingsPage(),
                    SppLayer1.createSettingsPage(),
                    SppLayer2.createSettingsPage(),
                ),
                "defaultTitle"
            )
        ).containsExactly("SppLayer2", "TitleLayer1", "TitleHome").inOrder()
    }
}
