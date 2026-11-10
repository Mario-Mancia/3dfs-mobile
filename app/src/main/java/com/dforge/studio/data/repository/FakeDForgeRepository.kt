package com.dforge.studio.data.repository

import com.dforge.studio.data.local.SessionStore
import com.dforge.studio.data.model.ArchivoEncargo
import com.dforge.studio.data.model.CategoriaServicio
import com.dforge.studio.data.model.Cotizacion
import com.dforge.studio.data.model.CrearEncargoRequest
import com.dforge.studio.data.model.Encargo
import com.dforge.studio.data.model.EncargoDetalle
import com.dforge.studio.data.model.EstadoEncargo
import com.dforge.studio.data.model.HistorialEncargo
import com.dforge.studio.data.model.Material
import com.dforge.studio.data.model.NuevaConsultaRequest
import com.dforge.studio.data.model.NuevaResenaRequest
import com.dforge.studio.data.model.Notificacion
import com.dforge.studio.data.model.Perfil
import com.dforge.studio.data.model.Producto
import com.dforge.studio.data.model.Promocion
import com.dforge.studio.data.model.Servicio
import com.dforge.studio.data.model.SolicitudSoporte
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

/**
 * Datos de demostración en memoria. Permite probar toda la app sin el backend.
 * Se activa con USAR_DATOS_DEMO = true en app/build.gradle.kts.
 * Cualquier correo y contraseña (de 4 caracteres o más) sirven para entrar.
 */
class FakeDForgeRepository(private val sesion: SessionStore) : DForgeRepository {

    private val mutex = Mutex()
    private val _noLeidas = MutableStateFlow(0)

    override val sesionActiva: Flow<Boolean> = sesion.sesionActiva
    override val noLeidas: StateFlow<Int> = _noLeidas.asStateFlow()

    // ---------------- Catálogo ----------------

    private val categorias = listOf(
        CategoriaServicio(1, "Impresión 3D"),
        CategoriaServicio(2, "Diseño 3D"),
        CategoriaServicio(3, "Asesorías"),
        CategoriaServicio(4, "Prototipado")
    )

    private val servicios = listOf(
        Servicio(1, 1, "Impresión 3D", "Impresión 3D de tu archivo",
            "Imprimimos tu modelo STL, OBJ, 3MF o STEP en PLA, PETG, ABS o TPU. El precio se calcula según el peso y el tiempo de impresión.",
            dinero("3.00"), requiereArchivo = true),
        Servicio(2, 2, "Diseño 3D", "Repuesto a la medida",
            "Diseñamos e imprimimos la pieza que necesitas a partir de medidas o fotos: perillas, soportes, tapas, clips.",
            dinero("10.00")),
        Servicio(3, 2, "Diseño 3D", "Diseño 3D personalizado",
            "Convertimos tu idea o boceto en un modelo 3D listo para imprimir.",
            dinero("5.00")),
        Servicio(4, 3, "Asesorías", "Asesoría técnica",
            "Te ayudamos a elegir el material, el relleno y la orientación adecuados para tu pieza.",
            BigDecimal.ZERO),
        Servicio(5, 4, "Prototipado", "Maqueta o prototipo",
            "Maquetas de arquitectura, carcasas para Arduino o ESP32 y prototipos para proyectos universitarios.",
            dinero("15.00"))
    )

    private val materiales = listOf(
        Material(1, "PLA", "FDM", "Fácil de imprimir, buen acabado. Ideal para decoración y prototipos.", dinero("0.03"), dinero("1.24")),
        Material(2, "PETG", "FDM", "Resistente al impacto, al agua y al sol. Para piezas funcionales.", dinero("0.035"), dinero("1.27")),
        Material(3, "ABS", "FDM", "Resiste el calor; se puede lijar. Piezas de carro y carcasas.", dinero("0.035"), dinero("1.04")),
        Material(4, "TPU", "FDM", "Flexible como hule. Fundas, empaques y topes.", dinero("0.06"), dinero("1.21"))
    )

