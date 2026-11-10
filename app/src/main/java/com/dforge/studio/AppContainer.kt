package com.dforge.studio

import android.content.Context
import com.dforge.studio.data.local.SessionStore
import com.dforge.studio.data.remote.ApiClient
import com.dforge.studio.data.repository.DForgeRepository
import com.dforge.studio.data.repository.FakeDForgeRepository
import com.dforge.studio.data.repository.RemoteDForgeRepository

/**
 * Inyección de dependencias manual (sin Hilt, para mantener el proyecto simple).
 * Aquí se decide si la app usa datos de demostración o la API real de Django.
 */
class AppContainer(context: Context) {

    val sesion = SessionStore(context)

    val repository: DForgeRepository =
        if (BuildConfig.USAR_DATOS_DEMO) {
            FakeDForgeRepository(sesion)
        } else {
            RemoteDForgeRepository(
                context = context,
                api = ApiClient.crear(BuildConfig.API_BASE_URL, sesion),
                sesion = sesion
            )
        }
}
