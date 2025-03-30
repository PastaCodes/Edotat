package at.e.api.faux

import at.e.api.Account
import at.e.api.Api
import at.e.api.Location
import at.e.api.Order
import at.e.api.Restaurant
import at.e.api.faux.lib.ObjectFuzzySearch
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

    private val RESTAURANTS_BY_UUID =
        RESTAURANTS
            .associateBy { it.uuid }
    private val TABLES_BY_UUID =
        TABLES
            .flatMap { (restaurantUuid, tables) ->
                tables.entries.associateWith { restaurantUuid }.entries
            }
            .associate { (tableEntry, restaurantUuid) ->
                val (tableUuid, table) = tableEntry
                tableUuid to (table to RESTAURANTS_BY_UUID[restaurantUuid]!!)
            }
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
            val (token, expiration) = generateToken()
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
            override suspend fun getActiveOrder(): Order? = delayed {
                null // TODO
            }

            override suspend fun close() {
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
    }
}
