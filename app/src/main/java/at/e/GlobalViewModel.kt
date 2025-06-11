package at.e

import android.Manifest.permission
import android.app.Application
import androidx.annotation.RequiresPermission
import androidx.annotation.StringRes
import androidx.biometric.BiometricManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.repeatable
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.datastore.preferences.core.Preferences
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import at.e.UserPreferences.Companion.dataStore
import at.e.api.Account
import at.e.api.Api
import at.e.api.Location
import at.e.api.Menu
import at.e.api.Order
import at.e.api.Restaurant
import at.e.api.Table
import at.e.api.api
import at.e.lib.Direction
import at.e.lib.LoadingState
import at.e.ui.home.FindTable
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class GlobalViewModel(val app: Application, nc: NavController) : ViewModel() {
    val biometricManager = BiometricManager.from(app)

    val userPreferences = UserPreferences(app.dataStore)

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Loading)
    val loginState = _loginState.asStateFlow()

    private val _orderState = MutableStateFlow<OrderState>(OrderState.Loading)
    val orderState = _orderState.asStateFlow()

    private val _findTableMethodPreference = LoadingState.flow<Int>()
    val findTableMethodPreference = _findTableMethodPreference.asStateFlow()

    private val _bottomBar = MutableStateFlow(false)
    val bottomBar = _bottomBar.asStateFlow()

    private val _floatingActionButton = MutableStateFlow<(@Composable () -> Unit)?>(null)
    val floatingActionButton = _floatingActionButton.asStateFlow()

    private val _currentTab = MutableStateFlow<Navigation.Destination?>(null)
    val currentTab = _currentTab.asStateFlow()

    private var switchingTab = false

    private val _forcedTransitionDirection = MutableStateFlow<Direction.Horizontal?>(null)
    val forcedTransitionDirection = _forcedTransitionDirection.asStateFlow()

    private val snackbarHostState = SnackbarHostState()

    private val shakeAnimation = Animatable(0f)
    private var shakeAmplitude = Dp.Unspecified
    val shakeOffset: Density.() -> IntOffset = {
        IntOffset((shakeAnimation.value * shakeAmplitude).roundToPx(), 0)
    }

    private val _missingLocationPermissions = MutableStateFlow(false)
    val missingLocationPermissions = _missingLocationPermissions.asStateFlow()

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(app)

    init {
        nc.addOnDestinationChangedListener { _, destination, _ ->
            if (!switchingTab) {
                val oldTab = _currentTab.value
                val newTab = Navigation.getTab(destination)
                _forcedTransitionDirection.value =
                    Navigation.getTabSwitchDirection(oldTab, newTab)?.inverse
                _currentTab.value = newTab
            }
        }
    }

    fun switchTab(tab: Navigation.Destination, nc: NavController) {
        val oldTab = _currentTab.value
        _forcedTransitionDirection.value = Navigation.getTabSwitchDirection(oldTab, tab)?.inverse
        switchingTab = true
        Navigation.switchTab(tab, nc)
        _currentTab.value = tab
    }

    fun notifySwitchingTab() {
        switchingTab = true
    }

    fun notifyFinishedSwitchingTab() {
        switchingTab = false
    }

    fun <T> savePreference(key: Preferences.Key<T>, value: T) {
        viewModelScope.launch {
            userPreferences.save(key, value)
        }
    }

    fun <T> deletePreference(key: Preferences.Key<T>) {
        viewModelScope.launch {
            userPreferences.delete(key)
        }
    }

    fun savePreferredMethod(method: FindTable.Method) {
        _findTableMethodPreference.value = LoadingState.Data(method.toPreference())
        savePreference(UserPreferences.Keys.FindTablePreferredMethod, method.toPreference())
    }

    fun deletePreferredMethod() {
        _findTableMethodPreference.value = LoadingState.Data(FindTable.Method.NO_PREFERENCE)
        deletePreference(UserPreferences.Keys.FindTablePreferredMethod)
    }

    fun configureScreen(hasBottomBar: Boolean, floatingActionButton: (@Composable () -> Unit)?) {
        _bottomBar.value = hasBottomBar
        _floatingActionButton.value = floatingActionButton
    }

    sealed interface LoginState {
        data object Loading : LoginState
        data object AutoLoginFailed : LoginState
        data object ManualLoginFailed : LoginState
        class LoggedIn(val account: Account, val connection: Api.Connection) : LoginState
        data object LoggedOut : LoginState
        data object RegisterFailed : LoginState
        data class Registered(val account: Account) : LoginState
    }

    fun tryAutoLogin(activity: FragmentActivity, crs: CoroutineScope) {
        _loginState.value = LoginState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = Authentication.autoLogin(activity, crs, this@GlobalViewModel)) {
                null -> _loginState.value = LoginState.AutoLoginFailed
                else -> _loginState.value =
                    LoginState.LoggedIn(account = result.first, connection = result.second)
            }
        }
    }

    fun tryManualLogin(email: String, password: String, activity: FragmentActivity, crs: CoroutineScope) {
        _loginState.value = LoginState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = Authentication.manualLogin(email, password, activity, crs, this@GlobalViewModel)) {
                null -> _loginState.value = LoginState.ManualLoginFailed
                else -> _loginState.value =
                    LoginState.LoggedIn(account = result.first, connection = result.second)
            }
        }
    }

    fun logout() {
        _loginState.value = LoginState.LoggedOut
        viewModelScope.launch(Dispatchers.IO) {
            Authentication.logout(this@GlobalViewModel)
        }
    }

    fun logoutAndDeleteAccount() {
        viewModelScope.launch(Dispatchers.IO) {
            Authentication.logoutAndDeleteAccount(this@GlobalViewModel)
            _loginState.value = LoginState.LoggedOut
        }
    }

    fun tryRegister(email: String, password: String, activity: FragmentActivity, crs: CoroutineScope) {
        _loginState.value = LoginState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            when (val account = Authentication.register(email, password, activity, crs, this@GlobalViewModel)) {
                is Account -> _loginState.value = LoginState.Registered(account)
                null -> _loginState.value = LoginState.RegisterFailed
            }
        }
    }

    val requireConnection
        get() = (this.loginState.value as LoginState.LoggedIn).connection

    sealed interface OrderState {
        data object Loading : OrderState
        data object None : OrderState
        data object InvalidQrCode : OrderState
        data object TableNotFound : OrderState
        open class SelectedRestaurant(val restaurant: Restaurant) : OrderState
        class TableCodeNotFound(restaurant: Restaurant) : SelectedRestaurant(restaurant)
        open class SelectedTable(val table: Table) : SelectedRestaurant(table.restaurant)
        open class SelectedMenu(val menu: Menu, table: Table) : SelectedTable(table)
        data class Active(val order: Order) : SelectedMenu(order.menu, order.table)
    }

    val requireOrder
        get() = (this.orderState.value as OrderState.Active).order

    fun resetOrder() {
        assert(_orderState.value !is OrderState.Active)
        _orderState.value = OrderState.None
    }

    fun orderStateBack() {
        when (val os = _orderState.value) {
            is OrderState.SelectedMenu -> _orderState.value = OrderState.SelectedTable(os.table)
            is OrderState.SelectedTable -> _orderState.value = OrderState.SelectedRestaurant(os.restaurant)
            is OrderState.SelectedRestaurant -> _orderState.value = OrderState.None
            else -> { }
        }
    }

    fun consumeTableError() {
        _orderState.value = OrderState.None
    }

    fun findTable(uuid: Uuid) {
        _orderState.value = OrderState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            _orderState.value = when (val response = api.findTable(uuid)) {
                is Table -> {
                    OrderState.SelectedTable(response)
                }
                else -> OrderState.TableNotFound
            }
        }
    }

    fun notifyInvalidQrCode() {
        _orderState.value = OrderState.InvalidQrCode
    }

    fun selectRestaurant(restaurant: Restaurant) {
        _orderState.value = OrderState.SelectedRestaurant(restaurant)
    }

    fun findTableByCode(code: String) {
        val restaurant = when (val os = _orderState.value) {
            is OrderState.SelectedRestaurant -> os.restaurant
            else -> throw IllegalStateException()
        }
        _orderState.value = OrderState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            _orderState.value = when (val table = api.findTable(code, restaurant)) {
                is Table -> OrderState.SelectedTable(table)
                else -> OrderState.TableCodeNotFound(restaurant)
            }
        }
    }

    fun selectMenu(menu: Menu) {
        when (val os = _orderState.value) {
            is OrderState.SelectedTable -> {
                _orderState.value = OrderState.SelectedMenu(menu, os.table)
            }
            else -> throw IllegalStateException()
        }
    }

    fun beginOrder() {
        viewModelScope.launch(Dispatchers.IO) {
            when (val os = _orderState.value) {
                is OrderState.SelectedMenu -> {
                    when (val ls = _loginState.value) {
                        is LoginState.LoggedIn -> {
                            val order = ls.connection.beginOrder(os.menu, os.table)
                            _orderState.value = OrderState.Active(order)
                        }
                        else -> throw IllegalStateException()
                    }
                }
                else -> throw IllegalStateException()
            }
        }
    }

    fun loadActiveOrder() {
        viewModelScope.launch(Dispatchers.IO) {
            when (val ls = _loginState.value) {
                is LoginState.LoggedIn -> {
                    val order = ls.connection.getActiveOrder()
                    _orderState.value = when (order) {
                        is Order -> OrderState.Active(order)
                        else -> OrderState.None
                    }
                }
                else -> throw IllegalStateException()
            }
        }
    }

    fun loadFindTableMethodPreference() {
        viewModelScope.launch {
            _findTableMethodPreference.value = LoadingState.Data(
                userPreferences.findTableMethodPreference.first()
            )
        }
    }

    fun showSnackbar(
        @StringRes messageResId: Int,
        @StringRes actionResId: Int? = null,
        withDismissAction: Boolean = true,
        action: (() -> Unit)? = null,
    ) {
        viewModelScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = app.getString(messageResId),
                actionLabel = if (actionResId != null) app.getString(actionResId) else null,
                withDismissAction = withDismissAction,
            )
            if (result == SnackbarResult.ActionPerformed && action != null) {
                action()
            }
        }
    }

    @Composable
    fun SnackbarHost(modifier: Modifier = Modifier) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = modifier,
        )
    }

    fun shake(
        crs: CoroutineScope,
        iterations: Int = 3,
        amplitude: Dp = 4.dp,
        durationMillis: Int = 100,
    ) {
        shakeAmplitude = amplitude
        crs.launch {
            shakeAnimation.animateTo(
                targetValue = 0f,
                animationSpec = repeatable(
                    iterations = iterations,
                    animation = keyframes {
                        this.durationMillis = durationMillis
                        1f at durationMillis / 4
                        (-1f) at 3 * durationMillis / 4
                        0f at durationMillis
                    },
                )
            )
        }
    }

    @RequiresPermission(anyOf = [
        permission.ACCESS_COARSE_LOCATION,
        permission.ACCESS_FINE_LOCATION,
    ])
    fun getUserLocation(action: (Location, Float) -> Unit) {
        val request = CurrentLocationRequest.Builder()
            .setMaxUpdateAgeMillis(20.seconds.inWholeMilliseconds)
            .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            .build()
        fusedLocationClient.getCurrentLocation(request, null)
            .addOnSuccessListener { location ->
                assert(location.hasAccuracy())
                action(
                    Location(
                        latitudeDegrees = location.latitude.toFloat(),
                        longitudeDegrees = location.longitude.toFloat(),
                    ),
                    /* accuracyRadiusMeters = */ location.accuracy
                )
            }
    }

    fun notifyMissingLocationPermissions() {
        _missingLocationPermissions.value = true
    }

    fun consumeMissingLocationPermissions() {
        _missingLocationPermissions.value = false
    }

    class Factory(
        private val app: Application,
        private val nc: NavController,
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GlobalViewModel(app, nc) as T
        }
    }
}
