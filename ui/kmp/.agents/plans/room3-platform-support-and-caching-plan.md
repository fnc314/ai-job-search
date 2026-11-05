# Implementation Plan: Room 3 Multiplatform Support & Offline-First Caching Policy

## Goal Description
Expand the Room 3 Kotlin Multiplatform (KMP) database setup in `ui/kmp` to provide full platform support across Android, Desktop JVM, iOS, and Web (`wasmJs` / `js`), implement bidirectional entity mappers, and enforce an **offline-first caching and synchronization policy** across all feature repositories.

Furthermore, preserve the isolation of newer feature-specific data models (such as `ApplicationAge`, `FilterCriteria`, `SortCriteria`, `JobApplicationItemUiModel`, `JobApplicationDetailUiModel`, `AddJobApplicationFormUiModel`, `TrackerEditItemUiModel`, `SettingsUiModel`, etc.) so they remain decoupled from Room SQL persistence.

---

## Records Verification (`../../records` & `tracker.schema.json`)

Verification against the canonical records and schema confirms the exact shape of UUIDv7 fields:
1. **`records/proto/src/main/proto/tracker/v1/application.proto`**:
   - `string id = 1;`: The human-readable slug document identifier (e.g., `2026-03-pnc-lead`), used as the Firestore document key (`applications/{id}`).
   - `string uuid = 22;`: The canonical, RFC 4122 variant UUIDv7 identifier.
2. **`tracker.schema.json`**:
   - Mandates `"schemaVersion": 7` as a required integer property on every tracked application document (`"required": ["id", "date", "company", "companyId", "status", "schemaVersion", "createdAt", "updatedAt"]`).
   - Declares `#/$defs/uuidV7` with pattern `^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$`.
   - `application.uuid` is typed as `#/$defs/uuidV7`.
   - Auxiliary event, edit, and artifact models (`eventId`, `proposalId`, `artifactId`) directly mandate `#/$defs/uuidV7`.
3. **`scripts/contract.py` & `scripts/alignment.py`**:
   - Monotonic UUIDv7 generated locally via `contract.newUuid()`.
   - `uuidTimestamp(val)` extracts the 48-bit timestamp from the first 80 bits.

Therefore, in SQLite:
- `JobApplicationEntity` uses `@PrimaryKey public val uuid: Uuid7` as the primary key.
- `public val slugId: String` preserves the human-readable slug with a unique index.
- Lookups can query by either `uuid` or `slugId` via `getByIdOrUuid(id)`.

---

## User Review Required

> [!IMPORTANT]
> **KSP (Kotlin Symbol Processing) for Room 3 Compiler**:
> Room 3.0 exclusively uses KSP to generate database implementations (`AppDatabase_Impl`), DAOs, and SQLite statements across Kotlin Multiplatform targets. We will:
> 1. Register `ksp` and `androidx-room3-compiler` in `gradle/libs.versions.toml` within a new bundle `room-compiler = ["androidx-room3-compiler"]`.
> 2. Add `implementation(libs.ksp.gradle.plugin)` to `gradle/tools/project-plugins/build.gradle.kts`.
> 3. Update `DataSourceDependenciesConventionPlugin` to automatically apply `com.google.devtools.ksp` and bind `androidx-room3-compiler` across all multiplatform target configurations (`kspCommonMainMetadata`, `kspAndroid`, `kspJvm`, `kspIosSimulatorArm64`, `kspIosArm64`, `kspWasmJs`, `kspJs`).
> 4. Retain runtime fallback in `Local*DataSourceImpl` so that in memory-restricted test runners or sandboxes without SQLite, data access falls back gracefully.

> [!NOTE]
> **Data Model Preservation Guarantee**:
> All newer data models defined per feature remain strictly untouched and isolated from the database layer. Database entities (`JobApplicationEntity`) only interface directly with the canonical domain model (`JobApplication`).

---

## Proposed Changes

### Component 0: Gradle Catalog & Project Plugins — KSP & Room Compiler

