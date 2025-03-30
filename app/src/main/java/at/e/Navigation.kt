package at.e

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import at.e.ui.Transitions.slidingComposable
import at.e.ui.home.FindTable
import at.e.ui.home.Redirect
import at.e.ui.loading.Loading
import at.e.ui.login.Login
import at.e.ui.login.Register
import kotlinx.serialization.Serializable

object Navigation {
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
        data object RecentOrders : Destination

        @Serializable
        data object AccountAndSettings : Destination
    }

    @Composable
    fun Setup(
        nc: NavHostController,
        gvm: GlobalViewModel,
        innerPadding: PaddingValues,
    ) {
        NavHost(
            navController = nc,
            startDestination = Destination.Loading,
        ) {
            composable<Destination.Loading> {
                gvm.bottomBar(false)
                Loading.Screen(innerPadding, gvm)
            }
            composable<Destination.Login> {
                gvm.bottomBar(false)
                Login.Screen(innerPadding, gvm)
            }
            composable<Destination.Register> {
                gvm.bottomBar(false)
                Register.Screen(innerPadding, gvm)
            }
            navigation<Destination.Home>(startDestination = Destination.Home.Redirect) {
                composable<Destination.Home.Redirect> {
                    Redirect(gvm)
                }
                slidingComposable<Destination.Home.FindTable.ChooseMethod> {
                    gvm.bottomBar(true)
                    FindTable.ChooseMethod.Screen(innerPadding, gvm)
                }
                slidingComposable<Destination.Home.FindTable.Method.QrCode> {
                    gvm.bottomBar(true)
                    FindTable.Method.QrCode.Screen(innerPadding, gvm)
                }
                slidingComposable<Destination.Home.FindTable.Method.NearMe> { backStackEntry ->
                    gvm.bottomBar(true)
                    val route = backStackEntry.toRoute<Destination.Home.FindTable.Method.NearMe>()
                    FindTable.Method.NearMe.Screen(innerPadding, route.isInitial, gvm)
                }
                slidingComposable<Destination.Home.FindTable.Method.Search> { backStackEntry ->
                    gvm.bottomBar(true)
                    val route = backStackEntry.toRoute<Destination.Home.FindTable.Method.Search>()
                    FindTable.Method.Search.Screen(innerPadding, route.isInitial, gvm)
                }
                slidingComposable<Destination.Home.FindTable.EnterCode> {
                    gvm.bottomBar(true)
                    FindTable.EnterCode.Screen(innerPadding, gvm)
                }
            }
            /*
            navigation<Destination.RecentOrders>(startDestination = TODO()) {
                // TODO
            }
            navigation<Destination.AccountAndSettings>(startDestination = TODO()) {
                // TODO
            }
             */
        }
    }

    val ClearBackStack: NavOptionsBuilder.() -> Unit = {
        popUpTo(0) {
            inclusive = true
        }
    }
}
