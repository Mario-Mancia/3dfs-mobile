package com.dforge.studio.data.model

import java.math.BigDecimal

/*
 * Modelos de la app, alineados con la base de datos de DForge Studio.
 *
 * - Gson convierte automáticamente camelCase <-> snake_case (ver ApiClient),
 *   así que "idCliente" aquí corresponde a "id_cliente" en Django.
 * - Todos los campos tienen valor por defecto para que Gson no deje nulos
 *   inesperados cuando la API omite un campo.
 * - Los importes son BigDecimal (Django REST Framework envía los NUMERIC como texto "12.50").
 * - Las fechas llegan como texto ISO 8601 (TIMESTAMPTZ) y se formatean en util/Formato.kt.
 */

// ---------- Autenticación y perfil ----------

data class LoginRequest(
    val correo: String = "",
    val password: String = ""
)

data class TokenResponse(
    val access: String = "",
    val refresh: String = ""
)

data class Perfil(
    val id: Long = 0,
    val nombres: String = "",
    val apellidos: String = "",
    val correo: String = "",
    val telefono: String? = null,
    val tipoCliente: String = "PERSONA",     // PERSONA | EMPRESA
    val nombreEmpresa: String? = null,
    val direccion: String? = null
) {
    val nombreCompleto: String get() = "$nombres $apellidos".trim()
}

// ---------- Catálogo ----------

data class CategoriaServicio(
    val id: Long = 0,
    val nombre: String = "",
    val descripcion: String? = null
)

data class Servicio(
    val id: Long = 0,
    val idCategoria: Long = 0,
    val categoria: String = "",              // nombre de la categoría (lo envía el serializer)
    val nombre: String = "",
    val descripcion: String = "",
    val precioBase: BigDecimal = BigDecimal.ZERO,
    val requiereArchivo: Boolean = false,
    val activo: Boolean = true
)

/** Nueva tabla propuesta: materiales (antes iba escondido en el JSONB de especificaciones). */
data class Material(
    val id: Long = 0,
    val nombre: String = "",                 // PLA, PETG, ABS, TPU
    val tecnologia: String = "FDM",
    val descripcion: String = "",
    val precioPorGramo: BigDecimal = BigDecimal.ZERO,
    val densidadGCm3: BigDecimal = BigDecimal.ZERO,
    val activo: Boolean = true
)

/** Nueva tabla propuesta: productos del catálogo (línea de productos del plan de negocios). */
data class Producto(
    val id: Long = 0,
    val nombre: String = "",
    val descripcion: String = "",
    val idMaterial: Long? = null,
    val material: String = "",
    val precio: BigDecimal = BigDecimal.ZERO,
    val stockTerminado: Int = 0,
    val imagenUrl: String? = null,
    val activo: Boolean = true
)

data class Promocion(
    val id: Long = 0,
    val nombre: String = "",
    val descripcion: String? = null,
    val tipoDescuento: String = "PORCENTAJE", // PORCENTAJE | MONTO_FIJO
    val valorDescuento: BigDecimal = BigDecimal.ZERO,
    val fechaInicio: String = "",
    val fechaFin: String = ""
)

// ---------- Encargos ----------

/**
 * Estados del encargo. Incluye los que propusimos agregar:
 * BORRADOR, COTIZADO y ESPERANDO_PAGO.
 */
enum class EstadoEncargo(val etiqueta: String) {
    BORRADOR("Borrador"),
    PENDIENTE("Pendiente"),
    EN_REVISION("En revisión"),
    COTIZADO("Cotizado"),
    ESPERANDO_PAGO("Esperando pago"),
    APROBADO("Aprobado"),
    EN_PRODUCCION("En producción"),
    EN_ESPERA("En espera"),
    FINALIZADO("Listo para entregar"),
    ENTREGADO("Entregado"),
    CANCELADO("Cancelado");

    companion object {
        fun de(valor: String?): EstadoEncargo? = entries.firstOrNull { it.name == valor }

        /** Estados que se consideran "activos" para el cliente. */
        val activos = setOf(PENDIENTE, EN_REVISION, COTIZADO, ESPERANDO_PAGO, APROBADO, EN_PRODUCCION, EN_ESPERA, FINALIZADO)

        /** Línea de avance que se muestra en el detalle del pedido. */
        val lineaDeAvance = listOf(PENDIENTE, EN_REVISION, COTIZADO, ESPERANDO_PAGO, EN_PRODUCCION, FINALIZADO, ENTREGADO)
    }
}

data class Encargo(
    val id: Long = 0,
    val codigo: String = "",
    val idCliente: Long = 0,
    val titulo: String = "",
    val descripcion: String = "",
    val estado: String = "PENDIENTE",
    val origen: String = "APP",              // WEB | APP | PANEL  (campo propuesto)
    val prioridad: String = "MEDIA",          // campo propuesto
    val fechaSolicitud: String = "",
    val fechaEntregaEstimada: String? = null,
    val fechaEntregaReal: String? = null,
    val subtotal: BigDecimal = BigDecimal.ZERO,
    val descuento: BigDecimal = BigDecimal.ZERO,
    val impuesto: BigDecimal = BigDecimal.ZERO,
    val total: BigDecimal = BigDecimal.ZERO,
    val observaciones: String? = null,
    val tieneResena: Boolean = false,
    val detalles: List<EncargoDetalle> = emptyList(),
    val cotizaciones: List<Cotizacion> = emptyList(),
    val historial: List<HistorialEncargo> = emptyList()
) {
    val estadoEnum: EstadoEncargo? get() = EstadoEncargo.de(estado)

    /** Cotización que espera respuesta del cliente (si hay). */
    val cotizacionPendiente: Cotizacion? get() = cotizaciones.firstOrNull { it.estado == "ENVIADA" }
}

