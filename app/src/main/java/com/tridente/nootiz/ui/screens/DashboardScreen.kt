package com.tridente.nootiz.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tridente.nootiz.data.model.Folder
import com.tridente.nootiz.data.model.Note
import com.tridente.nootiz.data.model.User
import com.tridente.nootiz.data.remote.NotizApiClient
import com.tridente.nootiz.ui.theme.ErrorRedDark
import com.tridente.nootiz.ui.theme.IceBlueCard
import com.tridente.nootiz.ui.theme.IceBlueChip
import com.tridente.nootiz.ui.theme.IceBlueSurface
import com.tridente.nootiz.ui.theme.LimeAccent
import com.tridente.nootiz.ui.theme.NavyToast
import com.tridente.nootiz.ui.theme.OffWhiteBackground
import com.tridente.nootiz.ui.theme.PureWhite
import com.tridente.nootiz.ui.theme.RoyalBlue
import com.tridente.nootiz.ui.theme.SkyBlueCard
import com.tridente.nootiz.ui.theme.TextPrimaryNavy
import com.tridente.nootiz.ui.theme.TextSecondarySlate
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    currentUser: User?,
    serverUrl: String,
    apiClient: NotizApiClient,
    onOpenNote: (Note) -> Unit,
    onCreateNewNote: (selectedFolderId: String?) -> Unit,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var folders by remember { mutableStateOf<List<Folder>>(emptyList()) }
    var notes by remember { mutableStateOf<List<Note>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var sharedOnlyFilter by remember { mutableStateOf(false) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Folder dialogs
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var folderToEdit by remember { mutableStateOf<Folder?>(null) }
    var editFolderName by remember { mutableStateOf("") }

    fun loadData() {
        isLoading = true
        errorMessage = null
        scope.launch {
            val foldersRes = apiClient.getFolders()
            foldersRes.onSuccess { folders = it }

            val notesRes = apiClient.getNotes(
                folderId = if (sharedOnlyFilter) null else selectedFolderId,
                tag = selectedTag,
                sharedOnly = sharedOnlyFilter
            )
            isLoading = false
            notesRes.onSuccess {
                notes = it
            }.onFailure { err ->
                errorMessage = err.message ?: "No se pudieron cargar las notas"
            }
        }
    }

    LaunchedEffect(selectedFolderId, sharedOnlyFilter, selectedTag) {
        loadData()
    }

    val allAvailableTags = remember(notes) {
        notes.flatMap { it.tags }.distinct().sorted()
    }

    val filteredNotes = remember(notes, searchQuery) {
        if (searchQuery.isBlank()) {
            notes
        } else {
            val q = searchQuery.trim().lowercase()
            notes.filter { n ->
                n.title.lowercase().contains(q) ||
                    n.body.lowercase().contains(q) ||
                    n.tags.any { t -> t.lowercase().contains(q) }
            }
        }
    }

    Scaffold(
        containerColor = OffWhiteBackground,
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onCreateNewNote(selectedFolderId) },
                containerColor = RoyalBlue,
                contentColor = PureWhite,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.size(58.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Nueva nota",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 22.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // Top User Pill & Actions Row (matching Image 3 "Ana")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(IceBlueSurface)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(RoyalBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (currentUser?.username?.firstOrNull()?.uppercaseChar() ?: 'U').toString(),
                            color = PureWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentUser?.username ?: "Usuario",
                        color = TextPrimaryNavy,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { loadData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar",
                            tint = TextSecondarySlate
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión",
                            tint = TextSecondarySlate
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Dashboard Headline (matching Image 1 & Image 3)
            Text(
                text = "Tu espacio de ideas\nen orden. Todo contigo.",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimaryNavy
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar ("Buscar en tus notas" / "Buscar una nota")
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Buscar en tus notas",
                        color = TextSecondarySlate.copy(alpha = 0.75f),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = TextSecondarySlate
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Limpiar búsqueda",
                                tint = TextSecondarySlate
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = IceBlueSurface,
                    unfocusedContainerColor = IceBlueSurface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = TextPrimaryNavy,
                    unfocusedTextColor = TextPrimaryNavy
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Folder & Shared Filter Chips Row ("Mis Notas", "Trabajo", etc.)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "Mis Notas" (All)
                val allSelected = selectedFolderId == null && !sharedOnlyFilter
                FilterPill(
                    label = "Mis Notas",
                    selected = allSelected,
                    onClick = {
                        selectedFolderId = null
                        sharedOnlyFilter = false
                    }
                )

                // Dynamic Folders from GET /api/folders
                folders.forEach { folder ->
                    val isSelected = selectedFolderId == folder.id && !sharedOnlyFilter
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) RoyalBlue else PureWhite)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) RoyalBlue else IceBlueCard,
                                shape = RoundedCornerShape(50)
                            )
                            .clickable {
                                sharedOnlyFilter = false
                                selectedFolderId = if (isSelected) null else folder.id
                            }
                            .padding(
                                start = 14.dp,
                                end = if (isSelected) 8.dp else 14.dp,
                                top = 8.dp,
                                bottom = 8.dp
                            )
                    ) {
                        Text(
                            text = folder.name,
                            color = if (isSelected) PureWhite else TextPrimaryNavy,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar carpeta",
                                tint = PureWhite.copy(alpha = 0.9f),
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable {
                                        folderToEdit = folder
                                        editFolderName = folder.name
                                    }
                            )
                        }
                    }
                }

                // "Compartidas" filter chip (shared_only=true)
                FilterPill(
                    label = "Compartidas",
                    selected = sharedOnlyFilter,
                    onClick = {
                        sharedOnlyFilter = !sharedOnlyFilter
                        if (sharedOnlyFilter) selectedFolderId = null
                    }
                )

                // "+ Carpeta" button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(IceBlueSurface)
                        .clickable {
                            newFolderName = ""
                            showCreateFolderDialog = true
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Nueva carpeta",
                        tint = RoyalBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Carpeta",
                        color = RoyalBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Optional Tag Filter Row
            if (allAvailableTags.isNotEmpty() || selectedTag != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tagsToShow = if (selectedTag != null && !allAvailableTags.contains(selectedTag)) {
                        listOf(selectedTag!!) + allAvailableTags
                    } else {
                        allAvailableTags
                    }
                    tagsToShow.forEach { tag ->
                        val isTagSelected = selectedTag == tag
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isTagSelected) NavyToast else IceBlueChip)
                                .clickable {
                                    selectedTag = if (isTagSelected) null else tag
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                color = if (isTagSelected) PureWhite else TextPrimaryNavy,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (errorMessage != null) {
                Surface(
                    color = ErrorRedDark.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = ErrorRedDark,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Notes List
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = RoyalBlue)
                }
            } else if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No se encontraron notas para \"$searchQuery\""
                            else "Aún no hay notas aquí",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryNavy
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Toca el botón + para capturar tu primera idea.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondarySlate
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(filteredNotes, key = { _, n -> n.id }) { index, note ->
                        val folderName = folders.find { it.id == note.folderId }?.name
                        NoteDashboardCard(
                            note = note,
                            isFeatured = index == 0,
                            folderName = folderName,
                            onClick = { onOpenNote(note) }
                        )
                    }
                }
            }
        }
    }

    // Create Folder Dialog
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Nueva carpeta", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    placeholder = { Text("Ej. Trabajo, Proyectos...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = newFolderName.trim()
                        if (clean.isEmpty()) return@Button
                        showCreateFolderDialog = false
                        scope.launch {
                            apiClient.createFolder(clean).onSuccess { created ->
                                folders = (folders + created).sortedBy { it.name.lowercase() }
                                selectedFolderId = created.id
                            }.onFailure { err ->
                                errorMessage = err.message
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                ) {
                    Text("Crear", color = PureWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Edit / Delete Folder Dialog
    folderToEdit?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderToEdit = null },
            title = { Text("Gestionar carpeta", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editFolderName,
                        onValueChange = { editFolderName = it },
                        label = { Text("Nombre de la carpeta") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = {
                            folderToEdit = null
                            scope.launch {
                                apiClient.deleteFolder(folder.id).onSuccess {
                                    if (selectedFolderId == folder.id) selectedFolderId = null
                                    loadData()
                                }.onFailure { err ->
                                    errorMessage = err.message
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = ErrorRedDark
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Eliminar carpeta (conserva sus notas)", color = ErrorRedDark)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = editFolderName.trim()
                        if (clean.isEmpty()) return@Button
                        folderToEdit = null
                        scope.launch {
                            apiClient.renameFolder(folder.id, clean).onSuccess {
                                loadData()
                            }.onFailure { err ->
                                errorMessage = err.message
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                ) {
                    Text("Guardar", color = PureWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToEdit = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) RoyalBlue else PureWhite)
            .border(
                width = 1.dp,
                color = if (selected) RoyalBlue else IceBlueCard,
                shape = RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (selected) PureWhite else TextPrimaryNavy,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun NoteDashboardCard(
    note: Note,
    isFeatured: Boolean,
    folderName: String?,
    onClick: () -> Unit
) {
    val cardBg = if (isFeatured) SkyBlueCard else IceBlueCard
    val cleanPreview = remember(note.body) {
        note.body
            .lines()
            .map { line ->
                line.trim()
                    .removePrefix("### ")
                    .removePrefix("## ")
                    .removePrefix("# ")
                    .removePrefix("- [ ] ")
                    .removePrefix("- [x] ")
                    .removePrefix("- [X] ")
                    .removePrefix("- ")
                    .replace("**", "")
            }
            .filter { it.isNotBlank() && !it.startsWith("![") }
            .joinToString(" ")
            .ifBlank { "Sin contenido adicional" }
    }

    Surface(
        color = cardBg,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title.ifBlank { "Sin título" },
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimaryNavy,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = formatNoteTime(note.updatedAt),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimaryNavy.copy(alpha = 0.65f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = cleanPreview,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimaryNavy.copy(alpha = 0.82f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (folderName != null || note.tags.isNotEmpty() || !note.isOwner) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!note.isOwner) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(NavyToast)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = LimeAccent,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "@${note.ownerUsername} (${if (note.accessRole == "edit") "Edición" else "Lectura"})",
                                color = PureWhite,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (folderName != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(PureWhite.copy(alpha = 0.65f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = folderName,
                                color = TextPrimaryNavy,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    note.tags.take(3).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(PureWhite.copy(alpha = 0.55f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                color = TextPrimaryNavy,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

fun formatNoteTime(isoTimestamp: String): String {
    if (isoTimestamp.isBlank()) return "10:24"
    val tIndex = isoTimestamp.indexOf('T')
    if (tIndex != -1 && isoTimestamp.length >= tIndex + 6) {
        return isoTimestamp.substring(tIndex + 1, tIndex + 6)
    }
    return isoTimestamp.take(10)
}