#### [MODIFY] `gradle/libs.versions.toml`
Add `androidx-room3-compiler` directly into the existing `room` bundle and add the KSP plugin:
```toml
[versions]
  ksp = "2.4.21-1.0.31"

[libraries]
  androidx-room3-compiler = { module = "androidx.room3:room3-compiler", version.ref = "androidx-room3" }
  ksp-gradle-plugin = { module = "com.google.devtools.ksp:symbol-processing-gradle-plugin", version.ref = "ksp" }

[bundles]
  room = [
    "androidx-room3-runtime",
    "androidx-room3-common",
    "androidx-room3-compiler",
  ]

[plugins]
  ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

#### [MODIFY] `gradle/tools/project-plugins/build.gradle.kts`
Add KSP Gradle plugin to classpath:
```kotlin
dependencies {
  gradleApi()
  implementation(libs.android.gradle.plugin)
  implementation(libs.kotlin.gradle.plugin)
  implementation(libs.compose.gradle.plugin)
  implementation(libs.ksp.gradle.plugin)
}
```

#### [MODIFY] `gradle/tools/project-plugins/src/main/kotlin/dev/fnc314/tools/jobtracker/kmp/gradle/catalog/CatalogTypes.kt`
Extend the existing `enum class` declarations in `CatalogTypes`:
```kotlin
    public enum class Libraries(
        override val catalogKey: String,
    ) : CatalogTypes<Provider<MinimalExternalModuleDependency>> {
        // ... existing entries
        ANDROIDX_ROOM3_COMPILER("androidx-room3-compiler"),
        KSP_GRADLE_PLUGIN("ksp-gradle-plugin");
    }

    public enum class Plugins(
        override val catalogKey: String,
    ) : CatalogTypes<Provider<PluginDependency>> {
        // ... existing entries
        KSP("ksp");
    }
```
*(Note: `Bundles.ROOM("room")` already exists as an enum member and now includes `androidx-room3-compiler`)*

#### [MODIFY] `gradle/tools/project-plugins/src/main/kotlin/dev/fnc314/tools/jobtracker/kmp/gradle/plugins/config/DataSourceDependenciesConventionPlugin.kt`
Configure KSP plugin and compiler dependencies using the existing `Bundles.ROOM`:
```kotlin
// Apply KSP plugin to target project
target.pluginManager.apply(CatalogTypes.Plugins.KSP(target).get().pluginId)

// Bind Room 3 KSP compiler to each target using existing ROOM bundle
val targets = listOf("kspCommonMainMetadata", "kspAndroid", "kspJvm", "kspIosSimulatorArm64", "kspIosArm64", "kspWasmJs", "kspJs")
targets.forEach { configName ->
    target.configurations.matching { it.name == configName }.configureEach { config ->
        target.dependencies.add(config.name, CatalogTypes.Bundles.ROOM(target))
    }
}
```

---

### Component 1: Core Data (`core:data`) — Room 3 Platform Database Builders

We will implement expect/actual factory functions providing concrete platform-specific `RoomDatabase.Builder<AppDatabase>` instances.

#### [NEW] `core/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/data/database/DatabaseBuilder.kt`
- Declares:
  ```kotlin
  public expect fun createAppDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>

  public fun createAppDatabase(
      builder: RoomDatabase.Builder<AppDatabase> = createAppDatabaseBuilder()
  ): AppDatabase
  ```
- Configures common builder options: `fallbackToDestructiveMigration(true)` and `setQueryCoroutineContext(Dispatchers.IO)`.

#### [NEW] `core/data/src/androidMain/kotlin/dev/fnc314/tools/jobtracker/core/data/database/DatabaseBuilder.android.kt`
- Implements `createAppDatabaseBuilder()` using `ApplicationContextHolder.context`:
  ```kotlin
  public actual fun createAppDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
      val context = ApplicationContextHolder.context
      val dbFile = context.getDatabasePath("jobtracker.db")
      return Room.databaseBuilder<AppDatabase>(
          context = context,
          name = dbFile.absolutePath,
      )
  }
  ```

#### [NEW] `core/data/src/jvmMain/kotlin/dev/fnc314/tools/jobtracker/core/data/database/DatabaseBuilder.jvm.kt`
- Implements `createAppDatabaseBuilder()` using user home directory and `BundledSQLiteDriver()`:
  ```kotlin
  public actual fun createAppDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
      val userHome = System.getProperty("user.home")
      val dbDir = File(userHome, ".jobtracker").apply { mkdirs() }
      val dbFile = File(dbDir, "jobtracker.db")
      return Room.databaseBuilder<AppDatabase>(
          name = dbFile.absolutePath,
      ).setDriver(BundledSQLiteDriver())
  }
  ```

#### [NEW] `core/data/src/iosMain/kotlin/dev/fnc314/tools/jobtracker/core/data/database/DatabaseBuilder.ios.kt`
- Implements `createAppDatabaseBuilder()` using iOS application documents directory and `BundledSQLiteDriver()`:
  ```kotlin
  public actual fun createAppDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
      val documentDirectory = NSSearchPathForDirectoriesInDomains(
          NSDocumentDirectory,
          NSUserDomainMask,
          true
      ).firstOrNull() as? String ?: NSHomeDirectory()
      val dbFilePath = "$documentDirectory/jobtracker.db"
      return Room.databaseBuilder<AppDatabase>(
          name = dbFilePath,
      ).setDriver(BundledSQLiteDriver())
  }
  ```

#### [NEW] `core/data/src/wasmJsMain/kotlin/dev/fnc314/tools/jobtracker/core/data/database/DatabaseBuilder.wasmJs.kt`
#### [NEW] `core/data/src/jsMain/kotlin/dev/fnc314/tools/jobtracker/core/data/database/DatabaseBuilder.js.kt`
- Implements `createAppDatabaseBuilder()` returning `Room.inMemoryDatabaseBuilder<AppDatabase>()` for web targets.

---

### Component 2: Core Data (`core:data`) — Comprehensive Entity, Standard Library `kotlin.uuid.Uuid`, & Mappers

#### [NEW] `core/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/data/model/Uuid7.kt`
Leveraging Kotlin's standard library **`kotlin.uuid.Uuid`** (standardized in Kotlin stdlib 2.0.20+ with built-in `Uuid.generateV7()`), we provide a type-safe `Uuid7` value class:
```kotlin
package dev.fnc314.tools.jobtracker.core.data.model

