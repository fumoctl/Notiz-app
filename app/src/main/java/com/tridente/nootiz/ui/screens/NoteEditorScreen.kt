package com.tridente.nootiz.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.CalendarContract
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tridente.nootiz.data.local.SessionPreferences
import com.tridente.nootiz.data.model.AttachmentMetadata
import com.tridente.nootiz.data.model.Folder
import com.tridente.nootiz.data.model.Note
import com.tridente.nootiz.data.model.NoteReminder
import com.tridente.nootiz.data.model.NoteShareInfo
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
import com.tridente.nootiz.ui.theme.TextPrimaryNavy
import com.tridente.nootiz.ui.theme.TextSecondarySlate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    initialNote: Note?,
    initialFolderId: String?,
    currentUser: User?,
    sessionPrefs: SessionPreferences,
    apiClient: NotizApiClient,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentNote by remember { mutableStateOf(initialNote) }
    var title by remember { mutableStateOf(initialNote?.title ?: "") }
    var body by remember { mutableStateOf(initialNote?.body ?: "") }
    var tags by remember { mutableStateOf(initialNote?.tags ?: emptyList()) }
    var folderId by remember { mutableStateOf(initialNote?.folderId ?: initialFolderId) }

    var folders by remember { mutableStateOf<List<Folder>>(emptyList()) }
    var showFolderMenu by remember { mutableStateOf(false) }

    var attachments by remember { mutableStateOf<List<AttachmentMetadata>>(emptyList()) }
    val attachmentBitmaps = remember { mutableStateMapOf<String, ImageBitmap>() }
    var isUploadingImage by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var toastBannerText by remember { mutableStateOf<String?>(null) }

    // Tag dialog
    var showAddTagDialog by remember { mutableStateOf(false) }
    var newTagInput by remember { mutableStateOf("") }

    // New checklist item dialog
    var showAddChecklistDialog by remember { mutableStateOf(false) }
    var newChecklistText by remember { mutableStateOf("") }

    // Share Sheet State
    var showShareSheet by remember { mutableStateOf(false) }
    var sharesList by remember { mutableStateOf<List<NoteShareInfo>>(emptyList()) }

    // Calendar Reminder Sheet State
    var showReminderSheet by remember { mutableStateOf(false) }
    var activeReminder by remember {
        mutableStateOf(initialNote?.id?.let { sessionPrefs.getReminderForNote(it) })
    }

    val canEdit = currentNote?.canEdit ?: true
    val isOwner = currentNote?.isOwner ?: true

    fun showToastBanner(message: String) {
        toastBannerText = message
        scope.launch {
            delay(3200)
            if (toastBannerText == message) {
                toastBannerText = null
            }
        }
    }

    fun loadAttachments(noteId: String) {
        scope.launch {
            apiClient.getAttachments(noteId).onSuccess { list ->
                attachments = list
                list.forEach { meta ->
                    if (!attachmentBitmaps.containsKey(meta.id)) {
                        apiClient.downloadAttachmentBytes(noteId, meta.id).onSuccess { bytes ->
                            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (bmp != null) {
                                attachmentBitmaps[meta.id] = bmp.asImageBitmap()
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        apiClient.getFolders().onSuccess { folders = it }
        currentNote?.id?.let { id ->
            apiClient.getNote(id).onSuccess { fresh ->
                currentNote = fresh
                title = fresh.title
                body = fresh.body
                tags = fresh.tags
                folderId = fresh.folderId
            }
            loadAttachments(id)
        }
    }

    suspend fun saveNoteInternal(silent: Boolean = false): Note? {
        if (!canEdit) return currentNote
        val effectiveTitle = title.trim().ifEmpty { "Ideas para el proyecto" }
        isSaving = true
        errorMessage = null
        val existing = currentNote
        val result = if (existing == null) {
            apiClient.createNote(
                title = effectiveTitle,
                body = body,
                tags = tags,
                folderId = folderId
            )
        } else {
            apiClient.updateNote(
                noteId = existing.id,
                title = effectiveTitle,
                body = body,
                tags = tags,
                updateFolder = isOwner,
                folderId = folderId
            )
        }
        isSaving = false
        return result.fold(
            onSuccess = { saved ->
                currentNote = saved
                title = saved.title
                body = saved.body
                tags = saved.tags
                folderId = saved.folderId
                if (!silent) showToastBanner("Nota guardada")
                saved
            },
            onFailure = { err ->
                errorMessage = err.message ?: "Error al guardar la nota"
                null
            }
        )
    }

    // Image Picker Launcher for BLOB Upload (POST /api/notes/{id}/attachments)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ensuredNote = currentNote ?: saveNoteInternal(silent = true) ?: return@launch
            isUploadingImage = true
            errorMessage = null
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "image/png"
                val fileName = queryFileName(context, uri) ?: "imagen_${System.currentTimeMillis()}.png"
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    val uploadRes = apiClient.uploadAttachment(
                        noteId = ensuredNote.id,
                        filename = fileName,
                        contentType = mimeType,
                        bytes = bytes
                    )
                    uploadRes.onSuccess { meta ->
                        attachments = attachments + meta
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bmp != null) {
                            attachmentBitmaps[meta.id] = bmp.asImageBitmap()
                        }
                        showToastBanner("Imagen adjuntada")
                    }.onFailure { err ->
                        errorMessage = err.message ?: "Error al subir imagen"
                    }
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "No se pudo leer la imagen"
            } finally {
                isUploadingImage = false
            }
        }
    }

    // Parse interactive checklist lines (- [ ] or - [x]) from body
    val checklistItems = remember(body) {
        body.lines().mapIndexedNotNull { idx, line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("- [ ] ") -> Triple(idx, false, trimmed.removePrefix("- [ ] "))
                trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ") ->
                    Triple(idx, true, trimmed.substring(6))
                else -> null
            }
        }
    }

    Scaffold(
        containerColor = OffWhiteBackground,
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dark Navy Confirmation Toast ("Nota compartida" matching Image 2 & Image 4)
                AnimatedVisibility(
                    visible = toastBannerText != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        color = NavyToast,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = LimeAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = toastBannerText ?: "",
                                color = PureWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Bottom Formatting Bar (matching Image 1, 3, 4: H, T, Checklist, Image, Save)
                Surface(
                    color = PureWhite,
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, IceBlueCard, RoundedCornerShape(22.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // H (Heading button)
                            EditorToolPill(
                                text = "H",
                                active = false,
                                enabled = canEdit,
                                onClick = {
                                    val prefix = if (body.isEmpty() || body.endsWith("\n")) "## " else "\n\n## "
                                    body += "${prefix}Tres prioridades\n"
                                }
                            )

                            // T (Subtext / Bold button)
                            EditorToolPill(
                                text = "T",
                                active = true,
                                enabled = canEdit,
                                onClick = {
                                    val prefix = if (body.isEmpty() || body.endsWith("\n")) "" else "\n"
                                    body += "${prefix}Lo esencial: "
                                }
                            )

                            // Checklist item button
                            IconButton(
                                onClick = {
                                    newChecklistText = ""
                                    showAddChecklistDialog = true
                                },
                                enabled = canEdit,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(IceBlueSurface)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = "Añadir tarea",
                                    tint = RoyalBlue,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            // Image Attachment button
                            EditorToolPill(
                                text = "IMG",
                                active = false,
                                enabled = canEdit && !isUploadingImage,
                                onClick = { imagePickerLauncher.launch("image/*") }
                            )
                        }

                        // Save CTA Pill
                        Button(
                            onClick = {
                                scope.launch { saveNoteInternal(silent = false) }
                            },
                            enabled = canEdit && !isSaving,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LimeAccent,
                                contentColor = TextPrimaryNavy
                            ),
                            contentPadding = PaddingValues(
                                horizontal = 16.dp,
                                vertical = 8.dp
                            ),
                            modifier = Modifier.height(38.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = TextPrimaryNavy
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Guardar",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Top Navigation & Actions Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        scope.launch {
                            if (canEdit && (title.isNotBlank() || body.isNotBlank())) {
                                saveNoteInternal(silent = true)
                            }
                            onBack()
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(IceBlueSurface)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = TextPrimaryNavy
                    )
                }

                // Folder selector pill (only Owner can change folder_id per 4.2 Permission Matrix)
                Box {
                    val currentFolderName = folders.find { it.id == folderId }?.name ?: "Sin carpeta"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(IceBlueSurface)
                            .clickable(enabled = isOwner) { showFolderMenu = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = currentFolderName,
                            color = TextPrimaryNavy,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    DropdownMenu(
                        expanded = showFolderMenu,
                        onDismissRequest = { showFolderMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sin carpeta") },
                            onClick = {
                                folderId = null
                                showFolderMenu = false
                            }
                        )
                        folders.forEach { folder ->
                            DropdownMenuItem(
                                text = { Text(folder.name) },
                                onClick = {
                                    folderId = folder.id
                                    showFolderMenu = false
                                }
                            )
                        }
                    }
                }

                // Right Action Icons: Reminder, Share, Delete
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Calendar Reminder Button
                    IconButton(
                        onClick = {
                            scope.launch {
                                if (currentNote == null) {
                                    saveNoteInternal(silent = true)
                                }
                                showReminderSheet = true
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (activeReminder != null) LimeAccent else IceBlueSurface)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Recordatorio",
                            tint = TextPrimaryNavy,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Share Note Button (Owner can manage shares)
                    if (isOwner) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val ensured = currentNote ?: saveNoteInternal(silent = true)
                                    if (ensured != null) {
                                        apiClient.getNoteShares(ensured.id).onSuccess {
                                            sharesList = it
                                        }
                                        showShareSheet = true
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(IceBlueSurface)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartir nota",
                                tint = TextPrimaryNavy,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // Delete (owner) or Leave shared note (shared user)
                    if (currentNote != null) {
                        IconButton(
                            onClick = {
                                val noteId = currentNote?.id ?: return@IconButton
                                scope.launch {
                                    if (isOwner) {
                                        apiClient.deleteNote(noteId).onSuccess { onBack() }
                                            .onFailure { errorMessage = it.message }
                                    } else {
                                        val myId = currentUser?.id ?: return@launch
                                        apiClient.revokeNoteShare(noteId, myId).onSuccess { onBack() }
                                            .onFailure { errorMessage = it.message }
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(IceBlueSurface)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = if (isOwner) "Eliminar nota" else "Salir de nota compartida",
                                tint = ErrorRedDark,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (errorMessage != null) {
                Surface(
                    color = ErrorRedDark.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = ErrorRedDark,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Title & Time Row (matching Image 1 & Image 3: "Ideas para el lunes" + "10:24")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (title.isEmpty()) {
                        Text(
                            text = "Ideas para el lunes",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextSecondarySlate.copy(alpha = 0.45f)
                        )
                    }
                    BasicTextField(
                        value = title,
                        onValueChange = { if (canEdit) title = it },
                        readOnly = !canEdit,
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            color = TextPrimaryNavy
                        ),
                        cursorBrush = SolidColor(RoyalBlue),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = formatNoteTime(currentNote?.updatedAt ?: ""),
                    color = TextSecondarySlate,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tags Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tags.forEach { tag ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(IceBlueChip)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "#$tag",
                            color = RoyalBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (canEdit) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Quitar etiqueta",
                                tint = RoyalBlue,
                                modifier = Modifier
                                    .size(13.dp)
                                    .clickable { tags = tags - tag }
                            )
                        }
                    }
                }

                if (canEdit) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(IceBlueSurface)
                            .clickable {
                                newTagInput = ""
                                showAddTagDialog = true
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = TextSecondarySlate,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Etiqueta",
                            color = TextSecondarySlate,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Active Reminder Badge if set
            activeReminder?.let { rem ->
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(IceBlueChip)
                        .clickable { showReminderSheet = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = RoyalBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recordatorio: ${rem.dayOfMonth}/${rem.month}/${rem.year} a las %02d:%02d".format(rem.hour, rem.minute),
                        color = TextPrimaryNavy,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Markdown Body Editor
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp)
            ) {
                if (body.isEmpty()) {
                    Text(
                        text = "Una semana con menos ruido y más foco.\nEmpezar por lo importante, dejar espacio para lo inesperado.\n\n## Tres prioridades\n- [ ] Revisar la propuesta de diseño\n- [ ] Compartir ideas con el equipo\n- [ ] Reservar tiempo para crear",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondarySlate.copy(alpha = 0.45f)
                    )
                }
                BasicTextField(
                    value = body,
                    onValueChange = { if (canEdit) body = it },
                    readOnly = !canEdit,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = TextPrimaryNavy,
                        lineHeight = 24.sp
                    ),
                    cursorBrush = SolidColor(RoyalBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp)
                )
            }

            // Interactive Priority Checklist Section (matching Image 3 & Image 4 "Tres prioridades")
            if (checklistItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = PureWhite,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, IceBlueCard, RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Tres prioridades / Tareas",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimaryNavy
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        checklistItems.forEach { (lineIndex, isChecked, itemText) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(enabled = canEdit) {
                                        val lines = body.lines().toMutableList()
                                        if (lineIndex in lines.indices) {
                                            lines[lineIndex] = if (isChecked) {
                                                "- [ ] $itemText"
                                            } else {
                                                "- [x] $itemText"
                                            }
                                            body = lines.joinToString("\n")
                                        }
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (!canEdit) return@Checkbox
                                        val lines = body.lines().toMutableList()
                                        if (lineIndex in lines.indices) {
                                            lines[lineIndex] = if (checked) {
                                                "- [x] $itemText"
                                            } else {
                                                "- [ ] $itemText"
                                            }
                                            body = lines.joinToString("\n")
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = RoyalBlue)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = itemText,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    color = if (isChecked) TextSecondarySlate else TextPrimaryNavy
                                )
                            }
                        }
                    }
                }
            }

            // Attached BLOB Images Section (GET /api/notes/{id}/attachments)
            if (attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Imágenes adjuntas (${attachments.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimaryNavy
                )
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    attachments.forEach { att ->
                        Surface(
                            color = PureWhite,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, IceBlueCard, RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                val bmp = attachmentBitmaps[att.id]
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp,
                                        contentDescription = att.filename,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 140.dp, max = 240.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = att.filename,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimaryNavy,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${att.contentType} · ${att.sizeBytes / 1024} KB",
                                            fontSize = 11.sp,
                                            color = TextSecondarySlate
                                        )
                                    }
                                    if (canEdit) {
                                        IconButton(
                                            onClick = {
                                                val noteId = currentNote?.id ?: return@IconButton
                                                scope.launch {
                                                    apiClient.deleteAttachment(noteId, att.id).onSuccess {
                                                        attachments = attachments.filterNot { it.id == att.id }
                                                        attachmentBitmaps.remove(att.id)
                                                        showToastBanner("Imagen eliminada")
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Eliminar adjunto",
                                                tint = ErrorRedDark
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Add Tag Dialog
    if (showAddTagDialog) {
        AlertDialog(
            onDismissRequest = { showAddTagDialog = false },
            title = { Text("Añadir etiqueta", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    placeholder = { Text("Ej. rust, diseño, ideas") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val normalized = newTagInput.trim().lowercase().removePrefix("#")
                        if (normalized.isNotEmpty() && !tags.contains(normalized)) {
                            tags = tags + normalized
                        }
                        showAddTagDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                ) {
                    Text("Añadir", color = PureWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTagDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Add Checklist Item Dialog
    if (showAddChecklistDialog) {
        AlertDialog(
            onDismissRequest = { showAddChecklistDialog = false },
            title = { Text("Nueva prioridad / tarea", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newChecklistText,
                    onValueChange = { newChecklistText = it },
                    placeholder = { Text("Ej. Revisar la propuesta de diseño") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = newChecklistText.trim()
                        if (clean.isNotEmpty()) {
                            val sep = if (body.isEmpty() || body.endsWith("\n")) "" else "\n"
                            body += "$sep- [ ] $clean"
                        }
                        showAddChecklistDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                ) {
                    Text("Agregar", color = PureWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddChecklistDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // SHARE BOTTOM SHEET (matching Image 2 & Image 4 "Compartir nota")
    if (showShareSheet && currentNote != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var recipientInput by remember { mutableStateOf("") }
        var selectedPermission by remember { mutableStateOf("read") }
        var shareError by remember { mutableStateOf<String?>(null) }
        var isSharing by remember { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = { showShareSheet = false },
            sheetState = sheetState,
            containerColor = OffWhiteBackground,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp)
            ) {
                Text(
                    text = "Compartir nota",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimaryNavy
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Una buena idea merece compartirse",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondarySlate
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Note Preview Pill (matching Image 4 "Ideas para el lunes")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(IceBlueSurface)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = RoyalBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title.ifBlank { "Ideas para el lunes" },
                        color = TextPrimaryNavy,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Email o nombre de usuario",
                    color = TextPrimaryNavy,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = recipientInput,
                    onValueChange = {
                        recipientInput = it
                        shareError = null
                    },
                    placeholder = { Text("lucia@ejemplo.com") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = PureWhite,
                        unfocusedContainerColor = PureWhite,
                        focusedBorderColor = RoyalBlue,
                        unfocusedBorderColor = IceBlueCard
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Permission Selector ("read" vs "edit")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isRead = selectedPermission == "read"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isRead) RoyalBlue else IceBlueSurface)
                            .clickable { selectedPermission = "read" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Solo lectura (read)",
                            color = if (isRead) PureWhite else TextPrimaryNavy,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!isRead) RoyalBlue else IceBlueSurface)
                            .clickable { selectedPermission = "edit" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Puede editar (edit)",
                            color = if (!isRead) PureWhite else TextPrimaryNavy,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (shareError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = shareError ?: "",
                        color = ErrorRedDark,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Lime Green "Compartir" CTA Button (matching Image 2 & Image 4)
                Button(
                    onClick = {
                        val noteId = currentNote?.id ?: return@Button
                        if (recipientInput.isBlank()) {
                            shareError = "Ingresa el correo o username del destinatario."
                            return@Button
                        }
                        isSharing = true
                        shareError = null
                        scope.launch {
                            val res = apiClient.shareNote(
                                noteId = noteId,
                                recipient = recipientInput.trim(),
                                permission = selectedPermission
                            )
                            isSharing = false
                            res.onSuccess { info ->
                                sharesList = sharesList.filterNot {
                                    it.sharedWithUserId == info.sharedWithUserId
                                } + info
                                recipientInput = ""
                                showShareSheet = false
                                showToastBanner("Nota compartida")
                            }.onFailure { err ->
                                shareError = err.message ?: "No se pudo compartir la nota"
                            }
                        }
                    },
                    enabled = !isSharing,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LimeAccent,
                        contentColor = TextPrimaryNavy
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isSharing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = TextPrimaryNavy
                        )
                    } else {
                        Text(
                            text = "Compartir",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (sharesList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = IceBlueCard)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Usuarios con acceso",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryNavy
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    sharesList.forEach { share ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = RoyalBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${share.username} (${share.email})",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimaryNavy
                                    )
                                    Text(
                                        text = if (share.permission == "edit") "Permiso: Edición" else "Permiso: Lectura",
                                        fontSize = 11.sp,
                                        color = TextSecondarySlate
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    val noteId = currentNote?.id ?: return@IconButton
                                    scope.launch {
                                        apiClient.revokeNoteShare(noteId, share.sharedWithUserId).onSuccess {
                                            sharesList = sharesList.filterNot {
                                                it.sharedWithUserId == share.sharedWithUserId
                                            }
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Revocar acceso",
                                    tint = ErrorRedDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // CALENDAR REMINDER BOTTOM SHEET (matching Image 2 & Image 4 "Recordatorio")
    if (showReminderSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val today = remember { LocalDate.now() }
        var displayedYearMonth by remember {
            mutableStateOf(
                activeReminder?.let { YearMonth.of(it.year, it.month) } ?: YearMonth.of(today.year, today.monthValue)
            )
        }
        var selectedDay by remember {
            mutableIntStateOf(activeReminder?.dayOfMonth ?: today.dayOfMonth)
        }
        var selectedHour by remember { mutableIntStateOf(activeReminder?.hour ?: 10) }
        var selectedMinute by remember { mutableIntStateOf(activeReminder?.minute ?: 0) }
        var localAlertChecked by remember { mutableStateOf(activeReminder?.localAlert ?: true) }
        var googleCalendarChecked by remember { mutableStateOf(activeReminder?.googleCalendar ?: false) }

        ModalBottomSheet(
            onDismissRequest = { showReminderSheet = false },
            sheetState = sheetState,
            containerColor = OffWhiteBackground,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp)
            ) {
                Text(
                    text = "Recordatorio",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimaryNavy
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Vuelve a esta idea en el momento justo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondarySlate
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Month Header ("Octubre 2026")
                val monthNames = listOf(
                    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${monthNames[displayedYearMonth.monthValue - 1]} ${displayedYearMonth.year}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimaryNavy
                    )
                    Row {
                        IconButton(
                            onClick = {
                                displayedYearMonth = displayedYearMonth.minusMonths(1)
                                if (selectedDay > displayedYearMonth.lengthOfMonth()) {
                                    selectedDay = displayedYearMonth.lengthOfMonth()
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Mes anterior",
                                tint = TextPrimaryNavy
                            )
                        }
                        IconButton(
                            onClick = {
                                displayedYearMonth = displayedYearMonth.plusMonths(1)
                                if (selectedDay > displayedYearMonth.lengthOfMonth()) {
                                    selectedDay = displayedYearMonth.lengthOfMonth()
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Mes siguiente",
                                tint = TextPrimaryNavy
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Days of week header (L M X J V S D)
                val weekDays = listOf("L", "M", "X", "J", "V", "S", "D")
                Row(modifier = Modifier.fillMaxWidth()) {
                    weekDays.forEach { d ->
                        Text(
                            text = d,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondarySlate
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Calendar Grid
                val firstDayOfWeek = displayedYearMonth.atDay(1).dayOfWeek.value // 1 (Mon) .. 7 (Sun)
                val daysInMonth = displayedYearMonth.lengthOfMonth()
                val totalCells = ((firstDayOfWeek - 1 + daysInMonth + 6) / 7) * 7

                for (week in 0 until (totalCells / 7)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        for (dow in 0 until 7) {
                            val cellIndex = week * 7 + dow
                            val dayNum = cellIndex - (firstDayOfWeek - 1) + 1
                            if (dayNum in 1..daysInMonth) {
                                val isSelected = dayNum == selectedDay
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .padding(horizontal = 2.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) RoyalBlue else Color.Transparent)
                                        .clickable { selectedDay = dayNum },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNum.toString(),
                                        color = if (isSelected) PureWhite else TextPrimaryNavy,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f).height(34.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Selected Date Summary Pill (matching Image 4 "Viernes, 16 de octubre · 10:00")
                Surface(
                    color = IceBlueChip,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(
                            text = "$selectedDay de ${monthNames[displayedYearMonth.monthValue - 1].lowercase()} · %02d:%02d".format(
                                selectedHour,
                                selectedMinute
                            ),
                            color = TextPrimaryNavy,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            listOf(9 to 0, 10 to 0, 12 to 30, 16 to 0, 19 to 0).forEach { (h, m) ->
                                val timeSelected = selectedHour == h && selectedMinute == m
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(if (timeSelected) RoyalBlue else PureWhite)
                                        .clickable {
                                            selectedHour = h
                                            selectedMinute = m
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "%02d:%02d".format(h, m),
                                        color = if (timeSelected) PureWhite else TextPrimaryNavy,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Option 1: Alerta local (matching Image 4)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PureWhite)
                        .clickable { localAlertChecked = !localAlertChecked }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Checkbox(
                        checked = localAlertChecked,
                        onCheckedChange = { localAlertChecked = it },
                        colors = CheckboxDefaults.colors(checkedColor = RoyalBlue)
                    )
                    Text(
                        text = "Alerta local",
                        color = TextPrimaryNavy,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Option 2: Google Calendar (matching Image 4)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PureWhite)
                        .clickable { googleCalendarChecked = !googleCalendarChecked }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Checkbox(
                        checked = googleCalendarChecked,
                        onCheckedChange = { googleCalendarChecked = it },
                        colors = CheckboxDefaults.colors(checkedColor = RoyalBlue)
                    )
                    Text(
                        text = "Google Calendar",
                        color = TextPrimaryNavy,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Lime Green CTA "Guardar recordatorio" (matching Image 4)
                Button(
                    onClick = {
                        val noteId = currentNote?.id ?: "temp"
                        val rem = NoteReminder(
                            noteId = noteId,
                            noteTitle = title.ifBlank { "Ideas para el lunes" },
                            year = displayedYearMonth.year,
                            month = displayedYearMonth.monthValue,
                            dayOfMonth = selectedDay,
                            hour = selectedHour,
                            minute = selectedMinute,
                            localAlert = localAlertChecked,
                            googleCalendar = googleCalendarChecked
                        )
                        sessionPrefs.saveReminder(rem)
                        activeReminder = rem
                        showReminderSheet = false
                        showToastBanner("Recordatorio guardado")

                        if (googleCalendarChecked) {
                            openGoogleCalendarIntent(context, rem, body)
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LimeAccent,
                        contentColor = TextPrimaryNavy
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = "Guardar recordatorio",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorToolPill(
    text: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) IceBlueChip else IceBlueSurface)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = TextPrimaryNavy,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun queryFileName(context: Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return null
    return cursor.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (it.moveToFirst() && nameIndex >= 0) {
            it.getString(nameIndex)
        } else {
            null
        }
    }
}

private fun openGoogleCalendarIntent(context: Context, reminder: NoteReminder, noteBody: String) {
    try {
        val beginTime = Calendar.getInstance().apply {
            set(reminder.year, reminder.month - 1, reminder.dayOfMonth, reminder.hour, reminder.minute)
        }
        val endTime = Calendar.getInstance().apply {
            timeInMillis = beginTime.timeInMillis + 60 * 60 * 1000L
        }
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime.timeInMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime.timeInMillis)
            putExtra(CalendarContract.Events.TITLE, reminder.noteTitle)
            putExtra(CalendarContract.Events.DESCRIPTION, noteBody)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}
