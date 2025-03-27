package at.e.api.faux

import at.e.api.Account
import at.e.api.Api
import at.e.api.Order
import at.e.api.Restaurant
import at.e.api.faux.lib.ObjectFuzzySearch
import kotlinx.coroutines.delay
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

@OptIn(ExperimentalEncodingApi::class)
object FauxApi : Api {
    private const val TOKEN_LENGTH = 256
    private val TOKEN_DURATION = Duration.ofDays(100)
    private val rnd = SecureRandom.getInstanceStrong()

    private data class PasswordEntry(val info: Account, val passwordHash: String)
    private data class TokenEntry(val info: Account, val expiration: Long)

    private val accounts = mutableMapOf<String, PasswordEntry>()
    private val tokens = mutableMapOf<String, TokenEntry>()

    private fun hashPassword(password: String) =
        password.hashCode().toString() // Extremely safe algorithm

    private fun validatePassword(password: String, hash: String) =
        password.hashCode() == hash.toInt() // Extremely safe algorithm

    private fun generateToken(): Pair<String, Long> {
        assert(TOKEN_LENGTH % 4 == 0)
        val bytes = ByteArray(3 * TOKEN_LENGTH / 4)
        rnd.nextBytes(bytes)
        val token = Base64.encode(bytes)
        assert(token.length == TOKEN_LENGTH)
        val expiration = Instant.now().plus(TOKEN_DURATION).epochSecond
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
        val expired = Instant.ofEpochSecond(expiration).isAfter(Instant.now())
        return if (!expired) account else null
    }

    override suspend fun authenticate(
        email: String,
        password: String,
        requestToken: Boolean,
    ) = delayed {
        val account = validateCredentials(email, password) ?: return@delayed null
        val (newToken, newTokenExpiration) = generateAndStoreTokenIf(requestToken, account)
        Api.AuthResult(account, newToken, newTokenExpiration)
    }

    override suspend fun authenticateWithToken(
        currentToken: String,
        refreshToken: Boolean,
    ) = delayed {
        val account = validateToken(currentToken) ?: return@delayed null
        val (newToken, newTokenExpiration) = generateAndStoreTokenIf(refreshToken, account)
        Api.AuthResult(account, newToken, newTokenExpiration)
    }

    override suspend fun register(
        email: String,
        password: String,
        requestToken: Boolean
    ) = delayed {
        if (accounts.containsKey(email))
            return@delayed null
        val account = Account()
        accounts[email] = PasswordEntry(account, hashPassword(password))
        val (newToken, newTokenExpiration) = generateAndStoreTokenIf(requestToken, account)
        Api.AuthResult(account, newToken, newTokenExpiration)
    }

    override suspend fun getActiveOrder(account: Account): Order? = delayed {
        null // TODO
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
    private const val MAX_COUNT = 50

    override suspend fun getRestaurants(query: String) = delayed {
        when {
            query.isEmpty() -> RESTAURANTS.take(MAX_COUNT)
            else -> ObjectFuzzySearch.extractSorted(
                query, RESTAURANTS, TO_STRINGS, WEIGHTS, CUTOFF, MAX_COUNT
            )
        }
    }

    private suspend fun <R> delayed(action: () -> R): R {
        delay(Random.nextLong(200, 400)) // Simulate connection delay
        return action()
    }
}
