package dev.fnc314.tools.jobtracker.features.settings.data

import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.core.data.settings.SettingsStorage
import dev.fnc314.tools.jobtracker.core.data.settings.UiMode
import dev.fnc314.tools.jobtracker.features.settings.domain.usecase.ObserveSettingsUseCase
import dev.fnc314.tools.jobtracker.features.settings.domain.usecase.UpdateSettingsUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SettingsRepositoryTest {

    private class FakeSettingsStorage(
        initialValues: Map<String, String> = emptyMap(),
    ) : SettingsStorage {
        val map = initialValues.toMutableMap()
        override fun getString(key: String): String? = map[key]
        override fun putString(key: String, value: String) {
            map[key] = value
        }
    }

    @Test
    fun testSettings_InitialEmptyStorageReturnsDefaults() {
        val storage = FakeSettingsStorage()
        val repo = SettingsRepositoryImpl(storage)
        val observeUseCase = ObserveSettingsUseCase(repo)

        val settings = observeUseCase().value
        assertEquals(UiMode.SYSTEM, settings.uiMode)
        assertEquals("#2F5597", settings.seedColorHex)
    }

    @Test
    fun testSettings_CorruptedJsonRecoversToDefaults() {
        val storage = FakeSettingsStorage(mapOf("job_tracker_app_settings" to "{not valid json at all!"))
        val repo = SettingsRepositoryImpl(storage)
        val observeUseCase = ObserveSettingsUseCase(repo)

        val settings = observeUseCase().value
        // Must recover gracefully without throwing an exception and return default settings
        assertEquals(UiMode.SYSTEM, settings.uiMode)
        assertEquals("#2F5597", settings.seedColorHex)
    }

    @Test
    fun testSettings_PartialJsonWithMissingKeys() {
        // Only uiMode is specified; other fields must decode with defaults
        val storage = FakeSettingsStorage(mapOf("job_tracker_app_settings" to "{\"uiMode\":\"DARK\"}"))
        val repo = SettingsRepositoryImpl(storage)
        val observeUseCase = ObserveSettingsUseCase(repo)

        val settings = observeUseCase().value
        assertEquals(UiMode.DARK, settings.uiMode)
        assertEquals("#2F5597", settings.seedColorHex)
    }

    @Test
    fun testSettings_UpdateNotifiesObserverImmediately() {
        val storage = FakeSettingsStorage()
        val repo = SettingsRepositoryImpl(storage)
        val observeUseCase = ObserveSettingsUseCase(repo)
        val updateUseCase = UpdateSettingsUseCase(repo)

        assertEquals(UiMode.SYSTEM, observeUseCase().value.uiMode)

        val newSettings = AppSettings(uiMode = UiMode.LIGHT, seedColorHex = "#AABBCC")
        updateUseCase(newSettings)

        assertEquals(UiMode.LIGHT, observeUseCase().value.uiMode)
        assertEquals("#AABBCC", observeUseCase().value.seedColorHex)
        assertNotNull(storage.getString("job_tracker_app_settings"))
    }

    @Test
    fun testSettings_CustomSeedHexPersistence() {
        val storage = FakeSettingsStorage()
        val repo1 = SettingsRepositoryImpl(storage)
        val updateUseCase = UpdateSettingsUseCase(repo1)

        val updated = AppSettings(uiMode = UiMode.DARK, seedColorHex = "#FF5722")
        updateUseCase(updated)

        // Instantiate new repository from same storage to verify roundtrip persistence
        val repo2 = SettingsRepositoryImpl(storage)
        val observeUseCase2 = ObserveSettingsUseCase(repo2)

        assertEquals(UiMode.DARK, observeUseCase2().value.uiMode)
        assertEquals("#FF5722", observeUseCase2().value.seedColorHex)
    }
}
