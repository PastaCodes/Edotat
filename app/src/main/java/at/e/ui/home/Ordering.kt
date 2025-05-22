package at.e.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import at.e.GlobalViewModel
import at.e.api.Menu
import at.e.api.api
import at.e.lib.LoadingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object Ordering {
    @Composable
    fun Screen(innerPadding: PaddingValues, gvm: GlobalViewModel, nc: NavController) {
        val vm = viewModel<OrderingViewModel>()

        val orderState by gvm.orderState.collectAsState()
        LaunchedEffect(orderState) {
            when (val os = orderState) {
                is GlobalViewModel.OrderState.Active -> vm.fetchMenuItems(os.menu)
                else -> { }
            }
        }

        val items by vm.items.collectAsState()
        when (val i = items) {
            is LoadingState.Data -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .background(NavigationBarDefaults.containerColor),
                ) {
                    for (category in i.data.keys) {
                        TextButton(
                            onClick = { },
                            shape = RectangleShape,
                            contentPadding = PaddingValues(24.dp, 16.dp),
                        ) {
                            Text(
                                text = category.name.uppercase(),
                                fontSize = 18.sp,
                            )
                        }
                    }
                }
            }
            else -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
                    .consumeWindowInsets(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }

    class OrderingViewModel : ViewModel() {
        private val _items = LoadingState.flow<Map<Menu.Category, List<Menu.Item>>>()
        val items = _items.asStateFlow()

        fun fetchMenuItems(menu: Menu) {
            viewModelScope.launch(Dispatchers.IO) {
                _items.value = LoadingState.Data(api.getMenuItems(menu))
            }
        }
    }
}