import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * A strongly-typed, compile-time identifier representing an RFC 4122 Version 7 Universally Unique Identifier.
 *
 * ### Architectural Justification
 * 1. **Compile-Time Type Safety**: Distinguishes canonical UUIDv7 identifiers from arbitrary strings,
 *    human-readable document slugs (e.g. `"2026-03-pnc-lead"`), foreign keys, or random text at compile time,
 *    eliminating parameter-swap bugs across data source and repository boundaries.
 * 2. **Zero Runtime Allocation Overhead**: Marked with `@JvmInline value class` to ensure full unboxing at
 *    runtime across all targets (JVM, Android, iOS, and WebAssembly/JS) without object wrapper overhead.
 * 3. **Time-Ordered Monotonicity**: UUIDv7 embeds a 48-bit millisecond timestamp in the most significant bits.
 *    By implementing [Comparable], instances sort chronographically by construction, enabling natural time-ordered
 *    sorting without secondary database timestamp indexes.
 * 4. **Standard Library Integration**: Natively wraps [kotlin.uuid.Uuid], utilizing the official Kotlin standard
 *    library engine for validation, parsing, and cryptographic random entropy generation.
 *
 * @property value The underlying canonical 128-bit [Uuid] instance.
 */
@OptIn(ExperimentalUuidApi::class)
@JvmInline
public value class Uuid7(public val value: Uuid) : Comparable<Uuid7> {

    /**
     * Constructs a [Uuid7] by parsing a standard 8-4-4-4-12 hex-dash string representation.
     *
     * @param raw The canonical hex-dash string (e.g. `"0192e1b4-7b00-7000-8000-000000000001"`).
     * @throws IllegalArgumentException If [raw] does not conform to the canonical UUID format.
     */
    public constructor(raw: String) : this(Uuid.parse(raw))

    /**
     * Compares this [Uuid7] with [other] chronologically based on the embedded 48-bit timestamp prefix
     * and subsequent random sequence bits.
     *
     * @param other The identifier to compare against.
     * @return Zero if identical, negative if this instance was created before [other], or positive if after.
     */
    override fun compareTo(other: Uuid7): Int = this.value.compareTo(other.value)

    /**
     * Formats this identifier into the canonical RFC 4122 36-character lowercase hex-dash string.
     *
     * @return Formatted string in the standard `xxxxxxxx-xxxx-7xxx-xxxx-xxxxxxxxxxxx` format.
     */
    override fun toString(): String = value.toString()

    public companion object {
        /**
         * Generates a new cryptographically random, time-ordered RFC 4122 UUIDv7 using the system clock
         * and platform secure random entropy via [Uuid.Companion.generateV7].
         *
         * @return A newly minted, monotonic [Uuid7] instance.
         */
        public fun generate(): Uuid7 = Uuid7(Uuid.generateV7())

        /**
         * Parses a hex-dash string into a [Uuid7] instance.
         *
         * @param value The 36-character string representation.
         * @return The parsed [Uuid7].
         * @throws IllegalArgumentException If the string is malformed.
         */
        public fun parse(value: String): Uuid7 = Uuid7(Uuid.parse(value))

        /**
         * Safely attempts to parse a nullable or malformed string into a [Uuid7], returning `null` on failure.
         *
         * @param value The candidate string, or `null`.
         * @return The parsed [Uuid7] if valid, or `null` if the input is `null` or invalid.
         */
        public fun parseOrNull(value: String?): Uuid7? =
            if (value != null) Uuid.parseOrNull(value)?.let { Uuid7(it) } else null
    }
}
```

#### [NEW] `core/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/data/database/converter/DatabaseConverters.kt`
Room TypeConverters allowing Room to persist `Uuid` and `Uuid7` directly as `TEXT`:
```kotlin
package dev.fnc314.tools.jobtracker.core.data.database.converter

