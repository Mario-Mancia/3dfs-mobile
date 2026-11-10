package com.dforge.studio.data.remote

import com.dforge.studio.data.model.ArchivoEncargo
import com.dforge.studio.data.model.CategoriaServicio
import com.dforge.studio.data.model.Cotizacion
import com.dforge.studio.data.model.CrearEncargoRequest
import com.dforge.studio.data.model.Encargo
import com.dforge.studio.data.model.LoginRequest
import com.dforge.studio.data.model.Material
import com.dforge.studio.data.model.NuevaResenaRequest
import com.dforge.studio.data.model.Notificacion
import com.dforge.studio.data.model.Perfil
import com.dforge.studio.data.model.Producto
import com.dforge.studio.data.model.Promocion
import com.dforge.studio.data.model.Servicio
import com.dforge.studio.data.model.SolicitudSoporte
import com.dforge.studio.data.model.TokenResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path

/**
 * Endpoints que la app espera de la API de Django REST Framework.
 * La base es BuildConfig.API_BASE_URL (ej. http://10.0.2.2:8000/api/).
 * El README del proyecto lista estos endpoints para el equipo del backend.
 *
 * Las listas se esperan SIN paginación (o con la paginación desactivada para estas vistas).
 */
interface DForgeApi {

    // --- Autenticación (simplejwt con USERNAME_FIELD = "correo") ---
    @POST("auth/login/")
    suspend fun login(@Body body: LoginRequest): TokenResponse

    @GET("perfil/")
    suspend fun perfil(): Perfil

    // --- Catálogo ---
    @GET("categorias-servicios/")
    suspend fun categorias(): List<CategoriaServicio>

    @GET("servicios/")
    suspend fun servicios(): List<Servicio>

    @GET("productos/")
    suspend fun productos(): List<Producto>

    @GET("materiales/")
    suspend fun materiales(): List<Material>

    @GET("promociones/activas/")
    suspend fun promocionesActivas(): List<Promocion>

    // --- Encargos (solo los del cliente autenticado) ---
    @GET("encargos/")
    suspend fun encargos(): List<Encargo>

    @GET("encargos/{id}/")
    suspend fun encargo(@Path("id") id: Long): Encargo

    @POST("encargos/")
    suspend fun crearEncargo(@Body body: CrearEncargoRequest): Encargo

    /** Sube un archivo 3D al primer detalle del encargo. Campo del formulario: "archivo". */
    @Multipart
    @POST("encargos/{id}/archivos/")
    suspend fun subirArchivo(
        @Path("id") idEncargo: Long,
        @Part archivo: MultipartBody.Part
    ): ArchivoEncargo

    // --- Cotizaciones ---
    @POST("cotizaciones/{id}/aceptar/")
    suspend fun aceptarCotizacion(@Path("id") id: Long): Cotizacion

    @POST("cotizaciones/{id}/rechazar/")
    suspend fun rechazarCotizacion(@Path("id") id: Long): Cotizacion

    // --- Notificaciones ---
    @GET("notificaciones/")
    suspend fun notificaciones(): List<Notificacion>

    @POST("notificaciones/{id}/leer/")
    suspend fun marcarLeida(@Path("id") id: Long): Response<Unit>

    // --- Soporte y reseñas ---
    /** Campos: categoria, asunto, descripcion, medio_contacto_preferido y opcional "archivo_adjunto". */
    @Multipart
    @POST("soporte/")
    suspend fun crearConsulta(
        @PartMap campos: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part archivo: MultipartBody.Part?
    ): SolicitudSoporte

    @POST("resenas/")
    suspend fun crearResena(@Body body: NuevaResenaRequest): Response<Unit>
}
