package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ai.configureAppCheck
import com.example.ui.ClipCraftViewModel
import com.example.ui.screens.ExportsScreen
import com.example.ui.screens.LinkCheckScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.ClipCraftTheme
import kotlinx.serialization.Serializable

@Serializable
object StudioRoute

@Serializable
object LinkCheckRoute

@Serializable
object ExportsRoute

private data class NavTab(
    val labelRes: Int,
    val route: Any,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure Firebase App Check with launch intent before any UI or ViewModels
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            ClipCraftTheme(darkTheme = true) {
                ClipCraftApp()
            }
        }
    }
}

@Composable
fun ClipCraftApp(
    viewModel: ClipCraftViewModel = viewModel()
) {
    val navController = rememberNavController()
    val studioState by viewModel.studioState.collectAsStateWithLifecycle()
    val linkCheckState by viewModel.linkCheckState.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val exportedClips by viewModel.exportedClips.collectAsStateWithLifecycle()

    val tabs = listOf(
        NavTab(
            labelRes = R.string.nav_studio,
            route = StudioRoute,
            selectedIcon = Icons.Filled.Crop,
            unselectedIcon = Icons.Outlined.Crop,
            testTag = "nav_tab_studio"
        ),
        NavTab(
            labelRes = R.string.nav_link_check,
            route = LinkCheckRoute,
            selectedIcon = Icons.Filled.VerifiedUser,
            unselectedIcon = Icons.Outlined.VerifiedUser,
            testTag = "nav_tab_link_check"
        ),
        NavTab(
            labelRes = R.string.nav_exports,
            route = ExportsRoute,
            selectedIcon = Icons.Filled.FolderCopy,
            unselectedIcon = Icons.Outlined.FolderCopy,
            testTag = "nav_tab_exports"
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    fun navigateToTab(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.hasRoute(tab.route::class)
                            } == true
                            val label = stringResource(tab.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.hasRoute(tab.route::class)
                            } == true
                            val label = stringResource(tab.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = StudioRoute,
                    modifier = Modifier.weight(1f)
                ) {
                    composable<StudioRoute> {
                        StudioScreen(
                            studioState = studioState,
                            exportState = exportState,
                            onVideoPicked = viewModel::selectVideoUri,
                            onAnalyzeTopics = viewModel::analyzeVideoTopicsWithAi,
                            onApplyTopic = viewModel::applyTopicSuggestion,
                            onApplyAutoYouTubeReady = viewModel::applyAutoYouTubeReadyPreset,
                            onExportAllAiTopics = viewModel::exportAllAiTopics,
                            onSelectAspectPreset = viewModel::selectAspectPreset,
                            onPanCrop = viewModel::panCrop,
                            onResizeCropEdges = viewModel::updateCropEdges,
                            onResetCrop = viewModel::resetCrop,
                            onTrimChanged = viewModel::updateTrimRange,
                            onToggleMirror = viewModel::toggleMirror,
                            onRotate90 = viewModel::rotate90,
                            onAudioModeChanged = viewModel::setAudioMode,
                            onPickReplacementAudio = viewModel::selectReplacementAudio,
                            onExportQualityChanged = viewModel::setExportQuality,
                            onCreatorHandleChanged = viewModel::updateCreatorHandle,
                            onHookCaptionChanged = viewModel::updateHookCaption,
                            onOutputTitleChanged = viewModel::updateOutputTitle,
                            onStartExport = viewModel::startExport,
                            onCancelExport = viewModel::cancelExport,
                            onDismissExportBanner = viewModel::dismissExportBanner,
                            onNavigateToLinkCheck = { navigateToTab(LinkCheckRoute) },
                            onNavigateToExports = { navigateToTab(ExportsRoute) }
                        )
                    }

                    composable<LinkCheckRoute> {
                        LinkCheckScreen(
                            uiState = linkCheckState,
                            onUrlChange = viewModel::updateUrlInput,
                            onInspectUrl = viewModel::inspectYouTubeUrl,
                            onToggleLanguage = viewModel::toggleLanguageMode,
                            onToggleChecklistItem = viewModel::toggleChecklistItem,
                            onOpenStudio = { navigateToTab(StudioRoute) }
                        )
                    }

                    composable<ExportsRoute> {
                        ExportsScreen(
                            clips = exportedClips,
                            onDeleteClip = viewModel::deleteExportedClip,
                            onOpenStudio = { navigateToTab(StudioRoute) }
                        )
                    }
                }
            }
        }
    }
}