    private val productos = listOf(
        Producto(1, "Llavero con nombre", "Llavero personalizado con el nombre que quieras, en dos colores.", 1, "PLA", dinero("3.00"), 25),
        Producto(2, "Organizador de cables", "Mantiene en orden 3 cables sobre el escritorio.", 1, "PLA", dinero("3.50"), 12),
        Producto(3, "Porta tarjetas", "Para tarjetas de presentación de 9 × 5 cm.", 1, "PLA", dinero("6.00"), 6),
        Producto(4, "Caja para ESP32 con tapa", "Caja con ventilación y hueco para el cable USB.", 2, "PETG", dinero("7.00"), 4),
        Producto(5, "Maceta con plato", "Maceta de 8 cm con drenaje y su plato.", 2, "PETG", dinero("9.00"), 8),
        Producto(6, "Portalápices hexagonal", "Portalápices de escritorio con acabado seda.", 1, "PLA", dinero("8.00"), 5),
        Producto(7, "Soporte de celular", "Soporte firme con ranura para el cable de carga.", 1, "PLA", dinero("8.00"), 10),
        Producto(8, "Lámpara litofanía con foto", "Tu foto en relieve que se ilumina con luz LED.", 1, "PLA", dinero("20.00"), 0)
    )

    private val promociones = listOf(
        Promocion(1, "Temporada navideña", "10 % de descuento en productos del catálogo.", "PORCENTAJE",
            dinero("10"), "2026-11-01T00:00:00-06:00", "2026-12-24T23:59:00-06:00")
    )

    private val perfil = Perfil(1, "Cliente", "Demo", "demo@dforge.studio", "7000-0000", "PERSONA")

    // ---------------- Encargos y notificaciones de ejemplo ----------------

