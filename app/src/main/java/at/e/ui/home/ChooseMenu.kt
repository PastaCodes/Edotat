package at.e.ui.home

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import at.e.GlobalViewModel
import at.e.Navigation
import at.e.Navigation.ClearBackStack
import at.e.R
import at.e.api.Menu
import at.e.api.Restaurant
import at.e.api.api
import at.e.lib.LoadingState
import at.e.lib.formatLocalMinuteOfDayRange
import at.e.lib.inMinuteOfDayRange
import at.e.ui.Common
import at.e.ui.theme.EdotatIcons
import at.e.ui.theme.EdotatTheme.lowAlpha
import at.e.ui.theme.EdotatTheme.mediumAlpha
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock.System.now
import kotlinx.datetime.toLocalDateTime

object ChooseMenu {
    @Composable
    fun Screen(
        innerPadding: PaddingValues,
        gvm: GlobalViewModel,
        nc: NavController,
    ) {
        val vm = viewModel<MenusViewModel>()

        val orderState by gvm.orderState.collectAsState()
        LaunchedEffect(orderState) {
            when (val os = orderState) {
                is GlobalViewModel.OrderState.SelectedTable -> vm.fetchMenus(os.restaurant)
                else -> { }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .consumeWindowInsets(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Common.Back(textResId = R.string.find_table_enter_code_back, gvm, nc)
            Spacer(Modifier.height(24.dp))
            Text(
                text = gvm.app.getString(R.string.choose_menu_title),
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(32.dp))
            MenuResults(vm, gvm, nc)
        }
    }

    @Composable
    fun ColumnScope.MenuResults(vm: MenusViewModel, gvm: GlobalViewModel, nc: NavController) {
        val now = now()

        @Composable
        fun MenuCard(menu: Menu, enabled: Boolean, nc: NavController) {
            OutlinedCard(
                onClick = {
                    if (enabled) {
                        gvm.selectMenu(menu)
                        gvm.beginOrder()
                        nc.navigate(route = Navigation.Destination.Home.Ordering, ClearBackStack)
                    }
                },
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
            ) {
                Column {
                    Box(modifier = Modifier.weight(0.4f))
                    Row(
                        modifier = Modifier.weight(0.2f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = EdotatIcons.Menu,
                            contentDescription = null, // Icon is decorative
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .size(32.dp),
                        )
                        Text(
                            modifier = Modifier.weight(1f),
                            text = menu.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Icon(
                            imageVector = EdotatIcons.Forward,
                            contentDescription = null, // Icon is decorative
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .size(24.dp),
                        )
                    }
                    Text(
                        modifier = Modifier
                            .weight(0.4f)
                            .fillMaxWidth()
                            .padding(horizontal = 80.dp)
                            .mediumAlpha(),
                        text = formatLocalMinuteOfDayRange(
                            menu.startMinute,
                            menu.endMinute,
                            DateFormat.is24HourFormat(gvm.app),
                        ),
                        fontSize = 16.sp,
                    )
                }
            }
        }

        val menus by vm.menus.collectAsState()
        when (val m = menus) {
            is LoadingState.Data -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val enabledMenus = mutableListOf<Menu>()
                    val disabledMenus = mutableListOf<Menu>()
                    for (menu in m.data) {
                        val localTime = now.toLocalDateTime(menu.restaurant.timeZone).time
                        if (localTime.inMinuteOfDayRange(menu.startMinute, menu.endMinute)) {
                            enabledMenus.add(menu)
                        } else {
                            disabledMenus.add(menu)
                        }
                    }
                    for (menu in enabledMenus) {
                        MenuCard(menu, enabled = true, nc)
                    }
                    if (enabledMenus.isNotEmpty() && disabledMenus.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier
                                .padding(vertical = 16.dp)
                                .lowAlpha(),
                        )
                    }
                    for (menu in disabledMenus) {
                        MenuCard(menu, enabled = false, nc)
                    }
                }
            }
            else -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }

    class MenusViewModel : ViewModel() {
        private val _menus = LoadingState.flow<List<Menu>>()
        val menus = _menus.asStateFlow()

        fun fetchMenus(restaurant: Restaurant) {
            viewModelScope.launch(Dispatchers.IO) {
                _menus.value = LoadingState.Data(api.getMenus(restaurant))
            }
        }
    }
}