import androidx.room3.TypeConverter
import dev.fnc314.tools.jobtracker.core.data.model.Uuid7
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
public class DatabaseConverters {
    @TypeConverter
    public fun fromUuid(uuid: Uuid?): String? = uuid?.toString()

    @TypeConverter
    public fun toUuid(raw: String?): Uuid? = raw?.let { Uuid.parseOrNull(it) }

    @TypeConverter
    public fun fromUuid7(uuid: Uuid7?): String? = uuid?.value?.toString()

    @TypeConverter
    public fun toUuid7(raw: String?): Uuid7? = raw?.let { Uuid7.parseOrNull(it) }
}
```

#### [MODIFY] `core/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/data/database/entity/JobApplicationEntity.kt`
Expand `JobApplicationEntity` to use `Uuid7` as the primary key datum, while retaining the human-readable slug `id` (e.g. `2026-03-pnc-lead`) mirroring `application.proto`, and enforcing `schemaVersion = 7` per `tracker.schema.json`:
```kotlin
package dev.fnc314.tools.jobtracker.core.data.database.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import dev.fnc314.tools.jobtracker.core.data.model.Uuid7

@Entity(
    tableName = "job_applications",
    indices = [
        Index(value = ["slugId"], unique = true),
        Index(value = ["status"]),
        Index(value = ["company"]),
    ]
)
public data class JobApplicationEntity(
    @PrimaryKey public val uuid: Uuid7,
    public val slugId: String,
    public val schemaVersion: Int = 7,
    public val company: String,
    public val role: String?,
    public val location: String?,
    public val status: String,
    public val channel: String,
    public val jobId: String?,
    public val jobUrl: String?,
    public val description: String?,
    public val fitScore: Int = 0,
    public val appliedDate: String? = null,
    public val updatedAt: String? = null,
    public val createdAt: String? = null,
    public val notesRendered: String? = null,
    public val jobApplicationUrl: String? = null,
    public val tagsCsv: String = "",
    public val contactIdsCsv: String = "",
)
```
    public val updatedAt: String? = null,
    public val createdAt: String? = null,
    public val notesRendered: String? = null,
    public val jobApplicationUrl: String? = null,
    public val tagsCsv: String = "",
    public val contactIdsCsv: String = "",
)
```

#### [MODIFY] `core/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/data/database/dao/JobApplicationDao.kt`
Update `JobApplicationDao` queries to leverage `Uuid7` and `slugId`:
```kotlin
@Dao
public interface JobApplicationDao {
    @Query("SELECT * FROM job_applications ORDER BY uuid DESC")
    public fun observeAll(): Flow<List<JobApplicationEntity>>

    @Query("SELECT * FROM job_applications WHERE uuid = :uuid")
    public suspend fun getByUuid(uuid: Uuid7): JobApplicationEntity?

    @Query("SELECT * FROM job_applications WHERE slugId = :id OR uuid = :uuidString")
    public suspend fun getByIdOrUuid(id: String, uuidString: String = id): JobApplicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertAll(applications: List<JobApplicationEntity>): Unit

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insert(application: JobApplicationEntity): Unit

    @Query("DELETE FROM job_applications WHERE uuid = :uuid")
    public suspend fun deleteByUuid(uuid: Uuid7): Unit
}
```

