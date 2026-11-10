package com.dforge.studio.ui.screens.asesoria

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
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dforge.studio.data.model.CategoriaConsulta
import com.dforge.studio.data.model.Encargo
import com.dforge.studio.data.model.EstadoEncargo
import com.dforge.studio.data.model.MedioContacto
import com.dforge.studio.data.model.NuevaConsultaRequest
import com.dforge.studio.data.model.NuevaResenaRequest
import com.dforge.studio.data.repository.ArchivoSeleccionado
import com.dforge.studio.data.repository.DForgeRepository
import com.dforge.studio.ui.components.ChipsSeleccion
import com.dforge.studio.ui.components.DForgeTopBar
import com.dforge.studio.ui.components.Estrellas
import com.dforge.studio.ui.components.MensajeVacio
import com.dforge.studio.ui.theme.EstadoExito
import com.dforge.studio.util.Formato
import com.dforge.studio.util.ReglasArchivo
import com.dforge.studio.util.dforgeViewModel
import com.dforge.studio.util.leerArchivo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConsultaUiState(
    val categoria: CategoriaConsulta = CategoriaConsulta.MATERIALES,
    val asunto: String = "",
    val descripcion: String = "",
    val medio: MedioContacto = MedioContacto.CORREO,
    val adjunto: ArchivoSeleccionado? = null,
    val errorAdjunto: String? = null,
    val enviando: Boolean = false,
    val error: String? = null,
    val enviada: Boolean = false
)

data class OpinionUiState(
    val entregados: List<Encargo> = emptyList(),
    val encargoId: Long? = null,
    val puntuacion: Int = 5,
    val comentario: String = "",
    val enviando: Boolean = false,
    val error: String? = null,
    val enviada: Boolean = false
)

class AsesoriaViewModel(private val repo: DForgeRepository) : ViewModel() {
    private val _consulta = MutableStateFlow(ConsultaUiState())
    val consulta = _consulta.asStateFlow()

    private val _opinion = MutableStateFlow(OpinionUiState())
    val opinion = _opinion.asStateFlow()

    val noLeidas = repo.noLeidas

    init { cargarPedidosParaCalificar() }

    // ----- Consulta técnica -----
    fun onCategoria(c: CategoriaConsulta) = _consulta.update { it.copy(categoria = c) }
    fun onAsunto(v: String) = _consulta.update { it.copy(asunto = v.take(150), error = null) }
    fun onDescripcion(v: String) = _consulta.update { it.copy(descripcion = v.take(3000), error = null) }
    fun onMedio(m: MedioContacto) = _consulta.update { it.copy(medio = m) }
    fun onAdjunto(a: ArchivoSeleccionado) {
        val error = ReglasArchivo.validar(a, ReglasArchivo.formatosAdjunto)
        _consulta.update { if (error != null) it.copy(adjunto = null, errorAdjunto = error) else it.copy(adjunto = a, errorAdjunto = null) }
    }
    fun quitarAdjunto() = _consulta.update { it.copy(adjunto = null, errorAdjunto = null) }
    fun nuevaConsulta() = _consulta.update { ConsultaUiState() }

    fun enviarConsulta() {
        val s = _consulta.value
        when {
            s.asunto.isBlank() -> { _consulta.update { it.copy(error = "Escribe el asunto de tu consulta.") }; return }
            s.descripcion.trim().length < 15 -> {
                _consulta.update { it.copy(error = "Describe tu consulta con más detalle (mínimo 15 caracteres).") }; return
            }
        }
        viewModelScope.launch {
            _consulta.update { it.copy(enviando = true, error = null) }
            repo.enviarConsulta(
                NuevaConsultaRequest(s.categoria, s.asunto.trim(), s.descripcion.trim(), s.medio),
                s.adjunto
            ).onSuccess { _consulta.update { it.copy(enviando = false, enviada = true) } }
                .onFailure { e -> _consulta.update { it.copy(enviando = false, error = e.message) } }
        }
    }

    // ----- Opinión (reseña) -----
    // Regla de la base de datos: solo se califican encargos entregados.
    private fun cargarPedidosParaCalificar() {
        viewModelScope.launch {
            repo.misEncargos().onSuccess { lista ->
                val entregados = lista.filter { it.estadoEnum == EstadoEncargo.ENTREGADO && !it.tieneResena }
                _opinion.update { it.copy(entregados = entregados, encargoId = entregados.firstOrNull()?.id) }
            }
        }
    }

    fun onEncargoOpinion(id: Long) = _opinion.update { it.copy(encargoId = id) }
    fun onPuntuacion(p: Int) = _opinion.update { it.copy(puntuacion = p) }
    fun onComentario(v: String) = _opinion.update { it.copy(comentario = v.take(500)) }

    fun enviarOpinion() {
        val s = _opinion.value
        val id = s.encargoId ?: return
        viewModelScope.launch {
            _opinion.update { it.copy(enviando = true, error = null) }
            repo.enviarResena(NuevaResenaRequest(idEncargo = id, puntuacion = s.puntuacion,
                comentario = s.comentario.trim().ifBlank { null }))
                .onSuccess {
                    _opinion.update { OpinionUiState(enviada = true) }
                    cargarPedidosParaCalificar()
                }
                .onFailure { e -> _opinion.update { it.copy(enviando = false, error = e.message) } }
        }
    }
}

