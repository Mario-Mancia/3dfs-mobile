package com.dforge.studio.ui.screens.pedidos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dforge.studio.data.model.Encargo
import com.dforge.studio.data.model.EstadoEncargo
import com.dforge.studio.data.repository.DForgeRepository
import com.dforge.studio.ui.components.Cargando
import com.dforge.studio.ui.components.ChipsSeleccion
import com.dforge.studio.ui.components.DForgeTopBar
import com.dforge.studio.ui.components.EstadoChip
import com.dforge.studio.ui.components.Etiqueta
import com.dforge.studio.ui.components.MensajeError
import com.dforge.studio.ui.components.MensajeVacio
import com.dforge.studio.ui.theme.EstadoAlerta
import com.dforge.studio.util.Formato
import com.dforge.studio.util.UiState
import com.dforge.studio.util.dforgeViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

enum class FiltroPedidos(val etiqueta: String) {
    TODOS("Todos"),
    POR_APROBAR("Por aprobar"),
    ACTIVOS("Activos"),
    BORRADORES("Borradores"),
    ENTREGADOS("Entregados");

    fun aplica(e: Encargo): Boolean = when (this) {
        TODOS -> true
        POR_APROBAR -> e.cotizacionPendiente != null
        ACTIVOS -> e.estadoEnum in EstadoEncargo.activos
        BORRADORES -> e.estadoEnum == EstadoEncargo.BORRADOR
        ENTREGADOS -> e.estadoEnum == EstadoEncargo.ENTREGADO
    }
}

class PedidosViewModel(private val repo: DForgeRepository) : ViewModel() {
    private val _estado = MutableStateFlow<UiState<List<Encargo>>>(UiState.Cargando)
    val estado = _estado.asStateFlow()
    val noLeidas = repo.noLeidas

    fun cargar() {
        viewModelScope.launch {
            if (_estado.value !is UiState.Exito) _estado.value = UiState.Cargando
            repo.misEncargos()
                .onSuccess { _estado.value = UiState.Exito(it) }
                .onFailure { _estado.value = UiState.Error(it.message ?: "No se pudieron cargar tus pedidos.") }
        }
    }
}

@Composable
fun PedidosScreen(onNotificaciones: () -> Unit, onPedido: (Long) -> Unit) {
    val vm = dforgeViewModel { c, _ -> PedidosViewModel(c.repository) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val noLeidas by vm.noLeidas.collectAsStateWithLifecycle()
    var filtro by rememberSaveable { mutableStateOf(FiltroPedidos.TODOS) }

    // Se recarga cada vez que se entra a la pantalla (por si cambió un estado)
    LaunchedEffect(Unit) { vm.cargar() }

    Scaffold(
        topBar = {
            DForgeTopBar("Mis pedidos", noLeidas, onNotificaciones, acciones = {
                IconButton(onClick = vm::cargar) { Icon(Icons.Outlined.Refresh, contentDescription = "Actualizar") }
            })
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        when (val s = estado) {
            UiState.Cargando -> Cargando(Modifier.padding(padding))
            is UiState.Error -> MensajeError(s.mensaje, vm::cargar, Modifier.padding(padding))
            is UiState.Exito -> {
                val lista = s.datos.filter { filtro.aplica(it) }
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        ChipsSeleccion(FiltroPedidos.entries.toList(), filtro, { it.etiqueta }, { filtro = it })
                    }
                    if (lista.isEmpty()) item { MensajeVacio("No tienes pedidos en esta sección.") }
                    items(lista, key = { it.id }) { TarjetaPedido(it) { onPedido(it.id) } }
                }
            }
        }
    }
}

@Composable
private fun TarjetaPedido(encargo: Encargo, onClick: () -> Unit) {
    Card(onClick = onClick) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(encargo.codigo, style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                EstadoChip(encargo.estado)
            }
            Spacer(Modifier.height(6.dp))
            Text(encargo.titulo.ifBlank { encargo.descripcion }, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(encargo.descripcion, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Solicitado: ${Formato.fecha(encargo.fechaSolicitud)}",
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                if (encargo.cotizacionPendiente != null) Etiqueta("Cotización por aprobar", EstadoAlerta)
                else if (encargo.total > BigDecimal.ZERO) Text(Formato.dinero(encargo.total), fontWeight = FontWeight.Bold)
            }
        }
    }
}
