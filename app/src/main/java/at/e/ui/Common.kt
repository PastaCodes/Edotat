package at.e.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import at.e.GlobalViewModel
import at.e.Navigation
import at.e.R
import at.e.ui.theme.EdotatIcons
import at.e.ui.theme.EdotatTheme

context(GlobalViewModel)
fun Modifier.shakeable(condition: Boolean = true) =
    if (condition) this.offset(shakeOffset) else this

object Common {
    context(Context)
    @Composable
    fun Container(
        gvm: GlobalViewModel,
        nc: NavController,
        content: @Composable (PaddingValues, (Boolean) -> Unit) -> Unit,
    ) {
        val (bottomBarVisible, setBottomBarVisible) = remember { mutableStateOf(false) }
        EdotatTheme.Apply {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = { if (bottomBarVisible) BottomBar(nc) },
                snackbarHost = { gvm.SnackbarHost() },
                content = { innerPadding -> content(innerPadding, setBottomBarVisible) },
            )
        }
    }

    private data class BottomButton(
        val imageVector: ImageVector,
        @StringRes val textResId: Int,
        val route: Any,
    )

    private val bottomButtons = arrayOf(
        BottomButton(
            EdotatIcons.Meal,
            R.string.bottom_bar_home,
            Navigation.Destination.Home
        ),
        BottomButton(
            EdotatIcons.Recent,
            R.string.bottom_bar_orders,
            Navigation.Destination.RecentOrders
        ),
        BottomButton(
            EdotatIcons.Account,
            R.string.bottom_bar_account_and_settings,
            Navigation.Destination.AccountAndSettings
        ),
    )

    context(Context)
    @Composable
    fun BottomBar(nc: NavController) {
        var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
        NavigationBar {
            bottomButtons.forEachIndexed { index, button ->
                NavigationBarItem(
                    selected = selectedTabIndex == index,
                    onClick = {
                        selectedTabIndex = index
                        nc.navigate(route = button.route)
                    },
                    icon = {
                        Icon(
                            imageVector = button.imageVector,
                            contentDescription = null, // Icons are decorative
                        )
                    },
                    label = {
                        Text(
                            text = getString(button.textResId),
                        )
                    }
                )
            }
        }
    }
}
