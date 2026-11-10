package com.dforge.studio.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "sesion")

/**
 * Guarda los tokens JWT que entrega Django (djangorestframework-simplejwt).
 * Se guardan en DataStore para que la sesión se mantenga al cerrar la app.
 */
class SessionStore(private val context: Context) {

    private val claveAccess = stringPreferencesKey("access")
    private val claveRefresh = stringPreferencesKey("refresh")

    /** Copia en memoria para que el interceptor HTTP no tenga que leer disco en cada petición. */
    @Volatile
    var tokenEnMemoria: String? = null
        private set

    val token: Flow<String?> = context.dataStore.data.map { it[claveAccess] }

    val sesionActiva: Flow<Boolean> = token.map { !it.isNullOrBlank() }

    suspend fun cargar(): String? {
        tokenEnMemoria = token.first()
        return tokenEnMemoria
    }

    suspend fun guardar(access: String, refresh: String) {
        context.dataStore.edit {
            it[claveAccess] = access
            it[claveRefresh] = refresh
        }
        tokenEnMemoria = access
    }

    suspend fun cerrar() {
        context.dataStore.edit { it.clear() }
        tokenEnMemoria = null
    }
}
