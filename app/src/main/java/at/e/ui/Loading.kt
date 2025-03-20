package at.e.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import at.e.LoadPreference
import at.e.Navigation

object Loading {
    context(Context)
    @Composable
    fun Screen(innerPadding: PaddingValues, navController: NavController) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }

        var preferredMethod by UiState.remember<FindTable.Method?>()
        LaunchedEffect(preferredMethod) {
            when (val method = preferredMethod) {
                is UiState.Loading -> {
                    LoadPreference.string("find_table_preferred_method") {
                        preferredMethod = UiState.Data(FindTable.Method.fromPreference("search"))
                    }
                }
                is UiState.Data -> {
                    // Forget the loading screen
                    navController.popBackStack()
                    // Add the ChooseMethod screen to the back stack, although we may redirect
                    // to the preferred method screen
                    navController.navigate(Navigation.Destination.FindTable.ChooseMethod)
                    if (method.data != null) {
                        // Navigate to the preferred method screen
                        navController.navigate(method.data.route(true))
                    }
                }
            }
        }
    }
}
