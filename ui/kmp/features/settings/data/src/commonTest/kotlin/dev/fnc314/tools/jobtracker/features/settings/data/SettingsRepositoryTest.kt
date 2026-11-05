package dev.fnc314.tools.jobtracker.features.settings.data

import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.core.data.settings.SettingsStorage
import dev.fnc314.tools.jobtracker.core.data.settings.UiMode
import dev.fnc314.tools.jobtracker.features.settings.domain.usecase.ObserveSettingsUseCase
import dev.fnc314.tools.jobtracker.features.settings.domain.usecase.UpdateSettingsUseCase
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsRepositoryTest {

    private class FakeSettingsStorage : SettingsStorage {
        val map = mutableMapOf<String, String>()
        override fun getString(key: String): String? = map[key]
        override fun putString(key: String, value: String) {
            map[key] = value
        }
    }

    @Test
    fun testUpdateAndObserveSettings() {
        val storage = FakeSettingsStorage()
        val repo = SettingsRepositoryImpl(storage)
        val observeUseCase = ObserveSettingsUseCase(repo)
        val updateUseCase = UpdateSettingsUseCase(repo)

        assertEquals(UiMode.SYSTEM, observeUseCase().value.uiMode)

        val updated = AppSettings(uiMode = UiMode.DARK, seedColorHex = "#123456")
        updateUseCase(updated)

        assertEquals(UiMode.DARK, observeUseCase().value.uiMode)
        assertEquals("#123456", observeUseCase().value.seedColorHex)
    }
}
