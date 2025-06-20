package at.e.api.faux

import at.e.api.Account
import at.e.api.Api
import at.e.api.Location
import at.e.api.Menu
import at.e.api.Order
import at.e.api.Restaurant
import at.e.api.Suborder
import at.e.api.Table
import at.e.api.faux.lib.ObjectFuzzySearch
import at.e.lib.Money
import at.e.lib.euros
import at.e.lib.minuteOfDay
import at.e.lib.sumOf
import at.e.lib.times
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock.System.now
import kotlinx.datetime.Instant
import kotlinx.datetime.toLocalDateTime
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random
import kotlin.time.Duration.Companion.days
import kotlin.uuid.Uuid

@OptIn(ExperimentalEncodingApi::class)
object FauxApi : Api {
    private const val TOKEN_LENGTH = 256
    private val TOKEN_DURATION = 100.days

    private data class PasswordEntry(val info: Account, val passwordHash: String)
    private data class TokenEntry(val info: Account, val expiration: Long)

    private val accounts = mutableMapOf<String, PasswordEntry>()
    private val tokens = mutableMapOf<String, TokenEntry>()
    private val connections = mutableMapOf<Api.Connection, Account>()
    private val orders = mutableMapOf<Account, OrderState>() // Active orders
    private val orderHistory = mutableMapOf<Account, MutableList<OrderHistory>>()

    private val RESTAURANTS_BY_UUID =
        RESTAURANTS
            .associateBy { it.uuid }
    private val TABLES_BY_UUID =
        TABLES
            .flatMap { it.value.entries }
            .associate { it.key to it.value }
    private val TABLES_BY_CODE =
        TABLES
            .mapValues { (_, tables) ->
                tables.mapKeys { (_, table) ->
                    table.code
                }
            }

    private val Api.Connection.account
        get() = connections[this]!!

    private fun hashPassword(password: String) =
        password.hashCode().toString() // Extremely safe algorithm

    private fun validatePassword(password: String, hash: String) =
        password.hashCode() == hash.toInt() // Extremely safe algorithm

    private fun generateToken(): Pair<String, Long> {
        assert(TOKEN_LENGTH % 4 == 0)
        val bytes = ByteArray(3 * TOKEN_LENGTH / 4)
        Random.nextBytes(bytes)
        val token = Base64.encode(bytes)
        assert(token.length == TOKEN_LENGTH)
        val expiration = now().plus(TOKEN_DURATION).epochSeconds
        return token to expiration
    }

    private fun generateAndStoreTokenIf(
        generateCondition: Boolean,
        account: Account?,
    ) =
        if (generateCondition) {
            val (token, expiration) =
                if (account?.email == "dummy")
                    "dummy" to Instant.DISTANT_FUTURE.epochSeconds
                else
                    generateToken()
            if (account != null) {
                tokens[token] = TokenEntry(account, expiration)
            }
            token to expiration
        } else
            null to null

    private fun validateCredentials(email: String, password: String): Account? {
        val (account, passwordHash) = accounts[email] ?: return null
        return if (validatePassword(password, passwordHash)) account else null
    }

    private fun validateToken(token: String): Account? {
        val (account, expiration) = tokens[token] ?: return null
        val expired = now() > Instant.fromEpochSeconds(expiration)
        return if (!expired) account else null
    }

    private fun createConnection(account: Account) =
        object : Api.Connection {
            override suspend fun requestToken() = delayed {
                assert(this in connections)
                val (newToken, newTokenExpiration) = generateAndStoreTokenIf(true, account)
                Api.AuthResult(account, newToken, newTokenExpiration, this)
            }

            override suspend fun getActiveOrder() = delayed {
                assert(this in connections)
                orders[this.account]?.order
            }

            override suspend fun beginOrder(menu: Menu, table: Table) = delayed {
                assert(this in connections)
                assert(this.account !in orders)
                Order(
                    menu,
                    table,
                    this.account,
                    started = now().toLocalDateTime(table.restaurant.timeZone),
                ).also { orders[this.account] = OrderState(it) }
            }

            override suspend fun getActiveSuborder() = delayed {
                assert(this in connections)
                val state = orders[this.account]!!.activeSuborder
                if (state == null) {
                    null
                } else {
                    state.suborder to state.items
                }
            }

            override suspend fun beginSuborder() = delayed {
                assert(this in connections)
                val order = orders[this.account]!!
                assert(order.activeSuborder == null)
                val suborder = Suborder(
                    order.order,
                    started = now().toLocalDateTime(order.order.table.restaurant.timeZone),
                )
                order.activeSuborder = SuborderState(suborder)
                suborder to listOf<Order.Entry>()
            }

