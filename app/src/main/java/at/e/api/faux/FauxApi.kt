package at.e.api.faux

import at.e.api.Account
import at.e.api.Api
import at.e.api.Location
import at.e.api.Menu
import at.e.api.Order
import at.e.api.Restaurant
import at.e.api.Table
import at.e.api.faux.lib.ObjectFuzzySearch
import at.e.lib.minuteOfDay
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock.System.now
import kotlinx.datetime.Instant
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
    private val orders = mutableMapOf<Account, Order>()

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
                orders[this.account]
            }

            override suspend fun beginOrder(menu: Menu, table: Table) = delayed {
                assert(this in connections)
                assert(this.account !in orders)
                Order(menu, table).also { orders[this.account] = it }
            }

            override suspend fun deleteAccountAndClose() {
                assert(this in connections)
                accounts.entries.removeAll { (_, entry) ->
                    entry.info.email == account.email
                }
                tokens.entries.removeAll { (_, entry) ->
                    entry.info.email == account.email
                }
                connections.remove(this)
            }

            override suspend fun close() {
                assert(this in connections)
                connections.remove(this)
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
                Menu.Item("Bruschetta con Pomodorini", appetizers),
                Menu.Item("Caprese Salad", appetizers),
                Menu.Item("Olive all'Ascolana", appetizers),
            ),
            pasta to listOf(
                Menu.Item("Spaghetti alla Carbonara", pasta),
                Menu.Item("Tagliatelle al Ragù Bolognese", pasta),
                Menu.Item("Penne all'Arrabbiata", pasta),
                Menu.Item("Lasagna al Forno", pasta),
                Menu.Item("Ravioli Ricotta e Spinaci", pasta),
                Menu.Item("Linguine alle Vongole", pasta),
                Menu.Item("Bucatini all'Amatriciana", pasta),
                Menu.Item("Orecchiette con Cime di Rapa", pasta),
                Menu.Item("Pasta alla Norma", pasta),
            ),
            meat to listOf(
                Menu.Item("Osso Buco", meat),
                Menu.Item("Veal Marsala", meat),
                Menu.Item("Saltimbocca alla Romana", meat),
                Menu.Item("Bistecca alla Fiorentina", meat),
                Menu.Item("Pollo alla Cacciatora", meat),
                Menu.Item("Cotoletta alla Milanese", meat),
                Menu.Item("Tagliata di Manzo", meat),
            ),
            fish to listOf(
                Menu.Item("Grilled Branzino", fish),
                Menu.Item("Fritto Misto di Mare", fish),
                Menu.Item("Zuppa di Cozze", fish),
                Menu.Item("Spiedini di Gamberi e Calamari", fish),
                Menu.Item("Coda di Rospo alla Griglia", fish),
            ),
            sides to listOf(
                Menu.Item("Mixed Salad", sides),
                Menu.Item("Grilled Vegetables", sides),
                Menu.Item("Roasted Potatoes with Rosemary", sides),
                Menu.Item("French Fries", sides),
            ),
            pizza to listOf(
                Menu.Item("Margherita", pizza),
                Menu.Item("Quattro Stagioni", pizza),
                Menu.Item("Diavola", pizza),
                Menu.Item("Capricciosa", pizza),
                Menu.Item("Quattro Formaggi", pizza),
                Menu.Item("Prosciutto e Funghi", pizza),
                Menu.Item("Salsiccia e Friarielli", pizza),
                Menu.Item("Ortolana", pizza),
            ),
            desserts to listOf(
                Menu.Item("Tiramisù", desserts),
                Menu.Item("Panna Cotta", desserts),
                Menu.Item("Cassata Siciliana", desserts),
            ),
            drinks to listOf(
                Menu.Item("Still Water", drinks),
                Menu.Item("Sparkling Water", drinks),
                Menu.Item("Chinotto", drinks),
                Menu.Item("Oransoda", drinks),
                Menu.Item("Lemonsoda", drinks),
                Menu.Item("Straight Up Ethanol In A Glass", drinks),
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
