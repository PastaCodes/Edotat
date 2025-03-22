package at.e

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import at.e.ui.FindTable
import at.e.ui.Loading
import at.e.ui.Transitions.slidingComposable
import kotlinx.serialization.Serializable

object Navigation {
    object Destination {
        @Serializable
        object Loading

        object FindTable {
            @Serializable
            object ChooseMethod

            object Method {
                @Serializable
                data class QrCode(val isInitial: Boolean = false)

                @Serializable
                data class NearMe(val isInitial: Boolean = false)

                @Serializable
                data class Search(val isInitial: Boolean = false)
            }
        }
    }

    context(Context)
    @Composable
    fun Setup(innerPadding: PaddingValues) {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = Destination.Loading,
        ) {
            composable<Destination.Loading> {
                Loading.Screen(innerPadding, navController)
            }
            slidingComposable<Destination.FindTable.ChooseMethod> {
                FindTable.ChooseMethod.Screen(innerPadding, navController)
            }
            slidingComposable<Destination.FindTable.Method.QrCode> {
                // TODO
            }
            slidingComposable<Destination.FindTable.Method.NearMe> {
                // TODO
            }
            slidingComposable<Destination.FindTable.Method.Search> { backStackEntry ->
                val route = backStackEntry.toRoute<Destination.FindTable.Method.Search>()
                FindTable.Method.Search.Screen(innerPadding, navController, isInitial = route.isInitial)
            }
        }
    }
}
