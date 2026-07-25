package com.ikun.waktusholat.ui.screens

import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ikun.waktusholat.data.AdzanSoundCatalog
import com.ikun.waktusholat.data.AdzanSoundSetting
import com.ikun.waktusholat.data.CalculationMethodOption
import com.ikun.waktusholat.data.LocationMode
import com.ikun.waktusholat.data.MadhabOption
import com.ikun.waktusholat.data.PrayerId
import com.ikun.waktusholat.data.PrayerSettings

/**
 * Layar pengaturan — pengganti SettingActivity.java lama.
 * Semua nilai disimpan lewat callback ke PrayerSettingsRepository
 * (di-handle di MainActivity/PrayerScheduleScreen), bukan langsung di sini,
 * supaya screen ini tetap stateless & mudah di-preview.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: PrayerSettings,
    locationMode: LocationMode,
    adzanSound: AdzanSoundSetting,
    onBack: () -> Unit,
    onCalculationMethodChange: (CalculationMethodOption) -> Unit,
    onMadhabChange: (MadhabOption) -> Unit,
    onCorrectionChange: (PrayerId, Int) -> Unit,
    onUseAutoLocation: () -> Unit,
    onSetManualLocation: (latitude: Double, longitude: Double) -> Unit,
    onSelectBuiltInSound: (String) -> Unit,
    onSelectCustomSound: (Uri) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(horizontal = 16.dp)) {
            item {
                Text(
                    text = "Lokasi",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
                LocationSection(
                    mode = locationMode,
                    settings = settings,
                    onUseAutoLocation = onUseAutoLocation,
                    onSetManualLocation = onSetManualLocation,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }
            item {
                Text(
                    text = "Metode Perhitungan",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                MethodDropdown(selected = settings.calculationMethod, onSelected = onCalculationMethodChange)
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }
            item {
                Text(
                    text = "Madzhab Ashar",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                MadhabDropdown(selected = settings.madhabAshar, onSelected = onMadhabChange)
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }
            item {
                Text(
                    text = "Suara Adzan",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                AdzanSoundSection(
                    selected = adzanSound,
                    onSelectBuiltIn = onSelectBuiltInSound,
                    onSelectCustom = onSelectCustomSound,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }
            item {
                Text(
                    text = "Koreksi Manual per Waktu (menit)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            items(PrayerId.entries.filter { it != PrayerId.TERBIT }) { id ->
                CorrectionRow(
                    label = id.name.lowercase().replaceFirstChar { it.uppercase() },
                    minutes = settings.correctionMinutes[id] ?: 0,
                    onChange = { onCorrectionChange(id, it) },
                )
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun MethodDropdown(selected: CalculationMethodOption, onSelected: (CalculationMethodOption) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selected.displayName())
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CalculationMethodOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName()) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun MadhabDropdown(selected: MadhabOption, onSelected: (MadhabOption) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(if (selected == MadhabOption.SYAFII) "Syafi'i (bayangan 1x)" else "Hanafi (bayangan 2x)")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Syafi'i (bayangan 1x)") }, onClick = { onSelected(MadhabOption.SYAFII); expanded = false })
            DropdownMenuItem(text = { Text("Hanafi (bayangan 2x)") }, onClick = { onSelected(MadhabOption.HANAFI); expanded = false })
        }
    }
}

@Composable
private fun CorrectionRow(label: String, minutes: Int, onChange: (Int) -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = {
            Row {
                IconButton(onClick = { onChange(minutes - 1) }) { Text("-") }
                Text(
                    text = "$minutes",
                    modifier = Modifier.padding(horizontal = 8.dp).width(24.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
                IconButton(onClick = { onChange(minutes + 1) }) { Text("+") }
            }
        },
    )
}

@Composable
private fun LocationSection(
    mode: LocationMode,
    settings: PrayerSettings,
    onUseAutoLocation: () -> Unit,
    onSetManualLocation: (latitude: Double, longitude: Double) -> Unit,
) {
    var latitudeText by remember(settings.latitude) { mutableStateOf(settings.latitude.toString()) }
    var longitudeText by remember(settings.longitude) { mutableStateOf(settings.longitude.toString()) }

    Text(
        text = if (mode == LocationMode.AUTO) "Mode: GPS otomatis" else "Mode: Manual",
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row {
        OutlinedButton(onClick = onUseAutoLocation, enabled = mode != LocationMode.AUTO) {
            Text("Pakai GPS")
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row {
        OutlinedTextField(
            value = latitudeText,
            onValueChange = { latitudeText = it },
            label = { Text("Latitude") },
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedTextField(
            value = longitudeText,
            onValueChange = { longitudeText = it },
            label = { Text("Longitude") },
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Button(onClick = {
        val lat = latitudeText.toDoubleOrNull()
        val lon = longitudeText.toDoubleOrNull()
        if (lat != null && lon != null) onSetManualLocation(lat, lon)
    }) {
        Text("Simpan Lokasi Manual")
    }
}

/**
 * Pilihan suara adzan + tombol "Dengarkan dulu" per opsi, plus opsi "File
 * sendiri" lewat Storage Access Framework — semua bisa dicoba langsung di
 * layar ini tanpa perlu menunggu waktu adzan tiba.
 */
