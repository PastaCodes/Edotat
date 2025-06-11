package at.e.ui.home

import android.text.format.DateFormat
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import at.e.api.Order
import at.e.api.Suborder
import at.e.api.api
import at.e.lib.LoadingState
import at.e.lib.Money
import at.e.lib.dataOr
import at.e.lib.formatLocalMinuteOfDay
import at.e.lib.replaceOne
import at.e.lib.times
import at.e.lib.toMinuteOfDay
import at.e.ui.Common
import at.e.ui.home.Ordering.ItemCard
import at.e.ui.theme.EdotatIcons
import at.e.ui.theme.EdotatTheme
import at.e.ui.theme.EdotatTheme.lowAlpha
import at.e.ui.theme.EdotatTheme.mediumAlpha
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
                            }
                                .fillMaxWidth()
                                .height(20.dp)
                                .wrapContentHeight(align = Alignment.CenterVertically),
                        text = quantity.toString(),
                        textAlign = TextAlign.Center,
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
                    vm.fetchSuborder(gvm)
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
                            vm.itemQuantities.find { it.item == item }?.quantity ?: 0
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

        val itemQuantities = mutableStateListOf<Order.Entry>()

        val suborderHistory = mutableStateListOf<Pair<Suborder, List<Order.Entry>>>()

        val total = mutableStateOf<Money.Amount?>(null)

        fun fetchMenuItems(menu: Menu) {
            viewModelScope.launch(Dispatchers.IO) {
                _items.value = LoadingState.Data(api.getMenuItems(menu))
            }
        }

        fun selectCategory(category: Menu.Category) {
            _selectedCategory.value = category
        }

        fun fetchSuborder(gvm: GlobalViewModel, includeHistory: Boolean = false) {
            _hasActiveSuborder.value = LoadingState.Loading
            viewModelScope.launch(Dispatchers.IO) {
                val res = gvm.requireConnection.getActiveSuborder()
                if (res != null) {
                    _hasActiveSuborder.value = LoadingState.Data(true)
                    itemQuantities.clear()
                    itemQuantities.addAll(res.second)
                } else {
                    itemQuantities.clear()
                    _hasActiveSuborder.value = LoadingState.Data(false)
                }
            }
            if (includeHistory) {
                viewModelScope.launch(Dispatchers.IO) {
                    val history = gvm.requireConnection.getSuborderHistory()
                    suborderHistory.clear()
                    suborderHistory.addAll(history)
                }
                viewModelScope.launch(Dispatchers.IO) {
                    total.value = gvm.requireConnection.getCurrentTotal(
                        currency = gvm.requireOrder.table.restaurant.currency
                    )
                }
            }
        }

        private suspend fun checkOrBeginSuborder(gvm: GlobalViewModel) {
            if (!_hasActiveSuborder.value.forceData) {
                _hasActiveSuborder.value = LoadingState.Data(true)
                itemQuantities.clear()
                itemQuantities.addAll(gvm.requireConnection.beginSuborder().second)
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
                    val existingEntry = itemQuantities.find { it.item == item }
                    if (existingEntry != null) {
                        val newEntry = Order.Entry(item, existingEntry.quantity + 1)
                        itemQuantities.replaceOne(existingEntry, newEntry)
                    } else {
                        itemQuantities.add(Order.Entry(item, 1))
                    }
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
                    val existingEntry = itemQuantities.find { it.item == item }!!
                    if (existingEntry.quantity == 1) {
                        itemQuantities.remove(existingEntry)
                        if (itemQuantities.isEmpty()) {
                            _hasActiveSuborder.value = LoadingState.Data(false)
                        }
                    } else {
                        val newEntry = Order.Entry(item, existingEntry.quantity - 1)
                        itemQuantities.replaceOne(existingEntry, newEntry)
                    }
                    gvm.requireConnection.decrementItemQuantity(item)
                    callback(true)
                } catch (_: Exception) {
                    callback(false)
                }
            }
        }

        fun sendSuborder(gvm: GlobalViewModel) {
            viewModelScope.launch(Dispatchers.IO) {
                gvm.requireConnection.sendSuborder()
                fetchSuborder(gvm, includeHistory = true)
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
                vm.fetchSuborder(gvm, includeHistory = true)
            }
        }

        val hasActiveSuborder by vm.hasActiveSuborder.collectAsState()
        val total by vm.total
        val isPaying by gvm.isPaying.collectAsState()

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
            Spacer(Modifier.height(32.dp))
            if (hasActiveSuborder.isData()) {
                if (vm.itemQuantities.isEmpty()) {
                    Text(
                        text = gvm.app.getString(R.string.order_summary_empty),
                        fontSize = 20.sp,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .lowAlpha(),
                    )
                    Spacer(Modifier.height(16.dp))
                } else {
                    for ((item, _) in vm.itemQuantities) {
                        val quantity by remember(item) {
                            derivedStateOf {
                                vm.itemQuantities.find { it.item == item }?.quantity ?: 0
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
                Button(
                    onClick = {
                        vm.sendSuborder(gvm)
                    },
                    enabled = vm.itemQuantities.isNotEmpty(),
                    shape = EdotatTheme.RoundedCornerShape,
                    modifier = Modifier
                        .size(160.dp, 56.dp)
                        .align(Alignment.End),
                ) {
                    Text(
                        text = gvm.app.getString(R.string.order_summary_send),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = EdotatIcons.Send,
                        contentDescription = null, // Icon is decorative
                    )
                }
                Spacer(Modifier.height(32.dp))
                for ((suborder, items) in vm.suborderHistory) {
                    for ((item, quantity) in items) {
                        Row {
                            Text(
                                text = "${quantity}x",
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                modifier = Modifier.weight(1f),
                                text = item.name,
                            )
                            Text(
                                text = (item.price * quantity).toString(),
                                textAlign = TextAlign.End,
                            )
                        }
                    }
                    Text(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .lowAlpha(),
                        text = gvm.app.getString(
                            R.string.order_summary_sent_at,
                            formatLocalMinuteOfDay(
                                suborder.sent!!.time.toMinuteOfDay(),
                                is24Hour = DateFormat.is24HourFormat(gvm.app),
                            )
                        ),
                        fontStyle = FontStyle.Italic,
                    )
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                }
                if (total != null) {
                    Text(
                        modifier = Modifier.align(Alignment.End),
                        text = total!!.toString(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = {
                        gvm.pay(
                            merchantName = gvm.requireOrder.table.restaurant.name,
                            price = total!!
                        )
                    },
                    enabled = !hasActiveSuborder.dataOr(false),
                    shape = EdotatTheme.RoundedCornerShape,
                    modifier = Modifier
                        .size(200.dp, 56.dp)
                        .align(Alignment.CenterHorizontally),
                ) {
                    if (isPaying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 3.dp,
                            color =
                                if (!hasActiveSuborder.dataOr(false))
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    ButtonDefaults.buttonColors().disabledContentColor
                        )
                    } else {
                        Icon(
                            imageVector = EdotatIcons.Pay,
                            contentDescription = null, // Icon is decorative
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = gvm.app.getString(R.string.order_summary_pay_and_leave),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
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
