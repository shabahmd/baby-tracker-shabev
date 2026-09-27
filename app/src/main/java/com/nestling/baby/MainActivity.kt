package com.nestling.baby

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.nestling.baby.ui.NestlingApp
import com.nestling.baby.ui.NestlingViewModel
import com.nestling.baby.ui.QuickAction

/**
 * The only activity. No login, no onboarding, no interstitial — the launcher icon takes
 * a half-asleep parent straight to the three buttons.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: NestlingViewModel by viewModels { NestlingViewModel.factory(this) }

    private val quickAction = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        quickAction.value = intent?.getStringExtra(QuickAction.EXTRA)

        setContent {
            val action by quickAction
            NestlingApp(
                viewModel = viewModel,
                quickAction = action,
                onQuickActionHandled = { quickAction.value = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        quickAction.value = intent.getStringExtra(QuickAction.EXTRA)
    }

    override fun onResume() {
        super.onResume()
        // Midnight may have passed while the app sat in the background.
        viewModel.refreshToday()
    }
}
