package com.example.ui.settings

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.VideoLibraryApp
import com.example.data.backup.BackupManager
import com.example.data.backup.ImportMode
import com.example.extension.ExtensionManager
import com.example.ui.downloads.formatBytes
import com.example.ui.theme.ElectricViolet
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val userPrefs = remember { VideoLibraryApp.instance.userPreferences }
    val repository = remember { VideoLibraryApp.instance.videoRepository }
    val scope = rememberCoroutineScope()

    val themeMode by userPrefs.themeMode.collectAsState()
    val viewMode by userPrefs.viewMode.collectAsState()
    val skipInterval by userPrefs.skipIntervalSec.collectAsState()
    val playbackSpeed by userPrefs.playbackSpeed.collectAsState()
    val autoResume by userPrefs.autoResume.collectAsState()
    val gestures by userPrefs.gestureControls.collectAsState()

    val extensions by ExtensionManager.extensions.collectAsState()

    var showClearDataDialog by remember { mutableStateOf(false) }

    var showImportDialog by remember { mutableStateOf(false) }
    var selectedImportUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImportMode by remember { mutableStateOf(ImportMode.MERGE) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = BackupManager.exportToJson(context, repository, uri)
                if (result.isSuccess) {
                    Toast.makeText(context, "Exported ${result.getOrNull()} videos successfully", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Export failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedImportUri = uri
            showImportDialog = true
        }
    }

    fun calculateCacheSize(): Long {
        return (context.cacheDir.walkTopDown().sumOf { it.length() } +
                (context.externalCacheDir?.walkTopDown()?.sumOf { it.length() } ?: 0L))
    }

    var cacheSize by remember { mutableStateOf(calculateCacheSize()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Appearance Section
            SettingsSectionTitle(title = "Appearance", icon = Icons.Default.DarkMode)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("App Theme", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionChip(
                            title = "Dark",
                            isSelected = themeMode == "dark",
                            onClick = { userPrefs.setThemeMode("dark") },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionChip(
                            title = "Light",
                            isSelected = themeMode == "light",
                            onClick = { userPrefs.setThemeMode("light") },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionChip(
                            title = "System",
                            isSelected = themeMode == "system",
                            onClick = { userPrefs.setThemeMode("system") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Default View Style", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            Text(if (viewMode == "grid") "Grid with poster cards" else "Compact list view", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row {
                            ThemeOptionChip(title = "Grid", isSelected = viewMode == "grid", onClick = { userPrefs.setViewMode("grid") })
                            Spacer(modifier = Modifier.width(6.dp))
                            ThemeOptionChip(title = "List", isSelected = viewMode == "list", onClick = { userPrefs.setViewMode("list") })
                        }
                    }
                }
            }

            // Playback Section
            SettingsSectionTitle(title = "Playback & Controls", icon = Icons.Default.PlayCircle)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Skip Duration", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(5, 10, 15, 30).forEach { sec ->
                            ThemeOptionChip(
                                title = "${sec}s",
                                isSelected = skipInterval == sec,
                                onClick = { userPrefs.setSkipInterval(sec) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto Resume Playback", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            Text("Instantly jump back to where you left off", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = autoResume, onCheckedChange = { userPrefs.setAutoResume(it) })
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Touch Gestures", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            Text("Smooth swipe for volume & brightness, double tap to seek", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = gestures, onCheckedChange = { userPrefs.setGestureControls(it) })
                    }
                }
            }

            // Backup & Data Export / Import Section
            SettingsSectionTitle(title = "Backup & Data Export", icon = Icons.Default.Backup)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Database Backup", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    Text(
                        "Export your entire video library to a standard JSON backup or restore from a previous backup file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                exportLauncher.launch("video-library-backup-${System.currentTimeMillis()}.json")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                        ) {
                            Text("Export JSON")
                        }

                        OutlinedButton(
                            onClick = {
                                importLauncher.launch(arrayOf("application/json", "text/*"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Import JSON")
                        }
                    }
                }
            }

            // Extension Manager Section
            SettingsSectionTitle(title = "Extension Modules", icon = Icons.Default.Extension)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Media Providers & Importers", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    Text(
                        "Enable or disable modular crawlers, FTP scanners and metadata resolution engines.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )

                    extensions.forEach { ext ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(ext.name, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "v${ext.version}",
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = ext.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Switch(
                                checked = ext.isEnabled,
                                onCheckedChange = { ExtensionManager.toggleExtension(ext.id) }
                            )
                        }
                    }
                }
            }

            // Storage Section
            SettingsSectionTitle(title = "Storage & Library Data", icon = Icons.Default.Storage)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Cache Usage", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            Text(formatBytes(cacheSize), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = {
                                context.cacheDir.deleteRecursively()
                                context.externalCacheDir?.deleteRecursively()
                                cacheSize = calculateCacheSize()
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Clear Cache")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { showClearDataDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Clear Library Data")
                    }
                }
            }

            // About Section
            SettingsSectionTitle(title = "About", icon = Icons.Default.Info)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Video Library", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Version 2.0 (Android 16)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Designed for high performance, modern media discovery, web directory crawling, FTP scanning, offline playback, live streams, JSON data backups, and advanced video controls.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(90.dp))
        }
    }

    // Import Mode Dialog
    if (showImportDialog && selectedImportUri != null) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Library Backup") },
            text = {
                Column {
                    Text("Choose how to handle existing videos in your library:")
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedImportMode = ImportMode.MERGE }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedImportMode == ImportMode.MERGE,
                            onClick = { selectedImportMode = ImportMode.MERGE },
                            colors = RadioButtonDefaults.colors(selectedColor = ElectricViolet)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Merge", fontWeight = FontWeight.Bold)
                            Text("Add all videos, updating existing ones", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedImportMode = ImportMode.SKIP_DUPLICATES }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedImportMode == ImportMode.SKIP_DUPLICATES,
                            onClick = { selectedImportMode = ImportMode.SKIP_DUPLICATES },
                            colors = RadioButtonDefaults.colors(selectedColor = ElectricViolet)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Skip Duplicates", fontWeight = FontWeight.Bold)
                            Text("Only import new videos not already in library", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedImportMode = ImportMode.REPLACE }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedImportMode == ImportMode.REPLACE,
                            onClick = { selectedImportMode = ImportMode.REPLACE },
                            colors = RadioButtonDefaults.colors(selectedColor = ElectricViolet)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Replace Entire Library", fontWeight = FontWeight.Bold)
                            Text("Delete current videos and restore backup", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = selectedImportUri ?: return@Button
                        showImportDialog = false
                        scope.launch {
                            val res = BackupManager.importFromJson(context, repository, uri, selectedImportMode)
                            if (res.isSuccess) {
                                Toast.makeText(context, "Imported ${res.getOrNull()} videos successfully", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Import failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear Entire Library?") },
            text = { Text("This will remove all videos from your local database. Downloaded video files will remain intact.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.clearAll()
                            showClearDataDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

}

@Composable
fun SettingsSectionTitle(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    ) {
        Icon(icon, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun ThemeOptionChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) ElectricViolet else MaterialTheme.colorScheme.surface,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
