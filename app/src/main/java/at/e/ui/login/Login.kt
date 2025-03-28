package at.e.ui.login

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import at.e.GlobalViewModel
import at.e.Navigation
import at.e.Navigation.ClearBackStack
import at.e.R
import at.e.ui.shakeable
import at.e.ui.theme.EdotatIcons
import at.e.ui.theme.EdotatTheme
import at.e.ui.theme.EdotatTheme.mediumAlpha
import kotlinx.coroutines.launch

object Login {
    context(Context)
    @Composable
    fun Screen(innerPadding: PaddingValues, gvm: GlobalViewModel, nc: NavController) {
        val coroutineScope = rememberCoroutineScope()

        val loginState by gvm.loginState.collectAsState()
        val orderState by gvm.orderState.collectAsState()

        var email by rememberSaveable { mutableStateOf(
            when (val ls = loginState) {
                is GlobalViewModel.LoginState.Registered -> ls.account.email
                else -> ""
            }
        ) }
        var password by rememberSaveable { mutableStateOf("") }
        var passwordVisible by rememberSaveable { mutableStateOf(false) }

        var isEmailError by rememberSaveable { mutableStateOf(false) }
        var isPasswordError by rememberSaveable { mutableStateOf(false) }
        var isCredentialsError by rememberSaveable { mutableStateOf(false) }

        val lift = WindowInsets.ime.getBottom(LocalDensity.current) / 1400f

        LaunchedEffect(loginState, orderState) {
            when (loginState) {
                is GlobalViewModel.LoginState.LoggedIn -> {
                    when (orderState) {
                        is GlobalViewModel.OrderState.Loading -> gvm.loadActiveOrder()
                        else -> {
                            nc.navigate(route = Navigation.Destination.Home, ClearBackStack)
                        }
                    }
                }
                is GlobalViewModel.LoginState.ManualLoginFailed -> {
                    isEmailError = false
                    isPasswordError = false
                    isCredentialsError = true
                    gvm.shake()
                    gvm.showSnackbar(R.string.login_failed_credentials)
                }
                is GlobalViewModel.LoginState.Loading -> { }
                else -> gvm.logout()
            }
        }

        val tryLogin = {
            if (
                loginState !is GlobalViewModel.LoginState.Loading
            &&  loginState !is GlobalViewModel.LoginState.LoggedIn
            ) {
                isEmailError = email.isBlank()
                isPasswordError = password.isBlank()
                coroutineScope.launch {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        gvm.tryManualLogin(email, password)
                    } else {
                        gvm.shake()
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            contentAlignment = BiasAlignment(0f, 0.3f - lift),
        ) {
            Column(
                modifier = Modifier.width(280.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = getString(R.string.login_welcome_back),
                    textAlign = TextAlign.Center,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 48.sp,
                    modifier = Modifier.mediumAlpha(),
                )
                Spacer(Modifier.height(32.dp))
                OutlinedTextField(
                    label = {
                        Text(
                            text = getString(R.string.login_email_label),
                            fontSize = 18.sp,
                        )
                    },
                    value = email,
                    onValueChange = {
                        email = it
                        isEmailError = false
                        isCredentialsError = false
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    placeholder = {
                        Text(
                            text = getString(R.string.login_email_placeholder),
                            fontSize = 18.sp,
                            modifier = Modifier.mediumAlpha(),
                        )
                    },
                    shape = EdotatTheme.RoundedCornerShape,
                    textStyle = TextStyle(fontSize = 18.sp),
                    singleLine = true,
                    isError = isEmailError || isCredentialsError,
                    modifier = with(gvm) {
                        Modifier.shakeable(isEmailError || isCredentialsError)
                    },
                    supportingText = {
                        Text(
                            text =
                                if (isEmailError)
                                    getString(R.string.error_fill_this_field)
                                else
                                    "",
                        )
                    },
                )
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    label = {
                        Text(
                            text = getString(R.string.login_password_label),
                            fontSize = 18.sp,
                        )
                    },
                    value = password,
                    onValueChange = {
                        password = it
                        isPasswordError = false
                        isCredentialsError = false
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(onGo = { tryLogin() }),
                    visualTransformation =
                        if (passwordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                    placeholder = {
                        Text(
                            text = getString(R.string.login_password_placeholder),
                            fontSize = 18.sp,
                            modifier = Modifier.mediumAlpha(),
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                        ) {
                            Icon(
                                imageVector =
                                    if (passwordVisible)
                                        EdotatIcons.Visible
                                    else
                                        EdotatIcons.Invisible,
                                contentDescription =
                                    if (passwordVisible)
                                        getString(R.string.login_hide_password)
                                    else
                                        getString(R.string.login_show_password),
                            )
                        }
                    },
                    shape = EdotatTheme.RoundedCornerShape,
                    textStyle = TextStyle(fontSize = 18.sp),
                    singleLine = true,
                    isError = isPasswordError || isCredentialsError,
                    modifier = with(gvm) {
                        Modifier.shakeable(isPasswordError || isCredentialsError)
                    },
                    supportingText = {
                        Text(
                            text =
                                if (isPasswordError)
                                    getString(R.string.error_fill_this_field)
                                else
                                    "",
                        )
                    },
                )
                Box(
                    modifier = Modifier.fillMaxWidth().offset(y = (-16).dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                isPasswordError = false
                                isCredentialsError = false
                                if (email.isEmpty()) {
                                    isEmailError = true
                                    gvm.shake()
                                } else {
                                    gvm.showSnackbar(
                                        messageResId = R.string.login_reset_password_message,
                                        actionResId = R.string.action_ok,
                                        withDismissAction = false,
                                    )
                                }
                            }
                        },
                        shape = EdotatTheme.RoundedCornerShape,
                    ) {
                        Text(
                            text = getString(R.string.login_forgot_password),
                            fontSize = 16.sp,
                        )
                    }
                }
                Button(
                    onClick = tryLogin,
                    shape = EdotatTheme.RoundedCornerShape,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = loginState !is GlobalViewModel.LoginState.LoggedIn,
                ) {
                    if (
                        loginState is GlobalViewModel.LoginState.Loading
                    ||  loginState is GlobalViewModel.LoginState.LoggedIn
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 3.dp,
                            color =
                                if (loginState !is GlobalViewModel.LoginState.LoggedIn)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    ButtonDefaults.buttonColors().disabledContentColor
                        )
                    } else {
                        Text(
                            text = getString(R.string.login_button),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.height(64.dp))
                HorizontalDivider()
                TextButton(
                    onClick = {
                        nc.navigate(route = Navigation.Destination.Register)
                    },
                    shape = EdotatTheme.RoundedCornerShape,
                ) {
                    Text(
                        text = getString(R.string.login_go_to_register),
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }
}
