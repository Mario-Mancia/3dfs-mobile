package com.dforge.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DesignServices
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dforge.studio.data.model.EstadoEncargo
import com.dforge.studio.ui.theme.EstadoAlerta
import com.dforge.studio.ui.theme.EstadoError
import com.dforge.studio.ui.theme.EstadoExito
import com.dforge.studio.ui.theme.EstadoInfo
import com.dforge.studio.ui.theme.EstadoNeutro

/** Barra superior común: marca DForge Studio + título + campana de notificaciones. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DForgeTopBar(
    titulo: String,
    noLeidas: Int = 0,
    onNotificaciones: (() -> Unit)? = null,
    onAtras: (() -> Unit)? = null,
    acciones: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    "DFORGE STUDIO",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(titulo, style = MaterialTheme.typography.titleLarge)
            }
        },
        navigationIcon = {
            if (onAtras != null) {
                IconButton(onClick = onAtras) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                }
            }
        },
        actions = {
            acciones()
            if (onNotificaciones != null) {
                IconButton(onClick = onNotificaciones) {
                    BadgedBox(badge = { if (noLeidas > 0) Badge { Text("$noLeidas") } }) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Notificaciones")
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
}

/** Color y texto de cada estado del encargo. */
fun colorEstado(estado: EstadoEncargo?): Color = when (estado) {
    EstadoEncargo.BORRADOR, EstadoEncargo.EN_ESPERA, null -> EstadoNeutro
    EstadoEncargo.PENDIENTE, EstadoEncargo.EN_REVISION -> EstadoInfo
    EstadoEncargo.COTIZADO, EstadoEncargo.ESPERANDO_PAGO -> EstadoAlerta
    EstadoEncargo.APROBADO, EstadoEncargo.EN_PRODUCCION -> Color(0xFF0F6E6E)
    EstadoEncargo.FINALIZADO, EstadoEncargo.ENTREGADO -> EstadoExito
    EstadoEncargo.CANCELADO -> EstadoError
}

@Composable
fun EstadoChip(estado: String, modifier: Modifier = Modifier) {
    val enumEstado = EstadoEncargo.de(estado)
    val color = colorEstado(enumEstado)
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.12f),
        contentColor = color,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = enumEstado?.etiqueta ?: estado,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/** Etiqueta pequeña tipo "pastilla". */
@Composable
fun Etiqueta(texto: String, color: Color = MaterialTheme.colorScheme.primary) {
    Surface(color = color.copy(alpha = 0.12f), contentColor = color, shape = RoundedCornerShape(50)) {
        Text(
            texto,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun TituloSeccion(texto: String, accion: String? = null, onAccion: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(texto, style = MaterialTheme.typography.titleMedium)
        if (accion != null) TextButton(onClick = onAccion) { Text(accion) }
    }
}

@Composable
fun Cargando(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun MensajeError(mensaje: String, onReintentar: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.CloudOff, contentDescription = null, modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(12.dp))
        Text(mensaje, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(16.dp))
        Button(onClick = onReintentar) { Text("Reintentar") }
    }
}

@Composable
fun MensajeVacio(texto: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(texto, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Grupo de chips de selección única. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChipsSeleccion(
    opciones: List<T>,
    seleccionada: T?,
    etiqueta: (T) -> String,
    onSeleccion: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        opciones.forEach { opcion ->
            FilterChip(
                selected = opcion == seleccionada,
                onClick = { onSeleccion(opcion) },
                label = { Text(etiqueta(opcion)) }
            )
        }
    }
}

/** Ícono según la categoría del servicio. */
fun iconoCategoria(categoria: String): ImageVector = when {
    categoria.contains("Impresión", ignoreCase = true) -> Icons.Outlined.Print
    categoria.contains("Diseño", ignoreCase = true) -> Icons.Outlined.DesignServices
    categoria.contains("Asesor", ignoreCase = true) -> Icons.Outlined.Lightbulb
    else -> Icons.Outlined.Science
}

@Composable
fun IconoCirculo(icono: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun PuntoNoLeido() {
    Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
}

/** Selector de 1 a 5 estrellas. */
@Composable
fun Estrellas(valor: Int, onCambio: (Int) -> Unit) {
    Row {
        (1..5).forEach { i ->
            Icon(
                imageVector = if (i <= valor) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = "$i estrellas",
                tint = if (i <= valor) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onCambio(i) }
                    .padding(4.dp)
            )
            if (i < 5) Spacer(Modifier.width(2.dp))
        }
    }
}
