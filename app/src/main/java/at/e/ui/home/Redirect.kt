package at.e.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import at.e.GlobalViewModel
import at.e.Navigation

@Composable
fun Redirect(gvm: GlobalViewModel, nc: NavController) {
    val orderState by gvm.orderState.collectAsState()
    val ftmpState by gvm.findTableMethodPreference.collectAsState()

    LaunchedEffect(orderState, ftmpState) {
        gvm.notifySwitchingTab()
        if (orderState is GlobalViewModel.OrderState.Active) {
            nc.popBackStack() // Forget redirect
            nc.navigate(route = TODO())
        } else {
            gvm.resetOrder()
            val findTableMethodPreference = FindTable.Method.fromPreference(ftmpState.forceData)
            nc.popBackStack() // Forget redirect
            nc.navigate(route = Navigation.Destination.Home.FindTable.ChooseMethod)
            if (findTableMethodPreference != null) {
                nc.navigate(route = findTableMethodPreference.route(/* isInitial = */ true))
            }
            gvm.notifyFinishedSwitchingTab()
        }
    }
}
