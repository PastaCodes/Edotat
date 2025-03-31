package at.e.ui.home

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
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
import at.e.GlobalViewModel
import at.e.Navigation
import at.e.R
import at.e.UserPreferences
import at.e.api.Restaurant
import at.e.api.api
import at.e.lib.LoadingState
import at.e.ui.Common
import at.e.ui.shakeable
import at.e.ui.theme.EdotatIcons
import at.e.ui.theme.EdotatTheme
import at.e.ui.theme.EdotatTheme.mediumAlpha
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.uuid.Uuid

object FindTable {
    object ChooseMethod {
        @Composable
        fun Screen(innerPadding: PaddingValues, gvm: GlobalViewModel) {
            val orderState by gvm.orderState.collectAsState()
            val missingLocationPermissions by gvm.missingLocationPermissions.collectAsState()

            LaunchedEffect(orderState) {
                when (orderState) {
                    is GlobalViewModel.OrderState.TableNotFound -> {
                        gvm.showSnackbar(messageResId = R.string.find_table_not_found)
                        gvm.consumeTableError()
                    }
                    is GlobalViewModel.OrderState.InvalidQrCode -> {
                        gvm.showSnackbar(messageResId = R.string.qr_code_invalid)
                        gvm.consumeTableError()
                    }
                    else -> { }
                }
            }

            // State of the "set as preferred method" checkbox
            val (checkedState, setCheckedState) = rememberSaveable { mutableStateOf(false) }

            DisposableEffect(Unit) {
                onDispose {
                    setCheckedState(false) // Reset checkbox when leaving
                }
            }

            LaunchedEffect(missingLocationPermissions) {
                if (missingLocationPermissions) {
                    gvm.showSnackbar(
                        messageResId = R.string.permissions_location_missing,
                        actionResId = R.string.action_open_settings,
                        withDismissAction = true,
                    ) {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.fromParts("package", gvm.app.packageName, null)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        gvm.app.startActivity(intent)
                    }
                    gvm.consumeMissingLocationPermissions()
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.aligned(BiasAlignment.Vertical(0.2f)),
            ) {
                Header(large = true, gvm)
                Spacer(Modifier.height(16.dp))
                MethodButtons(checkedState, gvm)
                Spacer(Modifier.height(32.dp))
                SetPreferredMethod(checkedState, setCheckedState, gvm)
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

        @Composable
        private fun MethodButtons(checkedState: Boolean, gvm: GlobalViewModel) {
            Column(
                modifier = Modifier.width(300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                methodButtons.forEach { button ->
                    Button(
                        onClick = {
                            if (checkedState) {
                                gvm.savePreference(
                                    key = UserPreferences.Keys.FindTablePreferredMethod,
                                    value = button.method.toPreference(),
                                )
                            }
                            gvm.nc.navigate(
                                route = button.method.route(/* isInitial: */ false),
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
                                text = gvm.app.getString(button.textResId),
                                fontSize = 18.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }

        @Composable
        private fun SetPreferredMethod(
            checkedState: Boolean,
            setCheckedState: (Boolean) -> Unit,
            gvm: GlobalViewModel,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.toggleable(
                    value = checkedState,
                    onValueChange = setCheckedState,
                    role = Role.Checkbox
                ),
            ) {
                Checkbox(
                    checked = checkedState,
                    onCheckedChange = null,
                )
                Text(
                    text = gvm.app.getString(R.string.find_table_set_preferred_method),
                    fontSize = 18.sp,
                    modifier = Modifier. padding(start = 8.dp),
                )
            }
        }
    }

    @Composable
    private fun Header(large: Boolean, gvm: GlobalViewModel) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (large) 32.dp else 16.dp),
        ) {
            Text(
                text = gvm.app.getString(R.string.find_table_header),
                fontSize = if (large) 60.sp else 40.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.mediumAlpha(),
            )
            Text(
                text = gvm.app.getString(R.string.find_table_subheader),
                fontSize = 26.sp,
            )
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

        data object QrCode : Method(Navigation.Destination.Home.FindTable.Method::QrCode) {
            @Composable
            fun Screen(innerPadding: PaddingValues, gvm: GlobalViewModel) {
                val orderState by gvm.orderState.collectAsState()

                LaunchedEffect(Unit) {
                    val scannerOptions = GmsBarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build()
                    val scanner = GmsBarcodeScanning.getClient(gvm.app, scannerOptions)
                    scanner.startScan()
                        .addOnSuccessListener { barcode ->
                            try {
                                val rawValue = barcode.rawValue!!
                                val uuid = Uuid.parseHex(rawValue)
                                gvm.findTable(uuid)
                            } catch (_: Exception) {
                                gvm.notifyInvalidQrCode()
                                gvm.nc.navigateUp()
                            }
                        }
                        .addOnCanceledListener {
                            gvm.nc.navigateUp()
                        }
                        .addOnFailureListener {
                            gvm.notifyInvalidQrCode()
                            gvm.nc.navigateUp()
                        }
                }

                LaunchedEffect(orderState) {
                    when (orderState) {
                        is GlobalViewModel.OrderState.TableNotFound -> gvm.nc.navigateUp()
                        is GlobalViewModel.OrderState.SelectedTable -> {
                            gvm.nc.navigate(route = TODO())
                        }
                        else -> { }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        data object NearMe : Method(Navigation.Destination.Home.FindTable.Method::NearMe) {
            @Composable
            fun Screen(
                innerPadding: PaddingValues,
                isInitial: Boolean,
                gvm: GlobalViewModel,
            ) {
                val vm = viewModel<RestaurantsNearMeViewModel>()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp)
                        .consumeWindowInsets(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Common.Back(textResId = R.string.find_table_method_back, gvm)
                    if (isInitial) {
                        Spacer(Modifier.height(32.dp))
                        Header(large = false, gvm)
                        Spacer(Modifier.height(16.dp))
                    }
                    RestaurantResults(this@Column, vm, gvm)
                }
            }

            @Composable
            private fun RestaurantResults(
                columnScope: ColumnScope,
                vm: RestaurantsNearMeViewModel,
                gvm: GlobalViewModel,
            ) {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissionsResult ->
                    val anyGranted = permissionsResult.any { it.value }
                    if (anyGranted) {
                        vm.fetchNearby(gvm)
                    } else {
                        gvm.notifyMissingLocationPermissions()
                        gvm.nc.navigateUp()
                    }
                }

                val accuracyRadiusMeters by vm.accuracyRadiusMeters.collectAsState()

                val restaurants by vm.restaurants.collectAsState()
                LaunchedEffect(restaurants) {
                    if (restaurants is LoadingState.Loading) {
                        permissionLauncher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ))
                    }
                }

                Box(
                    modifier = with(columnScope) { Modifier.weight(1f) }
                        .fillMaxWidth().imePadding(),
                    contentAlignment = BiasAlignment(0f, -0.4f),
                ) {
                    when (val r = restaurants) {
                        is LoadingState.Loading -> CircularProgressIndicator()
                        is LoadingState.Data -> {
                            if (r.data.isEmpty()) {
                                Text(
                                    text =
                                        gvm.app.getString(R.string.restaurants_near_me_no_results),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            } else {
                                RestaurantList(r.data, accuracyRadiusMeters.forceData, gvm)
                            }
                        }
                    }
                }
            }

            private fun roundDistance(distance: Float) =
                (distance / 100).roundToInt() * 100

            @Composable
            private fun RestaurantList(
                restaurants: List<Pair<Restaurant, Float>>,
                accuracyRadiusMeters: Float,
                gvm: GlobalViewModel,
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(4.dp),
                ) {
                    items(restaurants) { (restaurant, distance) ->
                        val distanceMetersRange = IntRange(
                            roundDistance((distance - accuracyRadiusMeters).coerceAtLeast(0f)),
                            roundDistance((distance + accuracyRadiusMeters).coerceAtLeast(0f))
                        )

                        OutlinedCard(
                            onClick = {
                                gvm.selectRestaurant(restaurant)
                                gvm.nc.navigate(
                                    route = Navigation.Destination.Home.FindTable.EnterCode,
                                )
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
                                        text =
                                            gvm.app.getString(R.string.restaurants_near_me_distance)
                                                .format(
                                                    distanceMetersRange.first,
                                                    distanceMetersRange.last,
                                                ),
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

            class RestaurantsNearMeViewModel : ViewModel() {
                private val _accuracyRadiusMeters = LoadingState.flow<Float>()
                val accuracyRadiusMeters = _accuracyRadiusMeters.asStateFlow()

                private val _restaurants = LoadingState.flow<List<Pair<Restaurant, Float>>>()
                val restaurants = _restaurants.asStateFlow()

                fun fetchNearby(gvm: GlobalViewModel) {
                    gvm.getUserLocation { userLocation, accuracyRadiusMeters ->
                        viewModelScope.launch(Dispatchers.IO) {
                            _accuracyRadiusMeters.value = LoadingState.Data(accuracyRadiusMeters)
                            _restaurants.value = LoadingState.Data(api.getRestaurants(userLocation))
                        }
                    }
                }
            }
        }

        data object Search : Method(Navigation.Destination.Home.FindTable.Method::Search) {
            @Composable
            fun Screen(
                innerPadding: PaddingValues,
                isInitial: Boolean,
                gvm: GlobalViewModel,
            ) {
                val vm = viewModel<RestaurantsViewModel>()

                val isBack = gvm.nc.currentBackStackEntry!!.savedStateHandle.contains("isBack")
                gvm.nc.currentBackStackEntry!!.savedStateHandle["isBack"] = true

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp)
                        .consumeWindowInsets(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Common.Back(textResId = R.string.find_table_method_back, gvm)
                    if (isInitial) {
                        val lift = WindowInsets.ime.getBottom(LocalDensity.current) / 800f

                        Spacer(Modifier.height(32.dp - (24 * lift).dp))
                        Header(large = false, gvm)
                        Spacer(Modifier.height(16.dp - (12 * lift).dp))
                    }
                    SearchBar(isInitial, isBack, vm, gvm)
                    RestaurantResults(this@Column, vm, gvm)
                }
            }

            @Composable
            private fun SearchBar(
                isInitial: Boolean,
                isBack: Boolean,
                vm: RestaurantsViewModel,
                gvm: GlobalViewModel,
            ) {
                val query by vm.query.collectAsState()

                val searchBarFocus = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    if (!isInitial && !isBack) {
                        searchBarFocus.requestFocus()
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = vm::setQuery,
                    modifier = Modifier.fillMaxWidth().focusRequester(searchBarFocus),
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
                                    contentDescription =
                                        gvm.app.getString(R.string.icon_clear_search),
                                    modifier = Modifier.padding(end = 12.dp),
                                )
                            }
                        }
                    },
                    placeholder = {
                        Text(
                            text = gvm.app.getString(R.string.search_restaurants),
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.None,
                    )
                )
            }

            @Composable
            private fun RestaurantResults(
                columnScope: ColumnScope,
                vm: RestaurantsViewModel,
                gvm: GlobalViewModel,
            ) {
                val restaurants by vm.restaurants.collectAsState()

                Box(
                    modifier = with(columnScope) { Modifier.weight(1f) }
                        .fillMaxWidth().imePadding(),
                    contentAlignment = BiasAlignment(0f, -0.4f),
                ) {
                    when (val r = restaurants) {
                        is LoadingState.Loading -> CircularProgressIndicator()
                        is LoadingState.Data -> {
                            if (r.data.isEmpty()) {
                                Text(
                                    text =
                                        gvm.app.getString(R.string.search_restaurants_no_results),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            } else {
                                RestaurantList(r.data, gvm)
                            }
                        }
                    }
                }
            }

            @Composable
            private fun RestaurantList(
                restaurants: List<Restaurant>,
                gvm: GlobalViewModel,
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(4.dp),
                ) {
                    items(restaurants) { restaurant ->
                        OutlinedCard(
                            onClick = {
                                gvm.selectRestaurant(restaurant)
                                gvm.nc.navigate(
                                    route = Navigation.Destination.Home.FindTable.EnterCode,
                                )
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

            class RestaurantsViewModel : ViewModel() {
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

    object EnterCode {
        @Composable
        fun Screen(
            innerPadding: PaddingValues,
            gvm: GlobalViewModel,
        ) {
            val isBack = gvm.nc.currentBackStackEntry!!.savedStateHandle.contains("isBack")
            gvm.nc.currentBackStackEntry!!.savedStateHandle["isBack"] = true

            val orderState by gvm.orderState.collectAsState()
            val selectedRestaurant = rememberSaveable {
                (orderState as GlobalViewModel.OrderState.SelectedRestaurant).restaurant.name
            }

            var code by rememberSaveable { mutableStateOf("") }
            var isEmptyError by rememberSaveable { mutableStateOf(false) }
            var isNotFoundError by rememberSaveable { mutableStateOf(false) }

            val lift = WindowInsets.ime.getBottom(LocalDensity.current) / 1200f

            LaunchedEffect(orderState) {
                when (orderState) {
                    is GlobalViewModel.OrderState.SelectedTable -> {
                        gvm.nc.navigate(route = TODO())
                    }
                    is GlobalViewModel.OrderState.TableCodeNotFound -> {
                        isNotFoundError = true
                        gvm.showSnackbar(messageResId = R.string.find_table_not_found)
                    }
                    else -> { }
                }
            }

            val codeFocus = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                if (!isBack) {
                    codeFocus.requestFocus()
                }
            }

            val submit = {
                if (
                    orderState !is GlobalViewModel.OrderState.Loading
                &&  orderState !is GlobalViewModel.OrderState.SelectedTable
                ) {
                    isEmptyError = code.isBlank()
                    if (!isEmptyError) {
                        gvm.findTableByCode(code)
                    } else {
                        gvm.shake()
                    }
                }
            }

            @Composable
            fun ColumnScope.MainBox() {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = BiasAlignment(0f, -lift),
                ) {
                    Column(
                        modifier = Modifier.width(280.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = gvm.app.getString(R.string.find_table_enter_code_header),
                            textAlign = TextAlign.Center,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 40.sp,
                            modifier = Modifier.mediumAlpha(),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = gvm.app.getString(R.string.find_table_enter_code_subheader),
                            textAlign = TextAlign.Center,
                            fontSize = 26.sp,
                            lineHeight = 28.sp,
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = code,
                            onValueChange = {
                                isEmptyError = false
                                isNotFoundError = false
                                code = it
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Ascii,
                                imeAction = ImeAction.Go,
                            ),
                            keyboardActions = KeyboardActions(onGo = { submit() }),
                            shape = EdotatTheme.RoundedCornerShape,
                            textStyle = TextStyle(
                                fontSize = 40.sp,
                                textAlign = TextAlign.Center,
                            ),
                            singleLine = true,
                            isError = isEmptyError || isNotFoundError,
                            modifier = Modifier
                                .width(160.dp)
                                .focusRequester(codeFocus)
                                .shakeable(gvm, isEmptyError),
                            supportingText = {
                                Text(
                                    text =
                                        if (isEmptyError)
                                            gvm.app.getString(R.string.error_fill_this_field)
                                        else
                                            "",
                                )
                            },
                        )
                        Spacer(Modifier.height(32.dp - (24 * lift).dp))
                        Button(
                            onClick = submit,
                            shape = EdotatTheme.RoundedCornerShape,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = orderState !is GlobalViewModel.OrderState.SelectedTable,
                        ) {
                            if (
                                orderState is GlobalViewModel.OrderState.Loading
                            ||  orderState is GlobalViewModel.OrderState.SelectedTable
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 3.dp,
                                    color =
                                        if (orderState !is GlobalViewModel.OrderState.SelectedTable)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            ButtonDefaults.buttonColors().disabledContentColor
                                )
                            } else {
                                Text(
                                    text = gvm.app.getString(R.string.find_table_enter_code_button),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
                    .consumeWindowInsets(innerPadding),
            ) {
                Common.Back(textResId = R.string.find_table_enter_code_back, gvm)
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(12.dp, 24.dp),
                ) {
                    Column {
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
                            Text(
                                text = selectedRestaurant,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        HorizontalDivider()
                        MainBox()
                    }
                }
            }
        }
    }
}
