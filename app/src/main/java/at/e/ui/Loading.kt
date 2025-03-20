package at.e.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import at.e.LoadPreference
import at.e.Navigation
import at.e.ui.FindTable.toRoute

object Loading {
    context(Context)
    @Composable
    fun Screen(navController: NavController) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        var preferredMethod by UiState.remember<FindTable.Method>()
        LaunchedEffect(preferredMethod) {
            when (val method = preferredMethod) {
                is UiState.Loading -> {
                    LoadPreference.string("find_table_preferred_method") {
                        preferredMethod = UiState.Data(FindTable.Method.fromPreference(it))
                    }
                }
                is UiState.Data -> {
                    // TODO choose method screen should be in the back stack
                    navController.navigate(method.data.toRoute()) {
                        // TODO check whether this works or not
                        popUpTo(Navigation.Destination.Loading) { inclusive = true }
                    }
                }
            }
        }
    }
}
