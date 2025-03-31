package at.e.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.e.GlobalViewModel
import at.e.Navigation
import at.e.R
import at.e.lib.times
import at.e.ui.theme.EdotatIcons
import at.e.ui.theme.EdotatTheme
import kotlin.math.max

fun Modifier.shakeable(gvm: GlobalViewModel, condition: Boolean = true) =
    if (condition) this.offset(gvm.shakeOffset) else this

object Common {
    @Composable
    fun Container(
        gvm: GlobalViewModel,
        content: @Composable (PaddingValues) -> Unit,
    ) {
        val bottomBar by gvm.bottomBar.collectAsState()

        EdotatTheme.Apply(gvm) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize().systemBarsPadding(),
                    bottomBar = { if (bottomBar) BottomBar(gvm) },
                    snackbarHost = { SnackbarHost(gvm) },
                    containerColor = Color.Transparent,
                    content = { innerPadding -> content(innerPadding) },
                )
            }
        }
    }

    private data class BottomButton(
        val imageVector: ImageVector,
        @StringRes val textResId: Int,
        val route: Navigation.Destination,
    )

    private val bottomButtons = arrayOf(
        BottomButton(
            EdotatIcons.Meal,
            R.string.bottom_bar_home,
            Navigation.Destination.Home,
        ),
        BottomButton(
            EdotatIcons.Recent,
            R.string.bottom_bar_orders,
            Navigation.Destination.RecentOrders,
        ),
        BottomButton(
            EdotatIcons.Account,
            R.string.bottom_bar_account_and_settings,
            Navigation.Destination.AccountAndSettings,
        ),
    )

    @Composable
    fun BottomBar(gvm: GlobalViewModel) {
        val currentTab by gvm.currentTab.collectAsState()

        NavigationBar {
            bottomButtons.forEach { button ->
                val selected = currentTab == button.route

                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            Navigation.switchTab(button.route, gvm.nc)
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = button.imageVector,
                            contentDescription = null, // Icons are decorative
                        )
                    },
                    label = {
                        Text(
                            text = gvm.app.getString(button.textResId),
                        )
                    }
                )
            }
        }
    }

    @Composable
    private fun SnackbarHost(gvm: GlobalViewModel) {
        val bottomBarHeight = with(LocalDensity.current) { 80.dp.roundToPx() }
        val imePaddingHeight = WindowInsets.ime.getBottom(LocalDensity.current)
        val systemBarHeight = WindowInsets.systemBars.getBottom(LocalDensity.current)
        gvm.SnackbarHost(
            modifier = Modifier.offset {
                val bottomHeight = gvm.bottomBar.value * bottomBarHeight + systemBarHeight
                val offset = max(imePaddingHeight - bottomHeight, 0)
                IntOffset(0, -offset)
            },
        )
    }

    @Composable
    fun Back(@StringRes textResId: Int, gvm: GlobalViewModel) {
        TextButton(
            onClick = gvm.nc::navigateUp,
            shape = MaterialTheme.shapes.medium,
            contentPadding = PaddingValues(
                top = 8.dp,
                bottom = 8.dp,
                end = 16.dp,
            ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = EdotatIcons.Back,
                    contentDescription = null, // Icon is decorative
                    modifier = Modifier.size(32.dp),
                )
                Text(
                    text = gvm.app.getString(textResId),
                    fontSize = 16.sp,
                )
            }
        }
    }
}
