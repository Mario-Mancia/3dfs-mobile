package com.dforge.studio.data.repository

import android.net.Uri
import com.dforge.studio.data.model.CategoriaServicio
import com.dforge.studio.data.model.Cotizacion
import com.dforge.studio.data.model.CrearEncargoRequest
import com.dforge.studio.data.model.Encargo
import com.dforge.studio.data.model.Material
import com.dforge.studio.data.model.NuevaConsultaRequest
import com.dforge.studio.data.model.NuevaResenaRequest
import com.dforge.studio.data.model.Notificacion
import com.dforge.studio.data.model.Perfil
import com.dforge.studio.data.model.Producto
import com.dforge.studio.data.model.Promocion
import com.dforge.studio.data.model.Servicio
import com.dforge.studio.data.model.SolicitudSoporte
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Archivo elegido por el usuario en el celular (STL, STEP, foto, PDF...). */
data class ArchivoSeleccionado(
    val uri: Uri,
    val nombre: String,
    val tamanoBytes: Long,
    val mimeType: String?
) {
    val extension: String get() = nombre.substringAfterLast('.', "").lowercase()
}

/**
 * Contrato único de datos de la app. Las pantallas solo conocen esta interfaz.
 * Hay dos implementaciones:
 *  - FakeDForgeRepository: datos de demostración en memoria (no necesita backend).
 *  - RemoteDForgeRepository: API de Django REST Framework.
 */
interface DForgeRepository {

    val sesionActiva: Flow<Boolean>

    /** Notificaciones sin leer (para el globo de la campana). */
    val noLeidas: StateFlow<Int>

    suspend fun login(correo: String, password: String): Result<Unit>
    suspend fun cerrarSesion()
    suspend fun perfil(): Result<Perfil>

    suspend fun categorias(): Result<List<CategoriaServicio>>
    suspend fun servicios(): Result<List<Servicio>>
    suspend fun productos(): Result<List<Producto>>
    suspend fun materiales(): Result<List<Material>>
    suspend fun promocionesActivas(): Result<List<Promocion>>

    suspend fun misEncargos(): Result<List<Encargo>>
    suspend fun encargo(id: Long): Result<Encargo>
    suspend fun crearEncargo(solicitud: CrearEncargoRequest, archivo: ArchivoSeleccionado?): Result<Encargo>
    suspend fun responderCotizacion(idCotizacion: Long, aceptar: Boolean): Result<Cotizacion>

    suspend fun notificaciones(): Result<List<Notificacion>>
    suspend fun marcarLeida(id: Long): Result<Unit>

    suspend fun enviarConsulta(consulta: NuevaConsultaRequest, adjunto: ArchivoSeleccionado?): Result<SolicitudSoporte>
    suspend fun enviarResena(resena: NuevaResenaRequest): Result<Unit>
}
