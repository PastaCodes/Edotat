package at.e.ui.settings

import androidx.annotation.StringRes
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import at.e.Authentication
import at.e.GlobalViewModel
import at.e.Navigation
import at.e.Navigation.ClearBackStack
import at.e.R
import at.e.UserPreferences
import at.e.lib.LoadingState
import at.e.ui.Common
import at.e.ui.RoundedSquareIconButton
import at.e.ui.home.FindTable
import at.e.ui.theme.EdotatIcons
import at.e.ui.theme.EdotatTheme
import at.e.ui.theme.EdotatTheme.lowAlpha
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object AccountAndSettings {
    enum class AutoLoginOption(
        @StringRes val textResId: Int,
        val enabled: (GlobalViewModel) -> Boolean = { true },
        val save: suspend (FragmentActivity, CoroutineScope, GlobalViewModel) -> Boolean,
    ) {
        NEVER(
            textResId = R.string.settings_autologin_never,
            save = { _, _, gvm ->
                gvm.savePreference(UserPreferences.Keys.AutoLogin, false)
                gvm.savePreference(UserPreferences.Keys.AutoLoginRequireBiometrics, false)
                Authentication.deleteToken(gvm)
                true
            },
        ),
        ALWAYS(
            textResId = R.string.settings_autologin_always,
            save = { activity, crs, gvm ->
                gvm.savePreference(UserPreferences.Keys.AutoLogin, true)
                gvm.savePreference(UserPreferences.Keys.AutoLoginRequireBiometrics, false)
                if (gvm.userPreferences.authToken.first() == null) {
                    Authentication.requestToken(
                        (gvm.loginState.value as GlobalViewModel.LoginState.LoggedIn).connection,
                        gvm
                    )
                    true
                } else {
                    Authentication.decryptToken(activity, crs, gvm)
                }
            },
        ),
        REQUIRE_BIOMETRICS(
            textResId = R.string.settings_autologin_require_biometrics,
            enabled = { gvm -> Authentication.isBiometricAuthEnabled(gvm) },
            save = { activity, crs, gvm ->
                gvm.savePreference(UserPreferences.Keys.AutoLogin, true)
                gvm.savePreference(UserPreferences.Keys.AutoLoginRequireBiometrics, true)
                if (gvm.userPreferences.authToken.first() == null) {
                    Authentication.requestToken(
                        (gvm.loginState.value as GlobalViewModel.LoginState.LoggedIn).connection,
                        gvm
                    )
                }
                Authentication.encryptToken(activity, crs, gvm)
            },
        ),
        ;

        companion object {
            fun from(autologin: Boolean, requireBiometrics: Boolean) =
                when (autologin) {
                    true ->
                        when (requireBiometrics) {
                            true -> REQUIRE_BIOMETRICS
                            false -> ALWAYS
                        }
                    false -> NEVER
                }
        }
    }

    enum class FindTableMethodPreferenceOption(
        @StringRes val textResId: Int,
        private val method: FindTable.Method? = null,
    ) {
        ALWAYS_ASK(
            textResId = R.string.settings_find_table_always_ask,
        ),
        QR_CODE(
            textResId = R.string.settings_find_table_qr_code,
            method = FindTable.Method.QrCode,
        ),
        NEAR_ME(
            textResId = R.string.settings_find_table_near_me,
            method = FindTable.Method.NearMe,
        ),
        SEARCH(
            textResId = R.string.settings_find_table_search,
            method = FindTable.Method.Search,
        ),
        ;

        companion object {
            fun fromPreference(preference: Int) =
                when (preference) {
                    0 -> ALWAYS_ASK
                    1 -> QR_CODE
                    2 -> NEAR_ME
                    3 -> SEARCH
                    else -> throw IllegalArgumentException()
                }
        }

        fun save(gvm: GlobalViewModel) {
            if (this == ALWAYS_ASK) {
                gvm.deletePreferredMethod()
            } else {
                gvm.savePreferredMethod(method!!)
            }
        }
    }

    data class LocalSettings(
        val autoLoginSetting: AutoLoginOption,
        val ftmpSetting: FindTableMethodPreferenceOption,
    )

    @Composable
    fun Screen(
        innerPadding: PaddingValues,
        activity: FragmentActivity,
        gvm: GlobalViewModel,
        nc: NavController,
    ) {
        gvm.notifyFinishedSwitchingTab()

        val crs = rememberCoroutineScope()
        val vm = viewModel<SettingsViewModel>()

        val loginState by gvm.loginState.collectAsState()
        val account = (loginState as? GlobalViewModel.LoginState.LoggedIn)?.account

        val localSettings by vm.localSettings.collectAsState()
        LaunchedEffect(Unit) {
            vm.initSettings(gvm)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .consumeWindowInsets(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Common.Back(textResId = R.string.back_home, gvm, nc)
            Spacer(Modifier.height(32.dp))
            Text(
                text = gvm.app.getString(R.string.settings_your_account),
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.height(86.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = EdotatIcons.Account,
                        contentDescription = null, // Icon is decorative
                        modifier = Modifier
                            .padding(start = 24.dp, end = 12.dp)
                            .size(32.dp),
                    )
                    Text(
                        text = account?.email ?: "",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    RoundedSquareIconButton(
                        onClick = {
                            gvm.logout()
                            nc.navigate(route = Navigation.Destination.Login, ClearBackStack)
                        },
                        modifier = Modifier.padding(horizontal = 14.dp),
                    ) {
                        Icon(
                            imageVector = EdotatIcons.Logout,
                            contentDescription = gvm.app.getString(R.string.logout),
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            TextButton(
                onClick = {
                    gvm.showSnackbar(
                        messageResId = R.string.not_implemented, // TODO
                    )
                },
                shape = EdotatTheme.RoundedCornerShape,
            ) {
                Text(
                    text = gvm.app.getString(R.string.settings_change_password),
                    fontSize = 16.sp,
                )
            }
            TextButton(
                onClick = {
                    gvm.logoutAndDeleteAccount()
                    nc.navigate(route = Navigation.Destination.Login, ClearBackStack)
                },
                shape = EdotatTheme.RoundedCornerShape,
            ) {
                Text(
                    text = gvm.app.getString(R.string.settings_delete_account),
                    fontSize = 16.sp,
                )
            }
            Spacer(Modifier.height(32.dp))
            Text(
                text = gvm.app.getString(R.string.settings_local),
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(32.dp))
            Text(
                text = gvm.app.getString(R.string.settings_autologin_label),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Column(
                modifier = Modifier.selectableGroup().padding(4.dp, 8.dp),
            ) {
                AutoLoginOption.entries.forEach { option ->
                    val enabled = option.enabled(gvm)
                    val selected = localSettings.mapData { it.autoLoginSetting == option } ?: false
                    Row(
                        modifier = Modifier
                            .selectable(
                                selected = selected,
                                enabled = enabled,
                                onClick = {
                                    if (!selected && localSettings.isData()) {
                                        vm.selectAutoLoginOption(option, activity, crs, gvm)
                                    }
                                },
                                role = Role.RadioButton,
                            )
                            .fillMaxWidth()
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected,
                            enabled = enabled,
                            onClick = null,
                            modifier = Modifier.padding(end = 12.dp),
                        )
                        Text(
                            text = gvm.app.getString(option.textResId),
                            fontSize = 18.sp,
                            modifier = if (enabled) Modifier else Modifier.lowAlpha(),
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = gvm.app.getString(R.string.settings_find_table_method_preference_label),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Column(
                modifier = Modifier.selectableGroup().padding(4.dp, 8.dp),
            ) {
                FindTableMethodPreferenceOption.entries.forEach { option ->
                    val selected = localSettings.mapData { it.ftmpSetting == option } ?: false
                    Row(
                        modifier = Modifier
                            .selectable(
                                selected = selected,
                                onClick = {
                                    if (!selected && localSettings.isData()) {
                                        vm.selectFindTableMethodPreferenceOption(option, gvm)
                                    }
                                },
                                role = Role.RadioButton,
                            )
                            .fillMaxWidth()
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = null,
                            modifier = Modifier.padding(end = 12.dp),
                        )
                        Text(
                            text = gvm.app.getString(option.textResId),
                            fontSize = 18.sp,
                        )
                    }
                }
            }
        }
    }

    class SettingsViewModel : ViewModel() {
        private val _localSettings = LoadingState.flow<LocalSettings>()
        val localSettings = _localSettings.asStateFlow()

        fun initSettings(gvm: GlobalViewModel) {
            viewModelScope.launch {
                val autoLogin = gvm.userPreferences.autoLogin.first()
                val requireBiometrics = gvm.userPreferences.autoLoginRequireBiometrics.first()
                val autoLoginSetting = AutoLoginOption.from(autoLogin, requireBiometrics)

                val ftmp = gvm.findTableMethodPreference.first().forceData
                val ftmpSetting = FindTableMethodPreferenceOption.fromPreference(ftmp)

                _localSettings.value = LoadingState.Data(
                    LocalSettings(autoLoginSetting, ftmpSetting)
                )
            }
        }

        fun selectAutoLoginOption(
            option: AutoLoginOption,
            activity: FragmentActivity,
            crs: CoroutineScope,
            gvm: GlobalViewModel,
        ) {
            viewModelScope.launch {
                val saved = option.save(activity, crs, gvm)
                if (saved) {
                    _localSettings.value = LoadingState.Data(_localSettings.value.forceData.copy(
                        autoLoginSetting = option
                    ))
                }
            }
        }

        fun selectFindTableMethodPreferenceOption(
            option: FindTableMethodPreferenceOption,
            gvm: GlobalViewModel,
        ) {
            _localSettings.value = LoadingState.Data(_localSettings.value.forceData.copy(
                ftmpSetting = option
            ))
            option.save(gvm)
        }
    }
}
