package com.dforge.studio.ui.screens.pedidos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dforge.studio.data.model.ArchivoEncargo
import com.dforge.studio.data.model.Cotizacion
import com.dforge.studio.data.model.Encargo
import com.dforge.studio.data.model.EstadoEncargo
import com.dforge.studio.data.model.NuevaResenaRequest
import com.dforge.studio.data.repository.DForgeRepository
import com.dforge.studio.ui.components.Cargando
import com.dforge.studio.ui.components.DForgeTopBar
import com.dforge.studio.ui.components.EstadoChip
import com.dforge.studio.ui.components.Estrellas
import com.dforge.studio.ui.components.Etiqueta
import com.dforge.studio.ui.components.MensajeError
import com.dforge.studio.ui.components.colorEstado
import com.dforge.studio.ui.theme.EstadoAlerta
import com.dforge.studio.ui.theme.EstadoError
import com.dforge.studio.ui.theme.EstadoExito
import com.dforge.studio.util.Formato
import com.dforge.studio.util.UiState
import com.dforge.studio.util.dforgeViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

class PedidoDetalleViewModel(
    private val repo: DForgeRepository,
    argumentos: SavedStateHandle
) : ViewModel() {
    private val idEncargo: Long = checkNotNull(argumentos.get<Long>("id"))

    private val _estado = MutableStateFlow<UiState<Encargo>>(UiState.Cargando)
    val estado = _estado.asStateFlow()

    private val _procesando = MutableStateFlow(false)
    val procesando = _procesando.asStateFlow()

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje = _mensaje.asStateFlow()

    init { cargar() }

    fun cargar() {
        viewModelScope.launch {
            if (_estado.value !is UiState.Exito) _estado.value = UiState.Cargando
            repo.encargo(idEncargo)
                .onSuccess { _estado.value = UiState.Exito(it) }
                .onFailure { _estado.value = UiState.Error(it.message ?: "No se pudo cargar el pedido.") }
        }
    }

    fun responderCotizacion(cotizacion: Cotizacion, aceptar: Boolean) {
        viewModelScope.launch {
            _procesando.value = true
            repo.responderCotizacion(cotizacion.id, aceptar)
                .onSuccess {
                    _mensaje.value = if (aceptar) "Cotización aceptada. Te avisaremos cómo pagar el anticipo."
                    else "Cotización rechazada. El equipo te contactará."
                    cargar()
                }
                .onFailure { _mensaje.value = it.message }
            _procesando.value = false
        }
    }

    fun enviarResena(puntuacion: Int, comentario: String) {
        viewModelScope.launch {
            _procesando.value = true
            repo.enviarResena(NuevaResenaRequest(idEncargo = idEncargo, puntuacion = puntuacion,
                comentario = comentario.trim().ifBlank { null }))
                .onSuccess { _mensaje.value = "¡Gracias por tu opinión!"; cargar() }
                .onFailure { _mensaje.value = it.message }
            _procesando.value = false
        }
    }

    fun mensajeMostrado() { _mensaje.value = null }
}

