package at.e.ui.home

import android.content.Context
import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import at.e.GlobalViewModel
import at.e.Navigation
import at.e.R
import at.e.UserPreferences
import at.e.api.Restaurant
import at.e.api.api
import at.e.lib.LoadingState
import at.e.ui.theme.EdotatIcons
import at.e.ui.theme.EdotatTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data object FindTable {
    object ChooseMethod {
        context(Context)
        @Composable
        fun Screen(innerPadding: PaddingValues, gvm: GlobalViewModel, nc: NavController) {
            val vm = viewModel<VM>()
            DisposableEffect(Unit) {
                onDispose {
                    vm.setPreferredMethod = false // Reset checkbox when leaving
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Header(large = true)
                Spacer(modifier = Modifier.height(16.dp))
                MethodButtons(vm, gvm, nc)
                Spacer(modifier = Modifier.height(32.dp))
                SetPreferredMethod(vm)
            }
        }

        private data class MethodButton(
            val imageVector: ImageVector,
            @StringRes val textResId: Int,
            val method: Method,
        )

        private val methodButtons = listOf(
            MethodButton(EdotatIcons.QrCodeScanner, R.string.find_table_scan_qr_code, Method.QrCode),
            MethodButton(EdotatIcons.MyLocation, R.string.find_table_near_me, Method.NearMe),
            MethodButton(EdotatIcons.Search, R.string.find_table_search, Method.Search)
        )

        context(Context)
        @Composable
        private fun MethodButtons(vm: VM, gvm: GlobalViewModel, nc: NavController) {
            val coroutineScope = rememberCoroutineScope()

            Column(
                modifier = Modifier.width(300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                methodButtons.forEach { button ->
                    Button(
                        onClick = {
                            if (vm.setPreferredMethod) {
                                coroutineScope.launch {
                                    gvm.userPreferences.save(
                                        key = UserPreferences.Keys.FindTablePreferredMethod,
                                        value = button.method.toPreference(),
                                    )
                                }
                            }
                            nc.navigate(
                                route = button.method.route(/* isInitial = */ false),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(24.dp, 16.dp),
                        shape = EdotatTheme.RoundedCornerShape,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = button.imageVector,
                                contentDescription = null, // Icons are decorative
                                modifier = Modifier.size(40.dp),
                            )
                            Text(
                                text = getString(button.textResId),
                                fontSize = 18.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }

        context(Context)
        @Composable
        private fun SetPreferredMethod(vm: VM) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.toggleable(
                    value = vm.setPreferredMethod,
                    onValueChange = { vm.setPreferredMethod = it },
                    role = Role.Checkbox
                ),
            ) {
                Checkbox(
                    checked = vm.setPreferredMethod,
                    onCheckedChange = null,
                )
                Text(
                    text = getString(R.string.find_table_set_preferred_method),
                    fontSize = 18.sp,
                    modifier = Modifier. padding(start = 8.dp),
                )
            }
        }

        class VM : ViewModel() {
            var setPreferredMethod by mutableStateOf(false)
        }
    }

    @Composable
    private fun Header(large: Boolean) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (large) 32.dp else 16.dp),
        ) {
            Text(
                text = "Hungry?",
                fontSize = if (large) 60.sp else 40.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "Let's find your table.",
                fontSize = 26.sp,
            )
        }
    }

    context(Context)
    @Composable
    private fun Back(nc: NavController) {
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
                    text = getString(R.string.find_table_back),
                    fontSize = 16.sp,
                )
            }
        }
    }

    sealed class Method(val route: (/* isInitial: */ Boolean) -> Navigation.Destination) {
        companion object {
            const val NO_PREFERENCE = 0

            fun fromPreference(value: Int) =
                when (value) {
                    1 -> QrCode
                    2 -> NearMe
                    3 -> Search
                    else -> null
                }

            fun Method?.toRoute(isInitial: Boolean = false) =
                when (this) {
                    null -> Navigation.Destination.Home.FindTable.ChooseMethod
                    else -> this.route(isInitial)
                }
        }

        fun toPreference() =
            when (this) {
                QrCode -> 1
                NearMe -> 2
                Search -> 3
            }

        data object QrCode : Method(Navigation.Destination.Home.FindTable.Method::QrCode)

        data object NearMe : Method(Navigation.Destination.Home.FindTable.Method::NearMe)

        data object Search : Method(Navigation.Destination.Home.FindTable.Method::Search) {
            context(Context)
            @Composable
            fun Screen(innerPadding: PaddingValues, isInitial: Boolean, nc: NavController) {
                val vm = viewModel<VM>()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp)
                        .consumeWindowInsets(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Back(nc)
                    if (isInitial) {
                        Spacer(modifier = Modifier.height(32.dp))
                        Header(large = false)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    SearchBar(vm)
                    RestaurantResults(nc, vm)
                }
            }

            context(Context)
            @Composable
            private fun SearchBar(vm: VM) {
                val query by vm.query.collectAsState()

                OutlinedTextField(
                    value = query,
                    onValueChange = vm::setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    leadingIcon = {
                        Icon(
                            imageVector = EdotatIcons.Search,
                            contentDescription = null, // Icon is decorative
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = vm::clearQuery,
                            ) {
                                Icon(
                                    imageVector = EdotatIcons.Close,
                                    contentDescription = getString(R.string.icon_clear_search),
                                    modifier = Modifier.padding(end = 12.dp),
                                )
                            }
                        }
                    },
                    placeholder = {
                        Text(
                            text = getString(R.string.search_restaurants),
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.None,
                    )
                )
            }

            context(Context, ColumnScope)
            @Composable
            private fun RestaurantResults(nc: NavController, vm: VM) {
                val restaurants by vm.restaurants.collectAsState()

                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().imePadding(),
                    contentAlignment = BiasAlignment(0f, -0.4f),
                ) {
                    when (val r = restaurants) {
                        is LoadingState.Loading -> CircularProgressIndicator()
                        is LoadingState.Data -> {
                            if (r.data.isEmpty()) {
                                Text(
                                    text = getString(R.string.search_restaurants_no_results),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            } else {
                                RestaurantList(r.data, nc)
                            }
                        }
                    }
                }
            }

            @Composable
            private fun RestaurantList(restaurants: List<Restaurant>, nc: NavController) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(4.dp),
                ) {
                    items(restaurants) { restaurant ->
                        OutlinedCard(
                            onClick = {
                                nc.navigate(route = TODO())
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.height(86.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = EdotatIcons.Restaurant,
                                    contentDescription = null, // Icon is decorative
                                    modifier = Modifier
                                        .padding(horizontal = 24.dp)
                                        .size(32.dp),
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        text = restaurant.name,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = restaurant.address.toString(),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Icon(
                                    imageVector = EdotatIcons.Forward,
                                    contentDescription = null, // Icon is decorative
                                    modifier = Modifier
                                        .padding(horizontal = 24.dp)
                                        .size(24.dp),
                                )
                            }
                        }
                    }
                }
            }

            @OptIn(ExperimentalCoroutinesApi::class)
            class VM : ViewModel() {
                private val _query = MutableStateFlow("")
                val query = _query.asStateFlow()

                fun setQuery(query: String) {
                    _query.value = query
                }

                fun clearQuery() {
                    _query.value = ""
                }

                val restaurants: StateFlow<LoadingState<List<Restaurant>>> =
                    query
                        .flatMapLatest { query ->
                            flow {
                                emit(LoadingState.Loading)
                                emit(LoadingState.Data(withContext(Dispatchers.IO) {
                                    api.getRestaurants(query)
                                }))
                            }
                        }
                        .stateIn(viewModelScope, SharingStarted.Lazily, LoadingState.Loading)
            }
        }
    }
}
