package com.dforge.studio.ui.screens.encargar

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dforge.studio.ui.components.Cargando
import com.dforge.studio.ui.components.ChipsSeleccion
import com.dforge.studio.ui.components.DForgeTopBar
import com.dforge.studio.ui.components.MensajeError
import com.dforge.studio.util.Formato
import com.dforge.studio.util.dforgeViewModel
import com.dforge.studio.util.leerArchivo
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun EncargarScreen(
    onNotificaciones: () -> Unit,
    onEncargoCreado: (Long) -> Unit
) {
    val vm = dforgeViewModel { c, h -> EncargarViewModel(c.repository, h) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val noLeidas by vm.noLeidas.collectAsStateWithLifecycle()

    // Al crear el encargo, se abre su detalle
    LaunchedEffect(estado.encargoCreadoId) {
        estado.encargoCreadoId?.let {
            vm.navegacionRealizada()
            onEncargoCreado(it)
        }
    }

    Scaffold(
        topBar = { DForgeTopBar("Crear encargo", noLeidas, onNotificaciones) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        when {
            estado.cargandoCatalogo -> Cargando(Modifier.padding(padding))
            estado.errorCatalogo != null ->
                MensajeError(estado.errorCatalogo!!, vm::cargarCatalogo, Modifier.padding(padding))
            else -> FormularioEncargo(estado, vm, Modifier.padding(padding))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormularioEncargo(estado: EncargarUiState, vm: EncargarViewModel, modifier: Modifier) {
    val contexto = LocalContext.current
    var mostrarCalendario by remember { mutableStateOf(false) }

    val selectorArchivo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.onArchivo(contexto.leerArchivo(uri))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Tipo de encargo
        Paso("1", "¿Qué necesitas?") {
            ChipsSeleccion(
                opciones = ModoEncargo.entries.toList(),
                seleccionada = estado.modo,
                etiqueta = { it.etiqueta },
                onSeleccion = vm::onModo
            )
            Spacer(Modifier.height(8.dp))
            if (estado.modo == ModoEncargo.SERVICIO) {
                ChipsSeleccion(
                    opciones = estado.servicios,
                    seleccionada = estado.servicio,
                    etiqueta = { it.nombre },
                    onSeleccion = { vm.onServicio(it.id) }
                )
                estado.servicio?.let {
                    Text(it.descripcion, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                ChipsSeleccion(
                    opciones = estado.productos,
                    seleccionada = estado.producto,
                    etiqueta = { "${it.nombre} · ${Formato.dinero(it.precio)}" },
                    onSeleccion = { vm.onProducto(it.id) }
                )
            }
        }

        // 2. Datos del encargo
        Paso("2", "Cuéntanos de tu encargo") {
            OutlinedTextField(
                value = estado.titulo, onValueChange = vm::onTitulo,
                label = { Text(if (estado.modo == ModoEncargo.PRODUCTO) "Nombre del pedido (opcional)" else "Nombre del encargo") },
                placeholder = { Text("Ej.: Repuesto para licuadora") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = estado.descripcion, onValueChange = vm::onDescripcion,
                label = { Text(if (estado.modo == ModoEncargo.PRODUCTO) "Indicaciones (opcional)" else "Descripción") },
                placeholder = {
                    Text(
                        if (estado.modo == ModoEncargo.PRODUCTO) "Ej.: el nombre que llevará cada llavero"
                        else "Para qué se usará la pieza, medidas importantes, si debe resistir calor, agua o peso…"
                    )
                },
                minLines = 3, modifier = Modifier.fillMaxWidth()
            )
        }

        // 3. Archivo 3D (solo servicios)
        if (estado.modo == ModoEncargo.SERVICIO) {
            Paso("3", if (estado.servicio?.requiereArchivo == true) "Archivo 3D (obligatorio)" else "Archivo 3D o foto de referencia (opcional)") {
                val archivo = estado.archivo
                if (archivo == null) {
                    OutlinedButton(
                        onClick = { selectorArchivo.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.UploadFile, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Elegir archivo")
                    }
                    Text("Formatos: STL, OBJ, 3MF o STEP. Máximo 50 MB.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(archivo.nombre, fontWeight = FontWeight.SemiBold)
                            Text(Formato.tamano(archivo.tamanoBytes), style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = vm::quitarArchivo) {
                            Icon(Icons.Outlined.Close, contentDescription = "Quitar archivo")
                        }
                    }
                    Text("Al enviarlo, el sistema revisará que la pieza quepa en la impresora y calculará su peso.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                estado.errorArchivo?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // 4. Especificaciones
        Paso(if (estado.modo == ModoEncargo.SERVICIO) "4" else "3", "Especificaciones") {
            if (estado.necesitaMaterial) {
                Text("Material", style = MaterialTheme.typography.labelLarge)
                ChipsSeleccion(
                    opciones = estado.materiales,
                    seleccionada = estado.materiales.firstOrNull { it.id == estado.materialId },
                    etiqueta = { it.nombre },
                    onSeleccion = { vm.onMaterial(it.id) }
                )
                estado.materiales.firstOrNull { it.id == estado.materialId }?.let {
                    Text(it.descripcion, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(8.dp))
                Text("Relleno", style = MaterialTheme.typography.labelLarge)
                ChipsSeleccion(OPCIONES_RELLENO, estado.relleno, { it }, vm::onRelleno)
                Text("Más relleno = más resistencia, más peso y más costo.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text("Acabado", style = MaterialTheme.typography.labelLarge)
                ChipsSeleccion(OPCIONES_ACABADO, estado.acabado, { it }, vm::onAcabado)
                Spacer(Modifier.height(8.dp))
            }
            OutlinedTextField(
                value = estado.color, onValueChange = vm::onColor,
                label = { Text("Color (opcional)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Cantidad", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { vm.onCantidad(estado.cantidad - 1) }, enabled = estado.cantidad > 1) {
                    Icon(Icons.Outlined.Remove, contentDescription = "Menos")
                }
                Text("${estado.cantidad}", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { vm.onCantidad(estado.cantidad + 1) }) {
                    Icon(Icons.Outlined.Add, contentDescription = "Más")
                }
            }
            OutlinedButton(onClick = { mostrarCalendario = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Event, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(estado.fechaDeseada?.let { "Fecha deseada: ${Formato.fecha(it.toString())}" }
                    ?: "Fecha deseada de entrega (opcional)")
            }
        }

        // Resumen
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Resumen", style = MaterialTheme.typography.titleMedium)
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                if (estado.modo == ModoEncargo.PRODUCTO) {
                    FilaResumen("Producto", estado.producto?.nombre ?: "—")
                    FilaResumen("Precio unitario", Formato.dinero(estado.producto?.precio))
                    FilaResumen("Cantidad", "${estado.cantidad}")
                    FilaResumen("Subtotal estimado", Formato.dinero(estado.subtotalProducto), destacado = true)
                    Text("El IVA y las promociones vigentes se aplican en la cotización.",
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    FilaResumen("Servicio", estado.servicio?.nombre ?: "—")
                    if (estado.necesitaMaterial) {
                        FilaResumen("Material", estado.materiales.firstOrNull { it.id == estado.materialId }?.nombre ?: "—")
                    }
                    FilaResumen("Cantidad", "${estado.cantidad}")
                    val base = estado.servicio?.precioBase ?: BigDecimal.ZERO
                    FilaResumen("Precio", if (base.signum() == 0) "Sin costo" else "Desde ${Formato.dinero(base)}", destacado = true)
                    Text("El precio exacto se calcula con el peso y el tiempo de impresión de tu pieza. Si es estándar, la cotización llega automáticamente.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Errores de validación
        estado.errores.forEach {
            Text("• $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = { vm.enviar(comoBorrador = false) },
            enabled = !estado.enviando,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (estado.enviando) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary)
            else Text(if (estado.modo == ModoEncargo.PRODUCTO) "Enviar pedido" else "Enviar solicitud")
        }
        if (estado.modo == ModoEncargo.SERVICIO) {
            TextButton(
                onClick = { vm.enviar(comoBorrador = true) },
                enabled = !estado.enviando,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Guardar como borrador") }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (mostrarCalendario) {
        val minimoMillis = LocalDate.now().plusDays(2).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val estadoFecha = rememberDatePickerState(
            initialSelectedDateMillis = estado.fechaDeseada?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= minimoMillis
            }
        )
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.onFecha(estadoFecha.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    })
                    mostrarCalendario = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { vm.onFecha(null); mostrarCalendario = false }) { Text("Quitar fecha") }
            }
        ) {
            DatePicker(state = estadoFecha)
        }
    }
}

@Composable
private fun Paso(numero: String, titulo: String, contenido: @Composable () -> Unit) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("PASO $numero", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            contenido()
        }
    }
}

@Composable
private fun FilaResumen(etiqueta: String, valor: String, destacado: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(etiqueta, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(valor, fontWeight = if (destacado) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium)
    }
}
