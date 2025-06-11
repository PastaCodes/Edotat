package at.e.ui.orders

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import at.e.api.Order
import at.e.api.Suborder
import at.e.lib.LoadingState
import at.e.lib.formatLocalDate
import at.e.lib.formatLocalMinuteOfDay
import at.e.lib.sumOf
import at.e.lib.times
import at.e.lib.toMinuteOfDay
import at.e.ui.Common
import at.e.ui.theme.EdotatTheme.lowAlpha
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object RecentOrders {
    @Composable
    fun Screen(innerPadding: PaddingValues, gvm: GlobalViewModel, nc: NavController) {
        gvm.notifyFinishedSwitchingTab()

        val vm = viewModel<RecentOrdersViewModel>()
        val orderHistory by vm.orderHistory.collectAsState()
        LaunchedEffect(Unit) {
            vm.fetchHistory(gvm)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .consumeWindowInsets(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Common.Back(textResId = R.string.back_home, gvm, nc)
            Spacer(Modifier.height(24.dp))
            Text(
                text = gvm.app.getString(R.string.recent_orders_title),
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(32.dp))
            when (val oh = orderHistory) {
                is LoadingState.Data -> {
                    if (oh.data.isEmpty()) {
                        Text(
                            text = gvm.app.getString(R.string.recent_orders_empty),
                            fontSize = 20.sp,
                            fontStyle = FontStyle.Italic,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .wrapContentHeight(align = Alignment.CenterVertically)
                                .lowAlpha(),
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            for ((order, suborders) in oh.data) {
                                OutlinedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(24.dp, 24.dp, 24.dp, 0.dp),
                                    ) {
                                        Text(
                                            text = gvm.app.getString(
                                                R.string.recent_orders_card_title_format,
                                                order.table.restaurant.name,
                                                order.table.code
                                            ),
                                            modifier = Modifier.weight(1f),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            modifier = Modifier
                                                .lowAlpha(),
                                            text = formatLocalDate(order.started.date, gvm.app),
                                        )
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp, 0.dp, 24.dp, 24.dp),
                                    ) {
                                        for ((suborder, items) in suborders) {
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
                                        val currency = order.table.restaurant.currency
                                        val total = suborders
                                            .flatMap { it.second }
                                            .sumOf(currency) { it.item.price * it.quantity }
                                        Text(
                                            modifier = Modifier.align(Alignment.End),
                                            text = total.toString(),
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                is LoadingState.Loading -> Box(
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

    class RecentOrdersViewModel : ViewModel() {
        private val _orderHistory = LoadingState.flow<List<Pair<Order, List<Pair<Suborder, List<Order.Entry>>>>>>()
        val orderHistory = _orderHistory.asStateFlow()

        fun fetchHistory(gvm: GlobalViewModel) {
            viewModelScope.launch(Dispatchers.IO) {
                _orderHistory.value = LoadingState.Data(gvm.requireConnection.getOrderHistory())
            }
        }
    }
}
