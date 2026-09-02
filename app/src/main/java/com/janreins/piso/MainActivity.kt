package com.janreins.piso

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janreins.piso.ui.MainTab
import com.janreins.piso.ui.MainViewModel
import com.janreins.piso.ui.MoreSubScreen
import com.janreins.piso.ui.accounts.AccountsScreen
import com.janreins.piso.ui.activity.ActivityScreen
import com.janreins.piso.ui.activity.TransactionDialog
import com.janreins.piso.ui.budgets.BudgetsScreen
import com.janreins.piso.ui.debts.DebtsScreen
import com.janreins.piso.ui.goals.GoalsScreen
import com.janreins.piso.ui.home.HomeScreen
import com.janreins.piso.ui.invest.InvestScreen
import com.janreins.piso.ui.more.MoreScreen
import com.janreins.piso.ui.settings.SettingsScreen
import com.janreins.piso.ui.theme.PisoTheme
import com.janreins.piso.ui.theme.TealContainer
import com.janreins.piso.ui.theme.TealPrimary
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PisoTheme {
                PisoApp(viewModel = viewModel)
            }
        }
    }
}

data class NavigationTabItem(
    val tab: MainTab,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun PisoApp(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val moreSubScreen by viewModel.moreSubScreen.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Quick Add Transaction Modal from Home
    var quickAddType by remember { mutableStateOf<String?>(null) }

    // Handle back button press
    BackHandler(enabled = moreSubScreen != null || currentTab != MainTab.HOME) {
        if (moreSubScreen != null) {
            viewModel.closeMoreSubScreen()
        } else if (currentTab != MainTab.HOME) {
            viewModel.selectTab(MainTab.HOME)
        }
    }

    // Collect Snackbar notifications
    LaunchedEffect(Unit) {
        viewModel.messageEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val tabs = remember {
        listOf(
            NavigationTabItem(MainTab.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
            NavigationTabItem(MainTab.ACTIVITY, "Activity", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong, "nav_activity"),
            NavigationTabItem(MainTab.ACCOUNTS, "Accounts", Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance, "nav_accounts"),
            NavigationTabItem(MainTab.GOALS, "Goals", Icons.Filled.Flag, Icons.Outlined.Flag, "nav_goals"),
            NavigationTabItem(MainTab.MORE, "More", Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz, "nav_more")
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            // Show bottom navigation on main tabs
            if (moreSubScreen == null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("bottom_navigation_bar")
                ) {
                    tabs.forEach { tabItem ->
                        val selected = currentTab == tabItem.tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { viewModel.selectTab(tabItem.tab) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tabItem.selectedIcon else tabItem.unselectedIcon,
                                    contentDescription = tabItem.title
                                )
                            },
                            label = { Text(text = tabItem.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TealPrimary,
                                selectedTextColor = TealPrimary,
                                indicatorColor = TealContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag(tabItem.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Check if user is inside a More sub-screen
            if (moreSubScreen != null) {
                when (moreSubScreen) {
                    MoreSubScreen.BUDGETS -> BudgetsScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeMoreSubScreen() }
                    )
                    MoreSubScreen.DEBTS -> DebtsScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeMoreSubScreen() }
                    )
                    MoreSubScreen.INVEST -> InvestScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeMoreSubScreen() }
                    )
                    MoreSubScreen.SETTINGS -> SettingsScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closeMoreSubScreen() }
                    )
                    null -> {}
                }
            } else {
                // Main 5 Tabs
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tab_transition"
                ) { tab ->
                    when (tab) {
                        MainTab.HOME -> HomeScreen(
                            viewModel = viewModel,
                            onOpenAddTransaction = { preselectedType ->
                                quickAddType = preselectedType
                            }
                        )
                        MainTab.ACTIVITY -> ActivityScreen(viewModel = viewModel)
                        MainTab.ACCOUNTS -> AccountsScreen(viewModel = viewModel)
                        MainTab.GOALS -> GoalsScreen(viewModel = viewModel)
                        MainTab.MORE -> MoreScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    // --- Quick Add Transaction Dialog from Home Screen ---
    quickAddType?.let { type ->
        TransactionDialog(
            preselectedType = type,
            accounts = accounts,
            onDismiss = { quickAddType = null },
            onSave = { newTx ->
                viewModel.addTransaction(newTx)
                quickAddType = null
            }
        )
    }
}
