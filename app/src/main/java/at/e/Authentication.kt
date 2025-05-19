package at.e

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import at.e.api.Account
import at.e.api.Api
import at.e.api.api
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock.System.now
import kotlinx.datetime.Instant
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.io.encoding.Base64
import kotlin.time.Duration.Companion.days

// TODO be careful about overriding an encrypted token with a plain one!

object Authentication {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)

    private const val BIOMETRIC_AUTH_KEY = "BiometricAuthKey"
    private const val BIOMETRIC_AUTH_KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES

    private val keyGenerator =
        KeyGenerator.getInstance(
            BIOMETRIC_AUTH_KEY_ALGORITHM,
            ANDROID_KEYSTORE,
        )
            .also {
                val keyGenParameters =
                    KeyGenParameterSpec.Builder(
                        BIOMETRIC_AUTH_KEY,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    )
                        .apply {
                            setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            setUserAuthenticationRequired(true)
                        }
                        .build()
                it.init(keyGenParameters)
            }

    private const val BIOMETRIC_AUTH_ALGORITHM = "AES/GCM/NoPadding"

    init {
        keyStore.load(null)
        if (!keyStore.containsAlias(BIOMETRIC_AUTH_KEY)) {
            keyGenerator.generateKey()
        }
    }

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
            } else {
                gvm.userPreferences.delete(UserPreferences.Keys.AuthTokenExpiration)
            }
        } else {
            deleteToken(gvm)
        }
    }

    fun isBiometricAuthEnabled(gvm: GlobalViewModel) =
        gvm.biometricManager.canAuthenticate(BIOMETRIC_STRONG) == BIOMETRIC_SUCCESS

    private fun getBiometricAuthKey() =
        keyStore.getKey(BIOMETRIC_AUTH_KEY, null) as SecretKey

    private fun getBiometricAuthEncryptionCipher() =
        Cipher.getInstance(BIOMETRIC_AUTH_ALGORITHM).also {
            it.init(Cipher.ENCRYPT_MODE, getBiometricAuthKey())
        }

    private fun getBiometricAuthDecryptionCipher(iv: ByteArray) =
        Cipher.getInstance(BIOMETRIC_AUTH_ALGORITHM).also {
            val spec = GCMParameterSpec(128, iv)
            it.init(Cipher.DECRYPT_MODE, getBiometricAuthKey(), spec)
        }

    private suspend fun biometricPrompt(
        lockedCipher: Cipher,
        title: String,
        activity: FragmentActivity,
        crs: CoroutineScope,
        gvm: GlobalViewModel,
    ): Cipher? {
        if (!isBiometricAuthEnabled(gvm)) {
            return null
        }
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle("TMP1")
            .setDescription("TMP2")
            .setConfirmationRequired(false)
            .setNegativeButtonText("TMP3")
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()
        return suspendCoroutine { continuation ->
            val biometricPrompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(gvm.app),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult
                    ) {
                        val unlockedCipher = result.cryptoObject!!.cipher!!
                        continuation.resume(unlockedCipher)
                    }

                    override fun onAuthenticationFailed() {
                        continuation.resume(null)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        continuation.resume(null)
                    }
                })
            crs.launch {
                biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(lockedCipher))
            }
        }
    }

    private suspend fun biometricEncrypt(
        plainToken: String,
        activity: FragmentActivity,
        crs: CoroutineScope,
        gvm: GlobalViewModel,
    ): Pair<String, ByteArray>? {
        val lockedEncryptionCipher = getBiometricAuthEncryptionCipher()
        val iv = lockedEncryptionCipher.iv
        val unlockedEncryptionCipher =
            biometricPrompt(lockedEncryptionCipher, "PIEDI", activity, crs, gvm) ?: return null
        val plainBytes = plainToken.toByteArray(Charsets.UTF_8)
        val encryptedBytes = unlockedEncryptionCipher.doFinal(plainBytes)
        val encrypted = Base64.encode(encryptedBytes)
        return encrypted to iv
    }

    private suspend fun biometricDecrypt(
        encryptedToken: String,
        iv: ByteArray,
        activity: FragmentActivity,
        crs: CoroutineScope,
        gvm: GlobalViewModel,
    ): String? {
        val lockedDecryptionCipher = getBiometricAuthDecryptionCipher(iv)
        val unlockedDecryptionCipher = biometricPrompt(lockedDecryptionCipher, "PIEDI", activity, crs, gvm)
        val encryptedBytes = Base64.decode(encryptedToken)
        val decryptedBytes = unlockedDecryptionCipher?.doFinal(encryptedBytes) ?: return null
        val decrypted = String(decryptedBytes, Charsets.UTF_8)
        return decrypted
    }

    suspend fun autoLogin(
        activity: FragmentActivity,
        crs: CoroutineScope,
        gvm: GlobalViewModel,
    ): Pair<Account, Api.Connection>? {
        val autoLogin = gvm.userPreferences.autoLogin.first()
        if (!autoLogin) {
            return null
        }
        var token = gvm.userPreferences.authToken.first() ?: return null
        var doRefresh = true // Temporary value; only refresh if about to expire
        val tokenExpiration = gvm.userPreferences.authTokenExpiration.first()
        if (tokenExpiration != null) {
            val timeLeft = Instant.fromEpochSeconds(tokenExpiration.toLong()) - now()
            doRefresh = timeLeft < 30.days
        }
        val requireBiometrics = gvm.userPreferences.autoLoginRequireBiometrics.first()
        if (requireBiometrics) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                return null
            }
            val iv = gvm.userPreferences.authTokenIv.first() ?: return null
            token = biometricDecrypt(token, iv, activity, crs, gvm) ?: return null
        }
        val response = api.authenticateWithToken(token, refreshToken = doRefresh) ?: return null
        saveToken(response, gvm) // Save it even if we didn't ask for it
        gvm.savePreference(UserPreferences.Keys.NeverLoggedIn, false)
        return response.account to response.connection!!
    }

    suspend fun manualLogin(
        email: String,
        password: String,
        gvm: GlobalViewModel,
    ): Pair<Account, Api.Connection>? {
        deleteToken(gvm)
        val requestToken = gvm.userPreferences.autoLogin.first()
        val response = api.authenticate(email, password, requestToken) ?: return null
        if (requestToken) {
            saveToken(response, gvm)
        }
        gvm.savePreference(UserPreferences.Keys.NeverLoggedIn, false)
        return response.account to response.connection!!
    }

    suspend fun logout(gvm: GlobalViewModel) {
        when (val ls = gvm.loginState.value) {
            is GlobalViewModel.LoginState.LoggedIn -> ls.connection.close()
            else -> { }
        }
        deleteToken(gvm)
    }

    suspend fun logoutAndDeleteAccount(gvm: GlobalViewModel) {
        when (val ls = gvm.loginState.value) {
            is GlobalViewModel.LoginState.LoggedIn -> ls.connection.deleteAccountAndClose()
            else -> { }
        }
        deleteToken(gvm)
    }

    suspend fun deleteToken(gvm: GlobalViewModel) {
        gvm.userPreferences.delete(UserPreferences.Keys.AuthToken)
        gvm.userPreferences.delete(UserPreferences.Keys.AuthTokenExpiration)
        gvm.userPreferences.delete(UserPreferences.Keys.AuthTokenIv)
    }

    suspend fun requestToken(connection: Api.Connection, gvm: GlobalViewModel) {
        val response = connection.requestToken()
        saveToken(response, gvm)
    }

    suspend fun encryptToken(
        activity: FragmentActivity,
        crs: CoroutineScope,
        gvm: GlobalViewModel,
    ): Boolean {
        val plain = gvm.userPreferences.authToken.first() ?: return false
        val (encrypted, iv) = biometricEncrypt(plain, activity, crs, gvm) ?: return false
        gvm.savePreference(UserPreferences.Keys.AuthToken, encrypted)
        gvm.savePreference(UserPreferences.Keys.AuthTokenIv, iv)
        return true
    }

    suspend fun decryptToken(
        activity: FragmentActivity,
        crs: CoroutineScope,
        gvm: GlobalViewModel,
    ): Boolean {
        val encrypted = gvm.userPreferences.authToken.first() ?: return false
        val iv = gvm.userPreferences.authTokenIv.first() ?: return false
        val decrypted = biometricDecrypt(encrypted, iv, activity, crs, gvm) ?: return false
        gvm.savePreference(UserPreferences.Keys.AuthToken, decrypted)
        return true
    }

    suspend fun register(
        email: String,
        password: String,
        gvm: GlobalViewModel,
    ): Account? {
        deleteToken(gvm)
        val requestToken = gvm.userPreferences.autoLogin.first()
        val response = api.register(email, password, requestToken) ?: return null
        if (requestToken) {
            saveToken(response, gvm)
        }
        return response.account
    }
}