#### [MODIFY] `core/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/data/database/AppDatabase.kt`
Register `DatabaseConverters`:
```kotlin
@Database(entities = [JobApplicationEntity::class], version = 1)
@TypeConverters(DatabaseConverters::class)
public abstract class AppDatabase : RoomDatabase() {
    public abstract fun jobApplicationDao(): JobApplicationDao
}
```

#### [MODIFY] `core/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/data/model/JobApplication.kt`
Add the optional `uuid: Uuid7? = null` property to `JobApplication`, matching line 40 of `application.proto` (`string uuid = 22`):
```kotlin
public data class JobApplication(
    public val id: String,
    public val company: String,
    public val role: String?,
    public val location: String?,
    public val status: TrackerStatus,
    public val channel: ApplicationChannel,
    public val jobId: String?,
    public val jobUrl: String?,
    public val description: String?,
    public val tags: ImmutableList<String>,
    public val contactIds: ImmutableList<String>,
    public val compensation: Compensation?,
    public val locationDetail: Location?,
    public val fitRating: Int? = null,
    public val fitEvaluation: TrackerFit?,
    public val counters: TrackerCounts?,
    public val timeline: Timeline?,
    public val notesRendered: String?,
    public val createdAt: Instant?,
    public val updatedAt: Instant?,
    public val jobApplicationUrl: String?,
    public val uuid: Uuid7? = null,
    public val schemaVersion: Int = 7,
)
```
Update `Factory.createJobApplication` with `uuid: Uuid7? = null` and `schemaVersion: Int = 7`.

#### [NEW] `core/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/data/database/mapper/JobApplicationMapper.kt`
Bidirectional mapping functions resolving `uuid` (generating a fresh `Uuid7` if not present) and `slugId`:
```kotlin
public fun JobApplicationEntity.toDomain(): JobApplication
public fun JobApplication.toEntity(fallbackUuid: Uuid7? = null): JobApplicationEntity
```
Ensures complete fidelity with `Factory.createJobApplication(...)` for default missing fields, parses comma-separated lists for `tags` and `contactIds`, and binds `uuid` to `JobApplication`.

#### [MODIFY] `core/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/core/data/di/CoreDataModule.kt`
Register Room database bindings:
```kotlin
single<AppDatabase> { createAppDatabase() }
single<JobApplicationDao> { get<AppDatabase>().jobApplicationDao() }
```

---

### Component 3: Feature Data Sources — Database Integration with Fallback

#### [MODIFY] `features/job-applications-list/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/jobapplicationslist/data/source/LocalJobApplicationsListDataSource.kt`
Update `LocalJobApplicationsListDataSourceImpl`:
- Inject `jobApplicationDao: JobApplicationDao? = null` (or `appDatabase: AppDatabase? = null`).
- In `getJobApplications()`:
  - Query DAO entities and map with `.toDomain()`.
  - If DAO is empty or throws, fall back to memory cache.
- In `saveJobApplications(applications)`:
  - Write to DAO with `.insertAll(applications.map { it.toEntity() })`.
  - Update memory cache.

#### [MODIFY] `features/job-applications-details/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/jobapplicationsdetails/data/source/LocalJobApplicationsDetailsDataSource.kt`
Update `LocalJobApplicationsDetailsDataSourceImpl`:
- In `getJobApplication(id)`:
  - Query `jobApplicationDao.getById(id)?.toDomain()`.
  - Fall back to memory cache.
- In `saveJobApplication(application)`:
  - Write to `jobApplicationDao.insert(application.toEntity())`.
  - Update memory cache.

#### [MODIFY] `features/add-job-application/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/addjobapplication/data/source/LocalAddJobApplicationDataSource.kt`
Update `LocalAddJobApplicationDataSourceImpl`:
- In `saveJobApplication(application)`:
  - Persist to `jobApplicationDao.insert(application.toEntity())`.

---

