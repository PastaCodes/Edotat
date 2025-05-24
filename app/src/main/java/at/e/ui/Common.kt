package at.e.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
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
import androidx.navigation.NavController
import at.e.GlobalViewModel
import at.e.Navigation
import at.e.R
import at.e.lib.times
import at.e.ui.theme.EdotatIcons
import at.e.ui.theme.EdotatTheme
import kotlin.math.max

@Composable
fun RoundedSquareIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(58.dp, 58.dp),
        shape = EdotatTheme.RoundedCornerShape,
        colors = IconButtonDefaults.iconButtonColors().toButtonColors(),
        contentPadding = PaddingValues(),
        content = content,
    )
}

fun IconButtonColors.toButtonColors() = ButtonColors(
    containerColor = containerColor,
    contentColor = contentColor,
    disabledContainerColor = disabledContainerColor,
    disabledContentColor = disabledContentColor
)

fun Modifier.shakeable(gvm: GlobalViewModel, condition: Boolean = true) =
    if (condition) this.offset(gvm.shakeOffset) else this

object Common {
    @Composable
    fun Container(
        gvm: GlobalViewModel,
        nc: NavController,
        content: @Composable (PaddingValues) -> Unit,
    ) {
        val bottomBar by gvm.bottomBar.collectAsState()
        val fab by gvm.floatingActionButton.collectAsState()

        EdotatTheme.Apply(gvm) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize().systemBarsPadding(),
                    bottomBar = { if (bottomBar) BottomBar(gvm, nc) },
                    snackbarHost = { SnackbarHost(gvm) },
                    floatingActionButton = { fab?.invoke() },
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
    fun BottomBar(gvm: GlobalViewModel, nc: NavController) {
        val currentTab by gvm.currentTab.collectAsState()

        NavigationBar {
            bottomButtons.forEach { button ->
                val selected = currentTab == button.route

                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            gvm.switchTab(button.route, nc)
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
    fun Back(@StringRes textResId: Int, gvm: GlobalViewModel, nc: NavController) {
        TextButton(
            onClick = nc::navigateUp,
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

    @Composable
    fun OrderFloatingActionButton(gvm: GlobalViewModel, nc: NavController) {
        FloatingActionButton(
            modifier = Modifier.size(80.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            onClick = {
                nc.navigate(route = Navigation.Destination.Home.OrderSummary)
            },
        ) {
            Icon(
                modifier = Modifier.size(40.dp),
                imageVector = EdotatIcons.Order,
                contentDescription = gvm.app.getString(R.string.action_view_order)
            )
        }
    }
}
