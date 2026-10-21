package com.intima.log.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

/**
 * 安全管理：
 * - 数据库口令：随机生成 32 字节，用 Keystore AES-GCM 密钥加密后存 EncryptedSharedPreferences；
 * - PIN：PBKDF2WithHmacSHA256（210000 轮）+ 随机盐；
 * - 导出文件：PIN 派生密钥 AES-256-GCM。
 */
object SecurityManager {

    private const val PREFS = "intima_secure"
    private const val KEY_DB_PASS = "db_passphrase_enc"

    // ---------- 数据库口令 ----------

    private fun securePrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context, PREFS, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /** 获取或首次生成数据库口令 */
    fun getOrCreateDbPassphrase(context: Context): ByteArray {
        val prefs = securePrefs(context)
        val stored = prefs.getString(KEY_DB_PASS, null)
        if (stored != null) {
            return Base64.decode(stored, Base64.NO_WRAP)
        }
        val fresh = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.edit().putString(KEY_DB_PASS, Base64.encodeToString(fresh, Base64.NO_WRAP)).apply()
        return fresh
    }

    // ---------- PIN ----------

    private const val PBKDF2_ROUNDS = 210_000

    data class PinHash(val hash: String, val salt: String)

    fun hashPin(pin: String, salt: String? = null): PinHash {
        val saltBytes = salt?.let { Base64.decode(it, Base64.NO_WRAP) }
            ?: ByteArray(16).also { SecureRandom().nextBytes(it) }
        val spec = PBEKeySpec(pin.toCharArray(), saltBytes, PBKDF2_ROUNDS, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return PinHash(Base64.encodeToString(hash, Base64.NO_WRAP), Base64.encodeToString(saltBytes, Base64.NO_WRAP))
    }

    fun verifyPin(pin: String, hash: String, salt: String): Boolean =
        hashPin(pin, salt).hash == hash

    // ---------- 导出加密（AES-256-GCM，密钥由 PIN 派生） ----------

    const val EXPORT_MAGIC = "INTM"        // .intima 文件头
    const val EXPORT_VERSION: Byte = 1

    private fun exportKey(pin: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ROUNDS, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    /** 加密明文 → [MAGIC(4) | ver(1) | salt(16) | iv(12) | ciphertext] */
    fun encryptExport(plain: ByteArray, pin: String): ByteArray {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val key = exportKey(pin, salt)
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val ct = cipher.doFinal(plain)
        return EXPORT_MAGIC.toByteArray(Charsets.US_ASCII) +
            byteArrayOf(EXPORT_VERSION) + salt + iv + ct
    }

    /** 解密 .intima 文件内容；密码错误抛 IllegalArgumentException */
    fun decryptExport(data: ByteArray, pin: String): ByteArray {
        require(data.size > 33 && data.copyOfRange(0, 4).toString(Charsets.US_ASCII) == EXPORT_MAGIC) {
            "不是有效的 .intima 文件"
        }
        require(data[4] == EXPORT_VERSION) { "不支持的文件版本" }
        val salt = data.copyOfRange(5, 21)
        val iv = data.copyOfRange(21, 33)
        val ct = data.copyOfRange(33, data.size)
        val key = exportKey(pin, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        // 密码错误时 GCM 校验失败抛 AEADBadTagException
        return cipher.doFinal(ct)
    }

    // ---------- Keystore 通用加解密（WebDAV 密码等） ----------

    private const val KS_ALIAS = "intima_generic_aes"
    private const val KS_TRANSFORMATION = "AES/GCM/NoPadding"

    private fun keystoreKey(): javax.crypto.SecretKey {
        val ks = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(KS_ALIAS, null) as? javax.crypto.SecretKey)?.let { return it }
        val generator = javax.crypto.KeyGenerator.getInstance("AES")
        generator.init(
            android.security.keystore.KeyGenParameterSpec.Builder(
                KS_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                    android.security.keystore.KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    /** Keystore AES-GCM 加密，返回 base64(iv + ct)。
     *  加密时不传 IV（Android Keystore 禁止 caller-provided IV），由 Keystore 生成后从 cipher 读回。 */
    fun encryptWithKeystore(plain: String): String {
        val cipher = Cipher.getInstance(KS_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        val iv = cipher.iv                            // Keystore 生成的随机 12 字节 IV
        require(iv != null && iv.size == 12) { "Keystore 未返回 IV" }
        val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + ct, Base64.NO_WRAP)
    }

    /** 解密 encryptWithKeystore 的输出；失败抛异常 */
    fun decryptWithKeystore(encoded: String): String {
        val data = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = data.copyOfRange(0, 12)
        val ct = data.copyOfRange(12, data.size)
        val cipher = Cipher.getInstance(KS_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(ct), Charsets.UTF_8)
    }
}
