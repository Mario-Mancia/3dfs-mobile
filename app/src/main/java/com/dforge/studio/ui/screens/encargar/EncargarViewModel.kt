package com.dforge.studio.ui.screens.encargar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dforge.studio.data.model.CrearEncargoRequest
import com.dforge.studio.data.model.Material
import com.dforge.studio.data.model.Producto
import com.dforge.studio.data.model.Servicio
import com.dforge.studio.data.repository.ArchivoSeleccionado
import com.dforge.studio.data.repository.DForgeRepository
import com.dforge.studio.util.ReglasArchivo
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate

enum class ModoEncargo(val etiqueta: String) {
    SERVICIO("Servicio a la medida"),
    PRODUCTO("Producto del catálogo")
}

val OPCIONES_RELLENO = listOf("20 %", "50 %", "100 %")
val OPCIONES_ACABADO = listOf("Estándar", "Lijado", "Pintado")

data class EncargarUiState(
    // Catálogo
    val cargandoCatalogo: Boolean = true,
    val errorCatalogo: String? = null,
    val servicios: List<Servicio> = emptyList(),
    val productos: List<Producto> = emptyList(),
    val materiales: List<Material> = emptyList(),
    // Formulario
    val modo: ModoEncargo = ModoEncargo.SERVICIO,
    val servicioId: Long? = null,
    val productoId: Long? = null,
    val materialId: Long? = null,
    val titulo: String = "",
    val descripcion: String = "",
    val color: String = "",
    val relleno: String = OPCIONES_RELLENO.first(),
    val acabado: String = OPCIONES_ACABADO.first(),
    val cantidad: Int = 1,
    val fechaDeseada: LocalDate? = null,
    val archivo: ArchivoSeleccionado? = null,
    val errorArchivo: String? = null,
    // Envío
    val enviando: Boolean = false,
    val errores: List<String> = emptyList(),
    val encargoCreadoId: Long? = null
) {
    val servicio: Servicio? get() = servicios.firstOrNull { it.id == servicioId }
    val producto: Producto? get() = productos.firstOrNull { it.id == productoId }

    /** La asesoría no necesita material ni especificaciones de impresión. */
    val necesitaMaterial: Boolean
        get() = modo == ModoEncargo.SERVICIO && servicio?.categoria?.contains("Asesor", ignoreCase = true) != true

    val subtotalProducto: BigDecimal
        get() = (producto?.precio ?: BigDecimal.ZERO).multiply(BigDecimal(cantidad))
}

