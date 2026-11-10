package com.dforge.studio.ui.screens.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dforge.studio.data.model.Encargo
import com.dforge.studio.data.model.EstadoEncargo
import com.dforge.studio.data.model.Perfil
import com.dforge.studio.data.model.Promocion
import com.dforge.studio.data.model.Servicio
import com.dforge.studio.data.repository.DForgeRepository
import com.dforge.studio.ui.components.Cargando
import com.dforge.studio.ui.components.DForgeTopBar
import com.dforge.studio.ui.components.EstadoChip
import com.dforge.studio.ui.components.IconoCirculo
import com.dforge.studio.ui.components.MensajeError
import com.dforge.studio.ui.components.TituloSeccion
import com.dforge.studio.ui.components.iconoCategoria
import com.dforge.studio.util.Formato
import com.dforge.studio.util.UiState
import com.dforge.studio.util.dforgeViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DatosInicio(
    val perfil: Perfil,
    val encargos: List<Encargo>,
    val servicios: List<Servicio>,
    val promociones: List<Promocion>
) {
    val activos = encargos.count { it.estadoEnum in EstadoEncargo.activos }
    val porAprobar = encargos.count { it.cotizacionPendiente != null }
    val recientes = encargos.filter { it.estadoEnum != EstadoEncargo.BORRADOR }.take(3)
}

class InicioViewModel(private val repo: DForgeRepository) : ViewModel() {
    private val _estado = MutableStateFlow<UiState<DatosInicio>>(UiState.Cargando)
    val estado = _estado.asStateFlow()
    val noLeidas = repo.noLeidas

    init { cargar() }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = UiState.Cargando
            _estado.value = try {
                coroutineScope {
                    val perfil = async { repo.perfil().getOrThrow() }
                    val encargos = async { repo.misEncargos().getOrThrow() }
                    val servicios = async { repo.servicios().getOrThrow() }
                    val promos = async { repo.promocionesActivas().getOrDefault(emptyList()) }
                    // Actualiza el contador de la campana
                    launch { repo.notificaciones() }
                    UiState.Exito(DatosInicio(perfil.await(), encargos.await(), servicios.await(), promos.await()))
                }
            } catch (e: Exception) {
                UiState.Error(e.message ?: "No se pudo cargar el inicio.")
            }
        }
    }
}

@Composable
fun InicioScreen(
    onNotificaciones: () -> Unit,
    onPerfil: () -> Unit,
    onCrearEncargo: () -> Unit,
    onVerServicios: () -> Unit,
    onVerPedidos: () -> Unit,
    onPedido: (Long) -> Unit
) {
    val vm = dforgeViewModel { c, _ -> InicioViewModel(c.repository) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    val noLeidas by vm.noLeidas.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            DForgeTopBar(
                titulo = "Inicio",
                noLeidas = noLeidas,
                onNotificaciones = onNotificaciones,
                acciones = {
                    IconButton(onClick = onPerfil) {
                        Icon(Icons.Outlined.Person, contentDescription = "Mi perfil")
                    }
                }
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) { padding ->
        when (val s = estado) {
            UiState.Cargando -> Cargando(Modifier.padding(padding))
            is UiState.Error -> MensajeError(s.mensaje, vm::cargar, Modifier.padding(padding))
            is UiState.Exito -> ContenidoInicio(
                datos = s.datos,
                modifier = Modifier.padding(padding),
                onCrearEncargo = onCrearEncargo,
                onVerServicios = onVerServicios,
                onVerPedidos = onVerPedidos,
                onPedido = onPedido
            )
        }
    }
}

@Composable
private fun ContenidoInicio(
    datos: DatosInicio,
    modifier: Modifier,
    onCrearEncargo: () -> Unit,
    onVerServicios: () -> Unit,
    onVerPedidos: () -> Unit,
    onPedido: (Long) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Hola, ${datos.perfil.nombres}", style = MaterialTheme.typography.headlineSmall)
            Text(
                "¿Qué vamos a fabricar hoy?",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Tarjeta principal
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Damos forma a tus ideas", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Sube tu archivo 3D o cuéntanos qué necesitas. Si tu pieza es estándar, recibes la cotización automáticamente.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onCrearEncargo,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Outlined.AddCircle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Crear encargo")
                        }
                        OutlinedButton(
                            onClick = onVerServicios,
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) { Text("Servicios") }
                    }
                }
            }
        }

        // Indicadores del cliente
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Indicador("Pedidos activos", datos.activos.toString(), Modifier.weight(1f), onVerPedidos)
                Indicador("Cotizaciones por aprobar", datos.porAprobar.toString(), Modifier.weight(1f), onVerPedidos)
            }
        }

        // Promoción vigente
        datos.promociones.firstOrNull()?.let { promo ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocalOffer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(promo.nombre, fontWeight = FontWeight.SemiBold)
                            promo.descripcion?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                            Text("Hasta el ${Formato.fecha(promo.fechaFin)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Pedidos recientes
        if (datos.recientes.isNotEmpty()) {
            item { TituloSeccion("Tus pedidos recientes", "Ver todos", onVerPedidos) }
            items(datos.recientes, key = { it.id }) { encargo ->
                Card(onClick = { onPedido(encargo.id) }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(encargo.codigo, style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary)
                            Text(encargo.titulo.ifBlank { encargo.descripcion },
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold)
                        }
                        EstadoChip(encargo.estado)
                    }
                }
            }
        }

        // Servicios
        item { TituloSeccion("Nuestros servicios", "Ver todo", onVerServicios) }
        items(datos.servicios.take(3), key = { "s${it.id}" }) { servicio ->
            Card(onClick = onVerServicios) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconoCirculo(iconoCategoria(servicio.categoria))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(servicio.nombre, fontWeight = FontWeight.SemiBold)
                        Text(servicio.descripcion, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun Indicador(titulo: String, valor: String, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(valor, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text(titulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
