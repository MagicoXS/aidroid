package com.aui.llm.data
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
class ApiKeyManager(context: Context) {
    private val prefs: SharedPreferences
    init {
        val mk = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        prefs = EncryptedSharedPreferences.create(context, "aui_keys_v2", mk,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    }
    fun save(p: AiProvider, k: String) { prefs.edit().putString(p.name, k).apply() }
    fun get(p: AiProvider): String? = prefs.getString(p.name, null)
    fun delete(p: AiProvider) { prefs.edit().remove(p.name).apply() }
    fun hasKey(p: AiProvider) = !get(p).isNullOrEmpty()
}
