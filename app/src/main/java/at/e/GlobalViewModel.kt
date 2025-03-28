package at.e

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.repeatable
import androidx.compose.foundation.layout.imePadding
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import at.e.UserPreferences.Companion.dataStore
import at.e.api.Account
import at.e.api.Api
import at.e.api.Order
import at.e.lib.LoadingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GlobalViewModel(context: Context) : ViewModel() {
    val userPreferences = UserPreferences(context.dataStore)

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Loading)
    val loginState = _loginState.asStateFlow()

    private val _orderState = MutableStateFlow<OrderState>(OrderState.Loading)
    val orderState = _orderState.asStateFlow()

    private val _findTableMethodPreference = LoadingState.flow<Int>()
    val findTableMethodPreference = _findTableMethodPreference.asStateFlow()

    private val snackbarHostState = SnackbarHostState()

    private val shakeAnimation = Animatable(0f)
    private var shakeAmplitude = Dp.Unspecified
    val shakeOffset: Density.() -> IntOffset = {
        IntOffset((shakeAnimation.value * shakeAmplitude).toPx().toInt(), 0)
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

    suspend fun tryAutoLogin() {
        _loginState.value = LoginState.Loading
        withContext(Dispatchers.IO) {
            when (val result = Authentication.autoLogin(this@GlobalViewModel)) {
                null -> _loginState.value = LoginState.AutoLoginFailed
                else -> _loginState.value =
                    LoginState.LoggedIn(account = result.first, connection = result.second)
            }
        }
    }

    suspend fun tryManualLogin(email: String, password: String) {
        _loginState.value = LoginState.Loading
        withContext(Dispatchers.IO) {
            when (val result = Authentication.manualLogin(email, password, this@GlobalViewModel)) {
                null -> _loginState.value = LoginState.ManualLoginFailed
                else -> _loginState.value =
                    LoginState.LoggedIn(account = result.first, connection = result.second)
            }
        }
    }

    suspend fun logout() {
        withContext(Dispatchers.IO) {
            Authentication.logout(this@GlobalViewModel)
            _loginState.value = LoginState.LoggedOut
        }
    }

    context(Context)
    suspend fun tryRegister(
        email: String,
        password: String,
    ) {
        _loginState.value = LoginState.Loading
        withContext(Dispatchers.IO) {
            when (val account = Authentication.register(email, password, this@GlobalViewModel)) {
                is Account -> _loginState.value = LoginState.Registered(account)
                null -> _loginState.value = LoginState.RegisterFailed
            }
        }
    }

    sealed interface OrderState {
        data object Loading : OrderState
        data object None : OrderState
        data class Active(val order: Order) : OrderState
    }

    suspend fun loadActiveOrder() {
        withContext(Dispatchers.IO) {
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

    suspend fun loadFindTableMethodPreference() {
        _findTableMethodPreference.value = LoadingState.Data(
            userPreferences.findTableMethodPreference.first()
        )
    }

    context(Context)
    suspend fun showSnackbar(
        @StringRes messageResId: Int,
        @StringRes actionResId: Int? = null,
        withDismissAction: Boolean = true,
        action: (() -> Unit)? = null,
    ) {
        val result = snackbarHostState.showSnackbar(
            message = getString(messageResId),
            actionLabel = if (actionResId != null) getString(actionResId) else null,
            withDismissAction = withDismissAction,
        )
        if (result == SnackbarResult.ActionPerformed && action != null) {
            action()
        }
    }

    @Composable
    fun SnackbarHost() {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.imePadding(),
        )
    }

    context(CoroutineScope)
    fun shake(iterations: Int = 3, amplitude: Dp = 4.dp, durationMillis: Int = 100) {
        shakeAmplitude = amplitude
        launch {
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

    class Factory(private val context: Context) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GlobalViewModel(context) as T
        }
    }
}
