# DForge Studio — App Android (clientes)

App móvil de clientes de **DForge Studio**, hecha con **Kotlin + Jetpack Compose** (Material 3) y arquitectura **MVVM**. Consume la API de **Django REST Framework**; la base de datos vive en Supabase (PostgreSQL), pero la app nunca se conecta directo a ella.

## Cómo abrirla

1. Android Studio (Ladybug 2024.2 o más reciente) → **File › Open** → elige la carpeta `DForgeStudioApp`.
2. Espera a que termine **Gradle Sync**. La primera vez descarga Gradle 8.9 y las librerías.
3. Ejecuta en un emulador o en un celular con Android 8.0 (API 26) o superior.

> El proyecto no incluye `gradle-wrapper.jar`. Android Studio descarga Gradle con `gradle/wrapper/gradle-wrapper.properties`. Si quieren usar `./gradlew` desde la terminal, ejecuten una vez **Gradle › Tasks › build setup › wrapper** desde Android Studio.

## Modo demostración y modo real

En `app/build.gradle.kts`:

```kotlin
buildConfigField("boolean", "USAR_DATOS_DEMO", "true")   // datos de ejemplo, sin backend
buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000/api/\"")
```

- **`true`**: la app funciona sola con datos de ejemplo. Se entra con cualquier correo y una contraseña de 4 caracteres o más.
- **`false`**: la app usa la API de Django en `API_BASE_URL`.
  - `10.0.2.2` es la computadora donde corre Django, vista desde el **emulador**.
  - En un **celular físico**, usen la IP local de la computadora (ej. `http://192.168.1.50:8000/api/`), agréguenla en `res/xml/network_security_config.xml` y corran Django con `python manage.py runserver 0.0.0.0:8000`.

## Pantallas

| Pantalla | Qué hace |
|---|---|
| Login | Inicio de sesión con correo y contraseña (JWT) |
| Inicio | Saludo, pedidos activos, cotizaciones por aprobar, promoción vigente, pedidos recientes y servicios |
| Servicios y productos | Servicios por categoría y **catálogo de productos** con precio y disponibilidad |
| Crear encargo | Servicio a la medida o producto del catálogo; archivo 3D, material, relleno, acabado, color, cantidad y fecha deseada. Envía o guarda como **borrador** |
| Mis pedidos | Filtros: todos, por aprobar, activos, borradores, entregados |
| Detalle del pedido | Línea de avance, **cotización con aceptar/rechazar** y anticipo del 50 %, análisis del archivo (medidas, volumen, peso, tiempo), totales con IVA, historial y reseña |
| Asesoría | Consulta técnica con **categoría, adjunto y medio de contacto preferido**, y opinión (reseña) de pedidos entregados |
| Notificaciones | Lista con no leídas; al tocar una de pedido o cotización, abre ese pedido |
| Perfil | Datos del cliente y cierre de sesión |

## Ajustes a la base de datos que la app ya asume

1. **`encargos` antes que `cotizaciones`:** la cotización tiene `id_encargo` y un encargo puede tener varias. Nuevos estados del encargo: `BORRADOR`, `COTIZADO` y `ESPERANDO_PAGO`.
2. **`materiales`** como tabla, y `id_material` en `encargos_detalles`.
3. **`productos`** (catálogo), y `id_producto` opcional en `encargos_detalles`.
4. **`cantidad` entera** en los detalles.
5. **`solicitudes_soporte`** con `categoria`, `archivo_adjunto` y `medio_contacto_preferido`.
6. **Promociones relacionadas:** `id_promocion` en cotizaciones.
7. Campos de la **automatización**: en `archivos_encargos`: `volumen_cm3`, `dim_x/y/z`, `gramos_estimados`, `horas_estimadas`, `es_valido`, `mensaje_validacion`. En `encargos`: `origen` y `prioridad`. En `cotizaciones`: `generada_automaticamente`.
8. Sin la tabla `estados_usuarios`: la app no la usa.

## Endpoints que la app espera (para el backend en Django)