            override suspend fun incrementItemQuantity(item: Menu.Item) = delayed {
                assert(this in connections)
                orders[this.account]!!.activeSuborder!!.incrementItemQuantity(item)
            }

            override suspend fun decrementItemQuantity(item: Menu.Item) = delayed {
                assert(this in connections)
                val newQuantity = orders[this.account]!!.activeSuborder!!.decrementItemQuantity(item)
                if (orders[this.account]!!.activeSuborder!!.items.isEmpty()) {
                    orders[this.account]!!.activeSuborder = null
                }
                newQuantity
            }

            override suspend fun sendSuborder() {
                delayed {
                    assert(this in connections)
                    val order = orders[this.account]!!
                    val suborder = order.activeSuborder!!
                    suborder.suborder.sent =
                        now().toLocalDateTime(order.order.table.restaurant.timeZone)
                    order.suborderHistory.add(suborder)
                    order.activeSuborder = null
                }
            }

            override suspend fun getSuborderHistory() = delayed {
                assert(this in connections)
                orders[this.account]!!.suborderHistory
                    .map { state -> state.suborder to state.items }
            }

            override suspend fun getCurrentTotal(currency: Money.Currency) = delayed {
                assert(this in connections)
                orders[this.account]!!.suborderHistory
                    .flatMap { suborder -> suborder.items }
                    .sumOf(currency) { entry -> entry.item.price * entry.quantity }
            }

            override suspend fun endOrder() {
                delayed {
                    assert(this in connections)
                    assert(orders[this.account]!!.activeSuborder == null)
                    val orderState = orders.remove(this.account)!!
                    if (this.account !in orderHistory) {
                        orderHistory[this.account] = mutableListOf()
                    }
                    orderHistory[this.account]!!.add(orderState.toHistory())
                }
            }

            override suspend fun getOrderHistory() = delayed {
                assert(this in connections)
                if (this.account !in orderHistory) {
                    return@delayed listOf()
                }
                orderHistory[this.account]!!.map { history ->
                    history.order to history.suborders.map { suborder ->
                        suborder.suborder to suborder.items
                    }
                }
            }

            override suspend fun deleteAccountAndClose() {
                delayed {
                    assert(this in connections)
                    accounts.entries.removeAll { (_, entry) ->
                        entry.info.email == account.email
                    }
                    tokens.entries.removeAll { (_, entry) ->
                        entry.info.email == account.email
                    }
                    connections.remove(this)
                }
            }

            override suspend fun close() {
                delayed {
                    assert(this in connections)
                    connections.remove(this)
                    Unit
                }
            }
        }
            .also { connections[it] = account }

    override suspend fun authenticate(
        email: String,
        password: String,
        requestToken: Boolean,
    ) = delayed {
        val account = validateCredentials(email, password) ?: return@delayed null
        val (newToken, newTokenExpiration) = generateAndStoreTokenIf(requestToken, account)
        Api.AuthResult(account, newToken, newTokenExpiration, createConnection(account))
    }

    override suspend fun authenticateWithToken(
        currentToken: String,
        refreshToken: Boolean,
    ) = delayed {
        val account = validateToken(currentToken) ?: return@delayed null
        val (newToken, newTokenExpiration) = generateAndStoreTokenIf(refreshToken, account)
        Api.AuthResult(account, newToken, newTokenExpiration, createConnection(account))
    }

    override suspend fun register(
        email: String,
        password: String,
        requestToken: Boolean
    ) = delayed {
        if (accounts.containsKey(email))
            return@delayed null
        val account = Account(email)
        accounts[email] = PasswordEntry(account, hashPassword(password))
        val (newToken, newTokenExpiration) = generateAndStoreTokenIf(requestToken, account)
        Api.AuthResult(account, newToken, newTokenExpiration, createConnection(account))
    }

    override suspend fun findTable(uuid: Uuid) = delayed {
        TABLES_BY_UUID[uuid]
    }

