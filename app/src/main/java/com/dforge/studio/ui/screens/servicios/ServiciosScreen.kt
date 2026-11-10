package com.dforge.studio.ui.screens.servicios

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dforge.studio.data.model.CategoriaServicio
import com.dforge.studio.data.model.Producto
import com.dforge.studio.data.model.Servicio
import com.dforge.studio.data.repository.DForgeRepository
import com.dforge.studio.ui.components.Cargando
import com.dforge.studio.ui.components.ChipsSeleccion
import com.dforge.studio.ui.components.DForgeTopBar
import com.dforge.studio.ui.components.Etiqueta
import com.dforge.studio.ui.components.IconoCirculo
import com.dforge.studio.ui.components.MensajeError
import com.dforge.studio.ui.components.MensajeVacio
import com.dforge.studio.ui.components.iconoCategoria
import com.dforge.studio.ui.theme.EstadoAlerta
import com.dforge.studio.ui.theme.EstadoExito
import com.dforge.studio.util.Formato
import com.dforge.studio.util.UiState
import com.dforge.studio.util.dforgeViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class DatosCatalogo(
    val categorias: List<CategoriaServicio>,
    val servicios: List<Servicio>,
    val productos: List<Producto>
)

class ServiciosViewModel(private val repo: DForgeRepository) : ViewModel() {
    private val _estado = MutableStateFlow<UiState<DatosCatalogo>>(UiState.Cargando)
    val estado = _estado.asStateFlow()
    val noLeidas = repo.noLeidas

    init { cargar() }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = UiState.Cargando
            _estado.value = try {
                coroutineScope {
                    val c = async { repo.categorias().getOrThrow() }
                    val s = async { repo.servicios().getOrThrow() }
                    val p = async { repo.productos().getOrThrow() }
                    UiState.Exito(
                        DatosCatalogo(c.await(), s.await().filter { it.activo }, p.await().filter { it.activo })
                    )
                }
            } catch (e: Exception) {
                UiState.Error(e.message ?: "No se pudo cargar el catálogo.")
            }
        }
    }
}

@Composable
fun ServiciosScreen(
    onNotificaciones: () -> Unit,
    onSolicitarServicio: (Long) -> Unit,
    onPedirProducto: (Long) -> Unit
) {
    val vm = dforgeViewModel { c, _ -> ServiciosViewModel(c.repository) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val noLeidas by vm.noLeidas.collectAsStateWithLifecycle()
    var pestana by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = { DForgeTopBar("Servicios y productos", noLeidas, onNotificaciones) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = pestana) {
                Tab(selected = pestana == 0, onClick = { pestana = 0 }, text = { Text("Servicios") })
                Tab(selected = pestana == 1, onClick = { pestana = 1 }, text = { Text("Productos") })
            }
            when (val s = estado) {
                UiState.Cargando -> Cargando()
                is UiState.Error -> MensajeError(s.mensaje, vm::cargar)
                is UiState.Exito ->
                    if (pestana == 0) ListaServicios(s.datos, onSolicitarServicio)
                    else ListaProductos(s.datos.productos, onPedirProducto)
            }
        }
    }
}

@Composable
private fun ListaServicios(datos: DatosCatalogo, onSolicitar: (Long) -> Unit) {
    var categoria by rememberSaveable { mutableStateOf<String?>(null) }
    val opciones = listOf<String?>(null) + datos.categorias.map { it.nombre }
    val filtrados = datos.servicios.filter { categoria == null || it.categoria == categoria }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ChipsSeleccion(
                opciones = opciones,
                seleccionada = categoria,
                etiqueta = { it ?: "Todos" },
                onSeleccion = { categoria = it }
            )
        }
        if (filtrados.isEmpty()) item { MensajeVacio("No hay servicios en esta categoría.") }
        items(filtrados, key = { it.id }) { servicio ->
            Card {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconoCirculo(iconoCategoria(servicio.categoria))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(servicio.nombre, style = MaterialTheme.typography.titleMedium)
                            Text(servicio.categoria, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(servicio.descripcion, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (servicio.precioBase.compareTo(BigDecimal.ZERO) == 0) "Gratis"
                            else "Desde ${Formato.dinero(servicio.precioBase)}",
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        if (servicio.requiereArchivo) Etiqueta("Requiere archivo 3D")
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { onSolicitar(servicio.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Solicitar")
                    }
                }
            }
        }
    }
}

@Composable
private fun ListaProductos(productos: List<Producto>, onPedir: (Long) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (productos.isEmpty()) item { MensajeVacio("Todavía no hay productos en el catálogo.") }
        items(productos, key = { it.id }) { producto ->
            Card {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconoCirculo(Icons.Outlined.Inventory2)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(producto.nombre, style = MaterialTheme.typography.titleMedium)
                        Text(producto.descripcion, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Etiqueta(producto.material)
                            if (producto.stockTerminado > 0) Etiqueta("Disponible", EstadoExito)
                            else Etiqueta("Bajo pedido", EstadoAlerta)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(Formato.dinero(producto.precio), fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(onClick = { onPedir(producto.id) }) { Text("Pedir") }
                    }
                }
            }
        }
    }
}
