package com.dforge.studio.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dforge.studio.BuildConfig
import com.dforge.studio.data.repository.DForgeRepository
import com.dforge.studio.util.dforgeViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val correo: String = "",
    val password: String = "",
    val cargando: Boolean = false,
    val error: String? = null
)

class LoginViewModel(private val repo: DForgeRepository) : ViewModel() {
    private val _estado = MutableStateFlow(LoginUiState())
    val estado = _estado.asStateFlow()

    fun onCorreo(valor: String) = _estado.update { it.copy(correo = valor, error = null) }
    fun onPassword(valor: String) = _estado.update { it.copy(password = valor, error = null) }

    fun entrar() {
        val actual = _estado.value
        if (actual.correo.isBlank() || actual.password.isBlank()) {
            _estado.update { it.copy(error = "Escribe tu correo y tu contraseña.") }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            repo.login(actual.correo, actual.password)
                .onFailure { e -> _estado.update { it.copy(cargando = false, error = e.message) } }
            // Si el login es correcto, la sesión cambia y DForgeApp muestra el inicio automáticamente.
        }
    }
}

@Composable
fun LoginScreen() {
    val vm = dforgeViewModel { c, _ -> LoginViewModel(c.repository) }
    val estado by vm.estado.collectAsStateWithLifecycle()
    var verPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(32.dp))
        Icon(
            Icons.Outlined.ViewInAr, contentDescription = null,
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text("DForge Studio", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Damos forma a tus ideas",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        if (BuildConfig.USAR_DATOS_DEMO) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Modo demostración: entra con cualquier correo y una contraseña de 4 caracteres o más.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = estado.correo,
            onValueChange = vm::onCorreo,
            label = { Text("Correo electrónico") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = estado.password,
            onValueChange = vm::onPassword,
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = if (verPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { verPassword = !verPassword }) {
                    Icon(
                        if (verPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (verPassword) "Ocultar contraseña" else "Mostrar contraseña"
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { vm.entrar() }),
            modifier = Modifier.fillMaxWidth()
        )

        estado.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = vm::entrar,
            enabled = !estado.cargando,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (estado.cargando) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Iniciar sesión", fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "¿No tienes cuenta? Regístrate en la web de DForge Studio.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
