package com.dforge.studio.ui.screens.notificaciones

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dforge.studio.data.model.Notificacion
import com.dforge.studio.data.repository.DForgeRepository
import com.dforge.studio.ui.components.Cargando
import com.dforge.studio.ui.components.DForgeTopBar
import com.dforge.studio.ui.components.MensajeError
import com.dforge.studio.ui.components.MensajeVacio
import com.dforge.studio.ui.components.PuntoNoLeido
import com.dforge.studio.util.Formato
import com.dforge.studio.util.UiState
import com.dforge.studio.util.dforgeViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificacionesViewModel(private val repo: DForgeRepository) : ViewModel() {
    private val _estado = MutableStateFlow<UiState<List<Notificacion>>>(UiState.Cargando)
    val estado = _estado.asStateFlow()

    init { cargar() }

    fun cargar() {
        viewModelScope.launch {
            repo.notificaciones()
                .onSuccess { _estado.value = UiState.Exito(it) }
                .onFailure { _estado.value = UiState.Error(it.message ?: "No se pudieron cargar las notificaciones.") }
        }
    }

    fun abrir(n: Notificacion) {
        if (n.leida) return
        viewModelScope.launch {
            repo.marcarLeida(n.id).onSuccess {
                val actual = _estado.value
                if (actual is UiState.Exito) {
                    _estado.value = UiState.Exito(actual.datos.map { if (it.id == n.id) it.copy(leida = true) else it })
                }
            }
        }
    }
}

@Composable
fun NotificacionesScreen(onAtras: () -> Unit, onAbrirPedido: (Long) -> Unit) {
    val vm = dforgeViewModel { c, _ -> NotificacionesViewModel(c.repository) }
    val estado by vm.estado.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { DForgeTopBar("Notificaciones", onAtras = onAtras) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        when (val s = estado) {
            UiState.Cargando -> Cargando(Modifier.padding(padding))
            is UiState.Error -> MensajeError(s.mensaje, vm::cargar, Modifier.padding(padding))
            is UiState.Exito -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (s.datos.isEmpty()) item { MensajeVacio("No tienes notificaciones.") }
                items(s.datos, key = { it.id }) { n ->
                    TarjetaNotificacion(n) {
                        vm.abrir(n)
                        // Las notificaciones de encargos y cotizaciones abren el pedido relacionado
                        if (n.referenciaId != null && n.tipo in setOf("ENCARGO", "COTIZACION", "PAGO")) {
                            onAbrirPedido(n.referenciaId)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaNotificacion(n: Notificacion, onClick: () -> Unit) {
    val icono = when (n.tipo) {
        "COTIZACION" -> Icons.Outlined.RequestQuote
        "ENCARGO" -> Icons.Outlined.Inventory2
        "PAGO" -> Icons.Outlined.Payments
        "SOPORTE" -> Icons.Outlined.SupportAgent
        "PROMOCION" -> Icons.Outlined.LocalOffer
        else -> Icons.Outlined.Notifications
    }
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (n.leida) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(n.titulo, fontWeight = if (n.leida) FontWeight.Normal else FontWeight.Bold)
                Text(n.mensaje, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Formato.fechaHora(n.fechaCreacion), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!n.leida) Box(Modifier.padding(top = 6.dp).size(8.dp)) { PuntoNoLeido() }
        }
    }
}