@Composable
private fun AdzanSoundSection(
    selected: AdzanSoundSetting,
    onSelectBuiltIn: (String) -> Unit,
    onSelectCustom: (Uri) -> Unit,
) {
    val context = LocalContext.current
    var playingId by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    fun stopPreview() {
        mediaPlayer?.release()
        mediaPlayer = null
        playingId = null
    }

    fun playPreview(id: String, create: () -> MediaPlayer?) {
        stopPreview()
        val player = create() ?: return
        playingId = id
        mediaPlayer = player.apply {
            setOnCompletionListener { stopPreview() }
            start()
        }
    }

    DisposableEffect(Unit) {
        onDispose { mediaPlayer?.release() }
    }

    val customLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            onSelectCustom(uri)
        }
    }

    AdzanSoundCatalog.builtIn.forEach { sound ->
        val isSelected = selected.soundId == sound.id
        val isPlaying = playingId == sound.id
        ListItem(
            headlineContent = { Text(sound.label) },
            leadingContent = {
                RadioButton(selected = isSelected, onClick = { onSelectBuiltIn(sound.id) })
            },
            trailingContent = {
                IconButton(onClick = {
                    if (isPlaying) {
                        stopPreview()
                    } else {
                        playPreview(sound.id) {
                            runCatching { MediaPlayer.create(context, sound.rawResId) }.getOrNull()
                        }
                    }
                }) {
                    Icon(
                        if (isPlaying) Icons.Filled.Close else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Hentikan" else "Dengarkan",
                    )
                }
            },
        )
    }

    val customUriString = selected.customUri
    val customName = remember(customUriString) {
        customUriString?.let { queryDisplayName(context, Uri.parse(it)) }
    }
    ListItem(
        headlineContent = { Text("File sendiri") },
        supportingContent = { Text(customName ?: "Belum ada file dipilih") },
        leadingContent = {
            RadioButton(
                selected = selected.soundId == AdzanSoundCatalog.CUSTOM_ID,
                onClick = {
                    if (customUriString != null) {
                        onSelectCustom(Uri.parse(customUriString))
                    } else {
                        customLauncher.launch(arrayOf("audio/*"))
                    }
                },
            )
        },
        trailingContent = {
            Row {
                if (customUriString != null) {
                    val isPlaying = playingId == AdzanSoundCatalog.CUSTOM_ID
                    IconButton(onClick = {
                        if (isPlaying) {
                            stopPreview()
                        } else {
                            playPreview(AdzanSoundCatalog.CUSTOM_ID) {
                                runCatching { MediaPlayer.create(context, Uri.parse(customUriString)) }.getOrNull()
                            }
                        }
                    }) {
                        Icon(
                            if (isPlaying) Icons.Filled.Close else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Hentikan" else "Dengarkan",
                        )
                    }
                }
                TextButton(onClick = { customLauncher.launch(arrayOf("audio/*")) }) {
                    Text(if (customUriString != null) "Ganti" else "Pilih")
                }
            }
        },
    )
}

private fun queryDisplayName(context: android.content.Context, uri: Uri): String {
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else null
        }
    }.getOrNull() ?: uri.lastPathSegment ?: "File terpilih"
}

private fun CalculationMethodOption.displayName(): String = when (this) {
    CalculationMethodOption.KEMENAG -> "Kemenag RI (20°/18°)"
    CalculationMethodOption.MWL -> "Muslim World League (18°/17°)"
    CalculationMethodOption.ISNA -> "ISNA — North America (15°/15°)"
    CalculationMethodOption.UMM_AL_QURA -> "Umm al-Qura — Mekah (18.5°/90min)"
    CalculationMethodOption.EGYPTIAN -> "Egyptian (19.5°/17.5°)"
    CalculationMethodOption.KARACHI -> "Karachi (18°/18°)"
}