    override suspend fun getRestaurants(
        userLocation: Location,
        maxDistanceMeters: Float,
        maxCount: Int,
    ) = delayed {
        RESTAURANTS
            .map { restaurant ->
                val distanceMeters = FloatArray(1)
                android.location.Location.distanceBetween(
                    /* startLatitude = */ userLocation.latitudeDegrees.toDouble(),
                    /* startLongitude = */ userLocation.longitudeDegrees.toDouble(),
                    /* endLatitude = */ restaurant.location.latitudeDegrees.toDouble(),
                    /* endLongitude = */ restaurant.location.longitudeDegrees.toDouble(),
                    /* results = */ distanceMeters,
                )
                restaurant to distanceMeters[0]
            }
            .filter { (_, distanceMeters) ->
                distanceMeters < maxDistanceMeters
            }
            .sortedBy { (_, distanceMeters) ->
                distanceMeters
            }
    }

    private val TO_STRINGS: (Restaurant) -> List<String> = { restaurant ->
        listOf(
            restaurant.name,
            restaurant.address.streetAddress,
            restaurant.address.locality,
            restaurant.address.division,
            restaurant.address.country,
        )
    }
    private val WEIGHTS = listOf(3, 1, 2, 1, 1)
    private const val CUTOFF = 1000

    override suspend fun getRestaurants(query: String, maxCount: Int) = delayed {
        when {
            query.isEmpty() -> RESTAURANTS.take(maxCount)
            else -> ObjectFuzzySearch.extractSorted(
                query, RESTAURANTS, TO_STRINGS, WEIGHTS, CUTOFF, maxCount
            )
        }
    }

    override suspend fun getMenus(restaurant: Restaurant) = delayed {
        listOf(
            Menu("Breakfast menu", minuteOfDay(8, 0), minuteOfDay(10, 0), restaurant),
            Menu("Lunch menu", minuteOfDay(12, 0), minuteOfDay(14, 0), restaurant),
            Menu("Dinner menu", minuteOfDay(19, 0), minuteOfDay(21, 0), restaurant),
            Menu("Dummy menu", minuteOfDay(0, 0), minuteOfDay(24, 0), restaurant),
        )
    }

