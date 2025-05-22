package at.e.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import at.e.ui.theme.EdotatTheme.mediumAlpha
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
                            contentPadding = PaddingValues(32.dp, 24.dp),
                        ) {
                            Text(
                                text = category.name.uppercase(),
                                fontSize = 18.sp,
                            )
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier.padding(24.dp),
                ) {
                    val selectedCategory = i.data.keys.toList()[1]
                    item {
                        Text(
                            text = selectedCategory.name,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    items(i.data[selectedCategory]!!) { item ->
                        OutlinedCard(
                            onClick = { },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(24.dp, 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    modifier = Modifier.weight(0.7f),
                                ) {
                                    Text(
                                        text = item.name,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    if (item.description != null) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            modifier = Modifier.mediumAlpha(),
                                            text = item.description,
                                            fontSize = 16.sp,
                                            fontStyle = FontStyle.Italic,
                                        )
                                    }
                                }
                                Text(
                                    modifier = Modifier.weight(0.2f),
                                    text = item.price.toString(),
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Right,
                                )
                                Box( // TODO
                                    modifier = Modifier.weight(0.1f),
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
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
