package at.e.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
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
            is LoadingState.Data -> Text(
                text = i.data.toString(),
                modifier = Modifier.fillMaxSize(),
                textAlign = TextAlign.Center,
            )
            else -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }

    class OrderingViewModel : ViewModel() {
        private val _items = LoadingState.flow<Map<Menu.Category, Menu.Item>>()
        val items = _items.asStateFlow()

        fun fetchMenuItems(menu: Menu) {
            viewModelScope.launch(Dispatchers.IO) {
                _items.value = LoadingState.Data(api.getMenuItems(menu))
            }
        }
    }
}