class EncargarViewModel(
    private val repo: DForgeRepository,
    argumentos: SavedStateHandle
) : ViewModel() {

    private val _estado = MutableStateFlow(EncargarUiState())
    val estado = _estado.asStateFlow()
    val noLeidas = repo.noLeidas

    init {
        // Argumentos de navegación: llegar desde "Solicitar" (servicio) o "Pedir" (producto)
        val servicioInicial = argumentos.get<Long>("servicioId")?.takeIf { it > 0 }
        val productoInicial = argumentos.get<Long>("productoId")?.takeIf { it > 0 }
        _estado.update {
            it.copy(
                modo = if (productoInicial != null) ModoEncargo.PRODUCTO else ModoEncargo.SERVICIO,
                servicioId = servicioInicial,
                productoId = productoInicial
            )
        }
        cargarCatalogo()
    }

    fun cargarCatalogo() {
        viewModelScope.launch {
            _estado.update { it.copy(cargandoCatalogo = true, errorCatalogo = null) }
            try {
                coroutineScope {
                    val s = async { repo.servicios().getOrThrow() }
                    val p = async { repo.productos().getOrThrow() }
                    val m = async { repo.materiales().getOrThrow() }
                    val servicios = s.await().filter { it.activo }
                    val productos = p.await().filter { it.activo }
                    val materiales = m.await().filter { it.activo }
                    _estado.update { actual ->
                        actual.copy(
                            cargandoCatalogo = false,
                            servicios = servicios,
                            productos = productos,
                            materiales = materiales,
                            servicioId = actual.servicioId ?: servicios.firstOrNull()?.id,
                            materialId = actual.materialId ?: materiales.firstOrNull()?.id
                        )
                    }
                }
            } catch (e: Exception) {
                _estado.update { it.copy(cargandoCatalogo = false, errorCatalogo = e.message ?: "No se pudo cargar el catálogo.") }
            }
        }
    }

    fun onModo(modo: ModoEncargo) = _estado.update {
        it.copy(modo = modo, errores = emptyList(),
            productoId = if (modo == ModoEncargo.PRODUCTO) it.productoId ?: it.productos.firstOrNull()?.id else it.productoId)
    }
    fun onServicio(id: Long) = _estado.update { it.copy(servicioId = id, errores = emptyList()) }
    fun onProducto(id: Long) = _estado.update { it.copy(productoId = id, errores = emptyList()) }
    fun onMaterial(id: Long) = _estado.update { it.copy(materialId = id) }
    fun onTitulo(v: String) = _estado.update { it.copy(titulo = v.take(120)) }
    fun onDescripcion(v: String) = _estado.update { it.copy(descripcion = v.take(2000)) }
    fun onColor(v: String) = _estado.update { it.copy(color = v.take(40)) }
    fun onRelleno(v: String) = _estado.update { it.copy(relleno = v) }
    fun onAcabado(v: String) = _estado.update { it.copy(acabado = v) }
    fun onCantidad(v: Int) = _estado.update { it.copy(cantidad = v.coerceIn(1, 500)) }
    fun onFecha(fecha: LocalDate?) = _estado.update { it.copy(fechaDeseada = fecha) }

    fun onArchivo(archivo: ArchivoSeleccionado) {
        val error = ReglasArchivo.validar(archivo, ReglasArchivo.formatos3d)
        _estado.update {
            if (error != null) it.copy(archivo = null, errorArchivo = error)
            else it.copy(archivo = archivo, errorArchivo = null)
        }
    }

    fun quitarArchivo() = _estado.update { it.copy(archivo = null, errorArchivo = null) }

    fun enviar(comoBorrador: Boolean) {
        val s = _estado.value
        val errores = validar(s, comoBorrador)
        if (errores.isNotEmpty()) {
            _estado.update { it.copy(errores = errores) }
            return
        }

        val especificaciones = buildMap {
            if (s.color.isNotBlank()) put("Color", s.color.trim())
            if (s.modo == ModoEncargo.SERVICIO && s.necesitaMaterial) {
                put("Relleno", s.relleno)
                put("Acabado", s.acabado)
            }
        }
        val solicitud = CrearEncargoRequest(
            titulo = s.titulo.trim().ifBlank { s.producto?.nombre ?: s.servicio?.nombre ?: "Encargo" },
            descripcion = s.descripcion.trim().ifBlank { "Pedido de catálogo: ${s.producto?.nombre.orEmpty()}" },
            idServicio = if (s.modo == ModoEncargo.SERVICIO) s.servicioId else null,
            idProducto = if (s.modo == ModoEncargo.PRODUCTO) s.productoId else null,
            idMaterial = when {
                s.modo == ModoEncargo.PRODUCTO -> s.producto?.idMaterial
                s.necesitaMaterial -> s.materialId
                else -> null
            },
            cantidad = s.cantidad,
            especificaciones = especificaciones,
            fechaEntregaDeseada = s.fechaDeseada?.toString(),
            esBorrador = comoBorrador
        )

        viewModelScope.launch {
            _estado.update { it.copy(enviando = true, errores = emptyList()) }
            repo.crearEncargo(solicitud, if (s.modo == ModoEncargo.SERVICIO) s.archivo else null)
                .onSuccess { creado -> _estado.update { it.copy(enviando = false, encargoCreadoId = creado.id) } }
                .onFailure { e -> _estado.update { it.copy(enviando = false, errores = listOf(e.message ?: "No se pudo enviar.")) } }
        }
    }

    /** Se llama después de navegar al pedido creado, para no navegar dos veces. */
    fun navegacionRealizada() = _estado.update { EncargarUiState(
        cargandoCatalogo = false, servicios = it.servicios, productos = it.productos, materiales = it.materiales,
        servicioId = it.servicios.firstOrNull()?.id, materialId = it.materiales.firstOrNull()?.id
    ) }

    private fun validar(s: EncargarUiState, borrador: Boolean): List<String> = buildList {
        if (borrador) {
            if (s.titulo.isBlank() && s.descripcion.isBlank()) add("Escribe al menos un nombre o una descripción para guardar el borrador.")
            return@buildList
        }
        when (s.modo) {
            ModoEncargo.SERVICIO -> {
                val servicio = s.servicio
                if (servicio == null) add("Elige un servicio.")
                if (s.titulo.isBlank()) add("Escribe un nombre para tu encargo.")
                if (s.descripcion.trim().length < 10) add("Describe tu encargo con más detalle (mínimo 10 caracteres).")
                if (servicio?.requiereArchivo == true && s.archivo == null) add("Este servicio necesita tu archivo 3D (STL, OBJ, 3MF o STEP).")
                if (s.necesitaMaterial && s.materialId == null) add("Elige un material.")
            }
            ModoEncargo.PRODUCTO -> {
                if (s.producto == null) add("Elige un producto del catálogo.")
            }
        }
        val minima = LocalDate.now().plusDays(2)
        if (s.fechaDeseada != null && s.fechaDeseada.isBefore(minima)) {
            add("La fecha deseada debe ser a partir del $minima.")
        }
    }
}
