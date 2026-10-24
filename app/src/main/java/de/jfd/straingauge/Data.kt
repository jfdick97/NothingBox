package de.jfd.straingauge

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

val metricNames: List<String> get() = listOf(tr(R.string.metric_demands), tr(R.string.metric_tension), tr(R.string.metric_energy), tr(R.string.metric_agency))
val factorNames: List<String> get() = listOf(tr(R.string.data_workload_time_pressure), tr(R.string.data_too_many_tasks_at_once), tr(R.string.data_uncertainty_unanswered_questions),
    tr(R.string.data_my_own_standards_expectations), tr(R.string.data_interruptions_being_available), tr(R.string.data_conflict_emotional_strain),
    tr(R.string.data_personal_responsibilities_commitments), tr(R.string.data_too_little_sleep_insufficient_recovery), tr(R.string.data_extra_factor_1), tr(R.string.data_extra_factor_2))

@Entity(tableName = "days")
data class DayEntry(
    @PrimaryKey val date: String,
    val demands: Double?, val tension: Double?, val energy: Double?, val agency: Double?,
    val factors: Int = 0,
    val scanId: String? = null,
    // Original scanner result is retained separately from user corrections.
    val detected: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''") val custom1: String = "",
    @ColumnInfo(defaultValue = "''") val custom2: String = ""
) {
    fun values() = listOf(demands, tension, energy, agency)
    fun factorLabels() = factorNames.take(8) + listOf(custom1.ifBlank{factorNames[8]},custom2.ifBlank{factorNames[9]})
    fun selectedFactors() = factorLabels().filterIndexed{i,_-> factors and (1 shl i)!=0}
    fun validate() {
        require(LocalDate.parse(date).toString() == date) { tr(R.string.data_invalid_date, date) }
        require(values().all { it == null || (it.isFinite() && it in 0.0..10.0) }) { tr(R.string.data_scale_value_is_outside_0_10) }
        require(factors in 0..1023) { tr(R.string.data_invalid_stress_factors) }
        require(custom1.length<=120 && custom2.length<=120) { tr(R.string.data_extra_factor_names_must_not_exceed_120_characters) }
        require(factors and 256 == 0 || custom1.isNotBlank()) { tr(R.string.data_please_enter_a_name_for_extra_factor_1) }
        require(factors and 512 == 0 || custom2.isNotBlank()) { tr(R.string.data_please_enter_a_name_for_extra_factor_2) }
    }
}
@Entity(tableName = "scans")
data class ScanEntry(
    @PrimaryKey val id: String,
    val weekStart: String,
    val original: String,
    val aligned: String,
    val template: String = "BC-02/01",
    val createdAt: Long = System.currentTimeMillis(),
    val diagnostics: String = "[]"
)
@Dao
interface Store {
    @Query("SELECT * FROM days ORDER BY date") fun observeDays(): Flow<List<DayEntry>>
    @Query("SELECT * FROM scans ORDER BY createdAt DESC") fun observeScans(): Flow<List<ScanEntry>>
    @Query("SELECT * FROM days ORDER BY date") suspend fun days(): List<DayEntry>
    @Query("SELECT * FROM scans ORDER BY createdAt DESC") suspend fun scans(): List<ScanEntry>
    @Upsert suspend fun putDays(days: List<DayEntry>)
    @Upsert suspend fun putScans(scans: List<ScanEntry>)
    @Query("DELETE FROM days WHERE date = :date") suspend fun deleteDay(date: String)
    @Query("DELETE FROM scans WHERE id = :id") suspend fun deleteScan(id: String)
    @Query("UPDATE days SET scanId = NULL WHERE scanId = :id") suspend fun unlinkScan(id: String)
    @Query("DELETE FROM days WHERE date >= :start AND date <= :end")
    suspend fun deleteDaysInRange(
        start: String,
        end: String
    ): Int
    @Query("SELECT EXISTS(SELECT 1 FROM days WHERE date >= :start AND date <= :end)")
    suspend fun hasEntriesInRange(
        start: String,
        end: String
    ): Boolean
}
@Database(entities = [DayEntry::class, ScanEntry::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun store(): Store
    companion object {
        fun open(context: Context): AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, "strain-gauge.db").addMigrations(MIGRATION_1_2).build()
    }
}

object Entries {
    fun conflicts(old: List<DayEntry>, incoming: List<DayEntry>): List<String> {
        val byDate = old.associateBy { it.date }
        return incoming.filter { n -> byDate[n.date]?.let { o ->
            o.values() != n.values() || o.factors != n.factors || o.custom1 != n.custom1 || o.custom2 != n.custom2
        } == true }.map { it.date }
    }
    fun merge(old: List<DayEntry>, incoming: List<DayEntry>, replace: Boolean): List<DayEntry> {
        require(incoming.map { it.date }.distinct().size == incoming.size) { tr(R.string.data_duplicate_dates) }
        incoming.forEach { it.validate() }
        val existing = old.map { it.date }.toSet()
        return if (replace) incoming else incoming.filter { it.date !in existing }
    }
}

/** Additive migration: all existing days and scans remain untouched. */
val MIGRATION_1_2 = object: Migration(1,2) {
    override fun migrate(db:SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE days ADD COLUMN custom1 TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE days ADD COLUMN custom2 TEXT NOT NULL DEFAULT ''")
    }
}
