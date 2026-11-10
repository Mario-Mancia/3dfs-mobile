package com.dforge.studio.ui.screens.perfil

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dforge.studio.BuildConfig
import com.dforge.studio.data.model.Perfil
import com.dforge.studio.data.repository.DForgeRepository
import com.dforge.studio.ui.components.Cargando
import com.dforge.studio.ui.components.DForgeTopBar
import com.dforge.studio.ui.components.MensajeError
import com.dforge.studio.util.UiState
import com.dforge.studio.util.dforgeViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PerfilViewModel(private val repo: DForgeRepository) : ViewModel() {
    private val _estado = MutableStateFlow<UiState<Perfil>>(UiState.Cargando)
    val estado = _estado.asStateFlow()

    init { cargar() }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = UiState.Cargando
            repo.perfil()
                .onSuccess { _estado.value = UiState.Exito(it) }
                .onFailure { _estado.value = UiState.Error(it.message ?: "No se pudo cargar tu perfil.") }
        }
    }

    /** Al cerrar sesión, DForgeApp vuelve automáticamente a la pantalla de login. */
    fun cerrarSesion() {
        viewModelScope.launch { repo.cerrarSesion() }
    }
}

@Composable
fun PerfilScreen(onAtras: () -> Unit) {
    val vm = dforgeViewModel { c, _ -> PerfilViewModel(c.repository) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    var confirmarSalida by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { DForgeTopBar("Mi perfil", onAtras = onAtras) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        when (val s = estado) {
            UiState.Cargando -> Cargando(Modifier.padding(padding))
            is UiState.Error -> MensajeError(s.mensaje, vm::cargar, Modifier.padding(padding))
            is UiState.Exito -> {
                val p = s.datos
                Column(
                    Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
                        .navigationBarsPadding().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Outlined.AccountCircle, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(80.dp))
                    Text(p.nombreCompleto, style = MaterialTheme.typography.titleLarge)
                    Text(p.correo, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Dato("Tipo de cliente", if (p.tipoCliente == "EMPRESA") "Empresa" else "Persona")
                            p.nombreEmpresa?.let { Dato("Empresa", it) }
                            Dato("Teléfono", p.telefono ?: "—")
                            Dato("Dirección", p.direccion ?: "—")
                        }
                    }
                    Text(
                        "Para cambiar tus datos, ingresa a tu cuenta en la web de DForge Studio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { confirmarSalida = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Cerrar sesión")
                    }
                    Text("Versión ${BuildConfig.VERSION_NAME}" + if (BuildConfig.USAR_DATOS_DEMO) " · modo demostración" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("¿Cerrar sesión?") },
            confirmButton = { TextButton(onClick = { confirmarSalida = false; vm.cerrarSesion() }) { Text("Cerrar sesión") } },
            dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(etiqueta, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor)
    }
}
