package at.e.ui.login

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.semantics.Role
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
import at.e.R
import at.e.ui.shakeable
import at.e.ui.theme.EdotatIcons
import at.e.ui.theme.EdotatTheme
import at.e.ui.theme.EdotatTheme.mediumAlpha
import kotlinx.coroutines.launch

object Register {
    context(Context)
    @Composable
    fun Screen(innerPadding: PaddingValues, gvm: GlobalViewModel, nc: NavController) {
        val coroutineScope = rememberCoroutineScope()

        val loginState by gvm.loginState.collectAsState()

        var email by rememberSaveable { mutableStateOf("") }
        var password by rememberSaveable { mutableStateOf("") }
        var passwordVisible by rememberSaveable { mutableStateOf(false) }
        var stayLoggedIn by rememberSaveable { mutableStateOf(false) }

        var isEmailError by rememberSaveable { mutableStateOf(false) }
        var isPasswordError by rememberSaveable { mutableStateOf(false) }

        val lift = WindowInsets.ime.getBottom(LocalDensity.current) / 1400f

        LaunchedEffect(loginState) {
            when (loginState) {
                is GlobalViewModel.LoginState.LoggedIn -> {
                    nc.popBackStack() // Forget registration
                    nc.navigate(route = Navigation.Destination.Home)
                }
                is GlobalViewModel.LoginState.RegisterFailed -> {
                    isEmailError = true
                    isPasswordError = false
                    gvm.shake()
                    gvm.showSnackbar(R.string.register_failed_credentials)
                }
                else -> gvm.logout()
            }
        }

        val tryRegister = {
            if (loginState !is GlobalViewModel.LoginState.Loading) {
                isEmailError = email.isBlank()
                isPasswordError = password.isBlank()
                coroutineScope.launch {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        gvm.tryRegister(email, password, requestToken = stayLoggedIn)
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
                    text = getString(R.string.register_welcome),
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
                    isError = isEmailError,
                    modifier = with(gvm) {
                        Modifier.shakeable(isEmailError)
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
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(onGo = { tryRegister() }),
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
                    isError = isPasswordError,
                    modifier = with(gvm) {
                        Modifier.shakeable(isPasswordError)
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
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.toggleable(
                            value = stayLoggedIn,
                            onValueChange = { stayLoggedIn = it },
                            role = Role.Checkbox
                        ),
                    ) {
                        Checkbox(
                            checked = stayLoggedIn,
                            onCheckedChange = null,
                        )
                        Text(
                            text = getString(R.string.register_stay_logged_in),
                            fontSize = 18.sp,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = tryRegister,
                    shape = EdotatTheme.RoundedCornerShape,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    if (
                        loginState is GlobalViewModel.LoginState.Loading
                    ||  loginState is GlobalViewModel.LoginState.LoggedIn
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(
                            text = getString(R.string.register_button),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.height(64.dp))
                HorizontalDivider()
                TextButton(
                    onClick = {
                        nc.popBackStack()
                        nc.navigate(route = Navigation.Destination.Login)
                    },
                    shape = EdotatTheme.RoundedCornerShape,
                ) {
                    Text(
                        text = getString(R.string.register_go_to_login),
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }
}
