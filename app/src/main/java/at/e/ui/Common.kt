package at.e.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restaurant
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
import at.e.R
import at.e.ui.theme.EdotatTheme

object Common {
    val Icons = Filled

    context(Context)
    @Composable
    fun Container(
        navController: NavController,
        content: @Composable (PaddingValues, (Boolean) -> Unit) -> Unit,
    ) {
        val (bottomBarVisible, setBottomBarVisible) = remember { mutableStateOf(false) }
        EdotatTheme.Apply {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = { if (bottomBarVisible) BottomBar(navController) },
                content = { innerPadding -> content(innerPadding, setBottomBarVisible) },
            )
        }
    }

    private data class BottomButton(
        val imageVector: ImageVector,
        @StringRes val textResId: Int,
    )

    private val bottomButtons = listOf(
        BottomButton(Icons.Restaurant, R.string.bottom_bar_home),
        BottomButton(Icons.History, R.string.bottom_bar_orders),
        BottomButton(Icons.AccountCircle, R.string.bottom_bar_account_and_settings),
    )

    context(Context)
    @Composable
    fun BottomBar(navController: NavController) {
        var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
        NavigationBar {
            bottomButtons.forEachIndexed { index, button ->
                NavigationBarItem(
                    selected = selectedTabIndex == index,
                    onClick = {
                        selectedTabIndex = index
                        navController.navigate(route = TODO())
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
