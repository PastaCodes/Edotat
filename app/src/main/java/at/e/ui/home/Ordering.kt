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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import at.e.R
import at.e.api.Menu
import at.e.api.api
import at.e.lib.LoadingState
import at.e.ui.Common
import at.e.ui.home.Ordering.ItemCard
import at.e.ui.theme.EdotatIcons
import at.e.ui.theme.EdotatTheme.lowAlpha
import at.e.ui.theme.EdotatTheme.mediumAlpha
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.max
import android.util.Log

object Ordering {
    @Composable
    fun ItemCard(
        item: Menu.Item,
        quantity: Int,
        isLoading: Boolean,
        onPlusClick: () -> Unit,
        onMinusClick: () -> Unit,
        gvm: GlobalViewModel,
    ) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(24.dp, 16.dp, 12.dp, 16.dp),
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
                    modifier = Modifier.weight(0.18f),
                    text = item.price.toString(),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Right,
                )
                Spacer(modifier = Modifier.weight(0.02f))
                Column(
                    modifier = Modifier.weight(0.1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Button (
                        modifier = Modifier.size(24.dp),
                        onClick = onPlusClick,
                        shape = CircleShape,
                        colors = ButtonDefaults.textButtonColors(),
                        contentPadding = PaddingValues(),
                    ) {
                        Icon(
                            imageVector = EdotatIcons.Add,
                            contentDescription = gvm.app.getString(R.string.ordering_add),
                        )
                    }
                    Text(
                        modifier =
                            if (isLoading) {
                                Modifier.lowAlpha()
                            } else {
                                Modifier
                            },
                        text = quantity.toString(),
                    )
                    Button (
                        modifier = Modifier.size(24.dp),
                        onClick = onMinusClick,
                        enabled = quantity > 0,
                        shape = CircleShape,
                        colors = ButtonDefaults.textButtonColors(),
                        contentPadding = PaddingValues(),
                    ) {
                        Icon(
                            imageVector = EdotatIcons.Remove,
                            contentDescription = gvm.app.getString(R.string.ordering_remove),
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun Screen(innerPadding: PaddingValues, gvm: GlobalViewModel, nc: NavController) {
        val vm = viewModel<OrderingViewModel>()

        val orderState by gvm.orderState.collectAsState()
        LaunchedEffect(orderState) {
            when (val os = orderState) {
                is GlobalViewModel.OrderState.Active -> {
                    vm.fetchMenuItems(os.menu)
                    vm.fetchActiveSuborder(gvm)
                }
                else -> { }
            }
        }

        val hasActiveSuborder by vm.hasActiveSuborder.collectAsState()

        val items by vm.items.collectAsState()
        when (val i = items) {
            is LoadingState.Data -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            ) {
                val selectedCategory by vm.selectedCategory.collectAsState()
                val displayedCategory = with(selectedCategory) {
                    if (this != null && this in i.data.keys) {
                        this
                    } else {
                        i.data.keys.first()
                    }
                }

                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .background(NavigationBarDefaults.containerColor),
                ) {
                    for (category in i.data.keys) {
                        TextButton(
                            onClick = {
                                vm.selectCategory(category)
                            },
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
                    item {
                        Text(
                            text = displayedCategory.name,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    items(i.data[displayedCategory]!!) { item ->
                        val quantity by remember(item) { derivedStateOf {
                            vm.itemQuantities[item] ?: 0
                        } }
                        var isLoading by rememberSaveable(item) { mutableStateOf(false) }
                        ItemCard(
                            item,
                            quantity,
                            isLoading || hasActiveSuborder.isLoading(),
                            onPlusClick = {
                                isLoading = true
                                vm.incrementItemQuantity(item, gvm) {
                                    isLoading = false
                                }
                            },
                            onMinusClick = {
                                isLoading = true
                                vm.decrementItemQuantity(item, gvm) {
                                    isLoading = false
                                }
                            },
                            gvm,
                        )
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

        private val _selectedCategory = MutableStateFlow<Menu.Category?>(null)
        val selectedCategory = _selectedCategory.asStateFlow()

        private val _hasActiveSuborder = LoadingState.flow<Boolean>()
        val hasActiveSuborder = _hasActiveSuborder.asStateFlow()

        val itemQuantities = mutableStateMapOf<Menu.Item, Int>()

        fun fetchMenuItems(menu: Menu) {
            viewModelScope.launch(Dispatchers.IO) {
                _items.value = LoadingState.Data(api.getMenuItems(menu))
            }
        }

        fun selectCategory(category: Menu.Category) {
            _selectedCategory.value = category
        }

        fun fetchActiveSuborder(gvm: GlobalViewModel) {
            _hasActiveSuborder.value = LoadingState.Loading
            viewModelScope.launch(Dispatchers.IO) {
                itemQuantities.clear()
                val res = gvm.requireConnection.getActiveSuborder()
                if (res != null) {
                    _hasActiveSuborder.value = LoadingState.Data(true)
                    itemQuantities.putAll(res.second)
                } else {
                    _hasActiveSuborder.value = LoadingState.Data(false)
                }
            }
        }

        private suspend fun checkOrBeginSuborder(gvm: GlobalViewModel) {
            if (!_hasActiveSuborder.value.forceData) {
                _hasActiveSuborder.value = LoadingState.Data(true)
                itemQuantities.clear()
                itemQuantities.putAll(gvm.requireConnection.beginSuborder().second)
            }
        }

        fun incrementItemQuantity(
            item: Menu.Item,
            gvm: GlobalViewModel,
            callback: (Boolean) -> Unit, // Parameter indicates success or failure
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    checkOrBeginSuborder(gvm)
                    val prevQuantity = itemQuantities[item] ?: 0
                    itemQuantities[item] = prevQuantity + 1
                    itemQuantities[item] =
                        gvm.requireConnection.incrementItemQuantity(item)
                    callback(true)
                } catch (_: Exception) {
                    callback(false)
                }
            }
        }

        fun decrementItemQuantity(
            item: Menu.Item,
            gvm: GlobalViewModel,
            callback: (Boolean) -> Unit, // Parameter indicates success or failure
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    checkOrBeginSuborder(gvm)
                    val prevQuantity = itemQuantities[item] ?: 0
                    itemQuantities[item] = max(prevQuantity - 1, 0)
                    val newQuantity = gvm.requireConnection.decrementItemQuantity(item)
                    if (newQuantity == 0) {
                        itemQuantities.remove(item)
                    } else {
                        itemQuantities[item] = newQuantity
                    }
                    callback(true)
                } catch (_: Exception) {
                    callback(false)
                }
            }
        }
    }
}

object OrderSummary {
    @Composable
    fun Screen(innerPadding: PaddingValues, gvm: GlobalViewModel, nc: NavController) {
        val vm = viewModel<Ordering.OrderingViewModel>()

        val orderState by gvm.orderState.collectAsState()
        LaunchedEffect(orderState) {
            if (orderState is GlobalViewModel.OrderState.Active) {
                vm.fetchActiveSuborder(gvm)
            }
        }

        val hasActiveSuborder by vm.hasActiveSuborder.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .consumeWindowInsets(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Common.Back(textResId = R.string.order_summary_back, gvm, nc)
            Spacer(Modifier.height(24.dp))
            Text(
                text = gvm.app.getString(R.string.order_summary_title),
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (hasActiveSuborder.isData()) {
                Log.i("PIEDI", "IsEmpty? " + vm.itemQuantities.isEmpty())
                if (vm.itemQuantities.isEmpty()) {
                    Spacer(Modifier.height(48.dp))
                    Text(
                        text = gvm.app.getString(R.string.order_summary_empty),
                        fontSize = 20.sp,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .lowAlpha(),
                    )
                } else {
                    Spacer(Modifier.height(32.dp))
                    for (item in vm.itemQuantities.keys) {
                        val quantity by remember(item) {
                            derivedStateOf {
                                vm.itemQuantities[item] ?: 0
                            }
                        }
                        var isLoading by rememberSaveable(item) { mutableStateOf(false) }
                        ItemCard(
                            item,
                            quantity,
                            isLoading || hasActiveSuborder.isLoading(),
                            onPlusClick = {
                                isLoading = true
                                vm.incrementItemQuantity(item, gvm) {
                                    isLoading = false
                                }
                            },
                            onMinusClick = {
                                isLoading = true
                                vm.decrementItemQuantity(item, gvm) {
                                    isLoading = false
                                }
                            },
                            gvm,
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
