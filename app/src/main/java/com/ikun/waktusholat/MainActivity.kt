package com.ikun.waktusholat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ikun.waktusholat.alarm.AlarmScheduler
import com.ikun.waktusholat.data.AdzanSoundSetting
import com.ikun.waktusholat.data.LocationMode
import com.ikun.waktusholat.data.PrayerSettings
import com.ikun.waktusholat.data.PrayerSettingsRepository
import com.ikun.waktusholat.data.PrayerTime
import com.ikun.waktusholat.data.PrayerTimesRepository
import com.ikun.waktusholat.location.LocationProvider
import com.ikun.waktusholat.permission.PermissionHelper
import com.ikun.waktusholat.ui.screens.SettingsScreen
import com.ikun.waktusholat.ui.theme.WaktuSholatTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Dashboard sederhana — pengganti Dashboard.java + JadwalShalatFragment.java lama.
 * Lokasi sekarang dinamis (GPS via FusedLocationProviderClient, dengan fallback
 * ke lokasi/kota terakhir yang tersimpan di DataStore), dan semua permission
 * runtime yang dibutuhkan diminta di sini sebelum menampilkan jadwal.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WaktuSholatTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WaktuSholatRoot()
                }
            }
        }
    }
}

private sealed interface Screen {
    data object Dashboard : Screen
    data object Settings : Screen
}

@Composable
fun WaktuSholatRoot() {
    var screen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    val context = LocalContext.current
    val repository = remember { PrayerSettingsRepository(context) }
    val scope = rememberCoroutineScope()

    val settings by repository.settingsFlow.collectAsState(initial = PrayerSettings(
        latitude = PrayerSettingsRepository.DEFAULT_LATITUDE,
        longitude = PrayerSettingsRepository.DEFAULT_LONGITUDE,
    ))
    val locationMode by repository.locationMode.collectAsState(initial = LocationMode.AUTO)
    val adzanSound by repository.adzanSound.collectAsState(initial = AdzanSoundSetting())

    when (screen) {
        Screen.Dashboard -> PrayerScheduleScreen(
            settings = settings,
            onOpenSettings = { screen = Screen.Settings },
        )
        Screen.Settings -> SettingsScreen(
            settings = settings,
            locationMode = locationMode,
            adzanSound = adzanSound,
            onBack = { screen = Screen.Dashboard },
            onCalculationMethodChange = { method -> scope.launch { repository.saveCalculationMethod(method) } },
            onMadhabChange = { madhab -> scope.launch { repository.saveMadhab(madhab) } },
            onCorrectionChange = { id, minutes -> scope.launch { repository.saveCorrection(id, minutes) } },
            onUseAutoLocation = { scope.launch { repository.useAutoLocation() } },
            onSetManualLocation = { lat, lon -> scope.launch { repository.setManualLocation(lat, lon) } },
            onSelectBuiltInSound = { id -> scope.launch { repository.saveAdzanSound(id) } },
            onSelectCustomSound = { uri -> scope.launch { repository.saveCustomAdzanSound(uri.toString()) } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerScheduleScreen(settings: PrayerSettings, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { PrayerSettingsRepository(context) }
    val locationProvider = remember { LocationProvider(context) }

    var hasLocationPermission by remember { mutableStateOf(PermissionHelper.hasLocationPermission(context)) }
    var hasNotificationPermission by remember { mutableStateOf(PermissionHelper.hasNotificationPermission(context)) }
    var canScheduleExact by remember { mutableStateOf(PermissionHelper.canScheduleExactAlarms(context)) }
    var cityLabel by remember { mutableStateOf("Memuat lokasi...") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        hasLocationPermission = results[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasNotificationPermission = PermissionHelper.hasNotificationPermission(context)
    }

    val exactAlarmLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        canScheduleExact = PermissionHelper.canScheduleExactAlarms(context)
    }

    // Minta izin runtime sekali saat dashboard pertama dibuka.
    LaunchedEffect(Unit) {
        val missing = PermissionHelper.runtimePermissions().filterNot { permission ->
            androidx.core.content.ContextCompat.checkSelfPermission(context, permission) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    // Begitu izin lokasi ada, ambil fix GPS sekali dan simpan ke DataStore —
    // kecuali user sudah memilih mode lokasi manual (kota pilihan sendiri).
    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            cityLabel = "Izin lokasi belum diberikan — memakai lokasi tersimpan"
            return@LaunchedEffect
        }
        val mode = repository.locationMode.first()
        if (mode == LocationMode.MANUAL) {
            cityLabel = "Menggunakan lokasi manual (ubah di Pengaturan)"
            return@LaunchedEffect
        }
        val latLng = locationProvider.getCurrentLocation()
        if (latLng != null) {
            repository.saveLocation(latLng.latitude, latLng.longitude, latLng.altitude, cityName = null)
            cityLabel = "Lokasi GPS: %.4f, %.4f".format(latLng.latitude, latLng.longitude)
        } else {
            cityLabel = "GPS belum dapat fix — memakai lokasi tersimpan"
        }
    }

    val prayerTimes = remember(settings) {
        PrayerTimesRepository().calculate(settings)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Waktu Sholat") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Pengaturan")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text(
                text = "Jadwal Shalat Hari Ini",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = cityLabel, style = MaterialTheme.typography.bodyMedium)

            if (!canScheduleExact) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Izin alarm presisi belum aktif — adzan bisa meleset beberapa menit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = { exactAlarmLauncher.launch(PermissionHelper.exactAlarmSettingsIntent(context)) }) {
                    Text("Buka Pengaturan Alarm")
                }
            }
            if (!hasNotificationPermission) {
                Text(
                    text = "Izin notifikasi belum diberikan — pengingat adzan tidak akan muncul.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(prayerTimes) { prayer ->
                    PrayerRow(prayer)
                }
            }

            Button(
                onClick = {
                    AlarmScheduler(context).scheduleAll(prayerTimes)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Aktifkan Alarm Adzan Hari Ini")
            }
        }
    }
}

@Composable
fun PrayerRow(prayer: PrayerTime) {
    val formatter = remember { SimpleDateFormat("HH:mm", Locale("id", "ID")) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = prayer.label, style = MaterialTheme.typography.titleMedium)
        Text(text = formatter.format(prayer.time), style = MaterialTheme.typography.titleMedium)
    }
    HorizontalDivider()
}
