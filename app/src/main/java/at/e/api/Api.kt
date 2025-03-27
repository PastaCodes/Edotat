package at.e.api

import at.e.api.faux.FauxApi

val api: Api =
    FauxApi // Development mode only

interface Api {
    class AuthResult(
        val account: Account,
        val newToken: String? = null,
        val newTokenExpiration: Long? = null,
    )

    suspend fun authenticate(
        email: String,
        password: String,
        requestToken: Boolean = true,
    ): AuthResult?

    suspend fun authenticateWithToken(
        currentToken: String,
        refreshToken: Boolean = false,
    ): AuthResult?

    suspend fun register(email: String, password: String, requestToken: Boolean): AuthResult?

    suspend fun getActiveOrder(account: Account): Order?

    suspend fun getRestaurants(query: String): List<Restaurant>
}
