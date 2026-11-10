package com.dforge.studio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Paleta verde azulado tomada de los diseños de Stitch
val Teal900 = Color(0xFF0A4D4D)
val Teal700 = Color(0xFF0F6E6E)
val Teal500 = Color(0xFF2A9D96)
val Teal100 = Color(0xFFD6EFEC)
val Teal50 = Color(0xFFEAF6F4)
val Fondo = Color(0xFFF4FAF9)

// Colores de estado (usados en los chips de estado de los pedidos)
val EstadoInfo = Color(0xFF2563EB)
val EstadoAlerta = Color(0xFFB45309)
val EstadoExito = Color(0xFF15803D)
val EstadoError = Color(0xFFB91C1C)
val EstadoNeutro = Color(0xFF64748B)

private val ColoresClaros = lightColorScheme(
    primary = Teal700,
    onPrimary = Color.White,
    primaryContainer = Teal100,
    onPrimaryContainer = Teal900,
    secondary = Teal500,
    onSecondary = Color.White,
    secondaryContainer = Teal50,
    onSecondaryContainer = Teal900,
    background = Fondo,
    onBackground = Color(0xFF14201F),
    surface = Color.White,
    onSurface = Color(0xFF14201F),
    surfaceVariant = Teal50,
    onSurfaceVariant = Color(0xFF4A5B5A),
    outline = Color(0xFFB7CBC8),
    outlineVariant = Color(0xFFDCE8E6),
    surfaceContainer = Color(0xFFF0F7F6),
    surfaceContainerLow = Color(0xFFF7FBFA),
    surfaceContainerHigh = Color(0xFFE8F2F0)
)

private val ColoresOscuros = darkColorScheme(
    primary = Color(0xFF7CD3CB),
    onPrimary = Color(0xFF003734),
    primaryContainer = Color(0xFF0F4F4C),
    onPrimaryContainer = Teal100,
    secondary = Color(0xFF8FD0C9),
    background = Color(0xFF0E1514),
    surface = Color(0xFF151D1C),
    surfaceVariant = Color(0xFF1F2B2A),
    onSurfaceVariant = Color(0xFFB9CBC8),
    outline = Color(0xFF4F6461),
    outlineVariant = Color(0xFF2C3B39)
)

private val Tipografia = Typography(
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
)

private val Formas = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun DForgeTheme(
    oscuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (oscuro) ColoresOscuros else ColoresClaros,
        typography = Tipografia,
        shapes = Formas,
        content = content
    )
}
