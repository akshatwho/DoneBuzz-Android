package com.adhd.donebuzz.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import com.adhd.donebuzz.i18n.AppLanguage
import com.adhd.donebuzz.i18n.Strings
import com.adhd.donebuzz.logic.ReminderTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** Optional time estimate chosen when the item is created. */
    val durationMinutes: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /** null while the item is still active. */
    val completedAt: Long? = null,
) {
    val isDone: Boolean get() = completedAt != null
}

@Dao
interface TodoDao {
    /** Everything still open, plus anything finished since [dayStart]. Newest first. */
    @Query("SELECT * FROM todos WHERE completedAt IS NULL OR completedAt >= :dayStart ORDER BY createdAt DESC")
    fun today(dayStart: Long): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE completedAt IS NOT NULL ORDER BY completedAt DESC")
    fun finished(): Flow<List<Todo>>

    /** Completion times since [since], only used for the streak. */
    @Query("SELECT completedAt FROM todos WHERE completedAt >= :since")
    fun completionsSince(since: Long): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM todos")
    suspend fun count(): Int

    @Insert
    suspend fun insert(todo: Todo): Long

    @Update
    suspend fun update(todo: Todo)

    @Delete
    suspend fun delete(todo: Todo)
}

@Database(entities = [Todo::class], version = 1)
abstract class DoneBuzzDatabase : RoomDatabase() {
    abstract fun todos(): TodoDao

    companion object {
        @Volatile private var instance: DoneBuzzDatabase? = null

        fun get(context: Context): DoneBuzzDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, DoneBuzzDatabase::class.java, "donebuzz.db")
                .build()
                .also { instance = it }
        }
    }
}

// MARK: Dates

object Days {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    fun today(): LocalDate = LocalDate.now(zone)
    fun startMillis(day: LocalDate): Long = day.atStartOfDay(zone).toInstant().toEpochMilli()
    fun of(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
}

// MARK: Settings

private val Context.settingsStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val language: AppLanguage = AppLanguage.CHINESE,
    val reminderEnabled: Boolean = false,
    val reminderMinutes: Int = ReminderTime.DEFAULT_MINUTES,
    val whatsNewSeenRelease: Int = 0,
    /** The task whose focus timer is running (its id), the title shown, and when it ends. */
    val focusTodoId: Long? = null,
    val focusTitle: String = "",
    val focusEndMillis: Long = 0,
) {
    val strings: Strings get() = Strings(language)
}

class SettingsStore(private val context: Context) {
    private object Keys {
        val language = stringPreferencesKey("appLanguage")
        val reminderEnabled = booleanPreferencesKey("reminderEnabled")
        val reminderMinutes = intPreferencesKey("reminderMinutes")
        val whatsNewSeen = intPreferencesKey("whatsNewSeenRelease")
        val focusId = longPreferencesKey("focusTodoId")
        val focusTitle = stringPreferencesKey("focusTitle")
        val focusEnd = longPreferencesKey("focusEndMillis")
    }

    val settings: Flow<AppSettings> = context.settingsStore.data.map { p ->
        AppSettings(
            language = AppLanguage.from(p[Keys.language]),
            reminderEnabled = p[Keys.reminderEnabled] ?: false,
            reminderMinutes = p[Keys.reminderMinutes] ?: ReminderTime.DEFAULT_MINUTES,
            whatsNewSeenRelease = p[Keys.whatsNewSeen] ?: 0,
            focusTodoId = p[Keys.focusId],
            focusTitle = p[Keys.focusTitle] ?: "",
            focusEndMillis = p[Keys.focusEnd] ?: 0,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setLanguage(language: AppLanguage) = context.settingsStore.edit { it[Keys.language] = language.code }
    suspend fun setReminder(enabled: Boolean, minutes: Int) = context.settingsStore.edit {
        it[Keys.reminderEnabled] = enabled
        it[Keys.reminderMinutes] = minutes
    }
    suspend fun setWhatsNewSeen(release: Int) = context.settingsStore.edit { it[Keys.whatsNewSeen] = release }
    suspend fun setFocus(todoId: Long?, title: String, endMillis: Long) = context.settingsStore.edit {
        if (todoId == null) {
            it.remove(Keys.focusId)
            it.remove(Keys.focusTitle)
            it.remove(Keys.focusEnd)
        } else {
            it[Keys.focusId] = todoId
            it[Keys.focusTitle] = title
            it[Keys.focusEnd] = endMillis
        }
    }
}

/** Small synchronous state that must be read before the first frame (where the cat stands). */
class LocalState(context: Context) {
    private val prefs = context.getSharedPreferences("state", Context.MODE_PRIVATE)
    var catProgress: String?
        get() = prefs.getString("catProgress", null)
        set(value) = prefs.edit().putString("catProgress", value).apply()
}
