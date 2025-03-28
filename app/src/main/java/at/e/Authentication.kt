package at.e

import at.e.api.Account
import at.e.api.Api
import at.e.api.api
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.Instant

object Authentication {
    private fun biometricsLogin(): Boolean = true // TODO

    suspend fun autoLogin(gvm: GlobalViewModel): Pair<Account, Api.Connection>? {
        val token = gvm.userPreferences.authToken.first() ?: return null
        var doRefresh = true
        val tokenExpiration = gvm.userPreferences.authTokenExpiration.first()
        if (tokenExpiration != null) {
            val timeLeft = Duration.between(
                Instant.now(),
                Instant.ofEpochSecond(tokenExpiration)
            )
            doRefresh = timeLeft < Duration.ofDays(30)
        }
        val requireBiometrics = gvm.userPreferences.autoLoginRequireBiometrics.first()
        if (requireBiometrics) {
            val biometricsSuccess = biometricsLogin()
            if (!biometricsSuccess) {
                return null
            }
        }
        val response = api.authenticateWithToken(token, refreshToken = doRefresh) ?: return null
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
        }
        return response.account to response.connection!!
    }

    suspend fun manualLogin(
        email: String,
        password: String,
        requestToken: Boolean = false,
        gvm: GlobalViewModel,
    ): Pair<Account, Api.Connection>? {
        val response = api.authenticate(email, password, requestToken) ?: return null
        if (requestToken && response.newToken != null) {
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
    }

    suspend fun register(
        email: String,
        password: String,
        requestToken: Boolean,
        gvm: GlobalViewModel,
    ): Account? {
        val response = api.register(email, password, requestToken) ?: return null
        if (requestToken && response.newToken != null) {
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
        }
        return response.account
    }
}
