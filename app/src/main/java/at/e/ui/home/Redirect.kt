package at.e.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import at.e.GlobalViewModel
import at.e.Navigation

@Composable
fun Redirect(gvm: GlobalViewModel) {
    val orderState by gvm.orderState.collectAsState()
    val ftmpState by gvm.findTableMethodPreference.collectAsState()

    LaunchedEffect(orderState, ftmpState) {
        if (orderState is GlobalViewModel.OrderState.Active) {
            gvm.nc.popBackStack() // Forget redirect
            gvm.nc.navigate(route = TODO())
        } else if (orderState is GlobalViewModel.OrderState.None) {
            val findTableMethodPreference = FindTable.Method.fromPreference(ftmpState.forceData)
            gvm.nc.popBackStack() // Forget redirect
            gvm.nc.navigate(route = Navigation.Destination.Home.FindTable.ChooseMethod)
            if (findTableMethodPreference != null) {
                gvm.nc.navigate(route = findTableMethodPreference.route(/* isInitial = */ true))
            }
        }
    }
}