### Component 4: Feature Repositories — Synchronization & Caching Policies (Cache-First Reads, Remote-First Writes)

The synchronization contract across all repositories strictly distinguishes between **Reads** and **Writes/Mutations**:
1. **Reads (Cache-First)**: Queries read from the local Room SQLite database first for instant zero-latency UI rendering and full offline visibility. Background or forced refreshes query the remote source and persist updates locally.
2. **Writes / Mutations (Remote-First)**: Updates and inserts (`updateStatus`, `saveJobApplication`, `approveEdit`, `rejectEdit`, `recordEvent`) are **pushed to the remote authoritative backend FIRST**. Only upon successful remote confirmation is the resulting record reflected and persisted to the local Room database. If the remote operation fails (e.g., validation rejection or network error), the local database is not corrupted with unconfirmed state.

#### [MODIFY] `features/job-applications-list/domain/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/jobapplicationslist/domain/repository/JobApplicationsListRepository.kt`
Update contract to support optional force-refresh:
```kotlin
public interface JobApplicationsListRepository {
    public suspend fun getJobApplications(forceRefresh: Boolean = false): List<JobApplication>
}
```

#### [MODIFY] `features/job-applications-list/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/jobapplicationslist/data/repository/JobApplicationsListRepositoryImpl.kt`
Implement **Cache-First with Remote Synchronization**:
```kotlin
internal class JobApplicationsListRepositoryImpl(
    private val remoteDataSource: RemoteJobApplicationsListDataSource,
    private val localDataSource: LocalJobApplicationsListDataSource,
) : JobApplicationsListRepository {

    override suspend fun getJobApplications(forceRefresh: Boolean): List<JobApplication> {
        val cached = localDataSource.getJobApplications()
        if (cached.isNotEmpty() && !forceRefresh) {
            return cached
        }

        return try {
            val remote = remoteDataSource.getJobApplications()
            if (remote.isNotEmpty()) {
                localDataSource.saveJobApplications(remote)
                remote
            } else {
                cached
            }
        } catch (_: Exception) {
            // Graceful fallback when offline or remote network fails
            cached
        }
    }
}
```

#### [MODIFY] `features/job-applications-list/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/jobapplicationslist/data/LoadAllJobApplicationsUseCaseImpl.kt`
Pass `params.forceRefresh` to `repository.getJobApplications(params.forceRefresh)`.

#### [MODIFY] `features/job-applications-details/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/jobapplicationsdetails/data/repository/JobApplicationsDetailsRepositoryImpl.kt`
Implement Cache-First reads and **Remote-First writes**:
```kotlin
internal class JobApplicationsDetailsRepositoryImpl(
    private val remoteDataSource: RemoteJobApplicationsDetailsDataSource,
    private val networkDataSource: NetworkJobApplicationsDetailsDataSource,
    private val localDataSource: LocalJobApplicationsDetailsDataSource,
) : JobApplicationsDetailsRepository {

    // Read: Cache-first with remote fallback
    override suspend fun getJobApplication(id: String): JobApplication? {
        val cached = localDataSource.getJobApplication(id)
        if (cached != null) return cached
        val remote = remoteDataSource.getJobApplication(id)
        if (remote != null) {
            localDataSource.saveJobApplication(remote)
        }
        return remote
    }

    // Write: Remote-first, then reflected locally
    override suspend fun updateStatus(
        id: String,
        status: TrackerStatus,
        date: String?,
        note: String?,
    ): JobApplication {
        val updated = remoteDataSource.updateStatus(id, status, date, note)
        localDataSource.saveJobApplication(updated)
        return updated
    }

    override suspend fun updateFitScore(id: String, newScore: Int): JobApplication {
        val updated = remoteDataSource.updateFitScore(id, newScore)
        localDataSource.saveJobApplication(updated)
        return updated
    }

    override suspend fun recordEvent(event: TrackerEvent): String {
        val eventId = remoteDataSource.recordEvent(event)
        localDataSource.recordEvent(event)
        return eventId
    }
}
```

