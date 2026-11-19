package com.adhd.donebuzz

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.adhd.donebuzz.focus.FocusTimer
import com.adhd.donebuzz.reminders.Reminders
import com.adhd.donebuzz.ui.RootScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    /** Bumped each time the widget's + asks for the add sheet. */
    private val addRequests = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        // The scene is a bright daytime sky, so system bar icons stay dark.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        handle(intent)
        lifecycleScope.launch {
            FocusTimer.restore(this@MainActivity)
            Reminders.sync(this@MainActivity, app.settingsStore.current())
        }
        setContent { RootScreen(addRequests.intValue) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        if (intent?.action == ACTION_ADD) {
            addRequests.intValue++
            intent.action = null
        }
    }

    companion object {
        const val ACTION_ADD = "com.adhd.donebuzz.action.ADD"
    }
}
