package at.e

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import at.e.ui.home.FindTable
import at.e.ui.loading.Loading
import at.e.ui.Transitions.slidingComposable
import at.e.ui.home.Redirect
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
            }
        }

        @Serializable
        data object RecentOrders : Destination

        @Serializable
        data object AccountAndSettings : Destination
    }

    context(Context)
    @Composable
    fun Setup(
        nc: NavHostController,
        gvm: GlobalViewModel,
        innerPadding: PaddingValues,
        setBottomBarVisible: (Boolean) -> Unit,
    ) {
        NavHost(
            navController = nc,
            startDestination = Destination.Loading,
        ) {
            composable<Destination.Loading> {
                Loading.Screen(innerPadding, gvm, nc)
                setBottomBarVisible(false)
            }
            composable<Destination.Login> {
                Login.Screen(innerPadding, gvm, nc)
                setBottomBarVisible(false)
            }
            composable<Destination.Register> {
                Register.Screen(innerPadding, gvm, nc)
                setBottomBarVisible(false)
            }
            navigation<Destination.Home>(startDestination = Destination.Home.Redirect) {
                composable<Destination.Home.Redirect> {
                    Redirect(gvm, nc)
                }
                slidingComposable<Destination.Home.FindTable.ChooseMethod> {
                    FindTable.ChooseMethod.Screen(innerPadding, gvm, nc)
                    setBottomBarVisible(true)
                }
                slidingComposable<Destination.Home.FindTable.Method.QrCode> {
                    // TODO
                    setBottomBarVisible(true)
                }
                slidingComposable<Destination.Home.FindTable.Method.NearMe> {
                    // TODO
                    setBottomBarVisible(true)
                }
                slidingComposable<Destination.Home.FindTable.Method.Search> { backStackEntry ->
                    val route = backStackEntry.toRoute<Destination.Home.FindTable.Method.Search>()
                    FindTable.Method.Search.Screen(innerPadding, route.isInitial, nc)
                    setBottomBarVisible(true)
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
}
