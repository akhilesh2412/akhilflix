package com.akhilflix.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.akhilflix.app.data.model.AppConfig
import com.akhilflix.app.data.model.PluginInfo
import com.akhilflix.app.data.model.Video
import com.akhilflix.app.data.remote.AppRepository
import com.akhilflix.app.ui.ActionHandler
import com.akhilflix.app.ui.components.AppBottomBar
import com.akhilflix.app.ui.screens.CourseDetailScreen
import com.akhilflix.app.ui.screens.ExoPlayerScreen
import com.akhilflix.app.ui.screens.HomeScreen
import com.akhilflix.app.ui.screens.MyListScreen
import com.akhilflix.app.ui.screens.PluginDetailScreen
import com.akhilflix.app.ui.screens.PluginHomeScreen
import com.akhilflix.app.ui.screens.ReposScreen
import com.akhilflix.app.ui.screens.SearchScreen
import com.akhilflix.app.ui.screens.VideoPlayerScreen
import com.akhilflix.app.ui.theme.AkhilFlixTheme
import com.akhilflix.app.ui.viewmodel.HomeViewModel
import com.akhilflix.app.ui.viewmodel.PluginViewModel

/**
 * The plugin's whole app. The host only calls this from the plugin
 * interface — everything (navigation, screens, player) lives here.
 */
@Composable
fun AkhilFlixRoot() {
    val context = LocalContext.current
    val repository = remember { AppRepository(context) }
    val factory = remember {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HomeViewModel(repository) as T
        }
    }
    val viewModel: HomeViewModel = viewModel(factory = factory)
    val pluginFactory = remember {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                PluginViewModel(context.applicationContext) as T
        }
    }
    val pluginViewModel: PluginViewModel = viewModel(factory = pluginFactory)
    val state by viewModel.uiState.collectAsState()
    AkhilFlixTheme(theme = state.config.theme) {
        AkhilFlixNav(
            viewModel = viewModel,
            pluginViewModel = pluginViewModel,
            config = state.config,
            repository = repository
        )
    }
}

@Composable
private fun AkhilFlixNav(
    viewModel: HomeViewModel,
    pluginViewModel: PluginViewModel,
    config: AppConfig,
    repository: AppRepository
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: "home"
    // Bottom bar only on the tab screens, never on detail/player.
    val tabScreens = config.tabs.map { it.screen }
    val showBar = currentRoute in tabScreens
    // The plugin the user tapped (its .cs3 is loaded by PluginRunner).
    var currentPlugin by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<PluginInfo?>(null)
    }

    Scaffold(
        bottomBar = {
            if (showBar) {
                AppBottomBar(
                    tabs = config.tabs,
                    currentScreen = currentRoute,
                    onTabClick = { tab ->
                        navController.navigate(tab.screen) {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onCourseClick = { course, action ->
                        ActionHandler.courseClick(course, action, navController)
                    },
                    onWatchClick = { course ->
                        course.videos.firstOrNull()?.let {
                            navController.navigate("player/${it.id}")
                        }
                    },
                    onActionClick = { action ->
                        ActionHandler.perform(action, navController, context)
                    },
                    onReposClick = { navController.navigate("repos") },
                    onPluginClick = { plugin ->
                        currentPlugin = plugin
                        pluginViewModel.openPlugin(plugin)
                        navController.navigate("pluginHome")
                    }
                )
            }
            composable("courses") {
                HomeScreen(
                    viewModel = viewModel,
                    onCourseClick = { course, action ->
                        ActionHandler.courseClick(course, action, navController)
                    },
                    onWatchClick = { course ->
                        course.videos.firstOrNull()?.let {
                            navController.navigate("player/${it.id}")
                        }
                    },
                    onActionClick = { action ->
                        ActionHandler.perform(action, navController, context)
                    },
                    onReposClick = { navController.navigate("repos") },
                    onPluginClick = { plugin ->
                        currentPlugin = plugin
                        pluginViewModel.openPlugin(plugin)
                        navController.navigate("pluginHome")
                    }
                )
            }
            composable("repos") {
                ReposScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("search") {
                SearchScreen(
                    viewModel = viewModel,
                    onCourseClick = { navController.navigate("detail/${it.id}") }
                )
            }
            composable("mylist") {
                MyListScreen(
                    viewModel = viewModel,
                    repository = repository,
                    onCourseClick = { navController.navigate("detail/${it.id}") }
                )
            }
            composable(
                route = "detail/{courseId}",
                arguments = listOf(navArgument("courseId") { type = NavType.StringType })
            ) { entry ->
                val courseId = entry.arguments?.getString("courseId") ?: ""
                CourseDetailScreen(
                    viewModel = viewModel,
                    repository = repository,
                    courseId = courseId,
                    onVideoClick = { navController.navigate("player/${it.id}") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "player/{videoId}",
                arguments = listOf(navArgument("videoId") { type = NavType.StringType })
            ) { entry ->
                val videoId = entry.arguments?.getString("videoId") ?: ""
                VideoPlayerScreen(
                    viewModel = viewModel,
                    videoId = videoId,
                    onBack = { navController.popBackStack() }
                )
            }
            // --- Running CloudStream plugins (.cs3) ---
            composable("pluginHome") {
                val plugin = currentPlugin
                if (plugin == null) {
                    navController.popBackStack()
                } else {
                    PluginHomeScreen(
                        viewModel = pluginViewModel,
                        plugin = plugin,
                        onItemClick = { item ->
                            pluginViewModel.openItem(item)
                            navController.navigate("pluginDetail")
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable("pluginDetail") {
                PluginDetailScreen(
                    viewModel = pluginViewModel,
                    onStreamReady = { navController.navigate("pluginPlayer") },
                    onBack = {
                        pluginViewModel.clearDetail()
                        navController.popBackStack()
                    }
                )
            }
            composable("pluginPlayer") {
                val pluginState by pluginViewModel.uiState.collectAsState()
                val stream = pluginState.pendingStream
                if (stream == null) {
                    navController.popBackStack()
                } else {
                    ExoPlayerScreen(
                        video = Video(
                            id = "plugin-stream",
                            title = stream.title,
                            streamUrl = stream.url,
                            subtitleUrl = stream.subtitleUrl
                        ),
                        streamUrl = stream.url,
                        onBack = {
                            pluginViewModel.clearStream()
                            navController.popBackStack()
                        },
                        headers = stream.headers
                    )
                }
            }
        }
    }
}
