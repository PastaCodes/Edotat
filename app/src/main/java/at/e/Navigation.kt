package at.e

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import at.e.lib.Direction
import at.e.ui.Transitions.slidingComposable
import at.e.ui.home.FindTable
import at.e.ui.home.Redirect
import at.e.ui.loading.Loading
import at.e.ui.login.Login
import at.e.ui.login.Register
import at.e.ui.orders.RecentOrders
import at.e.ui.settings.AccountAndSettings
import kotlinx.serialization.Serializable

object Navigation {
    @Serializable
    sealed interface Destination {
        @Serializable
        data object Loading : Destination

        @Serializable
        data object Login : Destination

        @Serializable
        data object Register : Destination

        @Serializable
        data object Home : Destination {

            @Serializable
            data object Redirect : Destination

            object FindTable {
                @Serializable
                data object ChooseMethod : Destination

                object Method {
                    @Serializable
                    data class QrCode(val isInitial: Boolean = false) : Destination

                    @Serializable
                    data class NearMe(val isInitial: Boolean = false) : Destination

                    @Serializable
                    data class Search(val isInitial: Boolean = false) : Destination
                }

                @Serializable
                data object EnterCode : Destination
            }
        }

        @Serializable
        data object RecentOrders : Destination {
            @Serializable
            data object Main : Destination
        }

        @Serializable
        data object AccountAndSettings : Destination {
            @Serializable
            data object Main : Destination
        }
    }

    @Composable
    fun Setup(
        nc: NavHostController,
        activity: FragmentActivity,
        gvm: GlobalViewModel,
        innerPadding: PaddingValues,
    ) {
        NavHost(
            navController = nc,
            startDestination = Destination.Loading,
        ) {
            composable<Destination.Loading> {
                gvm.bottomBar(false)
                Loading.Screen(innerPadding, activity, gvm, nc)
            }
            composable<Destination.Login> {
                gvm.bottomBar(false)
                Login.Screen(innerPadding, activity, gvm, nc)
            }
            composable<Destination.Register> {
                gvm.bottomBar(false)
                Register.Screen(innerPadding, activity, gvm, nc)
            }
            navigation<Destination.Home>(startDestination = Destination.Home.Redirect) {
                composable<Destination.Home.Redirect> {
                    Redirect(gvm, nc)
                }
                slidingComposable<Destination.Home.FindTable.ChooseMethod>(
                    forcedDirection = gvm.forcedTransitionDirection,
                ) {
                    gvm.bottomBar(true)
                    FindTable.ChooseMethod.Screen(innerPadding, gvm, nc)
                }
                slidingComposable<Destination.Home.FindTable.Method.QrCode>(
                    forcedDirection = gvm.forcedTransitionDirection,
                ) {
                    gvm.bottomBar(true)
                    FindTable.Method.QrCode.Screen(innerPadding, gvm, nc)
                }
                slidingComposable<Destination.Home.FindTable.Method.NearMe>(
                    forcedDirection = gvm.forcedTransitionDirection,
                ) { backStackEntry ->
                    gvm.bottomBar(true)
                    val route = backStackEntry.toRoute<Destination.Home.FindTable.Method.NearMe>()
                    FindTable.Method.NearMe.Screen(innerPadding, route.isInitial, gvm, nc)
                }
                slidingComposable<Destination.Home.FindTable.Method.Search>(
                    forcedDirection = gvm.forcedTransitionDirection,
                ) { backStackEntry ->
                    gvm.bottomBar(true)
                    val route = backStackEntry.toRoute<Destination.Home.FindTable.Method.Search>()
                    FindTable.Method.Search.Screen(innerPadding, route.isInitial, gvm, nc)
                }
                slidingComposable<Destination.Home.FindTable.EnterCode>(
                    forcedDirection = gvm.forcedTransitionDirection,
                ) {
                    gvm.bottomBar(true)
                    FindTable.EnterCode.Screen(innerPadding, gvm, nc)
                }
            }
            navigation<Destination.RecentOrders>(
                startDestination = Destination.RecentOrders.Main,
            ) {
                slidingComposable<Destination.RecentOrders.Main>(
                    forcedDirection = gvm.forcedTransitionDirection,
                ) {
                    gvm.bottomBar(true)
                    RecentOrders.Screen(innerPadding, gvm)
                }
            }
            navigation<Destination.AccountAndSettings>(
                startDestination = Destination.AccountAndSettings.Main,
            ) {
                slidingComposable<Destination.AccountAndSettings.Main>(
                    forcedDirection = gvm.forcedTransitionDirection,
                ) {
                    gvm.bottomBar(true)
                    AccountAndSettings.Screen(innerPadding, activity, gvm, nc)
                }
            }
        }
    }

    private fun getTabIndex(tab: Destination) =
        when (tab) {
            Destination.Home -> 0
            Destination.RecentOrders -> 1
            Destination.AccountAndSettings -> 2
            else -> throw IllegalArgumentException()
        }

    fun getTabSwitchDirection(from: Destination?, to: Destination?): Direction.Horizontal? {
        if (from == null || to == null) {
            return null
        }
        val fromIndex = getTabIndex(from)
        val toIndex = getTabIndex(to)
        return when {
            toIndex < fromIndex -> Direction.RightToLeft
            toIndex > fromIndex -> Direction.LeftToRight
            else -> null
        }
    }

    private val tabPrefixes =
        listOf(Destination.Home, Destination.RecentOrders, Destination.AccountAndSettings)
            .associateBy { it::class.qualifiedName!! }

    fun getTab(destination: NavDestination): Destination? {
        val currentRoute = destination.route ?: return null
        return tabPrefixes.entries
            .firstOrNull { (prefix, _) -> currentRoute.startsWith(prefix) }
            ?.value
    }

    fun switchTab(tab: Destination, nc: NavController) {
        when (tab) {
            Destination.Home -> {
                nc.navigate(route = Destination.Home, ClearBackStack)
            }
            Destination.RecentOrders -> {
                nc.navigate(route = Destination.Home, ClearBackStack)
                nc.navigate(route = Destination.RecentOrders)
            }
            Destination.AccountAndSettings -> {
                nc.navigate(route = Destination.Home, ClearBackStack)
                nc.navigate(route = Destination.AccountAndSettings)
            }
            else -> throw IllegalArgumentException()
        }
    }

    val ClearBackStack: NavOptionsBuilder.() -> Unit = {
        popUpTo(0) {
            inclusive = true
        }
    }
}
