package at.e

import at.e.api.Account
import at.e.api.Api
import at.e.api.api
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock.System.now
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

object Authentication {
    private suspend fun saveToken(response: Api.AuthResult, gvm: GlobalViewModel) {
        if (response.newToken != null) {
            gvm.userPreferences.save(
                UserPreferences.Keys.AuthToken,
                response.newToken
            )
            if (response.newTokenExpiration != null) {
                gvm.userPreferences.save(
                    UserPreferences.Keys.AuthTokenExpiration,
                    response.newTokenExpiration
                )
            }
            gvm.userPreferences.save(
                UserPreferences.Keys.AuthTokenEmail,
                response.account.email
            )
        } else {
            val oldEmail = gvm.userPreferences.authTokenEmail.first()
            if (response.account.email != oldEmail) {
                gvm.userPreferences.delete(UserPreferences.Keys.AuthToken)
                gvm.userPreferences.delete(UserPreferences.Keys.AuthTokenExpiration)
                gvm.userPreferences.delete(UserPreferences.Keys.AuthTokenEmail)
            }
        }
    }

    private fun biometricsLogin(): Boolean = true // TODO

    suspend fun autoLogin(gvm: GlobalViewModel): Pair<Account, Api.Connection>? {
        val autoLogin = gvm.userPreferences.autoLogin.first()
        if (!autoLogin) {
            return null
        }
        val token = gvm.userPreferences.authToken.first() ?: return null
        var doRefresh = true // Temporary value; only refresh if about to expire
        val tokenExpiration = gvm.userPreferences.authTokenExpiration.first()
        if (tokenExpiration != null) {
            val timeLeft = Instant.fromEpochSeconds(tokenExpiration) - now()
            doRefresh = timeLeft < 30.days
        }
        val requireBiometrics = gvm.userPreferences.autoLoginRequireBiometrics.first()
        if (requireBiometrics) {
            val biometricsSuccess = biometricsLogin()
            if (!biometricsSuccess) {
                return null
            }
        }
        val response = api.authenticateWithToken(token, refreshToken = doRefresh) ?: return null
        saveToken(response, gvm) // Save it even if we didn't ask for it
        return response.account to response.connection!!
    }

    suspend fun manualLogin(
        email: String,
        password: String,
        gvm: GlobalViewModel,
    ): Pair<Account, Api.Connection>? {
        val requestToken = gvm.userPreferences.autoLogin.first()
        val response = api.authenticate(email, password, requestToken) ?: return null
        if (requestToken) {
            saveToken(response, gvm)
        }
        return response.account to response.connection!!
    }

    suspend fun logout(gvm: GlobalViewModel) {
        when (val ls = gvm.loginState.value) {
            is GlobalViewModel.LoginState.LoggedIn -> ls.connection.close()
            else -> { }
        }
        gvm.userPreferences.delete(UserPreferences.Keys.AuthToken)
        gvm.userPreferences.delete(UserPreferences.Keys.AuthTokenExpiration)
        gvm.userPreferences.delete(UserPreferences.Keys.AuthTokenEmail)
    }

    suspend fun register(
        email: String,
        password: String,
        gvm: GlobalViewModel,
    ): Account? {
        val requestToken = gvm.userPreferences.autoLogin.first()
        val response = api.register(email, password, requestToken) ?: return null
        if (requestToken) {
            saveToken(response, gvm)
        }
        return response.account
    }
}