@Composable
fun AsesoriaScreen(onNotificaciones: () -> Unit) {
    val vm = dforgeViewModel { c, _ -> AsesoriaViewModel(c.repository) }
    val noLeidas by vm.noLeidas.collectAsStateWithLifecycle()
    var pestana by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = { DForgeTopBar("Asesoría", noLeidas, onNotificaciones) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = pestana) {
                Tab(selected = pestana == 0, onClick = { pestana = 0 }, text = { Text("Asesoría técnica") })
                Tab(selected = pestana == 1, onClick = { pestana = 1 }, text = { Text("Enviar opinión") })
            }
            if (pestana == 0) FormularioConsulta(vm) else FormularioOpinion(vm)
        }
    }
}

@Composable
private fun FormularioConsulta(vm: AsesoriaViewModel) {
    val s by vm.consulta.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val selector = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.onAdjunto(contexto.leerArchivo(uri))
    }

    if (s.enviada) {
        Confirmacion(
            titulo = "Recibimos tu consulta",
            texto = "Un integrante del equipo te responderá por ${s.medio.etiqueta.lowercase()} en menos de 24 horas hábiles.",
            boton = "Hacer otra consulta",
            onBoton = vm::nuevaConsulta
        )
        return
    }

    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Text(
                "¿Dudas sobre materiales, medidas o tu pedido? Te ayudamos a elegir la mejor opción para tu pieza.",
                modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium
            )
        }
        Text("Categoría", style = MaterialTheme.typography.labelLarge)
        ChipsSeleccion(CategoriaConsulta.entries.toList(), s.categoria, { it.etiqueta }, vm::onCategoria)

        OutlinedTextField(
            value = s.asunto, onValueChange = vm::onAsunto, label = { Text("Asunto") },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = s.descripcion, onValueChange = vm::onDescripcion,
            label = { Text("Describe tu consulta") },
            placeholder = { Text("Para qué usarás la pieza, si debe resistir calor, peso o agua, medidas importantes…") },
            minLines = 4, modifier = Modifier.fillMaxWidth()
        )

        // Adjunto opcional (ajuste: campo archivo_adjunto)
        val adjunto = s.adjunto
        if (adjunto == null) {
            OutlinedButton(onClick = { selector.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.AttachFile, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Adjuntar archivo o plano (opcional)")
            }
            Text("PDF, PNG, JPG, STL o STEP. Máximo 50 MB.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(adjunto.nombre, fontWeight = FontWeight.SemiBold)
                    Text(Formato.tamano(adjunto.tamanoBytes), style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = vm::quitarAdjunto) { Icon(Icons.Outlined.Close, contentDescription = "Quitar") }
            }
        }
        s.errorAdjunto?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        // Medio de contacto preferido (ajuste: campo medio_contacto_preferido)
        Text("¿Cómo prefieres que te contactemos?", style = MaterialTheme.typography.labelLarge)
        ChipsSeleccion(MedioContacto.entries.toList(), s.medio, { it.etiqueta }, vm::onMedio)

        s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(onClick = vm::enviarConsulta, enabled = !s.enviando, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            if (s.enviando) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary)
            else Text("Enviar consulta")
        }
    }
}

@Composable
private fun FormularioOpinion(vm: AsesoriaViewModel) {
    val s by vm.opinion.collectAsStateWithLifecycle()

    if (s.enviada && s.entregados.isEmpty()) {
        Confirmacion("¡Gracias por tu opinión!", "Nos ayuda a mejorar cada pieza que fabricamos.", null) {}
        return
    }

    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (s.enviada) {
            Text("¡Gracias por tu opinión! Puedes calificar otro pedido.", color = EstadoExito)
        }
        if (s.entregados.isEmpty()) {
            MensajeVacio("Podrás dar tu opinión cuando recibas tu primer pedido.")
        } else {
            CamposOpinion(s, vm)
        }
    }
}

@Composable
private fun CamposOpinion(s: OpinionUiState, vm: AsesoriaViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("¿Qué pedido quieres calificar?", style = MaterialTheme.typography.labelLarge)
        ChipsSeleccion(
            opciones = s.entregados,
            seleccionada = s.entregados.firstOrNull { it.id == s.encargoId },
            etiqueta = { "${it.codigo} · ${it.titulo}" },
            onSeleccion = { vm.onEncargoOpinion(it.id) }
        )
        Text("Tu calificación", style = MaterialTheme.typography.labelLarge)
        Estrellas(s.puntuacion, vm::onPuntuacion)
        OutlinedTextField(
            value = s.comentario, onValueChange = vm::onComentario,
            label = { Text("Comentario (opcional)") }, minLines = 3, modifier = Modifier.fillMaxWidth()
        )
        s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = vm::enviarOpinion, enabled = !s.enviando, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            if (s.enviando) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary)
            else Text("Enviar opinión")
        }
    }
}

@Composable
private fun Confirmacion(titulo: String, texto: String, boton: String?, onBoton: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = EstadoExito, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text(titulo, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(texto, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (boton != null) {
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onBoton) { Text(boton) }
        }
    }
}
