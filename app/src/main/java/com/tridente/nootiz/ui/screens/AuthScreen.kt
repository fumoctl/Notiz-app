package com.tridente.nootiz.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tridente.nootiz.data.local.SessionPreferences
import com.tridente.nootiz.data.model.AuthResponse
import com.tridente.nootiz.data.remote.NotizApiClient
import com.tridente.nootiz.ui.theme.ErrorRed
import com.tridente.nootiz.ui.theme.LimeAccent
import com.tridente.nootiz.ui.theme.NavyBackground
import com.tridente.nootiz.ui.theme.NavyInputBorder
import com.tridente.nootiz.ui.theme.NavyInputSurface
import com.tridente.nootiz.ui.theme.PureWhite
import com.tridente.nootiz.ui.theme.RoyalBlue
import com.tridente.nootiz.ui.theme.SkyBlueCard
import com.tridente.nootiz.ui.theme.SuccessGreen
import com.tridente.nootiz.ui.theme.TextMutedLight
import com.tridente.nootiz.ui.theme.TextPrimaryNavy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
    sessionPrefs: SessionPreferences,
    apiClient: NotizApiClient,
    onAuthenticated: (AuthResponse) -> Unit
) {
    val scope = rememberCoroutineScope()

    var serverUrl by remember { mutableStateOf(SessionPreferences.DEFAULT_EMULATOR_URL) }
    var showServerPresets by remember { mutableStateOf(false) }
    var serverHealthStatus by remember { mutableStateOf<Boolean?>(null) }
    var checkingHealth by remember { mutableStateOf(false) }

    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var showGoogleDialog by remember { mutableStateOf(false) }
    var googleIdToken by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val loadedUrl = withContext(Dispatchers.IO) {
            sessionPrefs.ensureLoaded()
            sessionPrefs.baseUrl
        }
        serverUrl = loadedUrl
    }

    fun verifyServerConnection(urlToCheck: String) {
        val normalized = SessionPreferences.normalizeBaseUrl(urlToCheck)
        serverUrl = normalized
        sessionPrefs.baseUrl = normalized
        checkingHealth = true
        serverHealthStatus = null
        scope.launch {
            val result = apiClient.checkHealth(normalized)
            checkingHealth = false
            serverHealthStatus = result.getOrDefault(false)
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = NavyInputSurface,
        unfocusedContainerColor = NavyInputSurface,
        focusedBorderColor = SkyBlueCard,
        unfocusedBorderColor = NavyInputBorder,
        focusedTextColor = PureWhite,
        unfocusedTextColor = PureWhite,
        cursorColor = LimeAccent,
        focusedPlaceholderColor = TextMutedLight.copy(alpha = 0.6f),
        unfocusedPlaceholderColor = TextMutedLight.copy(alpha = 0.6f)
    )

    Scaffold(
        containerColor = NavyBackground,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Top Brand Icon (Upper-right mode switch button removed)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(RoyalBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "CloudNotes",
                    tint = LimeAccent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Title & Subtitle matching Image 1 & 3
            Text(
                text = if (isRegisterMode) "Crear perfil" else "CloudNotes",
                style = MaterialTheme.typography.headlineLarge,
                color = PureWhite
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isRegisterMode) {
                    "Configura tu cuenta para empezar a organizar tus ideas."
                } else {
                    "Tus ideas, siempre contigo."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextMutedLight
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Server / Base API URL Configuration Card
            Surface(
                color = NavyInputSurface.copy(alpha = 0.75f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NavyInputBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = SkyBlueCard,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "URL Base del Servidor / API",
                                color = TextMutedLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Connection status indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { verifyServerConnection(serverUrl) }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            when {
                                checkingHealth -> {
                                    Text("Probando...", color = LimeAccent, fontSize = 11.sp)
                                }
                                serverHealthStatus == true -> {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "API Online",
                                        color = SuccessGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                serverHealthStatus == false -> {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = ErrorRed,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text("Sin respuesta", color = ErrorRed, fontSize = 11.sp)
                                }
                                else -> {
                                    Text(
                                        text = "Probar conexión",
                                        color = SkyBlueCard,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = {
                            serverUrl = it
                            serverHealthStatus = null
                        },
                        placeholder = { Text("http://10.0.2.2:3000") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = PureWhite),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors,
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { verifyServerConnection(serverUrl) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Probar conexión",
                                        tint = LimeAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { showServerPresets = !showServerPresets },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = if (showServerPresets) {
                                            Icons.Default.KeyboardArrowUp
                                        } else {
                                            Icons.Default.KeyboardArrowDown
                                        },
                                        contentDescription = "Elegir servidor guardado",
                                        tint = TextMutedLight
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    AnimatedVisibility(visible = showServerPresets) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Text(
                                text = "Elegir servidor rápido:",
                                color = TextMutedLight,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val presets = listOf(
                                    "Emulador" to "http://10.0.2.2:3000",
                                    "Localhost" to "http://localhost:3000",
                                    "LAN" to "http://192.168.1.100:3000"
                                )
                                presets.forEach { (label, presetUrl) ->
                                    val selected = SessionPreferences.normalizeBaseUrl(serverUrl) == presetUrl
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(if (selected) RoyalBlue else NavyBackground)
                                            .border(
                                                1.dp,
                                                if (selected) SkyBlueCard else NavyInputBorder,
                                                RoundedCornerShape(50)
                                            )
                                            .clickable {
                                                verifyServerConnection(presetUrl)
                                            }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "$label ($presetUrl)",
                                            color = PureWhite,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Email Field (matching Image 1 & 3)
            Text(
                text = "Email",
                color = TextMutedLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = null
                },
                placeholder = { Text("hola@ejemplo.com") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Username Field (matching Image 1 & 3)
            Text(
                text = "Username",
                color = TextMutedLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    errorMessage = null
                },
                placeholder = { Text("Tu nombre de usuario") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Password Field (matching Image 1 & 3)
            Text(
                text = "Password",
                color = TextMutedLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                placeholder = { Text("••••••••") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(
                            text = if (passwordVisible) "Ocultar" else "Ver",
                            color = TextMutedLight,
                            fontSize = 11.sp
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = ErrorRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = ErrorRed,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (infoMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = SkyBlueCard.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = infoMessage ?: "",
                        color = SkyBlueCard,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Lime Accent CTA Button ("Entrar" / "Crear cuenta")
            Button(
                onClick = {
                    errorMessage = null
                    infoMessage = null
                    sessionPrefs.baseUrl = serverUrl

                    if (isRegisterMode) {
                        if (username.trim().length < 3) {
                            errorMessage = "El nombre de usuario debe tener al menos 3 caracteres."
                            return@Button
                        }
                        if (!email.contains("@")) {
                            errorMessage = "Ingresa un correo electrónico válido."
                            return@Button
                        }
                        if (password.length < 8) {
                            errorMessage = "La contraseña debe tener mínimo 8 caracteres."
                            return@Button
                        }
                        isLoading = true
                        scope.launch {
                            val res = apiClient.register(username.trim(), email.trim(), password)
                            isLoading = false
                            res.onSuccess { auth ->
                                sessionPrefs.token = auth.token
                                sessionPrefs.user = auth.user
                                onAuthenticated(auth)
                            }.onFailure { err ->
                                errorMessage = err.message ?: "No se pudo registrar el usuario"
                            }
                        }
                    } else {
                        val identifier = email.trim().ifEmpty { username.trim() }
                        if (identifier.isEmpty()) {
                            errorMessage = "Ingresa tu email o tu nombre de usuario."
                            return@Button
                        }
                        if (password.isEmpty()) {
                            errorMessage = "Ingresa tu contraseña."
                            return@Button
                        }
                        isLoading = true
                        scope.launch {
                            val res = apiClient.login(identifier, password)
                            isLoading = false
                            res.onSuccess { auth ->
                                sessionPrefs.token = auth.token
                                sessionPrefs.user = auth.user
                                onAuthenticated(auth)
                            }.onFailure { err ->
                                errorMessage = err.message ?: "Error al iniciar sesión"
                            }
                        }
                    }
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LimeAccent,
                    contentColor = TextPrimaryNavy,
                    disabledContainerColor = LimeAccent.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = when {
                        isLoading -> "Conectando..."
                        isRegisterMode -> "Crear cuenta"
                        else -> "Entrar"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryNavy
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Secondary links matching reference image:
            // "¿Olvidaste tu contraseña?"
            // "¿No tienes una cuenta? Crear cuenta"
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isRegisterMode) {
                    Text(
                        text = "¿Olvidaste tu contraseña?",
                        color = TextMutedLight,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                infoMessage = "Contacta al administrador del servidor o inicia sesión con Google OAuth."
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            isRegisterMode = !isRegisterMode
                            errorMessage = null
                            infoMessage = null
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isRegisterMode) "¿Ya tienes una cuenta? " else "¿No tienes una cuenta? ",
                        color = TextMutedLight,
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (isRegisterMode) "Iniciar sesión" else "Crear cuenta",
                        color = LimeAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                TextButton(
                    onClick = { showGoogleDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = SkyBlueCard,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Continuar con Google (ID Token)",
                        color = SkyBlueCard,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(SkyBlueCard.copy(alpha = 0.5f))
                    )
                    Text(
                        text = "UN ESPACIO PARA PENSAR",
                        color = TextMutedLight.copy(alpha = 0.65f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.4.sp,
                        textAlign = TextAlign.Center
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(SkyBlueCard.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }

    if (showGoogleDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleDialog = false },
            title = {
                Text(
                    text = "Acceso con Google OAuth2",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Pega el id_token emitido por Google Identity Services para validarlo contra POST /api/auth/google:",
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = googleIdToken,
                        onValueChange = { googleIdToken = it },
                        placeholder = { Text("eyJhbGciOiJSUzI1NiIs...") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (googleIdToken.isBlank()) return@Button
                        showGoogleDialog = false
                        isLoading = true
                        sessionPrefs.baseUrl = serverUrl
                        scope.launch {
                            val res = apiClient.loginWithGoogle(googleIdToken.trim())
                            isLoading = false
                            res.onSuccess { auth ->
                                sessionPrefs.token = auth.token
                                sessionPrefs.user = auth.user
                                onAuthenticated(auth)
                            }.onFailure { err ->
                                errorMessage = err.message ?: "Error al validar id_token de Google"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalBlue,
                        contentColor = PureWhite
                    )
                ) {
                    Text("Validar Token")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
