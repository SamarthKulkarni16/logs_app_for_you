package com.samarth.logsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samarth.logsapp.auth.AuthRepository
import com.samarth.logsapp.auth.AuthViewModel
import com.samarth.logsapp.auth.AuthViewModelFactory
import com.samarth.logsapp.auth.GoogleSignInHelper
import com.samarth.logsapp.data.remote.GeminiRepository
import com.samarth.logsapp.ui.screens.AccountScreen
import com.samarth.logsapp.ui.screens.LogScreen
import com.samarth.logsapp.ui.screens.LogViewModel
import com.samarth.logsapp.ui.screens.LogViewModelFactory
import com.samarth.logsapp.ui.screens.MonthGridScreen
import com.samarth.logsapp.ui.screens.MonthGridViewModel
import com.samarth.logsapp.ui.screens.MonthGridViewModelFactory
import com.samarth.logsapp.ui.screens.SignInScreen
import com.samarth.logsapp.ui.theme.LogsAppTheme
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as LogsApp
        val supabase = app.supabase

        val authRepository = AuthRepository(supabase)
        val googleSignInHelper = GoogleSignInHelper(
            context = this,
            webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        )
        val authViewModelFactory = AuthViewModelFactory(authRepository, googleSignInHelper)
        val logViewModelFactory = LogViewModelFactory(
            fileStore = app.logFileStore,
            syncManager = app.syncManager,
            currentUserId = { authRepository.currentUserId() }
        )
        val geminiRepository = GeminiRepository(apiKey = BuildConfig.GEMINI_API_KEY)
        val monthGridViewModelFactory = MonthGridViewModelFactory(app.logFileStore, geminiRepository)

        setContent {
            LogsAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val authViewModel: AuthViewModel = viewModel(factory = authViewModelFactory)
                    LogsAppRoot(
                        authViewModel = authViewModel,
                        logViewModelFactory = logViewModelFactory,
                        monthGridViewModelFactory = monthGridViewModelFactory,
                        app = app,
                        currentUserId = { authRepository.currentUserId() }
                    )
                }
            }
        }
    }
}

private enum class Screen { LOG, MONTH_GRID, ACCOUNT }

/**
 * Top-level router: shows the sign-in screen while signed out, otherwise
 * toggles between today's log and the month grid on triple-tap. Kicks off
 * a sync pass on launch and every time connectivity is regained, so
 * anything written offline catches up to Supabase automatically.
 */
@Composable
private fun LogsAppRoot(
    authViewModel: AuthViewModel,
    logViewModelFactory: LogViewModelFactory,
    monthGridViewModelFactory: MonthGridViewModelFactory,
    app: LogsApp,
    currentUserId: () -> String?
) {
    val sessionStatus by authViewModel.sessionStatus.collectAsState()

    when (sessionStatus) {
        is SessionStatus.Authenticated -> {
            var screen by remember { mutableStateOf(Screen.LOG) }

            val logViewModel: LogViewModel = viewModel(factory = logViewModelFactory)
            val monthGridViewModel: MonthGridViewModel = viewModel(factory = monthGridViewModelFactory)

            LaunchedEffect(Unit) {
                val userId = currentUserId() ?: return@LaunchedEffect
                launch { app.syncManager.syncPendingDays(userId) }
                launch {
                    app.connectivityObserver.observeOnlineEvents().collect {
                        app.syncManager.syncPendingDays(userId)
                    }
                }
            }

            when (screen) {
                Screen.LOG -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LogScreen(
                            viewModel = logViewModel,
                            onTripleTapToHistory = {
                                monthGridViewModel.refresh() // pick up anything written since the grid was last shown
                                screen = Screen.MONTH_GRID
                            }
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .windowInsetsPadding(WindowInsets.safeDrawing)
                                .size(64.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = { screen = Screen.ACCOUNT }
                                    )
                                }
                        )
                    }
                }
                Screen.MONTH_GRID -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MonthGridScreen(
                            viewModel = monthGridViewModel,
                            onTripleTapToLog = { screen = Screen.LOG }
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .windowInsetsPadding(WindowInsets.safeDrawing)
                                .size(64.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = { screen = Screen.ACCOUNT }
                                    )
                                }
                        )
                    }
                }
                Screen.ACCOUNT -> {
                    AccountScreen(
                        email = authViewModel.currentUserEmail,
                        onBack = { screen = Screen.LOG },
                        onSignOut = {
                            authViewModel.signOut()
                            screen = Screen.LOG
                        }
                    )
                }
            }
        }

        is SessionStatus.NotAuthenticated,
        is SessionStatus.RefreshFailure -> SignInScreen(authViewModel)

        is SessionStatus.Initializing -> Unit
    }
}
