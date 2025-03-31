package at.e.api

import at.e.api.faux.FauxApi
import kotlin.uuid.Uuid

val api: Api =
    FauxApi // Development mode only

interface Api {
    interface Connection {
        suspend fun getActiveOrder(): Order?

        suspend fun close()
    }

    data class AuthResult(
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

    suspend fun findTable(uuid: Uuid): Pair<Table, Restaurant>?

    suspend fun getRestaurants(
        userLocation: Location,
        maxDistanceMeters: Float = 5000f,
        maxCount: Int = 50,
    ): List<Pair<Restaurant, /* distanceMeters: */ Float>>

    suspend fun getRestaurants(
        query: String,
        maxCount: Int = 50,
    ): List<Restaurant>

    suspend fun findTable(code: String, restaurant: Restaurant): Table?
}