#### [MODIFY] `features/add-job-application/data/src/commonMain/kotlin/dev/fnc314/tools/jobtracker/features/addjobapplication/data/repository/AddJobApplicationRepositoryImpl.kt`
Implement **Remote-First writes**:
```kotlin
internal class AddJobApplicationRepositoryImpl(
    private val remoteDataSource: RemoteAddJobApplicationDataSource,
    private val localDataSource: LocalAddJobApplicationDataSource,
) : AddJobApplicationRepository {

    override suspend fun saveJobApplication(application: JobApplication): String {
        val id = remoteDataSource.saveJobApplication(application)
        localDataSource.saveJobApplication(application)
        return id
    }

    override suspend fun recordEvent(event: TrackerEvent): String {
        val id = remoteDataSource.recordEvent(event)
        localDataSource.recordEvent(event)
        return id
    }
}
```

---

### Component 5: Feature Models Preservation Guarantee

The newer models in each feature remain untouched and decoupled:
- **`features:job-applications-list`**: `ApplicationAge`, `FilterCriteria`, `SortCriteria`, `JobApplicationItemUiModel`
- **`features:job-applications-details`**: `JobApplicationDetailUiModel`, `DetailRoute`
- **`features:add-job-application`**: `AddJobApplicationFormUiModel`, `AddLeadFormUiModel`
- **`features:tracker-edits`**: `TrackerEditItemUiModel`, `TrackerEditChangeUiModel`
- **`features:sign-in`**: `UserAccountUiModel`
- **`features:settings`**: `SettingsUiModel`

These models continue to consume `JobApplication` (from `core:data:model`), with zero awareness of Room `@Entity`, `@Dao`, or SQLite tables.

---

### Component 6: Protos Regeneration & Terminal TUI Double Verification via `mise`

#### 1. Proto Regeneration
Ensure proto definitions remain in sync across the workspace:
- Command: `./gradlew :records:records-proto:generateProto :records:records-proto:assemble`
- Rebuilds and packages `:records:records-proto` and `:records:records-parser`.

#### 2. Terminal TUI & Tracker 2x Verification via `mise` Tasks
Per the repo invariant, direct execution of python binaries is strictly avoided in favor of the canonical `mise` task interface:
- **Verification 1 (Contract & Schema Validation)**: Execute `mise run //:tracker check --offline` to verify that the schema contract (`schemaVersion: 7`), document IDs, enums, and UUID definitions resolve and pass validation cleanly.
- **Verification 2 (Record Querying & TUI Interoperability)**:
  - Execute `mise run //:tracker list --limit 1` to confirm live application records deserialize and project through the CLI without errors.
  - Execute `mise run //:tui` to launch and visually verify that the curses dashboard displays application rows, status badges, and detail panes correctly.

---

## Verification Plan

### Automated Tests
1. **Mapper & Entity Tests**:
   - `core/data/src/commonTest/kotlin/dev/fnc314/tools/jobtracker/core/data/database/JobApplicationMapperTest.kt`:
     - Test `toEntity()` extracts all core fields, converts collections to CSV, sets timestamps, and populates `schemaVersion = 7`.
     - Test `toDomain()` reconstructs `JobApplication` with accurate enums and immutable lists.
2. **Repository Caching Policy Tests**:
   - `features/job-applications-list/data/src/commonTest/.../JobApplicationsListRepositoryTest.kt`:
     - Test cache-first returns local data when available without querying remote.
     - Test `forceRefresh = true` queries remote and updates local database.
     - Test remote failure falls back to cached data when offline.
3. **Proto Regeneration**:
   - Run `./gradlew :records:records-proto:generateProto :records:records-proto:assemble` to verify proto code generation.
4. **Tracker & TUI Double Verification (via `mise`)**:
   - Run `mise run //:tracker check --offline` to verify schema, definitions, and contract integrity.
   - Run `mise run //:tracker list --limit 1` to verify record projection and deserialization.
5. **KMP Build & README Validation**:
   ```bash
   ./gradlew compileCommonMainKotlinMetadata
   ./gradlew :core-data:jvmTest :features-job-applications-list-data:jvmTest :features-job-applications-details-data:jvmTest
   mise run //:ui:kmp:docs:readmes
   ./gradlew :apps-android:assembleDebug
   ```

### Manual Verification
- Verify that calling `getJobApplications()` without internet connectivity retrieves previously stored applications from Room SQLite database.
- Verify that `apps-android` debug APK compiles and includes Room SQLite runtime without symbol clashes.
- Verify `mise run //:tui` opens the terminal curses tracker cleanly.
