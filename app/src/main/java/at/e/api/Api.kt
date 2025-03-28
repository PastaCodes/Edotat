package at.e.api

import at.e.api.faux.FauxApi

val api: Api =
    FauxApi // Development mode only

interface Api {
    interface Connection {
        suspend fun getActiveOrder(): Order?

        suspend fun close()
    }

    class AuthResult(
        val account: Account,
        val newToken: String? = null,
        val newTokenExpiration: Long? = null,
        val connection: Connection? = null,
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

    suspend fun getRestaurants(query: String): List<Restaurant>
}