    private val encargos = mutableListOf(
        Encargo(
            id = 12, codigo = "ENC-2026-0012", idCliente = 1,
            titulo = "Soporte para monitor",
            descripcion = "Repuesto del soporte trasero de mi monitor; se quebró una pestaña.",
            estado = "COTIZADO", fechaSolicitud = hace(dias = 2),
            fechaEntregaEstimada = enDias(5),
            subtotal = dinero("12.00"), impuesto = dinero("1.56"), total = dinero("13.56"),
            detalles = listOf(
                EncargoDetalle(
                    id = 21, idServicio = 2, servicio = "Repuesto a la medida",
                    idMaterial = 2, material = "PETG", descripcion = "Soporte trasero para monitor",
                    cantidad = 1, precioUnitario = dinero("12.00"), subtotal = dinero("12.00"),
                    especificaciones = mapOf("Relleno" to "50 %", "Acabado" to "Estándar", "Color" to "Negro"),
                    archivos = listOf(
                        ArchivoEncargo(31, "soporte_monitor.stl", "STL", 845_000, hace(dias = 2),
                            volumenCm3 = dinero("38.5"), dimX = dinero("82"), dimY = dinero("40"), dimZ = dinero("25"),
                            gramosEstimados = dinero("49"), horasEstimadas = dinero("3.2"),
                            esValido = true, mensajeValidacion = "Archivo válido. Cabe en la impresora.")
                    )
                )
            ),
            cotizaciones = listOf(
                Cotizacion(41, 12, "COT-2026-0012", hace(dias = 1), enDias(2),
                    dinero("12.00"), BigDecimal.ZERO, dinero("1.56"), dinero("13.56"),
                    estado = "ENVIADA", generadaAutomaticamente = true,
                    observaciones = "49 g de PETG y 3.2 h de impresión, más el diseño del repuesto.")
            ),
            historial = listOf(
                HistorialEncargo(1, null, "PENDIENTE", "Solicitud recibida desde la app.", hace(dias = 2)),
                HistorialEncargo(2, "PENDIENTE", "EN_REVISION", "Archivo analizado automáticamente.", hace(dias = 2)),
                HistorialEncargo(3, "EN_REVISION", "COTIZADO", "Cotización generada automáticamente.", hace(dias = 1))
            )
        ),
        Encargo(
            id = 9, codigo = "ENC-2026-0009", idCliente = 1,
            titulo = "Cajas para ESP32",
            descripcion = "Dos cajas para el proyecto de sensores de la universidad.",
            estado = "EN_PRODUCCION", fechaSolicitud = hace(dias = 6),
            fechaEntregaEstimada = enDias(1),
            subtotal = dinero("14.00"), impuesto = dinero("1.82"), total = dinero("15.82"),
            detalles = listOf(
                EncargoDetalle(22, idProducto = 4, producto = "Caja para ESP32 con tapa", idMaterial = 1, material = "PLA",
                    descripcion = "Caja para ESP32 con tapa", cantidad = 2,
                    precioUnitario = dinero("7.00"), subtotal = dinero("14.00"),
                    especificaciones = mapOf("Color" to "Gris"))
            ),
            cotizaciones = listOf(
                Cotizacion(42, 9, "COT-2026-0009", hace(dias = 6), hace(dias = 3),
                    dinero("14.00"), BigDecimal.ZERO, dinero("1.82"), dinero("15.82"),
                    estado = "ACEPTADA", generadaAutomaticamente = true)
            ),
            historial = listOf(
                HistorialEncargo(4, null, "PENDIENTE", "Pedido del catálogo.", hace(dias = 6)),
                HistorialEncargo(5, "PENDIENTE", "COTIZADO", "Cotización generada automáticamente.", hace(dias = 6)),
                HistorialEncargo(6, "COTIZADO", "ESPERANDO_PAGO", "Aceptaste la cotización.", hace(dias = 5)),
                HistorialEncargo(7, "ESPERANDO_PAGO", "EN_PRODUCCION", "Anticipo confirmado. Tu pedido entró a la cola de impresión.", hace(dias = 4))
            )
        ),
        Encargo(
            id = 4, codigo = "ENC-2026-0004", idCliente = 1,
            titulo = "Llaveros para graduación",
            descripcion = "25 llaveros con el nombre de cada compañero.",
            estado = "ENTREGADO", fechaSolicitud = hace(dias = 20),
            fechaEntregaEstimada = hace(dias = 12), fechaEntregaReal = hace(dias = 12),
            subtotal = dinero("75.00"), descuento = dinero("7.50"), impuesto = dinero("8.78"), total = dinero("76.28"),
            detalles = listOf(
                EncargoDetalle(23, idProducto = 1, producto = "Llavero con nombre", idMaterial = 1, material = "PLA",
                    descripcion = "Llavero con nombre", cantidad = 25,
                    precioUnitario = dinero("3.00"), subtotal = dinero("75.00"),
                    especificaciones = mapOf("Colores" to "Azul y blanco"))
            ),
            cotizaciones = listOf(
                Cotizacion(43, 4, "COT-2026-0004", hace(dias = 20), hace(dias = 17),
                    dinero("75.00"), dinero("7.50"), dinero("8.78"), dinero("76.28"),
                    estado = "ACEPTADA", generadaAutomaticamente = true, idPromocion = 1, promocion = "Temporada navideña")
            ),
            historial = listOf(
                HistorialEncargo(8, null, "PENDIENTE", null, hace(dias = 20)),
                HistorialEncargo(9, "PENDIENTE", "EN_PRODUCCION", null, hace(dias = 18)),
                HistorialEncargo(10, "EN_PRODUCCION", "FINALIZADO", "Tu pedido está listo.", hace(dias = 13)),
                HistorialEncargo(11, "FINALIZADO", "ENTREGADO", null, hace(dias = 12))
            )
        ),
        Encargo(
            id = 15, codigo = "ENC-2026-0015", idCliente = 1,
            titulo = "Figura decorativa",
            descripcion = "Figura para escritorio; me falta subir el archivo.",
            estado = "BORRADOR", fechaSolicitud = hace(dias = 1),
            detalles = listOf(EncargoDetalle(24, idServicio = 1, servicio = "Impresión 3D de tu archivo",
                idMaterial = 1, material = "PLA", descripcion = "Figura decorativa", cantidad = 1))
        )
    )

