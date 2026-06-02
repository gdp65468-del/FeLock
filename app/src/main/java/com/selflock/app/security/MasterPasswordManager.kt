package com.selflock.app.security

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MasterPasswordManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun hasPasswordHash(): Boolean = prefs.getString(KEY_HASH, null) != null

    private fun getStoredHash(): String? = prefs.getString(KEY_HASH, null)
    private fun getStoredSalt(): String? = prefs.getString(KEY_SALT, null)

    suspend fun setPassword(password: String) = withContext(Dispatchers.Default) {
        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val hash = hashPassword(password, salt)
        prefs.edit()
            .putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putBoolean(KEY_ENABLED, true)
            .apply()
    }

    suspend fun verifyPassword(password: String): Boolean = withContext(Dispatchers.Default) {
        val storedHash = getStoredHash() ?: return@withContext false
        val storedSalt = getStoredSalt() ?: return@withContext false
        val salt = Base64.decode(storedSalt, Base64.NO_WRAP)
        val hash = hashPassword(password, salt)
        Base64.encodeToString(hash, Base64.NO_WRAP) == storedHash
    }

    fun clearPassword() {
        prefs.edit()
            .remove(KEY_HASH)
            .remove(KEY_SALT)
            .putBoolean(KEY_ENABLED, false)
            .apply()
    }

    private fun hashPassword(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }

    companion object {
        private const val PREFS_NAME = "selflock_secure_prefs"
        private const val KEY_ENABLED = "master_password_enabled"
        private const val KEY_HASH = "master_password_hash"
        private const val KEY_SALT = "master_password_salt"
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val ITERATIONS = 600000
        private const val KEY_LENGTH = 256
        private const val SALT_LENGTH = 32
    }
}
