package com.phms.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.phms.app.ui.screens.*
import com.phms.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

data class NavItem(val route: String, val label: String, val icon: ImageVector)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as PHMSApplication
        val viewModel = MainViewModel(app.repository, app.settingsRepository)

        setContent {
            PHMSTheme {
                PHMSApp(viewModel)
            }
        }
    }
}

@Composable
fun PHMSTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF4CAF50),
            onPrimary = Color.White,
            primaryContainer = Color(0xFF1B5E20),
            secondary = Color(0xFF81C784),
            background = Color(0xFF0D1117),
            surface = Color(0xFF161B22),
            surfaceVariant = Color(0xFF21262D),
            onBackground = Color(0xFFE6EDF3),
            onSurface = Color(0xFFE6EDF3),
            error = Color(0xFFFF5252)
        ),
        content = content
    )
}

val primaryNavItems = listOf(
    NavItem("dashboard", "Dashboard", Icons.Default.Home),
    NavItem("pigs", "Pigs", Icons.Default.Pets),
    NavItem("alerts", "Alerts", Icons.Default.Notifications),
    NavItem("feed", "Feed", Icons.Default.Grass),
    NavItem("more", "More", Icons.Default.GridView)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PHMSApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isOnboarded by viewModel.isOnboarded.collectAsState(initial = true)
    val farmSettings by viewModel.farmSettings.collectAsState()
    val activePigs by viewModel.activePigs.collectAsState()
    val activeAlerts by viewModel.activeAlerts.collectAsState()

    var showMoreSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Auto-launch Guided Tour on first launch after onboarding
    LaunchedEffect(isOnboarded, farmSettings.showTourOnFirstOpen) {
        if (isOnboarded && farmSettings.showTourOnFirstOpen) {
            viewModel.saveFarmSettings(farmSettings.copy(showTourOnFirstOpen = false))
            navController.navigate("help_center?autoTour=true")
        }
    }

    // Routes where bottom nav should show
    val topLevelRoutes = primaryNavItems.map { it.route } +
            listOf("health", "breeding", "market", "reports", "settings", "help_center")
    val showBottomBar = currentRoute in topLevelRoutes ||
            topLevelRoutes.any { currentRoute?.startsWith(it) == true }
    val showHeader = currentRoute != null && currentRoute != "onboarding" && !currentRoute.startsWith("pig_detail") &&
            !currentRoute.startsWith("add_edit_pig") && !currentRoute.startsWith("sale_flow") && currentRoute != "help_center"

    Scaffold(
        containerColor = Color(0xFF0D1117),
        topBar = {
            if (showHeader) {
                PHMSHeader(
                    farmName = farmSettings.farmName,
                    pigCount = activePigs.size,
                    alertCount = activeAlerts.size
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                PHMSBottomNav(
                    currentRoute = currentRoute,
                    alertCount = activeAlerts.count { it.priority == "Critical" },
                    onItemClick = { item ->
                        if (item.route == "more") {
                            showMoreSheet = true
                        } else {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = if (!isOnboarded) "onboarding" else "dashboard",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("onboarding") { OnboardingScreen(viewModel, navController) }
            composable("dashboard") { DashboardScreen(viewModel, navController) }
            composable("pigs") { PigsScreen(viewModel, navController) }
            composable("alerts") { AlertsScreen(viewModel) }
            composable("feed") { FeedScreen(viewModel) }
            composable("health") { HealthScreen(viewModel) }
            composable("breeding") { BreedingScreen(viewModel) }
            composable("market") {
                MarketScreen(viewModel, onStartSale = { navController.navigate("sale_flow") })
            }
            composable("reports") { ReportsHubScreen(viewModel) }
            composable("settings") { SettingsScreen(viewModel) }
            composable(
                route = "help_center?autoTour={autoTour}",
                arguments = listOf(navArgument("autoTour") { type = NavType.BoolType; defaultValue = false })
            ) { backStack ->
                val autoTour = backStack.arguments?.getBoolean("autoTour") ?: false
                HelpCenterScreen(viewModel = viewModel, navController = navController, autoStartTour = autoTour)
            }
            composable(
                route = "pig_detail/{pigId}",
                arguments = listOf(navArgument("pigId") { type = NavType.LongType })
            ) { backStack ->
                val pigId = backStack.arguments?.getLong("pigId") ?: return@composable
                PigDetailScreen(pigId = pigId, viewModel = viewModel, navController = navController)
            }
            composable(
                route = "add_edit_pig/{pigId}",
                arguments = listOf(navArgument("pigId") { type = NavType.LongType; defaultValue = -1L })
            ) { backStack ->
                val pigId = backStack.arguments?.getLong("pigId") ?: -1L
                AddEditPigScreen(pigId = pigId, viewModel = viewModel, navController = navController)
            }
            composable("sale_flow") {
                SaleFlowScreen(viewModel = viewModel, navController = navController)
            }
        }
    }

    if (showMoreSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF161B22),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            MoreMenuSheet(
                onNavigate = { route ->
                    showMoreSheet = false
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun PHMSHeader(farmName: String, pigCount: Int, alertCount: Int) {
    val today = remember {
        SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date())
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF1B5E20), Color(0xFF2E7D32), Color(0xFF388E3C))
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🐖 $farmName",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = today,
                        color = Color(0xFFB9F6CA),
                        fontSize = 12.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x33FFFFFF)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Pets, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text("$pigCount pigs", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    if (alertCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFB71C1C)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Warning, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Text("$alertCount critical", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PHMSBottomNav(
    currentRoute: String?,
    alertCount: Int,
    onItemClick: (NavItem) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF161B22),
        tonalElevation = 0.dp,
        modifier = Modifier.clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        primaryNavItems.forEach { item ->
            val selected = when (item.route) {
                "more" -> currentRoute in listOf("health", "breeding", "market", "reports", "settings", "help_center")
                else -> currentRoute?.startsWith(item.route) == true
            }
            NavigationBarItem(
                selected = selected,
                onClick = { onItemClick(item) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (item.route == "alerts" && alertCount > 0) {
                                Badge(containerColor = Color(0xFFFF5252)) {
                                    Text("$alertCount", fontSize = 9.sp)
                                }
                            }
                        }
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = item.label,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                label = {
                    Text(item.label, fontSize = 10.sp, maxLines = 1)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF4CAF50),
                    selectedTextColor = Color(0xFF4CAF50),
                    unselectedIconColor = Color(0xFF6E7681),
                    unselectedTextColor = Color(0xFF6E7681),
                    indicatorColor = Color(0xFF21262D)
                )
            )
        }
    }
}

@Composable
fun MoreMenuSheet(onNavigate: (String) -> Unit) {
    val moreItems = listOf(
        Triple("health", Icons.Default.LocalHospital, "Health & Vet"),
        Triple("breeding", Icons.Default.Favorite, "Breeding"),
        Triple("market", Icons.Default.ShoppingCart, "Market & Sales"),
        Triple("reports", Icons.Default.BarChart, "Reports"),
        Triple("settings", Icons.Default.Settings, "Settings"),
        Triple("help_center", Icons.Default.HelpOutline, "Help Center & Tour 🧭")
    )
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "More Modules",
            color = Color(0xFFE6EDF3),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        moreItems.forEach { (route, icon, label) ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF21262D),
                onClick = { onNavigate(route) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(icon, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                    Text(label, color = Color(0xFFE6EDF3), fontSize = 16.sp)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF6E7681), modifier = Modifier.size(20.dp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