    private val notificaciones = mutableListOf(
        Notificacion(1, "Tu cotización está lista",
            "ENC-2026-0012: total de \$13.56. Revísala y acéptala para pasar a producción.", "COTIZACION", 12, false, hace(dias = 1)),
        Notificacion(2, "Tu pedido entró a producción",
            "ENC-2026-0009 ya está en la cola de impresión.", "ENCARGO", 9, false, hace(dias = 4)),
        Notificacion(3, "Promoción de temporada",
            "10 % de descuento en productos del catálogo hasta el 24 de diciembre.", "PROMOCION", null, true, hace(dias = 8)),
        Notificacion(4, "Pedido entregado",
            "ENC-2026-0004 fue entregado. ¡Gracias por tu compra!", "ENCARGO", 4, true, hace(dias = 12))
    )

    private var siguienteId = 100L

    init {
        _noLeidas.value = notificaciones.count { !it.leida }
    }

    // ---------------- Sesión ----------------

    override suspend fun login(correo: String, password: String): Result<Unit> {
        delay(700)
        if (!correo.contains("@") || password.length < 4) {
            return Result.failure(Exception("Escribe un correo válido y una contraseña de al menos 4 caracteres."))
        }
        sesion.guardar(access = "token-demo", refresh = "refresh-demo")
        return Result.success(Unit)
    }

    override suspend fun cerrarSesion() = sesion.cerrar()

    override suspend fun perfil(): Result<Perfil> = ok(perfil)

    // ---------------- Catálogo ----------------

    override suspend fun categorias() = ok(categorias)
    override suspend fun servicios() = ok(servicios)
    override suspend fun productos() = ok(productos)
    override suspend fun materiales() = ok(materiales)
    override suspend fun promocionesActivas() = ok(promociones)

    // ---------------- Encargos ----------------

    override suspend fun misEncargos(): Result<List<Encargo>> =
        ok(mutex.withLock { encargos.sortedByDescending { it.fechaSolicitud } })

    override suspend fun encargo(id: Long): Result<Encargo> {
        val encontrado = mutex.withLock { encargos.firstOrNull { it.id == id } }
        return if (encontrado != null) ok(encontrado)
        else Result.failure(Exception("No se encontró el encargo."))
    }

    override suspend fun crearEncargo(
        solicitud: CrearEncargoRequest,
        archivo: ArchivoSeleccionado?
    ): Result<Encargo> {
        delay(900)
        val nuevo = mutex.withLock {
            val id = siguienteId++
            val codigo = "ENC-2026-%04d".format(id)
            val servicio = servicios.firstOrNull { it.id == solicitud.idServicio }
            val producto = productos.firstOrNull { it.id == solicitud.idProducto }
            val material = materiales.firstOrNull { it.id == solicitud.idMaterial }
            val estado = if (solicitud.esBorrador) EstadoEncargo.BORRADOR else EstadoEncargo.PENDIENTE
            val precioUnitario = producto?.precio ?: BigDecimal.ZERO
            val subtotal = precioUnitario.multiply(BigDecimal(solicitud.cantidad))

            val archivos = archivo?.let {
                listOf(
                    ArchivoEncargo(
                        id = siguienteId++, nombreArchivo = it.nombre,
                        formato = it.extension.uppercase(), tamanoBytes = it.tamanoBytes,
                        fechaSubida = ahora(), esValido = null,
                        mensajeValidacion = "Archivo recibido. El sistema lo está analizando para cotizar."
                    )
                )
            } ?: emptyList()

            val encargo = Encargo(
                id = id, codigo = codigo, idCliente = perfil.id,
                titulo = solicitud.titulo, descripcion = solicitud.descripcion,
                estado = estado.name, fechaSolicitud = ahora(),
                fechaEntregaEstimada = solicitud.fechaEntregaDeseada,
                subtotal = subtotal, total = subtotal,
                detalles = listOf(
                    EncargoDetalle(
                        id = siguienteId++, idServicio = servicio?.id, servicio = servicio?.nombre,
                        idProducto = producto?.id, producto = producto?.nombre,
                        idMaterial = material?.id, material = material?.nombre ?: producto?.material,
                        descripcion = producto?.nombre ?: servicio?.nombre ?: solicitud.titulo,
                        cantidad = solicitud.cantidad, precioUnitario = precioUnitario, subtotal = subtotal,
                        especificaciones = solicitud.especificaciones, archivos = archivos
                    )
                ),
                historial = listOf(
                    HistorialEncargo(siguienteId++, null, estado.name,
                        if (solicitud.esBorrador) "Borrador guardado." else "Solicitud recibida desde la app.", ahora())
                )
            )
            encargos.add(encargo)
            if (!solicitud.esBorrador) {
                notificaciones.add(0, Notificacion(siguienteId++, "Recibimos tu encargo",
                    "$codigo está en revisión. Te avisaremos cuando la cotización esté lista.",
                    "ENCARGO", id, false, ahora()))
                _noLeidas.value = notificaciones.count { !it.leida }
            }
            encargo
        }
        return Result.success(nuevo)
    }

