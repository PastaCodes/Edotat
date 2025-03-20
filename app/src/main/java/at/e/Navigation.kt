package at.e

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import at.e.ui.FindTable
import at.e.ui.Loading
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
                object QrCode

                @Serializable
                object NearMe

                @Serializable
                object Search
            }
        }
    }

    context(Context)
    @Composable
    fun Setup() {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = Loading,
        ) {
            composable<Destination.Loading> {
                Loading.Screen(navController)
            }
            composable<Destination.FindTable.ChooseMethod> {
                FindTable.ChooseMethod.Screen(navController)
            }
            composable<Destination.FindTable.Method.QrCode> {
                // TODO
            }
            composable<Destination.FindTable.Method.NearMe> {
                // TODO
            }
            composable<Destination.FindTable.Method.Search> {
                FindTable.Method.Search.Screen(navController)
            }
        }
    }
}
