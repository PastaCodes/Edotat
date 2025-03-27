package at.e.ui.loading

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import at.e.GlobalViewModel
import at.e.Navigation
import at.e.lib.LoadingState

object Loading {
    context(Context)
    @Composable
    fun Screen(innerPadding: PaddingValues, gvm: GlobalViewModel, nc: NavController) {
        val loginState by gvm.loginState.collectAsState()
        val orderState by gvm.orderState.collectAsState()
        val ftmpState by gvm.findTableMethodPreference.collectAsState()

        LaunchedEffect(Unit) {
            gvm.loadFindTableMethodPreference() // Preload for good measure
        }
        LaunchedEffect(loginState) {
            when (loginState) {
                is GlobalViewModel.LoginState.Loading -> gvm.tryAutoLogin()
                is GlobalViewModel.LoginState.AutoLoginFailed -> {
                    nc.popBackStack() // Forget loading screen
                    nc.navigate(route = Navigation.Destination.Login)
                }
                is GlobalViewModel.LoginState.LoggedIn -> gvm.loadActiveOrder()
                else -> gvm.logout()
            }
        }
        LaunchedEffect(orderState, ftmpState) {
            if (
                orderState is GlobalViewModel.OrderState.Active
            ||  (orderState is GlobalViewModel.OrderState.None && ftmpState is LoadingState.Data)
            ) {
                nc.popBackStack() // Forget loading screen
                nc.navigate(route = Navigation.Destination.Home)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
