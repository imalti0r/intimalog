package com.intima.log.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text as M3Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.intima.log.ui.calendar.CalendarScreen
import com.intima.log.ui.home.HomeScreen
import com.intima.log.ui.lock.AppLockGate
import com.intima.log.ui.me.MeScreen
import com.intima.log.ui.onboarding.OnboardingScreen
import com.intima.log.ui.partner.CombinationDetailScreen
import com.intima.log.ui.partner.CombinationListScreen
import com.intima.log.ui.partner.PartnerListScreen
import com.intima.log.ui.record.RecordEditScreen
import com.intima.log.ui.me.TagsManageScreen
import com.intima.log.ui.settings.BackupScreen
import com.intima.log.ui.settings.SettingsScreen
import com.intima.log.ui.stats.StatsScreen
import com.intima.log.ui.template.TemplateScreen
import com.intima.log.ui.theme.LocalIntimaPalette

/** 路由表 */
object Routes {
    const val HOME = "home"
    const val STATS = "stats"
    const val ME = "me"
    const val CALENDAR = "calendar"
    const val PARTNERS = "partners"
    const val TEMPLATES = "templates"
    const val SETTINGS = "settings"
    const val BACKUP = "backup"
    const val POSITIONS = "positions"
    const val TOYS = "toys"
    const val PROTECTIONS = "protections"
    const val PLACES = "places"
    const val COMBINATIONS = "combinations"
    const val RECORD_EDIT = "record_edit?recordId={recordId}"
    const val COMBINATION_DETAIL = "combination_detail/{partnerIds}"

    fun recordEdit(recordId: Long = 0) = "record_edit?recordId=$recordId"
    fun combinationDetail(partnerIds: List<Long>) =
        "combination_detail/${partnerIds.joinToString(",")}"
}

/** 底部导航的三 个主 Tab */
private data class MainTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val MAIN_TABS = listOf(
    MainTab(Routes.HOME, "首页", Icons.Rounded.Home),
    MainTab(Routes.CALENDAR, "日历", Icons.Rounded.CalendarMonth),
    MainTab(Routes.STATS, "统计", Icons.Rounded.BarChart),
    MainTab(Routes.ME, "我的", Icons.Rounded.Person),
)

/** 主 Tab 序号（决定切换方向）；非主 Tab 返回 -1 */
private fun tabIndex(route: String?) = MAIN_TABS.indexOfFirst { it.route == route }

private val EASE_OUT = CubicBezierEasing(0.25f, 0.8f, 0.25f, 1f)
private val EASE_IN = CubicBezierEasing(0.4f, 0f, 1f, 1f)
private val EASE_IN_OUT = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)
private const val TAB_DUR = 280
private const val PAGE_DUR = 300

@Composable
fun IntimaNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in MAIN_TABS.map { it.route }

    Column(modifier = Modifier.fillMaxSize().background(LocalIntimaPalette.current.bg)) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .weight(1f)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
            // 统一过渡策略：
            // 1) 主 Tab ↔ 主 Tab：无动画，瞬时切换（状态经 saveState/restoreState 保留，不重建）
            // 2) 一级 → 二级（push）：二级从右滑入覆盖，一级静止
            // 3) 返回（pop）：二级向右滑出，一级原位露出
            enterTransition = {
                val from = initialState.destination.route
                val to = targetState.destination.route
                if (tabIndex(from) >= 0 && tabIndex(to) >= 0) EnterTransition.None
                else slideInHorizontally(tween(PAGE_DUR, easing = EASE_IN_OUT)) { it }
                    .plus(fadeIn(tween(PAGE_DUR, easing = EASE_IN_OUT)))
            },
            exitTransition = {
                val from = initialState.destination.route
                val to = targetState.destination.route
                if (tabIndex(from) >= 0 && tabIndex(to) >= 0) ExitTransition.None
                else ExitTransition.None   // 一级页保持不动，被二级覆盖
            },
            popEnterTransition = { EnterTransition.None }, // 返回时一级页原位露出
            popExitTransition = {
                // 一级→一级 pop（理论不发生）也无动画；二级 pop 向右滑出
                val from = initialState.destination.route
                if (tabIndex(from) >= 0) ExitTransition.None
                else slideOutHorizontally(tween(PAGE_DUR, easing = EASE_IN_OUT)) { it }
                    .plus(fadeOut(tween(PAGE_DUR, easing = EASE_IN_OUT)))
            },
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onStartEdit = { id -> navController.navigate(Routes.recordEdit(id)) },
                )
            }
            composable(Routes.STATS) { StatsScreen() }
            composable(Routes.ME) {
                MeScreen(
                    onNavigate = { route -> navController.navigate(route) },
                )
            }
            composable(Routes.CALENDAR) {
                CalendarScreen(onEditRecord = { id -> navController.navigate(Routes.recordEdit(id)) })
            }
            composable(Routes.PARTNERS) {
                PartnerListScreen(
                    onOpenCombinations = { navController.navigate(Routes.COMBINATIONS) }
                )
            }
            composable(Routes.TEMPLATES) { TemplateScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(Routes.BACKUP) { BackupScreen() }
            composable(Routes.POSITIONS) {
                TagsManageScreen(title = "体位管理", kind = "position", onBack = { navController.popBackStack() })
            }
            composable(Routes.TOYS) {
                TagsManageScreen(title = "玩具管理", kind = "toy", onBack = { navController.popBackStack() })
            }
            composable(Routes.PROTECTIONS) {
                TagsManageScreen(title = "保护措施管理", kind = "protection", onBack = { navController.popBackStack() })
            }
            composable(Routes.PLACES) {
                TagsManageScreen(title = "地点管理", kind = "place", onBack = { navController.popBackStack() })
            }
            composable(Routes.COMBINATIONS) {
                CombinationListScreen(
                    onOpen = { ids -> navController.navigate(Routes.combinationDetail(ids)) }
                )
            }
            composable(
                Routes.RECORD_EDIT,
                arguments = listOf(navArgument("recordId") {
                    type = NavType.LongType; defaultValue = 0L
                })
            ) {
                RecordEditScreen(
                    onDone = { navController.popBackStack() },
                    onManageTags = { kind ->
                        navController.navigate(
                            when (kind) {
                                "toy" -> Routes.TOYS
                                "partner" -> Routes.PARTNERS
                                "protection" -> Routes.PROTECTIONS
                                "place" -> Routes.PLACES
                                else -> Routes.POSITIONS
                            }
                        )
                    },
                )
            }
            composable(
                Routes.COMBINATION_DETAIL,
                arguments = listOf(navArgument("partnerIds") { type = NavType.StringType })
            ) { entry ->
                val ids = entry.arguments?.getString("partnerIds")
                    ?.split(",")?.mapNotNull { it.toLongOrNull() } ?: emptyList()
                CombinationDetailScreen(partnerIds = ids)
            }
        }

        if (showBottomBar) {
            BottomNavBar(
                currentRoute = currentRoute,
                onSelect = { tab ->
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    }
}

/** Miuix 风格底部导航栏：白底、胶囊高亮 */
@Composable
private fun BottomNavBar(
    currentRoute: String?,
    onSelect: (MainTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LocalIntimaPalette.current.card)
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MAIN_TABS.forEach { tab ->
            val selected = currentRoute == tab.route
            val tint = if (selected) Color(0xFF3482FF) else LocalIntimaPalette.current.textHint
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(tab.icon, contentDescription = tab.label, tint = tint)
                M3Text(tab.label, fontSize = 11.sp, color = tint)
            }
        }
    }
}