    override suspend fun responderCotizacion(idCotizacion: Long, aceptar: Boolean): Result<Cotizacion> {
        delay(600)
        return mutex.withLock {
            val indice = encargos.indexOfFirst { e -> e.cotizaciones.any { it.id == idCotizacion } }
            if (indice < 0) return@withLock Result.failure(Exception("No se encontró la cotización."))
            val encargo = encargos[indice]
            val cotizacion = encargo.cotizaciones.first { it.id == idCotizacion }
                .copy(estado = if (aceptar) "ACEPTADA" else "RECHAZADA")
            val nuevoEstado = if (aceptar) EstadoEncargo.ESPERANDO_PAGO else EstadoEncargo.EN_REVISION
            val comentario = if (aceptar)
                "Aceptaste la cotización. Paga el anticipo del 50 % para iniciar la producción."
            else "Rechazaste la cotización. Un integrante del equipo te contactará."
            encargos[indice] = encargo.copy(
                estado = nuevoEstado.name,
                cotizaciones = encargo.cotizaciones.map { if (it.id == idCotizacion) cotizacion else it },
                historial = encargo.historial + HistorialEncargo(siguienteId++, encargo.estado, nuevoEstado.name, comentario, ahora())
            )
            Result.success(cotizacion)
        }
    }

    // ---------------- Notificaciones ----------------

    override suspend fun notificaciones(): Result<List<Notificacion>> =
        ok(mutex.withLock { notificaciones.toList() })

    override suspend fun marcarLeida(id: Long): Result<Unit> {
        mutex.withLock {
            val i = notificaciones.indexOfFirst { it.id == id }
            if (i >= 0) notificaciones[i] = notificaciones[i].copy(leida = true)
            _noLeidas.value = notificaciones.count { !it.leida }
        }
        return Result.success(Unit)
    }

    // ---------------- Soporte y reseñas ----------------

    override suspend fun enviarConsulta(
        consulta: NuevaConsultaRequest,
        adjunto: ArchivoSeleccionado?
    ): Result<SolicitudSoporte> {
        delay(800)
        return Result.success(
            SolicitudSoporte(
                id = siguienteId++, asunto = consulta.asunto, descripcion = consulta.descripcion,
                categoria = consulta.categoria.name,
                medioContactoPreferido = consulta.medioContactoPreferido.name,
                archivoAdjunto = adjunto?.nombre, creadoEn = ahora()
            )
        )
    }

    override suspend fun enviarResena(resena: NuevaResenaRequest): Result<Unit> {
        delay(600)
        mutex.withLock {
            val i = encargos.indexOfFirst { it.id == resena.idEncargo }
            if (i >= 0) encargos[i] = encargos[i].copy(tieneResena = true)
        }
        return Result.success(Unit)
    }

    // ---------------- Utilidades ----------------

    private suspend fun <T> ok(valor: T): Result<T> {
        delay(400) // simula la latencia de la red
        return Result.success(valor)
    }

    private companion object {
        fun dinero(valor: String): BigDecimal = BigDecimal(valor).setScale(2, RoundingMode.HALF_UP)
        fun ahora(): String = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString()
        fun hace(dias: Long): String = OffsetDateTime.now().minusDays(dias).truncatedTo(ChronoUnit.SECONDS).toString()
        fun enDias(dias: Long): String = LocalDate.now().plusDays(dias).toString()
    }
}
