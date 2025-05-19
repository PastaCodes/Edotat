package at.e.ui.orders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import at.e.GlobalViewModel

object RecentOrders {
    @Composable
    fun Screen(innerPadding: PaddingValues, gvm: GlobalViewModel) {
        gvm.notifyFinishedSwitchingTab()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            Text("RECENT ORDERS")
        }
    }
}