data class EncargoDetalle(
    val id: Long = 0,
    val idServicio: Long? = null,
    val servicio: String? = null,
    val idProducto: Long? = null,            // campo propuesto (productos del catálogo)
    val producto: String? = null,
    val idMaterial: Long? = null,            // campo propuesto (material relacional)
    val material: String? = null,
    val descripcion: String = "",
    val cantidad: Int = 1,                   // ajuste: entero, no NUMERIC(10,2)
    val precioUnitario: BigDecimal = BigDecimal.ZERO,
    val subtotal: BigDecimal = BigDecimal.ZERO,
    val especificaciones: Map<String, String> = emptyMap(), // relleno, acabado, color...
    val archivos: List<ArchivoEncargo> = emptyList()
)

data class ArchivoEncargo(
    val id: Long = 0,
    val nombreArchivo: String = "",
    val formato: String = "",
    val tamanoBytes: Long = 0,
    val fechaSubida: String = "",
    // Campos propuestos para la validación y cotización automática:
    val volumenCm3: BigDecimal? = null,
    val dimX: BigDecimal? = null,
    val dimY: BigDecimal? = null,
    val dimZ: BigDecimal? = null,
    val gramosEstimados: BigDecimal? = null,
    val horasEstimadas: BigDecimal? = null,
    val esValido: Boolean? = null,           // null = todavía en análisis
    val mensajeValidacion: String? = null
)

data class Cotizacion(
    val id: Long = 0,
    val idEncargo: Long = 0,                 // ajuste: la cotización apunta al encargo
    val codigo: String = "",
    val fechaEmision: String = "",
    val fechaVencimiento: String? = null,
    val subtotal: BigDecimal = BigDecimal.ZERO,
    val descuento: BigDecimal = BigDecimal.ZERO,
    val impuesto: BigDecimal = BigDecimal.ZERO,
    val total: BigDecimal = BigDecimal.ZERO,
    val estado: String = "ENVIADA",          // BORRADOR | ENVIADA | ACEPTADA | RECHAZADA | VENCIDA | CANCELADA
    val generadaAutomaticamente: Boolean = false, // campo propuesto
    val idPromocion: Long? = null,           // ajuste: promoción aplicada
    val promocion: String? = null,
    val observaciones: String? = null
)

data class HistorialEncargo(
    val id: Long = 0,
    val estadoAnterior: String? = null,
    val estadoNuevo: String = "",
    val comentario: String? = null,
    val fechaCambio: String = ""
)

/** Lo que la app envía para crear un encargo. Django decide el estado (BORRADOR o PENDIENTE). */
data class CrearEncargoRequest(
    val titulo: String = "",
    val descripcion: String = "",
    val idServicio: Long? = null,
    val idProducto: Long? = null,
    val idMaterial: Long? = null,
    val cantidad: Int = 1,
    val especificaciones: Map<String, String> = emptyMap(),
    val fechaEntregaDeseada: String? = null, // yyyy-MM-dd
    val esBorrador: Boolean = false,
    val origen: String = "APP"
)

// ---------- Notificaciones ----------

data class Notificacion(
    val id: Long = 0,
    val titulo: String = "",
    val mensaje: String = "",
    val tipo: String = "",                   // ENCARGO | COTIZACION | PAGO | SOPORTE | PROMOCION
    val referenciaId: Long? = null,
    val leida: Boolean = false,
    val fechaCreacion: String = ""
)

// ---------- Soporte (asesoría) y reseñas ----------

/** Categorías de consulta (ajuste: campo "categoria" en solicitudes_soporte). */
enum class CategoriaConsulta(val etiqueta: String) {
    MATERIALES("Materiales"),
    MEDIDAS("Medidas y ajuste"),
    DISENO("Diseño 3D"),
    PEDIDO("Mi pedido"),
    OTRO("Otro")
}

/** Ajuste: campo "medio_contacto_preferido" en solicitudes_soporte. */
enum class MedioContacto(val etiqueta: String) {
    CORREO("Correo"),
    WHATSAPP("WhatsApp"),
    LLAMADA("Llamada")
}

data class SolicitudSoporte(
    val id: Long = 0,
    val asunto: String = "",
    val descripcion: String = "",
    val categoria: String = "OTRO",
    val medioContactoPreferido: String = "CORREO",
    val archivoAdjunto: String? = null,      // ajuste: URL del adjunto
    val prioridad: String = "MEDIA",
    val estado: String = "ABIERTA",
    val creadoEn: String = ""
)

data class NuevaConsultaRequest(
    val categoria: CategoriaConsulta,
    val asunto: String,
    val descripcion: String,
    val medioContactoPreferido: MedioContacto
)

data class NuevaResenaRequest(
    val idEncargo: Long? = null,
    val idServicio: Long? = null,
    val puntuacion: Int = 5,
    val comentario: String? = null
)