Base: `/api/`. JSON en **snake_case**, montos `NUMERIC` (DRF los envía como texto `"12.50"`) y fechas ISO 8601. Las listas van **sin paginación**. Todos los endpoints, salvo el login, requieren `Authorization: Bearer <access>` y devuelven **solo los datos del cliente autenticado**.

| Método | Ruta | Cuerpo / respuesta |
|---|---|---|
| POST | `auth/login/` | `{correo, password}` → `{access, refresh}` (simplejwt con `USERNAME_FIELD = "correo"`) |
| GET | `perfil/` | `{id, nombres, apellidos, correo, telefono, tipo_cliente, nombre_empresa, direccion}` |
| GET | `categorias-servicios/` | `[{id, nombre, descripcion}]` |
| GET | `servicios/` | `[{id, id_categoria, categoria, nombre, descripcion, precio_base, requiere_archivo, activo}]` |
| GET | `productos/` | `[{id, nombre, descripcion, id_material, material, precio, stock_terminado, imagen_url, activo}]` |
| GET | `materiales/` | `[{id, nombre, tecnologia, descripcion, precio_por_gramo, densidad_g_cm3, activo}]` |
| GET | `promociones/activas/` | `[{id, nombre, descripcion, tipo_descuento, valor_descuento, fecha_inicio, fecha_fin}]` |
| GET | `encargos/` | Lista de encargos con `detalles`, `cotizaciones` e `historial` anidados |
| GET | `encargos/{id}/` | Un encargo con lo mismo anidado (los archivos van dentro de cada detalle) |
| POST | `encargos/` | `{titulo, descripcion, id_servicio, id_producto, id_material, cantidad, especificaciones, fecha_entrega_deseada, es_borrador, origen}` → encargo |
| POST | `encargos/{id}/archivos/` | multipart, campo `archivo` → archivo creado. **Aquí se dispara el análisis y la cotización automática** |
| POST | `cotizaciones/{id}/aceptar/` | → cotización (el encargo pasa a `ESPERANDO_PAGO` y se emite la factura) |
| POST | `cotizaciones/{id}/rechazar/` | → cotización (el encargo vuelve a `EN_REVISION`) |
| GET | `notificaciones/` | `[{id, titulo, mensaje, tipo, referencia_id, leida, fecha_creacion}]` |
| POST | `notificaciones/{id}/leer/` | 200/204 |
| POST | `soporte/` | multipart: `categoria`, `asunto`, `descripcion`, `medio_contacto_preferido`, `archivo_adjunto` (opcional) |
| POST | `resenas/` | `{id_encargo, id_servicio, puntuacion, comentario}`. Solo se aceptan encargos `ENTREGADO` |

Errores: DRF responde `{"detail": "..."}` o `{"campo": ["mensaje"]}`, y la app muestra ese texto al usuario. Un `401` cierra la sesión.

## Estructura del código

```
app/src/main/java/com/dforge/studio/
├── AppContainer.kt            ← decide datos demo o API real
├── DForgeApplication.kt, MainActivity.kt
├── data/
│   ├── model/Modelos.kt       ← modelos alineados con la BD
│   ├── remote/                ← Retrofit (DForgeApi) y cliente HTTP con JWT
│   ├── local/SessionStore.kt  ← tokens en DataStore
│   └── repository/            ← interfaz + implementación demo + implementación remota
├── ui/
│   ├── navigation/            ← rutas y barra inferior
│   ├── components/            ← barra superior, chips de estado, etc.
│   ├── screens/               ← una carpeta por pantalla (Screen + ViewModel)
│   └── theme/                 ← colores y tipografía
└── util/                      ← formatos de dinero y fecha, archivos, UiState
```

## Pendiente para la siguiente etapa

- **Notificaciones push** con Firebase Cloud Messaging (requiere `google-services.json` y el endpoint `dispositivos/` para registrar el token).
- **Renovación automática del token** con `auth/refresh/`. Hoy, cuando el token vence, se pide iniciar sesión otra vez.
- **Inicio de sesión con Google** (django-allauth + Credential Manager en Android).
- **Fotos de productos** con Coil, cuando la API envíe `imagen_url`.
- **Visor 3D** del archivo dentro de la app (opcional).
