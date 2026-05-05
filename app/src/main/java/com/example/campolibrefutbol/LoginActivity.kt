package com.example.campolibrefutbol

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.example.campolibrefutbol.ui.theme.CampoLibreFutbolTheme

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userDb = SQLiteUserHelper(this)
        userDb.ensureDefaultAdminUser()

        enableEdgeToEdge()
        setContent {
            CampoLibreFutbolTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LoginScreen(
                        modifier = Modifier.padding(innerPadding),
                        onLogin = { correo, contrasena ->
                            when (val result = userDb.validateUserWithPolicy(correo, contrasena)) {
                                is LoginValidationResult.Success -> {
                                    val normalizedEmail = correo.trim().lowercase()
                                    startActivity(
                                        Intent(this, MainActivity::class.java)
                                            .putExtra("user_email", normalizedEmail)
                                            .putExtra("is_admin", userDb.isAdminUser(normalizedEmail))
                                    )
                                    finish()
                                }

                                is LoginValidationResult.InvalidCredentials -> {
                                    Toast.makeText(
                                        this,
                                        "Correo o contraseña incorrectos. Intentos restantes: ${result.remainingAttempts}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                is LoginValidationResult.Locked -> {
                                    Toast.makeText(
                                        this,
                                        "Cuenta bloqueada temporalmente. Intenta en ${result.secondsRemaining} segundos",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },
                        onRegister = { correo, contrasena ->
                            val policyError = userDb.getPasswordPolicyError(contrasena)
                            if (policyError != null) {
                                Toast.makeText(this, policyError, Toast.LENGTH_LONG).show()
                                return@LoginScreen
                            }

                            if (userDb.userExists(correo)) {
                                Toast.makeText(this, "Ese correo ya está registrado", Toast.LENGTH_SHORT).show()
                            } else {
                                val ok = userDb.registerUser(correo, contrasena)
                                if (ok) {
                                    Toast.makeText(this, "Usuario registrado. Ahora puedes iniciar sesión", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this, "No se pudo registrar el usuario", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onChangePassword = { correo, nuevaContrasena ->
                            val policyError = userDb.getPasswordPolicyError(nuevaContrasena)
                            if (policyError != null) {
                                Toast.makeText(this, policyError, Toast.LENGTH_LONG).show()
                                return@LoginScreen
                            }

                            val ok = userDb.updatePassword(correo, nuevaContrasena)
                            if (ok) {
                                Toast.makeText(this, "Contraseña actualizada", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "No se encontró ese correo", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onValidationError = { message ->
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    initialMode: AuthMode = AuthMode.LOGIN,
    onLogin: (String, String) -> Unit = { _, _ -> },
    onRegister: (String, String) -> Unit = { _, _ -> },
    onChangePassword: (String, String) -> Unit = { _, _ -> },
    onValidationError: (String) -> Unit = {}
) {
    var mode by remember { mutableStateOf(initialMode) }
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "⚽ Campo Libre Fútbol", style = MaterialTheme.typography.titleLarge)

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = when (mode) {
                        AuthMode.LOGIN -> "Iniciar sesión"
                        AuthMode.REGISTER -> "Registrar usuario"
                        AuthMode.CHANGE_PASSWORD -> "Cambiar contraseña"
                    },
                    style = MaterialTheme.typography.titleMedium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == AuthMode.LOGIN,
                        onClick = { mode = AuthMode.LOGIN; correo = ""; contrasena = ""; passwordVisible = false },
                        label = { Text("Login") }
                    )
                    FilterChip(
                        selected = mode == AuthMode.REGISTER,
                        onClick = { mode = AuthMode.REGISTER; correo = ""; contrasena = ""; passwordVisible = false },
                        label = { Text("Registro") }
                    )
                    FilterChip(
                        selected = mode == AuthMode.CHANGE_PASSWORD,
                        onClick = { mode = AuthMode.CHANGE_PASSWORD; correo = ""; contrasena = ""; passwordVisible = false },
                        label = { Text("Cambiar clave") }
                    )
                }
            }
        }

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = contrasena,
            onValueChange = { contrasena = it },
            label = {
                Text(
                    when (mode) {
                        AuthMode.CHANGE_PASSWORD -> "Nueva contraseña"
                        else -> "Contraseña"
                    }
                )
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (correo.isBlank() || contrasena.isBlank()) {
                    onValidationError("Completa correo y contraseña")
                    return@Button
                }

                if (!correo.contains("@")) {
                    onValidationError("Ingresa un correo válido")
                    return@Button
                } else {
                    when (mode) {
                        AuthMode.LOGIN -> onLogin(correo.trim(), contrasena)
                        AuthMode.REGISTER -> onRegister(correo.trim(), contrasena)
                        AuthMode.CHANGE_PASSWORD -> onChangePassword(correo.trim(), contrasena)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = when (mode) {
                    AuthMode.LOGIN -> "Ingresar"
                    AuthMode.REGISTER -> "Crear cuenta"
                    AuthMode.CHANGE_PASSWORD -> "Actualizar contraseña"
                }
            )
        }

        Text(
            text = "La contraseña debe tener 8+ caracteres, mayúscula, número y símbolo.",
            style = MaterialTheme.typography.bodyMedium
        )

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "👤 Usuario Administrador de Demo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Correo: admin@campolibre.com",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Contraseña: Admin123!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Box(modifier = Modifier.weight(1f))

        Text(
            text = "Datos guardados localmente en SQLite",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

enum class AuthMode {
    LOGIN,
    REGISTER,
    CHANGE_PASSWORD
}

@Preview(showBackground = true, name = "Login")
@Composable
fun PreviewLoginScreen() {
    CampoLibreFutbolTheme {
        LoginScreen()
    }
}

@Preview(showBackground = true, name = "Registro")
@Composable
fun PreviewRegisterScreen() {
    CampoLibreFutbolTheme {
        LoginScreen(initialMode = AuthMode.REGISTER)
    }
}

@Preview(showBackground = true, name = "Cambiar Clave")
@Composable
fun PreviewChangePasswordScreen() {
    CampoLibreFutbolTheme {
        LoginScreen(initialMode = AuthMode.CHANGE_PASSWORD)
    }
}