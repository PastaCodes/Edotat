package at.e.api

import at.e.api.faux.FauxApi
import at.e.lib.Money
import kotlin.uuid.Uuid

val api: Api =
    FauxApi // Development mode only

interface Api {
    interface Connection {
        suspend fun requestToken(): AuthResult

        suspend fun getActiveOrder(): Order?

        suspend fun beginOrder(menu: Menu, table: Table): Order

        suspend fun getActiveSuborder(): Pair<Suborder, List<Order.Entry>>?

        suspend fun beginSuborder(): Pair<Suborder, List<Order.Entry>>

        suspend fun incrementItemQuantity(item: Menu.Item): Int

        suspend fun decrementItemQuantity(item: Menu.Item): Int

        suspend fun sendSuborder()

        suspend fun getSuborderHistory(): List<Pair<Suborder, List<Order.Entry>>>

        suspend fun getCurrentTotal(currency: Money.Currency): Money.Amount

        suspend fun endOrder() // In the real world this would need a verification system

        suspend fun deleteAccountAndClose()

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

    suspend fun findTable(uuid: Uuid): Table?

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

    suspend fun getMenus(restaurant: Restaurant): List<Menu>

    suspend fun getMenuItems(menu: Menu): Map<Menu.Category, List<Menu.Item>>
}