    // Item names and descriptions were chosen with the help of generative AI
    override suspend fun getMenuItems(menu: Menu) = delayed {
        val appetizers = Menu.Category("Appetizers", menu)
        val pasta = Menu.Category("Pasta", menu)
        val meat = Menu.Category("Meat", menu)
        val fish = Menu.Category("Fish", menu)
        val sides = Menu.Category("Sides", menu)
        val pizza = Menu.Category("Pizza", menu)
        val desserts = Menu.Category("Desserts", menu)
        val drinks = Menu.Category("Drinks", menu)
        mapOf(
            appetizers to listOf(
                Menu.Item("Bruschetta con Pomodorini", "Grilled bread topped with cherry tomatoes, garlic, basil, and olive oil.", 4.00.euros(), appetizers),
                Menu.Item("Caprese Salad", "Fresh mozzarella, ripe tomatoes, basil, and olive oil.", 5.00.euros(), appetizers),
                Menu.Item("Olive all'Ascolana", "Fried green olives stuffed with seasoned meat.", 5.00.euros(), appetizers),
            ),
            pasta to listOf(
                Menu.Item("Spaghetti alla Carbonara", "Spaghetti with eggs, pecorino, guanciale, and black pepper.", 9.00.euros(), pasta),
                Menu.Item("Tagliatelle al Ragù Bolognese", "Tagliatelle with slow-cooked beef ragù.", 8.00.euros(), pasta),
                Menu.Item("Penne all'Arrabbiata", "Penne in spicy tomato sauce with garlic and chili.", 7.00.euros(), pasta),
                Menu.Item("Lasagna al Forno", "Layered pasta with meat ragù, béchamel, and cheese.", 8.00.euros(), pasta),
                Menu.Item("Ravioli Ricotta e Spinaci", "Pasta filled with ricotta cheese and spinach.", 8.00.euros(), pasta),
                Menu.Item("Linguine alle Vongole", "Linguine with clams in garlic white wine sauce.", 9.00.euros(), pasta),
                Menu.Item("Bucatini all'Amatriciana", "Bucatini with guanciale, tomato sauce, and pecorino.", 8.00.euros(), pasta),
                Menu.Item("Orecchiette con Cime di Rapa", "Orecchiette pasta with turnip greens, garlic, and chili.", 8.00.euros(), pasta),
                Menu.Item("Pasta alla Norma", "Pasta with tomato sauce, fried eggplant, and ricotta salata.", 9.00.euros(), pasta),
            ),
            meat to listOf(
                Menu.Item("Osso Buco", "Braised veal shanks with vegetables, white wine, and gremolata.", 10.00.euros(), meat),
                Menu.Item("Veal Marsala", "Veal cutlets in Marsala wine and mushroom sauce.", 9.00.euros(), meat),
                Menu.Item("Saltimbocca alla Romana", "Veal with prosciutto and sage in white wine sauce.", 8.00.euros(), meat),
                Menu.Item("Bistecca alla Fiorentina", "Thick-cut T-bone steak, grilled rare.", 15.00.euros(), meat),
                Menu.Item("Pollo alla Cacciatora", "Chicken stewed with tomatoes, onions, and herbs.", 7.50.euros(), meat),
                Menu.Item("Cotoletta alla Milanese", "Breaded and fried veal cutlet.", 8.00.euros(), meat),
                Menu.Item("Tagliata di Manzo", "Sliced grilled beef steak with arugula and parmesan.", 9.00.euros(), meat),
            ),
            fish to listOf(
                Menu.Item("Grilled Branzino", "Whole sea bass grilled with herbs and lemon.", 7.00.euros(), fish),
                Menu.Item("Fritto Misto di Mare", "Mixed fried seafood with shrimp, squid, and fish.", 9.00.euros(), fish),
                Menu.Item("Zuppa di Cozze", "Mussels cooked in tomato and white wine broth.", 8.00.euros(), fish),
                Menu.Item("Spiedini di Gamberi e Calamari", "Skewered grilled shrimp and squid.", 8.00.euros(), fish),
                Menu.Item("Coda di Rospo alla Griglia", "Grilled monkfish with herbs and olive oil.", 8.00.euros(), fish),
            ),
            sides to listOf(
                Menu.Item("Grilled Vegetables", "Zucchini, bell peppers, and eggplant grilled with olive oil.", 4.00.euros(), sides),
                Menu.Item("Roasted Potatoes with Rosemary", "Crispy oven-roasted potatoes seasoned with rosemary.", 5.00.euros(), sides),
                Menu.Item("French Fries", null, 5.00.euros(), sides),
            ),
            pizza to listOf(
                Menu.Item("Margherita", "Tomato sauce, mozzarella, and basil.", 7.00.euros(), pizza),
                Menu.Item("Diavola", "Spicy salami, tomato sauce, and mozzarella.", 8.00.euros(), pizza),
                Menu.Item("Capricciosa", "Tomato, mozzarella, mushrooms, artichokes, ham, and olives.", 8.00.euros(), pizza),
                Menu.Item("Quattro Formaggi", "Mozzarella, gorgonzola, parmesan, and fontina.", 8.00.euros(), pizza),
                Menu.Item("Prosciutto e Funghi", "Ham and mushroom, tomato, and mozzarella.", 7.50.euros(), pizza),
                Menu.Item("Salsiccia e Friarielli", "Sausage and bitter broccoli rabe with mozzarella.", 8.50.euros(), pizza),
                Menu.Item("Ortolana", "Assorted grilled vegetables with tomato sauce and mozzarella.", 7.50.euros(), pizza),
            ),
            desserts to listOf(
                Menu.Item("Tiramisù", "Layered dessert with coffee-soaked ladyfingers, mascarpone, and cocoa.", 6.00.euros(), desserts),
                Menu.Item("Panna Cotta", "Creamy gelatin dessert served with berry sauce.", 7.00.euros(), desserts),
                Menu.Item("Cassata Siciliana", "Sponge cake layered with ricotta, candied fruit, and marzipan.", 8.00.euros(), desserts),
            ),
            drinks to listOf(
                Menu.Item("Still Water", null, 2.00.euros(), drinks),
                Menu.Item("Sparkling Water", null, 2.00.euros(), drinks),
                Menu.Item("Chinotto", null, 3.00.euros(), drinks),
                Menu.Item("Oransoda", null, 3.00.euros(), drinks),
                Menu.Item("Lemonsoda", null, 3.00.euros(), drinks),
                Menu.Item("Straight Up Ethanol In A Glass", null, 0.01.euros(), drinks),
            ),
        )
    }

    override suspend fun findTable(code: String, restaurant: Restaurant) = delayed {
        val tables = TABLES_BY_CODE[restaurant.uuid] ?: return@delayed null
        tables[code]
    }

    private suspend fun <R> delayed(action: () -> R): R {
        delay(Random.nextLong(200, 400)) // Simulate connection delay
        return action()
    }

    init {
        // Add a dummy account
        val account = Account("dummy")
        val password = "dummy"
        accounts[account.email] = PasswordEntry(account, hashPassword(password))
        tokens["dummy"] = TokenEntry(account, Instant.DISTANT_FUTURE.epochSeconds)
    }
}
