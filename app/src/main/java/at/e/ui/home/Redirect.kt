package at.e.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import at.e.GlobalViewModel
import at.e.ui.home.FindTable.Method.Companion.toRoute

@Composable
fun Redirect(gvm: GlobalViewModel, nc: NavController) {
    val orderState by gvm.orderState.collectAsState()
    val ftmpState by gvm.findTableMethodPreference.collectAsState()

    LaunchedEffect(orderState, ftmpState) {
        if (orderState is GlobalViewModel.OrderState.Active) {
            nc.popBackStack() // Forget redirect
            nc.navigate(route = TODO())
        } else if (orderState is GlobalViewModel.OrderState.None) {
            val findTableMethodPreference = FindTable.Method.fromPreference(ftmpState.forceData)
            nc.popBackStack() // Forget redirect
            nc.navigate(route = findTableMethodPreference.toRoute(isInitial = true))
        }
    }
}
