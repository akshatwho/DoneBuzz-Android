package com.adhd.donebuzz

import android.app.Application
import android.content.Context
import com.adhd.donebuzz.data.DoneBuzzDatabase
import com.adhd.donebuzz.data.LocalState
import com.adhd.donebuzz.data.SettingsStore
import com.adhd.donebuzz.data.Todo
import com.adhd.donebuzz.focus.FocusTimer
import com.adhd.donebuzz.reminders.Reminders
import com.adhd.donebuzz.widget.TodayWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers

class DoneBuzzApp : Application() {
    /** Work that must finish even if the screen that started it goes away. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { DoneBuzzDatabase.get(this) }
    val settingsStore by lazy { SettingsStore(this) }
    val localState by lazy { LocalState(this) }
    val repository by lazy { TodoRepository(this) }

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        appScope.launch {
            // Language changes re-word the widget and the scheduled reminder.
            settingsStore.settings.distinctUntilChangedBy { it.language }.drop(1).collect { settings ->
                TodayWidget().updateAll(this@DoneBuzzApp)
                Reminders.sync(this@DoneBuzzApp, settings)
                Notifications.createChannels(this@DoneBuzzApp)
            }
        }
    }
}

val Context.app: DoneBuzzApp get() = applicationContext as DoneBuzzApp

/** Every change to the list goes through here, so the widget always follows. */
class TodoRepository(private val app: DoneBuzzApp) {
    private val dao get() = app.database.todos()

    fun add(title: String, minutes: Int?) = write { dao.insert(Todo(title = title, durationMinutes = minutes)) }
    fun update(todo: Todo) = write { dao.update(todo) }
    fun setDone(todo: Todo, done: Boolean) = write {
        dao.update(todo.copy(completedAt = if (done) System.currentTimeMillis() else null))
        if (done) FocusTimer.stopIfRunning(app, todo.id)
    }
    fun delete(todo: Todo) = write {
        dao.delete(todo)
        FocusTimer.stopIfRunning(app, todo.id)
    }

    private fun write(block: suspend () -> Unit) {
        app.appScope.launch {
            block()
            TodayWidget().updateAll(app)
        }
    }
}
