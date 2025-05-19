package at.e

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.StringRes
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

    private suspend fun saveToken(
        response: Api.AuthResult,
        activity: FragmentActivity,
        crs: CoroutineScope,
        gvm: GlobalViewModel,
    ): Boolean {
        if (response.newToken != null) {
            var token = response.newToken
            if (gvm.userPreferences.autoLoginRequireBiometrics.first()) {
                val (encrypted, iv) = biometricEncrypt(token, R.string.biometric_prompt_subtitle_encrypt, activity, crs, gvm) ?: return false
                token = encrypted
                gvm.savePreference(UserPreferences.Keys.AuthTokenIv, iv)
            }
            gvm.userPreferences.save(
                UserPreferences.Keys.AuthToken,
                token
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
        return true
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
        @StringRes subtitleResId: Int,
        activity: FragmentActivity,
        crs: CoroutineScope,
        gvm: GlobalViewModel,
    ): Cipher? {
        if (!isBiometricAuthEnabled(gvm)) {
            return null
        }
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.biometric_prompt_title))
            .setSubtitle(activity.getString(subtitleResId))
            .setNegativeButtonText(activity.getString(R.string.action_cancel))
            .setConfirmationRequired(false)
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
        @StringRes promptTitleResId: Int,
        activity: FragmentActivity,
        crs: CoroutineScope,
        gvm: GlobalViewModel,
    ): Pair<String, ByteArray>? {
        val lockedEncryptionCipher = getBiometricAuthEncryptionCipher()
        val iv = lockedEncryptionCipher.iv
        val unlockedEncryptionCipher =
            biometricPrompt(lockedEncryptionCipher, promptTitleResId, activity, crs, gvm) ?: return null
        val plainBytes = plainToken.toByteArray(Charsets.UTF_8)
        val encryptedBytes = unlockedEncryptionCipher.doFinal(plainBytes)
        val encrypted = Base64.encode(encryptedBytes)
        return encrypted to iv
    }

    private suspend fun biometricDecrypt(
        encryptedToken: String,
        iv: ByteArray,
        @StringRes promptTitleResId: Int,
        activity: FragmentActivity,
        crs: CoroutineScope,
        gvm: GlobalViewModel,
    ): String? {
        val lockedDecryptionCipher = getBiometricAuthDecryptionCipher(iv)
        val unlockedDecryptionCipher = biometricPrompt(lockedDecryptionCipher, promptTitleResId, activity, crs, gvm)
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
            token = biometricDecrypt(token, iv, R.string.biometric_prompt_subtitle_decrypt, activity, crs, gvm) ?: return null
        }
        val response = api.authenticateWithToken(token, refreshToken = doRefresh) ?: return null
        if (!requireBiometrics || doRefresh) { // If we don't need to bother the user, save the token even if we didn't ask for it
            saveToken(response, activity, crs, gvm) || return null
        }
        gvm.savePreference(UserPreferences.Keys.NeverLoggedIn, false)
        return response.account to response.connection!!
    }

    suspend fun manualLogin(
        email: String,
        password: String,
        activity: FragmentActivity,
        crs: CoroutineScope,
        gvm: GlobalViewModel,
    ): Pair<Account, Api.Connection>? {
        deleteToken(gvm)
        val requestToken = gvm.userPreferences.autoLogin.first()
        val response = api.authenticate(email, password, requestToken) ?: return null
        if (requestToken) {
            saveToken(response, activity, crs, gvm) || return null
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

    suspend fun requestToken(
        connection: Api.Connection,
        activity: FragmentActivity,
        crs: CoroutineScope,
        gvm: GlobalViewModel,
    ): Boolean {
        val response = connection.requestToken()
        return saveToken(response, activity, crs, gvm)
    }

    suspend fun encryptToken(
        activity: FragmentActivity,
        crs: CoroutineScope,
        gvm: GlobalViewModel,
    ): Boolean {
        val plain = gvm.userPreferences.authToken.first() ?: return false
        val (encrypted, iv) = biometricEncrypt(plain, R.string.biometric_prompt_subtitle_encrypt, activity, crs, gvm) ?: return false
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
        val decrypted = biometricDecrypt(encrypted, iv, R.string.biometric_prompt_subtitle_decrypt, activity, crs, gvm) ?: return false
        gvm.savePreference(UserPreferences.Keys.AuthToken, decrypted)
        return true
    }

    suspend fun register(
        email: String,
        password: String,
        activity: FragmentActivity,
        crs: CoroutineScope,
        gvm: GlobalViewModel,
    ): Account? {
        deleteToken(gvm)
        val requestToken = gvm.userPreferences.autoLogin.first()
        val response = api.register(email, password, requestToken) ?: return null
        if (requestToken) {
            saveToken(response, activity, crs, gvm) || return null
        }
        return response.account
    }
}
