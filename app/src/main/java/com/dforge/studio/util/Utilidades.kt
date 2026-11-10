package com.dforge.studio.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.createSavedStateHandle
import com.dforge.studio.AppContainer
import com.dforge.studio.DForgeApplication
import com.dforge.studio.data.repository.ArchivoSeleccionado
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Estado genérico de una pantalla que carga datos. */
sealed interface UiState<out T> {
    data object Cargando : UiState<Nothing>
    data class Exito<T>(val datos: T) : UiState<T>
    data class Error(val mensaje: String) : UiState<Nothing>
}

/**
 * Crea un ViewModel que recibe el contenedor de dependencias y el SavedStateHandle
 * (argumentos de navegación). Uso: val vm = dforgeViewModel { c, h -> MiViewModel(c.repository, h) }
 */
@Composable
inline fun <reified VM : ViewModel> dforgeViewModel(
    crossinline crear: (AppContainer, SavedStateHandle) -> VM
): VM {
    val app = LocalContext.current.applicationContext as DForgeApplication
    return viewModel(
        factory = viewModelFactory {
            initializer { crear(app.container, createSavedStateHandle()) }
        }
    )
}

object Formato {
    private val localeSv: Locale = Locale.forLanguageTag("es-SV")
    private val moneda: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US) // El Salvador usa USD
    private val soloFecha = DateTimeFormatter.ofPattern("d MMM yyyy", localeSv)
    private val fechaHora = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", localeSv)

    fun dinero(valor: BigDecimal?): String = moneda.format(valor ?: BigDecimal.ZERO)

    /** Acepta "2026-11-09T15:42:00-06:00" o "2026-11-09". */
    fun fecha(iso: String?): String {
        if (iso.isNullOrBlank()) return "—"
        return try {
            OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).format(soloFecha)
        } catch (_: Exception) {
            try { LocalDate.parse(iso.take(10)).format(soloFecha) } catch (_: Exception) { iso }
        }
    }

    fun fechaHora(iso: String?): String {
        if (iso.isNullOrBlank()) return "—"
        return try {
            OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).format(fechaHora)
        } catch (_: Exception) {
            fecha(iso)
        }
    }

    fun tamano(bytes: Long): String = when {
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}

/** Lee nombre y tamaño de un archivo elegido con el selector del sistema. */
fun Context.leerArchivo(uri: Uri): ArchivoSeleccionado {
    var nombre = uri.lastPathSegment ?: "archivo"
    var tamano = 0L
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val iNombre = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val iTamano = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            if (iNombre >= 0) nombre = cursor.getString(iNombre) ?: nombre
            if (iTamano >= 0 && !cursor.isNull(iTamano)) tamano = cursor.getLong(iTamano)
        }
    }
    return ArchivoSeleccionado(uri, nombre, tamano, contentResolver.getType(uri))
}

/** Reglas de archivos permitidos (las mismas que valida Django). */
object ReglasArchivo {
    const val TAMANO_MAXIMO = 50L * 1024 * 1024 // 50 MB
    val formatos3d = setOf("stl", "obj", "3mf", "step", "stp")
    val formatosAdjunto = formatos3d + setOf("pdf", "png", "jpg", "jpeg")

    fun validar(archivo: ArchivoSeleccionado, permitidos: Set<String>): String? = when {
        archivo.extension !in permitidos ->
            "Formato .${archivo.extension} no permitido. Usa: ${permitidos.joinToString { it.uppercase() }}."
        archivo.tamanoBytes > TAMANO_MAXIMO -> "El archivo supera el máximo de 50 MB."
        else -> null
    }
}
