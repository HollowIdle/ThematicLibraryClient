package com.example.thematiclibraryclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.thematiclibraryclient.data.common.SessionExpiredNotifier
import com.example.thematiclibraryclient.ui.navigation.RootNavigation
import com.example.thematiclibraryclient.ui.navigation.ScreenRoute
import com.example.thematiclibraryclient.ui.theme.ThematicLibraryClientTheme
import com.example.thematiclibraryclient.ui.viewmodel.AuthState
import com.example.thematiclibraryclient.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var sessionExpiredNotifier: SessionExpiredNotifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ThematicLibraryClientTheme {
                val navController = rememberNavController()
                val authState by viewModel.authState.collectAsState()

                LaunchedEffect(Unit) {
                    sessionExpiredNotifier.events.collectLatest {
                        viewModel.onSessionExpired()

                        navController.navigate(ScreenRoute.AUTH_GRAPH_ROUTE) {
                            popUpTo(navController.graph.id) {
                                inclusive = true
                            }
                        }
                    }
                }

                when(authState){
                    is AuthState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ){
                            CircularProgressIndicator()
                        }
                    }
                    is AuthState.Authenticated -> {
                        RootNavigation(
                            navController = navController,
                            startDestination = ScreenRoute.MAIN_GRAPH_ROUTE,
                            onSyncRequest = { viewModel.startSync() }
                        )
                    }
                    is AuthState.Unauthenticated -> {
                        RootNavigation(
                            navController = navController,
                            startDestination = ScreenRoute.AUTH_GRAPH_ROUTE,
                            onSyncRequest = { viewModel.startSync() }
                        )
                    }
                }

            }
        }
    }
}
