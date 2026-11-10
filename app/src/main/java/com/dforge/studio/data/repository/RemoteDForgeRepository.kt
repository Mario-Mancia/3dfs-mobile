package com.dforge.studio.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.dforge.studio.data.local.SessionStore
import com.dforge.studio.data.model.CategoriaServicio
import com.dforge.studio.data.model.Cotizacion
import com.dforge.studio.data.model.CrearEncargoRequest
import com.dforge.studio.data.model.Encargo
import com.dforge.studio.data.model.LoginRequest
import com.dforge.studio.data.model.Material
import com.dforge.studio.data.model.NuevaConsultaRequest
import com.dforge.studio.data.model.NuevaResenaRequest
import com.dforge.studio.data.model.Notificacion
import com.dforge.studio.data.model.Perfil
import com.dforge.studio.data.model.Producto
import com.dforge.studio.data.model.Promocion
import com.dforge.studio.data.model.Servicio
import com.dforge.studio.data.model.SolicitudSoporte
import com.dforge.studio.data.remote.DForgeApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.source
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

/** Implementación real: habla con la API de Django REST Framework. */
class RemoteDForgeRepository(
    context: Context,
    private val api: DForgeApi,
    private val sesion: SessionStore
) : DForgeRepository {

    private val resolver: ContentResolver = context.contentResolver
    private val _noLeidas = MutableStateFlow(0)

    override val sesionActiva: Flow<Boolean> = sesion.sesionActiva
    override val noLeidas: StateFlow<Int> = _noLeidas.asStateFlow()

    override suspend fun login(correo: String, password: String): Result<Unit> = llamar {
        val tokens = api.login(LoginRequest(correo.trim(), password))
        sesion.guardar(tokens.access, tokens.refresh)
    }

    override suspend fun cerrarSesion() {
        sesion.cerrar()
        _noLeidas.value = 0
    }

    override suspend fun perfil(): Result<Perfil> = llamar { api.perfil() }
    override suspend fun categorias(): Result<List<CategoriaServicio>> = llamar { api.categorias() }
    override suspend fun servicios(): Result<List<Servicio>> = llamar { api.servicios() }
    override suspend fun productos(): Result<List<Producto>> = llamar { api.productos() }
    override suspend fun materiales(): Result<List<Material>> = llamar { api.materiales() }
    override suspend fun promocionesActivas(): Result<List<Promocion>> = llamar { api.promocionesActivas() }

    override suspend fun misEncargos(): Result<List<Encargo>> =
        llamar { api.encargos().sortedByDescending { it.fechaSolicitud } }

    override suspend fun encargo(id: Long): Result<Encargo> = llamar { api.encargo(id) }

    override suspend fun crearEncargo(
        solicitud: CrearEncargoRequest,
        archivo: ArchivoSeleccionado?
    ): Result<Encargo> = llamar {
        // 1) Se crea el encargo (Django lo deja en BORRADOR o PENDIENTE).
        val creado = api.crearEncargo(solicitud)
        // 2) Si hay archivo, se sube. Django lo analiza en segundo plano (volumen, gramos, validez).
        if (archivo != null) {
            api.subirArchivo(creado.id, parteArchivo("archivo", archivo))
            api.encargo(creado.id)  // se devuelve el encargo actualizado con su archivo
        } else {
            creado
        }
    }

    override suspend fun responderCotizacion(idCotizacion: Long, aceptar: Boolean): Result<Cotizacion> =
        llamar {
            if (aceptar) api.aceptarCotizacion(idCotizacion) else api.rechazarCotizacion(idCotizacion)
        }

    override suspend fun notificaciones(): Result<List<Notificacion>> = llamar {
        api.notificaciones().sortedByDescending { it.fechaCreacion }.also { lista ->
            _noLeidas.value = lista.count { !it.leida }
        }
    }

    override suspend fun marcarLeida(id: Long): Result<Unit> = llamar {
        val respuesta = api.marcarLeida(id)
        if (!respuesta.isSuccessful) throw HttpException(respuesta)
        _noLeidas.value = (_noLeidas.value - 1).coerceAtLeast(0)
    }

    override suspend fun enviarConsulta(
        consulta: NuevaConsultaRequest,
        adjunto: ArchivoSeleccionado?
    ): Result<SolicitudSoporte> = llamar {
        val texto = "text/plain".toMediaTypeOrNull()
        val campos = mapOf(
            "categoria" to consulta.categoria.name.toRequestBody(texto),
            "asunto" to consulta.asunto.toRequestBody(texto),
            "descripcion" to consulta.descripcion.toRequestBody(texto),
            "medio_contacto_preferido" to consulta.medioContactoPreferido.name.toRequestBody(texto)
        )
        api.crearConsulta(campos, adjunto?.let { parteArchivo("archivo_adjunto", it) })
    }

    override suspend fun enviarResena(resena: NuevaResenaRequest): Result<Unit> = llamar {
        val respuesta = api.crearResena(resena)
        if (!respuesta.isSuccessful) throw HttpException(respuesta)
    }

    // ---------------------------------------------------------------------

    private fun parteArchivo(campo: String, archivo: ArchivoSeleccionado): MultipartBody.Part {
        val cuerpo = UriRequestBody(resolver, archivo.uri, archivo.mimeType, archivo.tamanoBytes)
        return MultipartBody.Part.createFormData(campo, archivo.nombre, cuerpo)
    }

    /** Ejecuta una llamada y traduce los errores a mensajes entendibles para el usuario. */
    private suspend fun <T> llamar(bloque: suspend () -> T): Result<T> = try {
        Result.success(bloque())
    } catch (e: HttpException) {
        if (e.code() == 401) sesion.cerrar()
        Result.failure(Exception(mensajeDeError(e)))
    } catch (e: IOException) {
        Result.failure(Exception("No hay conexión con el servidor. Revisa tu internet e inténtalo de nuevo."))
    } catch (e: Exception) {
        Result.failure(Exception(e.message ?: "Ocurrió un error inesperado."))
    }

    private fun mensajeDeError(e: HttpException): String {
        if (e.code() == 401) return "Tu sesión expiró o los datos de acceso no son correctos."
        if (e.code() >= 500) return "El servidor tuvo un problema. Inténtalo más tarde."
        val cuerpo = e.response()?.errorBody()?.string().orEmpty()
        return try {
            // DRF responde {"detail": "..."} o {"campo": ["error"]}
            val json = JSONObject(cuerpo)
            json.optString("detail").takeIf { it.isNotBlank() } ?: run {
                val clave = json.keys().asSequence().firstOrNull()
                when (val valor = clave?.let { json.get(it) }) {
                    is JSONArray -> valor.optString(0)
                    null -> null
                    else -> valor.toString()
                }
            } ?: "No se pudo completar la operación (${e.code()})."
        } catch (_: Exception) {
            "No se pudo completar la operación (${e.code()})."
        }
    }
}

/** Envía el contenido de un archivo del celular sin cargarlo completo en memoria. */
private class UriRequestBody(
    private val resolver: ContentResolver,
    private val uri: Uri,
    private val mimeType: String?,
    private val tamano: Long
) : RequestBody() {
    override fun contentType(): MediaType? =
        (mimeType ?: "application/octet-stream").toMediaTypeOrNull()

    override fun contentLength(): Long = if (tamano > 0) tamano else -1

    override fun writeTo(sink: BufferedSink) {
        val entrada = resolver.openInputStream(uri)
            ?: throw IOException("No se pudo leer el archivo seleccionado.")
        entrada.source().use { sink.writeAll(it) }
    }
}
