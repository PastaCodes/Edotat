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
import at.e.Navigation
import at.e.UserPreferences
import at.e.ui.FindTable.Method.Companion.toRoute

object Loading {
    context(Context)
    @Composable
    fun Screen(navController: NavController, innerPadding: PaddingValues) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }

        var methodPreference by UiState.remember<Int>()
        LaunchedEffect(methodPreference) {
            when (val method = methodPreference) {
                is UiState.Loading -> {
                    UserPreferences.load(
                        key = UserPreferences.Keys.FindTablePreferredMethod,
                        defaultValue = FindTable.Method.NO_PREFERENCE,
                    ) {
                        methodPreference = UiState.Data(it)
                    }
                }
                is UiState.Data -> {
                    val preferredMethod = FindTable.Method.fromPreference(method.data)
                    navController.popBackStack()
                    navController.navigate(route = Navigation.Destination.FindTable.ChooseMethod)
                    navController.navigate(route = preferredMethod.toRoute(isInitial = true))
                }
            }
        }
    }
}