@Composable
fun PedidoDetalleScreen(onAtras: () -> Unit) {
    val vm = dforgeViewModel { c, h -> PedidoDetalleViewModel(c.repository, h) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val procesando by vm.procesando.collectAsStateWithLifecycle()
    val mensaje by vm.mensaje.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(mensaje) {
        mensaje?.let { snackbar.showSnackbar(it); vm.mensajeMostrado() }
    }

    val titulo = (estado as? UiState.Exito)?.datos?.codigo ?: "Pedido"
    Scaffold(
        topBar = { DForgeTopBar(titulo, onAtras = onAtras) },
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        when (val s = estado) {
            UiState.Cargando -> Cargando(Modifier.padding(padding))
            is UiState.Error -> MensajeError(s.mensaje, vm::cargar, Modifier.padding(padding))
            is UiState.Exito -> DetalleEncargo(
                encargo = s.datos,
                procesando = procesando,
                modifier = Modifier.padding(padding),
                onResponder = vm::responderCotizacion,
                onResena = vm::enviarResena
            )
        }
    }
}

@Composable
private fun DetalleEncargo(
    encargo: Encargo,
    procesando: Boolean,
    modifier: Modifier,
    onResponder: (Cotizacion, Boolean) -> Unit,
    onResena: (Int, String) -> Unit
) {
    var dialogoResena by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Encabezado
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(encargo.titulo.ifBlank { "Encargo" }, style = MaterialTheme.typography.titleLarge)
                Text("Solicitado el ${Formato.fecha(encargo.fechaSolicitud)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            EstadoChip(encargo.estado)
        }
        Text(encargo.descripcion, style = MaterialTheme.typography.bodyMedium)

        // Línea de avance
        if (encargo.estadoEnum != EstadoEncargo.BORRADOR && encargo.estadoEnum != EstadoEncargo.CANCELADO) {
            LineaDeAvance(encargo.estadoEnum)
        }

        // Cotización pendiente: el cliente acepta o rechaza
        encargo.cotizacionPendiente?.let { cot ->
            TarjetaCotizacion(cot, procesando, onResponder)
        }

        // Fechas
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Entrega", style = MaterialTheme.typography.titleMedium)
                Fila("Fecha estimada", Formato.fecha(encargo.fechaEntregaEstimada))
                if (encargo.fechaEntregaReal != null) Fila("Entregado", Formato.fecha(encargo.fechaEntregaReal))
            }
        }

        // Detalle de lo pedido
        encargo.detalles.forEach { d ->
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(d.producto ?: d.servicio ?: d.descripcion, style = MaterialTheme.typography.titleMedium)
                    d.material?.let { Fila("Material", it) }
                    Fila("Cantidad", "${d.cantidad}")
                    d.especificaciones.forEach { (k, v) -> Fila(k, v) }
                    if (d.subtotal > BigDecimal.ZERO) Fila("Subtotal", Formato.dinero(d.subtotal))
                    d.archivos.forEach { ArchivoAnalizado(it) }
                }
            }
        }

        // Totales del encargo
        if (encargo.total > BigDecimal.ZERO) {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Fila("Subtotal", Formato.dinero(encargo.subtotal))
                    if (encargo.descuento > BigDecimal.ZERO) Fila("Descuento", "− ${Formato.dinero(encargo.descuento)}")
                    Fila("IVA (13 %)", Formato.dinero(encargo.impuesto))
                    HorizontalDivider()
                    Fila("Total", Formato.dinero(encargo.total), destacado = true)
                }
            }
        }

        // Reseña (solo pedidos entregados)
        if (encargo.estadoEnum == EstadoEncargo.ENTREGADO && !encargo.tieneResena) {
            Button(onClick = { dialogoResena = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Calificar este pedido")
            }
        }

        // Historial
        if (encargo.historial.isNotEmpty()) {
            Text("Historial", style = MaterialTheme.typography.titleMedium)
            encargo.historial.sortedByDescending { it.fechaCambio }.forEach { h ->
                Row {
                    Box(
                        Modifier.padding(top = 6.dp).size(10.dp).clip(CircleShape)
                            .background(colorEstado(EstadoEncargo.de(h.estadoNuevo)))
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(EstadoEncargo.de(h.estadoNuevo)?.etiqueta ?: h.estadoNuevo, fontWeight = FontWeight.SemiBold)
                        h.comentario?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        Text(Formato.fechaHora(h.fechaCambio), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (dialogoResena) {
        DialogoResena(
            onCerrar = { dialogoResena = false },
            onEnviar = { p, c -> onResena(p, c); dialogoResena = false }
        )
    }
}

@Composable
private fun LineaDeAvance(actual: EstadoEncargo?) {
    val pasos = EstadoEncargo.lineaDeAvance
    val indiceActual = pasos.indexOf(actual).let { if (it < 0) 0 else it }
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("Avance", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            pasos.forEachIndexed { i, paso ->
                val hecho = i <= indiceActual
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                    Box(
                        Modifier.size(12.dp).clip(CircleShape).background(
                            if (hecho) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        paso.etiqueta,
                        fontWeight = if (i == indiceActual) FontWeight.Bold else FontWeight.Normal,
                        color = if (hecho) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaCotizacion(cot: Cotizacion, procesando: Boolean, onResponder: (Cotizacion, Boolean) -> Unit) {
    var confirmar by remember { mutableStateOf<Boolean?>(null) }

    Card(colors = CardDefaults.cardColors(containerColor = EstadoAlerta.copy(alpha = 0.08f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Cotización ${cot.codigo}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (cot.generadaAutomaticamente) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Etiqueta("Automática")
                }
            }
            cot.observaciones?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Fila("Subtotal", Formato.dinero(cot.subtotal))
            if (cot.descuento > BigDecimal.ZERO) {
                Fila(cot.promocion?.let { "Descuento ($it)" } ?: "Descuento", "− ${Formato.dinero(cot.descuento)}")
            }
            Fila("IVA (13 %)", Formato.dinero(cot.impuesto))
            HorizontalDivider()
            Fila("Total", Formato.dinero(cot.total), destacado = true)
            Fila("Anticipo (50 %)", Formato.dinero(cot.total.divide(BigDecimal(2), 2, java.math.RoundingMode.HALF_UP)))
            cot.fechaVencimiento?.let {
                Text("Válida hasta el ${Formato.fecha(it)}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { confirmar = false }, enabled = !procesando, modifier = Modifier.weight(1f)) {
                    Text("Rechazar")
                }
                Button(onClick = { confirmar = true }, enabled = !procesando, modifier = Modifier.weight(1f)) {
                    Text("Aceptar")
                }
            }
        }
    }

    confirmar?.let { aceptar ->
        AlertDialog(
            onDismissRequest = { confirmar = null },
            title = { Text(if (aceptar) "¿Aceptar la cotización?" else "¿Rechazar la cotización?") },
            text = {
                Text(
                    if (aceptar) "Para iniciar la producción deberás pagar un anticipo de " +
                        Formato.dinero(cot.total.divide(BigDecimal(2), 2, java.math.RoundingMode.HALF_UP)) + "."
                    else "El equipo revisará tu encargo y podrá enviarte una nueva cotización."
                )
            },
            confirmButton = {
                TextButton(onClick = { onResponder(cot, aceptar); confirmar = null }) {
                    Text(if (aceptar) "Aceptar" else "Rechazar")
                }
            },
            dismissButton = { TextButton(onClick = { confirmar = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun ArchivoAnalizado(archivo: ArchivoEncargo) {
    Spacer(Modifier.height(6.dp))
    HorizontalDivider()
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(archivo.nombreArchivo, fontWeight = FontWeight.SemiBold)
            Text("${archivo.formato} · ${Formato.tamano(archivo.tamanoBytes)}", style = MaterialTheme.typography.bodySmall)
        }
        val (icono, color) = when (archivo.esValido) {
            true -> Icons.Outlined.CheckCircle to EstadoExito
            false -> Icons.Outlined.ErrorOutline to EstadoError
            null -> Icons.Outlined.HourglassTop to EstadoAlerta
        }
        Icon(icono, contentDescription = null, tint = color)
    }
    archivo.mensajeValidacion?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    // Resultados del análisis automático del archivo
    if (archivo.dimX != null && archivo.dimY != null && archivo.dimZ != null) {
        Fila("Medidas", "${archivo.dimX.toPlainString()} × ${archivo.dimY.toPlainString()} × ${archivo.dimZ.toPlainString()} mm")
    }
    archivo.volumenCm3?.let { Fila("Volumen", "${it.toPlainString()} cm³") }
    archivo.gramosEstimados?.let { Fila("Peso estimado", "${it.toPlainString()} g") }
    archivo.horasEstimadas?.let { Fila("Tiempo de impresión", "${it.toPlainString()} h") }
}

@Composable
private fun DialogoResena(onCerrar: () -> Unit, onEnviar: (Int, String) -> Unit) {
    var puntuacion by remember { mutableIntStateOf(5) }
    var comentario by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Califica tu pedido") },
        text = {
            Column {
                Estrellas(puntuacion) { puntuacion = it }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = comentario, onValueChange = { comentario = it.take(500) },
                    label = { Text("Comentario (opcional)") }, minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = { onEnviar(puntuacion, comentario) }) { Text("Enviar") } },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}

@Composable
private fun Fila(etiqueta: String, valor: String, destacado: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(etiqueta, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (destacado) FontWeight.Bold else FontWeight.Normal)
    }
}
