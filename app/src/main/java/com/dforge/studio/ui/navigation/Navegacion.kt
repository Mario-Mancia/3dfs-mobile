package com.dforge.studio.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dforge.studio.DForgeApplication
import com.dforge.studio.ui.components.Cargando
import com.dforge.studio.ui.screens.asesoria.AsesoriaScreen
import com.dforge.studio.ui.screens.encargar.EncargarScreen
import com.dforge.studio.ui.screens.inicio.InicioScreen
import com.dforge.studio.ui.screens.login.LoginScreen
import com.dforge.studio.ui.screens.notificaciones.NotificacionesScreen
import com.dforge.studio.ui.screens.pedidos.PedidoDetalleScreen
import com.dforge.studio.ui.screens.pedidos.PedidosScreen
import com.dforge.studio.ui.screens.perfil.PerfilScreen
import com.dforge.studio.ui.screens.servicios.ServiciosScreen

object Rutas {
    const val LOGIN = "login"
    const val INICIO = "inicio"
    const val SERVICIOS = "servicios"
    const val ENCARGAR = "encargar"
    const val ENCARGAR_CON_ARGS = "encargar?servicioId={servicioId}&productoId={productoId}"
    const val PEDIDOS = "pedidos"
    const val PEDIDO_DETALLE = "pedido/{id}"
    const val ASESORIA = "asesoria"
    const val NOTIFICACIONES = "notificaciones"
    const val PERFIL = "perfil"

    fun encargar(servicioId: Long? = null, productoId: Long? = null) =
        "encargar?servicioId=${servicioId ?: -1}&productoId=${productoId ?: -1}"

    fun pedido(id: Long) = "pedido/$id"
}

private data class ItemBarra(val ruta: String, val etiqueta: String, val icono: ImageVector)

private val itemsBarra = listOf(
    ItemBarra(Rutas.INICIO, "Inicio", Icons.Outlined.Home),
    ItemBarra(Rutas.SERVICIOS, "Servicios", Icons.Outlined.Category),
    ItemBarra(Rutas.ENCARGAR, "Encargar", Icons.Outlined.AddCircle),
    ItemBarra(Rutas.PEDIDOS, "Pedidos", Icons.AutoMirrored.Outlined.ListAlt),
    ItemBarra(Rutas.ASESORIA, "Asesoría", Icons.Outlined.SupportAgent)
)

/** Punto de entrada de la interfaz: decide entre login y la app según la sesión. */
@Composable
fun DForgeApp() {
    val app = LocalContext.current.applicationContext as DForgeApplication
    val sesionActiva by app.container.repository.sesionActiva
        .collectAsStateWithLifecycle(initialValue = null)

    when (val activa = sesionActiva) {
        null -> Cargando()
        // key(): al iniciar o cerrar sesión se crea una navegación nueva (se limpia el historial)
        else -> key(activa) {
            NavegacionPrincipal(inicio = if (activa) Rutas.INICIO else Rutas.LOGIN)
        }
    }
}

@Composable
private fun NavegacionPrincipal(inicio: String) {
    val nav = rememberNavController()
    val entrada by nav.currentBackStackEntryAsState()
    val rutaActual = entrada?.destination?.route?.substringBefore('?')
    val mostrarBarra = itemsBarra.any { it.ruta == rutaActual }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (mostrarBarra) {
                NavigationBar {
                    itemsBarra.forEach { item ->
                        NavigationBarItem(
                            selected = rutaActual == item.ruta,
                            onClick = { nav.irAPestana(item.ruta) },
                            icon = { Icon(item.icono, contentDescription = null) },
                            label = { Text(item.etiqueta) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = inicio,
            modifier = Modifier.padding(padding)
        ) {
            composable(Rutas.LOGIN) {
                LoginScreen()
            }
            composable(Rutas.INICIO) {
                InicioScreen(
                    onNotificaciones = { nav.navigate(Rutas.NOTIFICACIONES) },
                    onPerfil = { nav.navigate(Rutas.PERFIL) },
                    onCrearEncargo = { nav.irAPestana(Rutas.ENCARGAR) },
                    onVerServicios = { nav.irAPestana(Rutas.SERVICIOS) },
                    onVerPedidos = { nav.irAPestana(Rutas.PEDIDOS) },
                    onPedido = { nav.navigate(Rutas.pedido(it)) }
                )
            }
            composable(Rutas.SERVICIOS) {
                ServiciosScreen(
                    onNotificaciones = { nav.navigate(Rutas.NOTIFICACIONES) },
                    onSolicitarServicio = { nav.navigate(Rutas.encargar(servicioId = it)) },
                    onPedirProducto = { nav.navigate(Rutas.encargar(productoId = it)) }
                )
            }
            composable(
                route = Rutas.ENCARGAR_CON_ARGS,
                arguments = listOf(
                    navArgument("servicioId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("productoId") { type = NavType.LongType; defaultValue = -1L }
                )
            ) {
                EncargarScreen(
                    onNotificaciones = { nav.navigate(Rutas.NOTIFICACIONES) },
                    onEncargoCreado = { id ->
                        nav.navigate(Rutas.pedido(id)) {
                            popUpTo(Rutas.ENCARGAR_CON_ARGS) { inclusive = true }
                        }
                    }
                )
            }
            composable(Rutas.PEDIDOS) {
                PedidosScreen(
                    onNotificaciones = { nav.navigate(Rutas.NOTIFICACIONES) },
                    onPedido = { nav.navigate(Rutas.pedido(it)) }
                )
            }
            composable(
                route = Rutas.PEDIDO_DETALLE,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) {
                PedidoDetalleScreen(onAtras = { nav.popBackStack() })
            }
            composable(Rutas.ASESORIA) {
                AsesoriaScreen(onNotificaciones = { nav.navigate(Rutas.NOTIFICACIONES) })
            }
            composable(Rutas.NOTIFICACIONES) {
                NotificacionesScreen(
                    onAtras = { nav.popBackStack() },
                    onAbrirPedido = { nav.navigate(Rutas.pedido(it)) }
                )
            }
            composable(Rutas.PERFIL) {
                PerfilScreen(onAtras = { nav.popBackStack() })
            }
        }
    }
}

/** Navega a una pestaña de la barra inferior conservando el estado de cada una. */
private fun NavHostController.irAPestana(ruta: String) {
    navigate(ruta) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
