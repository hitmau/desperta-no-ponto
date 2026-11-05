package br.com.despertanoponto

import android.Manifest
import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import java.io.File
import java.text.SimpleDateFormat
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import androidx.compose.animation.core.animateDpAsState

private val Background = Color(0xFF081016)
private val SheetColor = Color(0xF212171D)
private val SearchColor = Color(0xF52C3035)
private val Raised = Color(0xFF252B31)
private val Mint = Color(0xFF66D9B5)
private val Muted = Color(0xFFAAB4BE)
private val Blue = Color(0xFF74B9FF)
private val WarmOrange = Color(0xFFFFB37A)

private data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val speedMetersPerSecond: Float? = null
)

private enum class PendingLocationAction { NONE, MANUAL, RECURRING, ROUTE_SCHEDULE, USE_CURRENT }

private enum class MapLayerMode(val label: String, val description: String) {
    MAP("Mapa", "Mapa completo com ruas e locais"),
    ROADS("Vias", "Visual claro com destaque para ruas"),
    SATELLITE("Satélite", "Imagens aéreas do terreno")
}

private enum class RouteLocationTarget(val label: String) {
    ORIGIN("origem"),
    DESTINATION("destino")
}

private enum class RouteHistoryPeriod(val label: String) {
    WEEKDAY("Dia da semana"),
    MONTH("Mês"),
    YEAR("Ano")
}

private data class RoutePeriodSummary(
    val label: String,
    val journeyCount: Int,
    val averageDurationSeconds: Long?,
    val shortestDurationSeconds: Long?,
    val longestDurationSeconds: Long?
)

private val RoadsTileSource = XYTileSource(
    "CARTO Positron",
    0,
    20,
    256,
    ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/light_all/",
        "https://b.basemaps.cartocdn.com/light_all/",
        "https://c.basemaps.cartocdn.com/light_all/",
        "https://d.basemaps.cartocdn.com/light_all/"
    ),
    "© OpenStreetMap contributors, © CARTO"
)

private val SatelliteTileSource = object : OnlineTileSourceBase(
    "Esri World Imagery",
    0,
    19,
    256,
    "",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"),
    "Tiles © Esri"
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        val row = MapTileIndex.getY(pMapTileIndex)
        val column = MapTileIndex.getX(pMapTileIndex)
        return "${baseUrl}$zoom/$row/$column"
    }
}

private object MapLayerStore {
    private const val PREFS = "map_preferences"
    private const val KEY_LAYER = "layer"

    fun load(context: Context): MapLayerMode = runCatching {
        MapLayerMode.valueOf(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_LAYER, MapLayerMode.MAP.name)!!
        )
    }.getOrDefault(MapLayerMode.MAP)

    fun save(context: Context, layer: MapLayerMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAYER, layer.name)
            .apply()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DespertaTheme { DespertaApp() } }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching { RouteScheduleManager.syncAll(applicationContext) }
        }
    }
}

@Composable
private fun DespertaApp() {
    val context = LocalContext.current
    var onboardingCompleted by remember { mutableStateOf(OnboardingStore.isCompleted(context)) }

    if (onboardingCompleted) {
        DespertaMapScreen()
    } else {
        OnboardingScreen(
            onFinish = {
                OnboardingStore.complete(context)
                onboardingCompleted = true
            }
        )
    }
}

@Composable
private fun OnboardingScreen(onFinish: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = Background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(76.dp),
                color = Color(0x263DD7A9),
                shape = CircleShape,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Alarm, null, tint = Mint, modifier = Modifier.size(42.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "Bem-vindo ao Desperta no Ponto",
                color = Color.White,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Um despertador que acompanha sua localização e avisa quando você estiver chegando.",
                color = Muted,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))

            OnboardingStep(
                number = "1",
                icon = Icons.Default.Search,
                title = "Escolha o destino",
                description = "Pesquise um endereço no topo ou toque no mapa para marcar o ponto onde quer chegar."
            )
            OnboardingStep(
                number = "2",
                icon = Icons.Default.Settings,
                title = "Ajuste os sinais",
                description = "Na engrenagem, defina o raio de chegada, o afastamento, o som, a vibração e o modo de uso."
            )
            OnboardingStep(
                number = "3",
                icon = Icons.Default.MyLocation,
                title = "Autorize sua localização",
                description = "Permita a localização precisa para o marcador acompanhar seu movimento em tempo real."
            )
            OnboardingStep(
                number = "4",
                icon = Icons.Default.NotificationsActive,
                title = "Ative o despertador",
                description = "Toque em ativar e mantenha a notificação ativa. Ela mostra distância e tempo mesmo com a tela bloqueada."
            )

            Spacer(Modifier.height(18.dp))
            Text(
                "Você verá esta apresentação apenas uma vez.",
                color = Muted,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Mint,
                    contentColor = Background
                )
            ) {
                Text("Começar a usar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun OnboardingStep(
    number: String,
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SheetColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(46.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(color = Color(0x263DD7A9), shape = CircleShape, modifier = Modifier.fillMaxSize()) {}
                Icon(icon, null, tint = Mint, modifier = Modifier.size(22.dp))
                Surface(
                    modifier = Modifier
                        .size(19.dp)
                        .align(Alignment.TopEnd),
                    color = Mint,
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(number, color = Background, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text(description, color = Muted, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun DespertaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
            primary = Mint,
            onPrimary = Background,
            secondary = Blue,
            background = Background,
            surface = SheetColor,
            surfaceVariant = Raised,
            onSurface = Color(0xFFF2F5F7),
            onSurfaceVariant = Muted
        ),
        content = content
    )
}

@SuppressLint("MissingPermission")
@Composable
private fun DespertaMapScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val store = remember { DestinationStore(context) }
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val scope = rememberCoroutineScope()

    var config by remember { mutableStateOf(store.load()) }
    var currentLocation by remember {
        mutableStateOf(CurrentLocationStore.load(context)?.let {
            LocationPoint(it.latitude, it.longitude, it.accuracyMeters, it.speedMetersPerSecond)
        })
    }
    var permissionGranted by remember { mutableStateOf(hasLocationPermission(context)) }
    var permissionMessage by remember { mutableStateOf<String?>(null) }
    var permissionRequested by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf(PendingLocationAction.NONE) }
    var searchText by remember { mutableStateOf(config.name) }
    var searchResults by remember { mutableStateOf(emptyList<PlaceResult>()) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var searching by remember { mutableStateOf(false) }
    var trackingActive by remember { mutableStateOf(TrackingStateStore.isActive(context)) }
    var journeyElapsedSeconds by remember { mutableStateOf(0L) }
    var destinationHistory by remember { mutableStateOf(emptyList<RouteJourneyStats>()) }
    var routeProfiles by remember { mutableStateOf(emptyList<RouteProfile>()) }
    var routeSchedules by remember { mutableStateOf(emptyList<RouteSchedule>()) }
    var pendingRouteSchedule by remember { mutableStateOf<RouteSchedule?>(null) }
    var activeOriginKey by remember { mutableStateOf(TrackingStateStore.runOriginKey(context)) }
    var centerRequest by remember { mutableStateOf(0) }
    var settingsOpen by remember { mutableStateOf(false) }
    var arrivalRadiusEditorOpen by remember { mutableStateOf(false) }
    var arrivalRadiusText by remember(config.arrivalRadiusMeters) {
        mutableStateOf(config.arrivalRadiusMeters.toString())
    }
    var layerMenuOpen by remember { mutableStateOf(false) }
    var footerCollapsed by remember { mutableStateOf(true) }
    var sheetCollapseRequest by remember { mutableIntStateOf(0) }
    var mapLayer by remember { mutableStateOf(MapLayerStore.load(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                footerCollapsed = true
                sheetCollapseRequest += 1
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            permissionMessage = "Permita as notificações para o despertador funcionar com a tela bloqueada."
        }
    }
    val distanceToDestinationMeters = distanceBetween(
        currentLocation?.latitude,
        currentLocation?.longitude,
        config.latitude,
        config.longitude
    )
    val estimatedArrivalSeconds = estimateArrivalSeconds(
        distanceToDestinationMeters,
        config.arrivalRadiusMeters,
        currentLocation?.speedMetersPerSecond
    )
    val currentDestinationKey = config.latitude?.let { latitude ->
        config.longitude?.let { longitude -> DestinationHistoryStore.destinationKey(latitude, longitude) }
    }
    val currentDestinationStats = destinationHistory.firstOrNull {
        it.destinationKey == currentDestinationKey && (!trackingActive || it.originKey == activeOriginKey)
    }

    fun saveConfig(next: DestinationConfig) {
        config = next.normalized()
        store.save(config)
        if (config.activationMode == ActivationMode.MANUAL) {
            ScheduleManager.sync(context, config)
        }
    }

    fun refreshRouteAutomation() {
        scope.launch {
            val (profiles, schedules) = loadRouteAutomation(context)
            routeProfiles = profiles
            routeSchedules = schedules
        }
    }

    fun saveRouteProfile(profile: RouteProfile) {
        scope.launch {
            withContext(Dispatchers.IO) {
                val historyStore = DestinationHistoryStore(context)
                try { historyStore.saveRouteProfile(profile) } finally { historyStore.close() }
            }
            refreshRouteAutomation()
            RouteAlarmWidgetProvider.updateAll(context)
        }
    }

    fun deleteRouteProfile(profile: RouteProfile) {
        if (TrackingStateStore.activeRouteProfileId(context) == profile.id && TrackingStateStore.isActive(context)) {
            permissionMessage = "Não é possível remover a rota enquanto ela está em acompanhamento."
            return
        }
        routeSchedules.filter { it.routeProfileId == profile.id }.forEach { RouteScheduleManager.cancel(context, it.id) }
        scope.launch {
            withContext(Dispatchers.IO) {
                val historyStore = DestinationHistoryStore(context)
                try { historyStore.deleteRouteProfile(profile.id) } finally { historyStore.close() }
            }
            refreshRouteAutomation()
            RouteScheduleManager.syncAll(context)
            RouteAlarmWidgetProvider.updateAll(context)
        }
    }

    fun saveRouteScheduleNow(schedule: RouteSchedule) {
        scope.launch {
            withContext(Dispatchers.IO) {
                val historyStore = DestinationHistoryStore(context)
                try { historyStore.saveRouteSchedule(schedule) } finally { historyStore.close() }
            }
            refreshRouteAutomation()
            RouteScheduleManager.syncAll(context)
            if (!RouteScheduleManager.exactAlarmPermissionGranted(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissionMessage = "Rotina salva. Sem a permissão de horário exato, o Android pode atrasar o início."
            }
        }
    }

    fun deleteRouteSchedule(schedule: RouteSchedule) {
        RouteScheduleManager.cancel(context, schedule.id)
        scope.launch {
            withContext(Dispatchers.IO) {
                val historyStore = DestinationHistoryStore(context)
                try { historyStore.deleteRouteSchedule(schedule.id) } finally { historyStore.close() }
            }
            refreshRouteAutomation()
            RouteScheduleManager.syncAll(context)
        }
    }

    fun deactivateForNewDestination() {
        if (trackingActive || TrackingStateStore.isActive(context)) {
            context.stopService(Intent(context, TrackingService::class.java))
            TrackingStateStore.stopRun(context)
            trackingActive = false
        }
    }

    fun saveCurrentLocation(location: Location) {
        val point = LocationPoint(
            location.latitude,
            location.longitude,
            location.accuracy,
            smoothedMovementSpeed(location, currentLocation?.speedMetersPerSecond)
        )
        currentLocation = point
        CurrentLocationStore.save(
            context,
            point.latitude,
            point.longitude,
            point.accuracyMeters,
            speedMetersPerSecond = point.speedMetersPerSecond
        )
        centerRequest += 1
        val precision = point.accuracyMeters.roundToInt().coerceAtLeast(1)
        permissionMessage = if (precision <= 30) {
            "Localização GPS precisa: aproximadamente ±$precision m."
        } else {
            "Localização obtida com precisão aproximada de ±$precision m."
        }
        if (pendingAction == PendingLocationAction.USE_CURRENT) {
            pendingAction = PendingLocationAction.NONE
            deactivateForNewDestination()
            saveConfig(
                config.copy(
                    name = "Minha localização atual",
                    latitude = point.latitude,
                    longitude = point.longitude
                )
            )
            searchText = "Minha localização atual"
            permissionMessage = "Destino definido pela sua localização atual."
        }
    }

    fun requestAlarmNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun refreshCurrentLocation() {
        if (!permissionGranted) return
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setGranularity(Granularity.GRANULARITY_FINE)
            .setMaxUpdateAgeMillis(0L)
            .setDurationMillis(20_000L)
            .build()
        val cancellation = CancellationTokenSource()
        locationClient.getCurrentLocation(request, cancellation.token)
            .addOnSuccessListener { fresh ->
                if (fresh != null) saveCurrentLocation(fresh)
                else {
                    permissionMessage = "Não foi possível obter uma posição GPS nova. Vá para um local aberto e tente novamente."
                }
            }
            .addOnFailureListener {
                permissionMessage = "Falha ao obter localização precisa. Verifique se o GPS está ativado."
            }
    }

    fun startManualTracking() {
        if (!config.hasDestination()) return
        requestAlarmNotificationPermission()
        store.save(config)
        TrackingStateStore.startRun(context)
        journeyElapsedSeconds = 0L
        ContextCompat.startForegroundService(
            context,
            Intent(context, TrackingService::class.java).setAction(TrackingService.ACTION_START_MANUAL)
        )
        trackingActive = true
        permissionMessage = "Despertador ativo. Você pode bloquear a tela."
    }

    fun syncRecurring() {
        if (!config.hasDestination()) return
        requestAlarmNotificationPermission()
        store.save(config)
        ScheduleManager.sync(context, config)
        permissionMessage = "Recorrência salva neste aparelho."
    }

    fun commitArrivalRadius() {
        val radius = arrivalRadiusText.toIntOrNull() ?: run {
            arrivalRadiusText = config.arrivalRadiusMeters.toString()
            return
        }
        val normalized = radius.coerceIn(50, 50000)
        arrivalRadiusText = normalized.toString()
        saveConfig(config.copy(arrivalRadiusMeters = normalized))
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (pendingAction == PendingLocationAction.RECURRING) {
            if (granted || Build.VERSION.SDK_INT < 29) {
                pendingAction = PendingLocationAction.NONE
                syncRecurring()
            } else {
                pendingAction = PendingLocationAction.NONE
                permissionMessage = "Para usar a recorrência, permita localização o tempo todo nas configurações do Android."
            }
        } else if (pendingAction == PendingLocationAction.ROUTE_SCHEDULE) {
            val schedule = pendingRouteSchedule
            pendingAction = PendingLocationAction.NONE
            pendingRouteSchedule = null
            if (schedule != null) {
                if (granted || Build.VERSION.SDK_INT < 29) {
                    saveRouteScheduleNow(schedule)
                } else {
                    saveRouteScheduleNow(schedule.copy(enabled = false))
                    permissionMessage = "Rotina salva desativada. Permita localização o tempo todo para habilitá-la."
                }
            }
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val precise = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            hasPreciseLocationPermission(context)
        val granted = precise || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        permissionGranted = granted
        if (!granted) {
            permissionMessage = "A localização é necessária para mostrar sua posição e despertar no destino."
            pendingAction = PendingLocationAction.NONE
            return@rememberLauncherForActivityResult
        }
        if (!precise) {
            permissionMessage = "O Android concedeu apenas localização aproximada. Ative “Localização precisa” para usar o despertador."
            pendingAction = PendingLocationAction.NONE
            refreshCurrentLocation()
            return@rememberLauncherForActivityResult
        }
        permissionMessage = "Localização precisa autorizada."
        refreshCurrentLocation()
        when (pendingAction) {
            PendingLocationAction.MANUAL -> {
                pendingAction = PendingLocationAction.NONE
                startManualTracking()
            }
            PendingLocationAction.RECURRING -> {
                if (Build.VERSION.SDK_INT >= 29 && ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    pendingAction = PendingLocationAction.NONE
                    syncRecurring()
                }
            }
            PendingLocationAction.ROUTE_SCHEDULE -> {
                if (Build.VERSION.SDK_INT >= 29 && ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    val schedule = pendingRouteSchedule
                    pendingRouteSchedule = null
                    pendingAction = PendingLocationAction.NONE
                    schedule?.let(::saveRouteScheduleNow)
                }
            }
            PendingLocationAction.USE_CURRENT -> Unit
            PendingLocationAction.NONE -> Unit
        }
    }

    fun requestLocation(action: PendingLocationAction = PendingLocationAction.NONE) {
        pendingAction = action
        if (!hasPreciseLocationPermission(context)) {
            locationLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        } else {
            permissionGranted = true
            refreshCurrentLocation()
            when (action) {
                PendingLocationAction.MANUAL -> {
                    pendingAction = PendingLocationAction.NONE
                    startManualTracking()
                }
                PendingLocationAction.RECURRING -> {
                    if (Build.VERSION.SDK_INT >= 29 && ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    } else {
                        pendingAction = PendingLocationAction.NONE
                        syncRecurring()
                    }
                }
                PendingLocationAction.ROUTE_SCHEDULE -> {
                    if (Build.VERSION.SDK_INT >= 29 && ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    } else {
                        val schedule = pendingRouteSchedule
                        pendingRouteSchedule = null
                        pendingAction = PendingLocationAction.NONE
                        schedule?.let(::saveRouteScheduleNow)
                    }
                }
                PendingLocationAction.USE_CURRENT -> Unit
                PendingLocationAction.NONE -> Unit
            }
        }
    }

    fun requestRouteScheduleSave(schedule: RouteSchedule) {
        if (schedule.enabled && Build.VERSION.SDK_INT >= 29 && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingRouteSchedule = schedule
            requestLocation(PendingLocationAction.ROUTE_SCHEDULE)
        } else {
            saveRouteScheduleNow(schedule)
        }
    }

    fun useCurrentAsDestination() {
        currentLocation?.let {
            deactivateForNewDestination()
            saveConfig(
                config.copy(
                    name = "Minha localização atual",
                    latitude = it.latitude,
                    longitude = it.longitude
                )
            )
            permissionMessage = "Destino definido pela sua localização atual."
            searchText = "Minha localização atual"
        } ?: requestLocation(PendingLocationAction.USE_CURRENT)
    }

    fun selectPlace(name: String, latitude: Double, longitude: Double) {
        deactivateForNewDestination()
        saveConfig(config.copy(name = name, latitude = latitude, longitude = longitude))
        searchText = name
        searchResults = emptyList()
        permissionMessage = "Destino escolhido. Toque no ícone do despertador para ativar."
    }

    fun toggleTracking() {
        if (!config.hasDestination()) {
            permissionMessage = "Escolha um destino antes de ativar o despertador."
            return
        }
        if (trackingActive || TrackingStateStore.isActive(context)) {
            context.stopService(Intent(context, TrackingService::class.java))
            TrackingStateStore.stopRun(context)
            trackingActive = false
            permissionMessage = "Despertador desativado."
        } else {
            requestLocation(PendingLocationAction.MANUAL)
        }
    }

    fun performSearch() {
        if (searchText.isBlank()) return
        searching = true
        searchError = null
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { SearchRepository.search(searchText) }
            }
            result.onSuccess { results ->
                searchResults = results
                if (results.isEmpty()) searchError = "Nenhum local encontrado."
            }.onFailure { searchError = "Não foi possível buscar agora." }
            searching = false
        }
    }

    LaunchedEffect(Unit) {
        destinationHistory = loadDestinationHistory(context)
        var lastLoadedCompletedRun = 0L
        var settingsWereOpen = false
        while (true) {
            val activeNow = TrackingStateStore.isActive(context)
            val startedAt = TrackingStateStore.startedAt(context)
            val originKey = TrackingStateStore.runOriginKey(context)
            val arrived = TrackingStateStore.hasArrived(context)
            if (trackingActive != activeNow) trackingActive = activeNow
            if (activeOriginKey != originKey) activeOriginKey = originKey
            if (activeNow && startedAt != null && !arrived) {
                journeyElapsedSeconds = ((System.currentTimeMillis() - startedAt) / 1_000L).coerceAtLeast(0L)
            }

            val completedRun = TrackingStateStore.lastCompletedStartedAt(context)
            if (completedRun > 0L && completedRun != lastLoadedCompletedRun) {
                destinationHistory = loadDestinationHistory(context)
                journeyElapsedSeconds = TrackingStateStore.lastCompletedDurationSeconds(context) ?: journeyElapsedSeconds
                lastLoadedCompletedRun = completedRun
            }

            if (settingsOpen && !settingsWereOpen) {
                destinationHistory = loadDestinationHistory(context)
                val (profiles, schedules) = loadRouteAutomation(context)
                routeProfiles = profiles
                routeSchedules = schedules
            }
            settingsWereOpen = settingsOpen
            delay(1_000L)
        }
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted && !permissionRequested) {
            permissionRequested = true
            requestLocation()
        } else if (permissionGranted) {
            refreshCurrentLocation()
        }
    }

    DisposableEffect(permissionGranted, locationClient) {
        var callback: LocationCallback? = null
        if (permissionGranted) {
            callback = object : LocationCallback() {
                private var lastPersistedAt = 0L

                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    val point = LocationPoint(
                        location.latitude,
                        location.longitude,
                        location.accuracy,
                        smoothedMovementSpeed(location, currentLocation?.speedMetersPerSecond)
                    )
                    currentLocation = point
                    val now = System.currentTimeMillis()
                    if (now - lastPersistedAt >= 1_000L) {
                        CurrentLocationStore.save(
                            context,
                            point.latitude,
                            point.longitude,
                            point.accuracyMeters,
                            now,
                            point.speedMetersPerSecond
                        )
                        lastPersistedAt = now
                    }
                }
            }
            val liveRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500L)
                .setMinUpdateIntervalMillis(250L)
                .setMaxUpdateDelayMillis(500L)
                .setMinUpdateDistanceMeters(0f)
                .setWaitForAccurateLocation(false)
                .build()
            runCatching {
                locationClient.requestLocationUpdates(liveRequest, callback!!, Looper.getMainLooper())
            }
        }
        onDispose {
            callback?.let { locationClient.removeLocationUpdates(it) }
        }
    }

    if (settingsOpen) {
        BackHandler { settingsOpen = false }
        SettingsScreen(
            config = config,
            journeyHistory = destinationHistory,
            routeProfiles = routeProfiles,
            routeSchedules = routeSchedules,
            exactAlarmPermissionGranted = RouteScheduleManager.exactAlarmPermissionGranted(context),
            currentLocation = currentLocation,
            onBack = { settingsOpen = false },
            onModeChange = { saveConfig(config.copy(activationMode = it)) },
            onScheduleTime = {
                TimePickerDialog(
                    context,
                    { _, hour, minute -> saveConfig(config.copy(scheduleHour = hour, scheduleMinute = minute)) },
                    config.scheduleHour,
                    config.scheduleMinute,
                    true
                ).show()
            },
            onDaysChange = { saveConfig(config.copy(daysMask = it)) },
            onArrivalRadiusChange = { saveConfig(config.copy(arrivalRadiusMeters = it)) },
            onDepartureRadiusChange = { saveConfig(config.copy(departureRadiusMeters = it)) },
            onDepartureAlertChange = { saveConfig(config.copy(departureAlertEnabled = it)) },
            onSoundChange = { saveConfig(config.copy(soundEnabled = it)) },
            onVibrationChange = { saveConfig(config.copy(vibrationEnabled = it)) },
            onArrivalAlarmDurationChange = { saveConfig(config.copy(arrivalAlarmDurationSeconds = it)) },
            onSaveRouteProfile = ::saveRouteProfile,
            onDeleteRouteProfile = ::deleteRouteProfile,
            onSaveRouteSchedule = ::requestRouteScheduleSave,
            onDeleteRouteSchedule = ::deleteRouteSchedule,
            onRequestExactAlarmPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                .setData(Uri.parse("package:${context.packageName}"))
                        )
                    }
                }
            }
        )
        return
    }

    Box(Modifier.fillMaxSize().background(Background)) {
        MapPicker(
            modifier = Modifier.fillMaxSize(),
            selectedLatitude = config.latitude,
            selectedLongitude = config.longitude,
            arrivalRadiusMeters = config.arrivalRadiusMeters,
            currentLatitude = currentLocation?.latitude,
            currentLongitude = currentLocation?.longitude,
            centerRequest = centerRequest,
            mapLayer = mapLayer,
            onLocationSelected = { latitude, longitude ->
                scope.launch {
                    val name = withContext(Dispatchers.IO) {
                        reverseGeocode(context, latitude, longitude)
                    }
                    selectPlace(name, latitude, longitude)
                }
            }
        )

        SearchOverlay(
            value = searchText,
            searching = searching,
            onValueChange = {
                searchText = it
                if (it.isBlank()) searchResults = emptyList()
            },
            onSearch = ::performSearch,
            onClear = { searchText = ""; searchResults = emptyList(); searchError = null }
        )

        MapTravelHud(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = 12.dp,
                    bottom = if (footerCollapsed) 72.dp else 444.dp
                ),
            active = trackingActive,
            elapsedSeconds = if (trackingActive) journeyElapsedSeconds else currentDestinationStats?.lastDurationSeconds,
            averageSeconds = currentDestinationStats?.averageDurationSeconds,
            shortestSeconds = currentDestinationStats?.shortestDurationSeconds,
            longestSeconds = currentDestinationStats?.longestDurationSeconds,
            journeyCount = currentDestinationStats?.journeyCount ?: 0,
            speedMetersPerSecond = currentLocation?.speedMetersPerSecond
        )

        if (searchResults.isNotEmpty() || searchError != null) {
            SearchResultsOverlay(
                results = searchResults,
                error = searchError,
                onSelect = {
                    // Mantém o endereço escolhido visível no campo e usa o
                    // resultado tocado como o destino confirmado no mapa.
                    searchText = it.displayName
                    selectPlace(it.displayName, it.latitude, it.longitude)
                }
            )
        }

        val trackingButtonOffset by animateDpAsState(
            targetValue = if (footerCollapsed) 0.dp else (-68).dp,
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            label = "tracking_button_offset"
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp, bottom = 300.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MapActionButton(
                icon = Icons.Default.Layers,
                contentDescription = "Camadas do mapa",
                tint = Color.White,
                onClick = { layerMenuOpen = !layerMenuOpen }
            )
            MapActionButton(
                icon = Icons.Default.Settings,
                contentDescription = "Configurações",
                tint = Mint,
                onClick = { settingsOpen = true }
            )
            Box(Modifier.size(52.dp)) {
                MapActionButton(
                    icon = Icons.Default.RadioButtonChecked,
                    contentDescription = "Raio de chegada",
                    tint = WarmOrange,
                    onClick = {
                        if (arrivalRadiusEditorOpen) commitArrivalRadius()
                        arrivalRadiusEditorOpen = !arrivalRadiusEditorOpen
                    }
                )
                androidx.compose.animation.AnimatedVisibility(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .wrapContentWidth(unbounded = true)
                        .offset(x = (-60).dp),
                    visible = arrivalRadiusEditorOpen,
                    enter = fadeIn(tween(180)) + expandHorizontally(expandFrom = Alignment.End),
                    exit = fadeOut(tween(120)) + shrinkHorizontally(shrinkTowards = Alignment.End)
                ) {
                    ArrivalRadiusEditor(
                        value = arrivalRadiusText,
                        onValueChange = { arrivalRadiusText = it.filter(Char::isDigit).take(6) },
                        onCommit = {
                            commitArrivalRadius()
                            arrivalRadiusEditorOpen = false
                        }
                    )
                }
            }
            MapActionButton(
                icon = Icons.Default.MyLocation,
                contentDescription = "Minha localização",
                tint = Blue,
                onClick = { requestLocation() }
            )
            TrackingToggleButton(
                modifier = Modifier.offset(x = trackingButtonOffset),
                active = trackingActive,
                enabled = config.hasDestination(),
                onClick = ::toggleTracking
            )
        }

        if (layerMenuOpen) {
            MapLayerMenu(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 76.dp, end = 78.dp),
                selected = mapLayer,
                onSelected = { selected ->
                    mapLayer = selected
                    MapLayerStore.save(context, selected)
                    layerMenuOpen = false
                }
            )
        }

        BottomDestinationSheet(
            modifier = Modifier.align(Alignment.BottomCenter),
            config = config,
            permissionGranted = permissionGranted,
            locationAvailable = currentLocation != null,
            locationAccuracyMeters = currentLocation?.accuracyMeters,
            distanceToDestinationMeters = distanceToDestinationMeters,
            estimatedArrivalSeconds = estimatedArrivalSeconds,
            permissionMessage = permissionMessage,
            trackingActive = trackingActive,
            collapseRequest = sheetCollapseRequest,
            onPermission = { requestLocation() },
            onUseCurrent = ::useCurrentAsDestination,
            onSheetCollapsedChange = { footerCollapsed = it },
            onStop = {
                context.stopService(Intent(context, TrackingService::class.java))
                TrackingStateStore.stopRun(context)
                trackingActive = false
                permissionMessage = "Acompanhamento desativado."
            }
        )
    }
}

@Composable
private fun SearchOverlay(
    value: String,
    searching: Boolean,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .fillMaxWidth(),
        color = SearchColor,
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 12.dp
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Pesquise aqui", color = Color(0xFFD2D4D6), fontSize = 18.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Mint) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (value.isNotBlank()) {
                        IconButton(onClick = onClear) {
                            Icon(Icons.Default.Close, contentDescription = "Limpar", tint = Color.White)
                        }
                    }
                    IconButton(onClick = onSearch) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = if (searching) "Buscando" else "Buscar",
                            tint = Color.White
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Mint,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
    }
}

@Composable
private fun SearchResultsOverlay(
    results: List<PlaceResult>,
    error: String?,
    onSelect: (PlaceResult) -> Unit
) {
    Surface(
        modifier = Modifier
            .statusBarsPadding()
            .padding(start = 14.dp, end = 14.dp, top = 76.dp)
            .fillMaxWidth()
            .heightIn(max = 280.dp),
        color = Color(0xF51B2127),
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 14.dp
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(8.dp)) {
            results.forEach { place ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(place) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, null, tint = Mint)
                    Spacer(Modifier.width(10.dp))
                    Text(place.displayName, color = Color.White, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            error?.let { Text(it, color = Color(0xFFFFA0A0), modifier = Modifier.padding(12.dp), fontSize = 13.sp) }
        }
    }
}

@Composable
private fun MapActionButton(icon: ImageVector, contentDescription: String, tint: Color = Color.White, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(52.dp).clickable(onClick = onClick),
        color = Color(0xE52B3035),
        shape = CircleShape,
        shadowElevation = 10.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(23.dp))
        }
    }
}

@Composable
private fun ArrivalRadiusEditor(
    value: String,
    onValueChange: (String) -> Unit,
    onCommit: () -> Unit
) {
    Surface(
        color = Color(0xF51B2127),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 10.dp
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.width(190.dp).padding(4.dp),
            singleLine = true,
            label = { Text("Raio de chegada", fontSize = 11.sp) },
            supportingText = { Text("Centro até a borda", color = Color(0xFFD8E2E8), fontSize = 9.sp) },
            suffix = { Text("m", color = Color.White) },
            trailingIcon = {
                IconButton(onClick = onCommit) {
                    Icon(Icons.Default.Check, contentDescription = "Salvar raio", tint = Mint)
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color(0xFFE4EBEF),
                focusedIndicatorColor = Color(0xFFDCE7EC),
                unfocusedIndicatorColor = Color(0xFF9BAAB4),
                focusedTrailingIconColor = Mint,
                unfocusedTrailingIconColor = Mint,
                focusedSuffixColor = Color.White,
                unfocusedSuffixColor = Color.White,
                cursorColor = Mint
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onCommit() })
        )
    }
}

@Composable
private fun TrackingToggleButton(
    modifier: Modifier,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color(0xB52B3035)
            active -> Color(0xE53D8F78)
            else -> Color(0xE52B3035)
        },
        animationSpec = tween(durationMillis = 260),
        label = "tracking_button_background"
    )
    val iconTint by animateColorAsState(
        targetValue = when {
            !enabled -> Muted
            active -> Mint
            else -> Color(0xFFD2D7DC)
        },
        animationSpec = tween(durationMillis = 260),
        label = "tracking_button_tint"
    )
    Surface(
        modifier = modifier
            .size(58.dp)
            .clickable(enabled = enabled, onClick = onClick),
        color = backgroundColor,
        shape = CircleShape,
        shadowElevation = 12.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = active && enabled,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "tracking_button_icon"
            ) { isActive ->
                Icon(
                    imageVector = if (isActive) Icons.Default.NotificationsActive else Icons.Default.Alarm,
                    contentDescription = if (isActive) "Despertador ativo" else "Ativar despertador",
                    tint = iconTint,
                    modifier = Modifier.size(25.dp)
                )
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.dp, bottom = 8.dp)
                    .size(9.dp),
                color = if (active && enabled) Mint else Color(0xFF6F7880),
                shape = CircleShape
            ) {}
        }
    }
}

@Composable
private fun MapLayerMenu(
    modifier: Modifier,
    selected: MapLayerMode,
    onSelected: (MapLayerMode) -> Unit
) {
    Surface(
        modifier = modifier.width(228.dp),
        color = Color(0xF21B2127),
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 14.dp
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("Tipo de mapa", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            MapLayerMode.entries.forEach { layer ->
                val icon = when (layer) {
                    MapLayerMode.MAP -> Icons.Default.Map
                    MapLayerMode.ROADS -> Icons.Default.Layers
                    MapLayerMode.SATELLITE -> Icons.Default.SatelliteAlt
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onSelected(layer) },
                    color = if (selected == layer) Color(0x303DD7A9) else Raised,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, null, tint = if (selected == layer) Mint else Color.White)
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(layer.label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(layer.description, color = Color(0xFFD8E2E8), fontSize = 10.sp)
                        }
                        if (selected == layer) {
                            Icon(Icons.Default.Check, null, tint = Mint, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            Text(
                "Satélite: imagens Esri. Mapa e vias: OpenStreetMap/CARTO.",
                color = Color(0xFFC4CFD6),
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun SettingsScreen(
    config: DestinationConfig,
    journeyHistory: List<RouteJourneyStats>,
    routeProfiles: List<RouteProfile>,
    routeSchedules: List<RouteSchedule>,
    exactAlarmPermissionGranted: Boolean,
    currentLocation: LocationPoint?,
    onBack: () -> Unit,
    onModeChange: (ActivationMode) -> Unit,
    onScheduleTime: () -> Unit,
    onDaysChange: (Int) -> Unit,
    onArrivalRadiusChange: (Int) -> Unit,
    onDepartureRadiusChange: (Int) -> Unit,
    onDepartureAlertChange: (Boolean) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    onArrivalAlarmDurationChange: (Int) -> Unit,
    onSaveRouteProfile: (RouteProfile) -> Unit,
    onDeleteRouteProfile: (RouteProfile) -> Unit,
    onSaveRouteSchedule: (RouteSchedule) -> Unit,
    onDeleteRouteSchedule: (RouteSchedule) -> Unit,
    onRequestExactAlarmPermission: () -> Unit
) {
    val context = LocalContext.current
    var automationTab by remember { mutableStateOf(false) }
    var selectedHistoryRoute by remember { mutableStateOf<RouteJourneyStats?>(null) }
    var historyRecords by remember { mutableStateOf<List<RouteJourneyRecord>?>(null) }
    var historyLoading by remember { mutableStateOf(false) }
    var historyPeriod by remember { mutableStateOf(RouteHistoryPeriod.WEEKDAY) }

    BackHandler(enabled = selectedHistoryRoute != null) {
        selectedHistoryRoute = null
    }

    LaunchedEffect(selectedHistoryRoute?.originKey, selectedHistoryRoute?.destinationKey) {
        val route = selectedHistoryRoute
        if (route == null) {
            historyRecords = null
            historyLoading = false
        } else {
            historyLoading = true
            historyRecords = withContext(Dispatchers.IO) {
                val store = DestinationHistoryStore(context)
                try {
                    store.loadJourneysForRoute(route.originKey, route.destinationKey)
                } finally {
                    store.close()
                }
            }
            historyLoading = false
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (selectedHistoryRoute != null) selectedHistoryRoute = null else onBack()
                }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = if (selectedHistoryRoute == null) "Voltar" else "Voltar ao histórico"
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        if (selectedHistoryRoute == null) "Configurações do despertador" else "Detalhes do histórico",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        selectedHistoryRoute?.let { "${it.originName ?: "Origem"} → ${it.name}" }
                            ?: "Alterações salvas automaticamente",
                        color = Muted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (selectedHistoryRoute == null) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = !automationTab,
                        onClick = { automationTab = false },
                        label = { Text("Geral") }
                    )
                    FilterChip(
                        selected = automationTab,
                        onClick = { automationTab = true },
                        label = { Text("Widgets e rotinas") }
                    )
                }
            }

            when {
                selectedHistoryRoute != null -> {
                    val route = selectedHistoryRoute!!
                    if (historyLoading || historyRecords == null) {
                        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Text("Carregando viagens…", color = Muted)
                        }
                    } else {
                        RouteHistoryDetailsScreen(
                            modifier = Modifier.weight(1f),
                            route = route,
                            records = historyRecords.orEmpty(),
                            period = historyPeriod,
                            onPeriodChange = { historyPeriod = it }
                        )
                    }
                }
                !automationTab -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(color = Raised, shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Destino atual", color = Muted, fontSize = 11.sp)
                        Text(
                            if (config.hasDestination()) config.name else "Nenhum destino selecionado",
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text("Distâncias", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CompactRadiusField(
                        "Chegada",
                        config.arrivalRadiusMeters,
                        onArrivalRadiusChange,
                        Modifier.weight(1f)
                    )
                    CompactRadiusField(
                        "Afastamento",
                        config.departureRadiusMeters,
                        onDepartureRadiusChange,
                        Modifier.weight(1f)
                    )
                }
                Text(
                    "O alarme de chegada toca ao entrar no círculo. O afastamento dispara quando você se distancia essa medida do ponto mais próximo registrado no trajeto.",
                    color = Muted,
                    fontSize = 11.sp
                )

                Text("Ativação", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                ModeSelector(config.activationMode, onModeChange)

                if (config.activationMode != ActivationMode.MANUAL) {
                    Surface(color = Raised, shape = RoundedCornerShape(14.dp)) {
                        Column(
                            Modifier.padding(13.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Alarm, null, tint = Blue, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Horário recorrente", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                OutlinedButton(onClick = onScheduleTime) {
                                    Text("%02d:%02d".format(config.scheduleHour, config.scheduleMinute))
                                }
                            }
                            DaysSelector(config.daysMask, onDaysChange)
                        }
                    }
                }

                Text("Sinais", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Surface(color = Raised, shape = RoundedCornerShape(14.dp)) {
                    Column(
                        Modifier.padding(13.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SettingSwitch(
                            "Sinal ao se afastar",
                            "Avisa quando sair do raio de afastamento.",
                            config.departureAlertEnabled,
                            onDepartureAlertChange
                        )
                        SettingSwitch(
                            "Som do despertador",
                            "Usa o toque de alarme do Android.",
                            config.soundEnabled,
                            onSoundChange,
                            Icons.Default.VolumeUp
                        )
                        SettingSwitch(
                            "Vibração",
                            "Vibra até dispensar o alarme.",
                            config.vibrationEnabled,
                            onVibrationChange,
                            Icons.Default.Vibration
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("Duração do alarme de chegada", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(
                                    "De 1 a 3.600 s; ao terminar, o alarme e o acompanhamento serão encerrados.",
                                    color = Muted,
                                    fontSize = 11.sp
                                )
                            }
                            ArrivalAlarmDurationField(
                                value = config.arrivalAlarmDurationSeconds,
                                onValueChange = onArrivalAlarmDurationChange
                            )
                        }
                    }
                }

                Text("Histórico por rota", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (journeyHistory.isEmpty()) {
                    Surface(color = Raised, shape = RoundedCornerShape(14.dp)) {
                        Text(
                            "Os tempos por origem e destino aparecerão aqui após uma chegada.",
                            color = Muted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    journeyHistory.forEach { item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedHistoryRoute = item
                                historyRecords = null
                                historyLoading = true
                            },
                            color = Raised,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                Text(
                                    item.originName ?: formatCoordinates(item.originLatitude, item.originLongitude),
                                    color = Mint,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("↓", color = Muted, fontSize = 13.sp)
                                    Spacer(Modifier.width(7.dp))
                                    Text(
                                        item.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(
                                        "MÉDIA" to item.averageDurationSeconds,
                                        "MENOR" to item.shortestDurationSeconds,
                                        "MAIOR" to item.longestDurationSeconds
                                    ).forEachIndexed { index, (label, seconds) ->
                                        Column(Modifier.weight(1f)) {
                                            Text(label, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                formatJourneyDuration(seconds),
                                                color = if (index == 0) Mint else Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                Text(
                                    "${item.journeyCount} ${if (item.journeyCount == 1) "trajeto concluído" else "trajetos concluídos"}",
                                    color = Muted,
                                    fontSize = 10.sp
                                )
                                Text("Toque para ver gráficos e viagens →", color = Blue, fontSize = 10.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                }
                else -> RouteAutomationSettings(
                modifier = Modifier.weight(1f),
                profiles = routeProfiles,
                schedules = routeSchedules,
                exactAlarmPermissionGranted = exactAlarmPermissionGranted,
                currentLocation = currentLocation,
                onSaveProfile = onSaveRouteProfile,
                onDeleteProfile = onDeleteRouteProfile,
                onSaveSchedule = onSaveRouteSchedule,
                onDeleteSchedule = onDeleteRouteSchedule,
                onRequestExactAlarmPermission = onRequestExactAlarmPermission
            )
            }
        }
    }
}

@Composable
private fun RouteHistoryDetailsScreen(
    modifier: Modifier,
    route: RouteJourneyStats,
    records: List<RouteJourneyRecord>,
    period: RouteHistoryPeriod,
    onPeriodChange: (RouteHistoryPeriod) -> Unit
) {
    val summaries = remember(records, period) { summarizeRoutePeriods(records, period) }
    val periodDescription = when (period) {
        RouteHistoryPeriod.WEEKDAY -> "dia da semana"
        RouteHistoryPeriod.MONTH -> "mês"
        RouteHistoryPeriod.YEAR -> "ano"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(color = Raised, shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        route.originName ?: formatCoordinates(route.originLatitude, route.originLongitude),
                        color = Mint,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("↓  ${route.name}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HistoryMetric("MÉDIA", route.averageDurationSeconds, true, Modifier.weight(1f))
                    HistoryMetric("MENOR", route.shortestDurationSeconds, false, Modifier.weight(1f))
                    HistoryMetric("MAIOR", route.longestDurationSeconds, false, Modifier.weight(1f))
                }
                Text(
                    "${route.journeyCount} ${if (route.journeyCount == 1) "trajeto concluído" else "trajetos concluídos"}",
                    color = Muted,
                    fontSize = 10.sp
                )
            }
        }

        Text("Estatísticas ao longo do tempo", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RouteHistoryPeriod.entries.forEach { option ->
                FilterChip(
                    selected = period == option,
                    onClick = { onPeriodChange(option) },
                    label = { Text(option.label) }
                )
            }
        }

        Text("Tempo médio por $periodDescription", color = Muted, fontSize = 11.sp)
        RouteHistoryBarChart(summaries)

        Text("Resumo por $periodDescription", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        summaries.forEach { summary ->
            Surface(color = Raised, shape = RoundedCornerShape(13.dp)) {
                Column(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(summary.label, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(
                            if (summary.journeyCount == 0) "Sem viagens"
                            else "${summary.journeyCount} ${if (summary.journeyCount == 1) "viagem" else "viagens"}",
                            color = Muted,
                            fontSize = 10.sp
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HistoryMetric("MÉDIA", summary.averageDurationSeconds, true, Modifier.weight(1f))
                        HistoryMetric("MENOR", summary.shortestDurationSeconds, false, Modifier.weight(1f))
                        HistoryMetric("MAIOR", summary.longestDurationSeconds, false, Modifier.weight(1f))
                    }
                }
            }
        }

        Text("Viagens, da mais recente", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (records.isEmpty()) {
            Surface(color = Raised, shape = RoundedCornerShape(13.dp)) {
                Text("Ainda não há viagens concluídas para esta rota.", color = Muted, modifier = Modifier.padding(14.dp))
            }
        } else {
            records.forEachIndexed { index, record ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(formatHistoryTimestamp(record.startedAtMillis), color = Color.White, fontSize = 12.sp)
                        record.completedAtMillis?.let {
                            Text("Chegada ${formatHistoryTimestamp(it)}", color = Muted, fontSize = 10.sp)
                        }
                    }
                    Text(
                        formatJourneyDuration(record.durationSeconds),
                        color = Mint,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
                if (index < records.lastIndex) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun HistoryMetric(label: String, seconds: Long?, highlight: Boolean, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        Text(
            seconds?.let(::formatJourneyDuration) ?: "—",
            color = if (highlight) Mint else Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun RouteHistoryBarChart(summaries: List<RoutePeriodSummary>) {
    val maximumAverage = summaries.mapNotNull { it.averageDurationSeconds }.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    Surface(color = Raised, shape = RoundedCornerShape(14.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            summaries.forEach { summary ->
                Column(
                    modifier = Modifier.width(68.dp).height(158.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        summary.averageDurationSeconds?.let(::formatCompactJourneyDuration) ?: "—",
                        color = if (summary.averageDurationSeconds == null) Muted else Mint,
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                        Canvas(Modifier.width(28.dp).fillMaxHeight()) {
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.06f),
                                size = size,
                                cornerRadius = CornerRadius(size.width / 2f)
                            )
                            val average = summary.averageDurationSeconds
                            if (average != null && average > 0L) {
                                val fraction = (average.toFloat() / maximumAverage.toFloat()).coerceIn(0f, 1f)
                                val barHeight = (size.height * fraction).coerceAtLeast(4.dp.toPx())
                                drawRoundRect(
                                    color = Mint,
                                    topLeft = Offset(0f, size.height - barHeight),
                                    size = Size(size.width, barHeight),
                                    cornerRadius = CornerRadius(size.width / 2f)
                                )
                            }
                        }
                    }
                    Text(summary.label, color = Color.White, fontSize = 10.sp, maxLines = 1)
                    Text("${summary.journeyCount} traj.", color = Muted, fontSize = 8.sp, maxLines = 1)
                }
            }
        }
    }
}

private fun summarizeRoutePeriods(
    records: List<RouteJourneyRecord>,
    period: RouteHistoryPeriod
): List<RoutePeriodSummary> {
    fun summarize(label: String, items: List<RouteJourneyRecord>): RoutePeriodSummary {
        val durations = items.map { it.durationSeconds }
        return RoutePeriodSummary(
            label = label,
            journeyCount = durations.size,
            averageDurationSeconds = durations.takeIf { it.isNotEmpty() }?.average()?.roundToLong(),
            shortestDurationSeconds = durations.minOrNull(),
            longestDurationSeconds = durations.maxOrNull()
        )
    }

    return when (period) {
        RouteHistoryPeriod.WEEKDAY -> {
            val grouped = records.groupBy { record ->
                Calendar.getInstance().apply { timeInMillis = record.startedAtMillis }.get(Calendar.DAY_OF_WEEK)
            }
            val days = listOf(
                Calendar.MONDAY to "Seg",
                Calendar.TUESDAY to "Ter",
                Calendar.WEDNESDAY to "Qua",
                Calendar.THURSDAY to "Qui",
                Calendar.FRIDAY to "Sex",
                Calendar.SATURDAY to "Sáb",
                Calendar.SUNDAY to "Dom"
            )
            days.map { (day, label) -> summarize(label, grouped[day].orEmpty()) }
        }
        RouteHistoryPeriod.MONTH -> records
            .groupBy { record ->
                Calendar.getInstance().apply { timeInMillis = record.startedAtMillis }.let {
                    it.get(Calendar.YEAR) to it.get(Calendar.MONTH)
                }
            }
            .toList()
            .sortedBy { (yearMonth, _) -> yearMonth.first * 12 + yearMonth.second }
            .map { (_, items) ->
                val label = SimpleDateFormat("MMM yyyy", Locale("pt", "BR"))
                    .format(Date(items.first().startedAtMillis))
                    .replace(".", "")
                summarize(label, items)
            }
        RouteHistoryPeriod.YEAR -> records
            .groupBy { record -> Calendar.getInstance().apply { timeInMillis = record.startedAtMillis }.get(Calendar.YEAR) }
            .toList()
            .sortedBy { it.first }
            .map { (year, items) -> summarize(year.toString(), items) }
    }
}

private fun formatHistoryTimestamp(timestamp: Long): String =
    SimpleDateFormat("EEE, dd/MM/yyyy · HH:mm", Locale("pt", "BR"))
        .format(Date(timestamp))
        .replaceFirstChar { it.titlecase(Locale("pt", "BR")) }

private fun formatCompactJourneyDuration(seconds: Long): String {
    val safeSeconds = seconds.coerceAtLeast(0L)
    val hours = safeSeconds / 3_600L
    val minutes = (safeSeconds % 3_600L) / 60L
    return when {
        hours > 0L -> "${hours}h${minutes.toString().padStart(2, '0')}"
        minutes > 0L -> "${minutes} min"
        else -> "${safeSeconds}s"
    }
}

@Composable
private fun RouteAutomationSettings(
    modifier: Modifier,
    profiles: List<RouteProfile>,
    schedules: List<RouteSchedule>,
    exactAlarmPermissionGranted: Boolean,
    currentLocation: LocationPoint?,
    onSaveProfile: (RouteProfile) -> Unit,
    onDeleteProfile: (RouteProfile) -> Unit,
    onSaveSchedule: (RouteSchedule) -> Unit,
    onDeleteSchedule: (RouteSchedule) -> Unit,
    onRequestExactAlarmPermission: () -> Unit
) {
    var profileEditorOpen by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<RouteProfile?>(null) }
    var scheduleEditorOpen by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<RouteSchedule?>(null) }
    var deletingProfile by remember { mutableStateOf<RouteProfile?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Crie rotas e horários para iniciar o acompanhamento sem adicionar controles à tela principal.",
            color = Muted,
            fontSize = 12.sp
        )

        Surface(color = Raised, shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Widgets na tela inicial", fontWeight = FontWeight.SemiBold)
                Text(
                    "Adicione o widget Desperta no Ponto pela lista de widgets do Android. Cada instância pode usar uma rota diferente; mantenha a tela pressionada para adicionar mais de um.",
                    color = Muted,
                    fontSize = 11.sp
                )
                Text(
                    "A quilometragem exibida é a distância direta estimada entre os pontos, não o percurso pelas ruas.",
                    color = Color(0xFFB8C4CC),
                    fontSize = 10.sp
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Rotas salvas", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { editingProfile = null; profileEditorOpen = true }) {
                Text("Nova rota")
            }
        }
        if (profiles.isEmpty()) {
            EmptyAutomationCard("Cadastre uma origem e um destino para criar seu primeiro widget ou horário automático.")
        } else {
            profiles.forEach { profile ->
                Surface(color = Raised, shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(profile.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("${profile.originName}  →  ${profile.destinationName}", color = Mint, fontSize = 11.sp)
                        Text("Raio de chegada: ${profile.arrivalRadiusMeters} m", color = Muted, fontSize = 10.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { editingProfile = profile; profileEditorOpen = true }) { Text("Editar") }
                            TextButton(onClick = { deletingProfile = profile }) { Text("Excluir") }
                        }
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Inícios automáticos", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick = { editingSchedule = null; scheduleEditorOpen = true },
                enabled = profiles.isNotEmpty()
            ) { Text("Novo horário") }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !exactAlarmPermissionGranted) {
            Surface(color = Color(0xFF332B1E), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Precisão de horário não autorizada", color = Color(0xFFFFD38A), fontWeight = FontWeight.SemiBold)
                    Text(
                        "Sem “Alarmes e lembretes”, o Android ainda tentará iniciar a rotina, mas poderá atrasá-la para economizar bateria.",
                        color = Color(0xFFE6D9C2),
                        fontSize = 11.sp
                    )
                    TextButton(onClick = onRequestExactAlarmPermission) { Text("Autorizar horários precisos") }
                }
            }
        }

        if (schedules.isEmpty()) {
            EmptyAutomationCard("Exemplo: Casa → Trabalho às 07:00 e Trabalho → Casa às 17:00, todos os dias.")
        } else {
            schedules.forEach { schedule ->
                val profile = profiles.firstOrNull { it.id == schedule.routeProfileId }
                if (profile != null) {
                    Surface(color = Raised, shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("%02d:%02d".format(schedule.hour, schedule.minute), color = Mint, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Text(profile.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                                Switch(
                                    checked = schedule.enabled,
                                    onCheckedChange = { onSaveSchedule(schedule.copy(enabled = it)) }
                                )
                            }
                            Text(formatScheduleDays(schedule.daysMask), color = Muted, fontSize = 11.sp)
                            Text(
                                if (schedule.announceStart) "Anúncio por voz ativado" else "Sem anúncio por voz",
                                color = Muted,
                                fontSize = 10.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { editingSchedule = schedule; scheduleEditorOpen = true }) { Text("Editar") }
                                TextButton(onClick = { onDeleteSchedule(schedule) }) { Text("Excluir") }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    if (profileEditorOpen) {
        RouteProfileEditorDialog(
            profile = editingProfile,
            currentLocation = currentLocation,
            onDismiss = { profileEditorOpen = false },
            onSave = {
                onSaveProfile(it)
                profileEditorOpen = false
            }
        )
    }
    if (scheduleEditorOpen) {
        RouteScheduleEditorDialog(
            profiles = profiles,
            schedule = editingSchedule,
            onDismiss = { scheduleEditorOpen = false },
            onSave = {
                onSaveSchedule(it)
                scheduleEditorOpen = false
            }
        )
    }
    deletingProfile?.let { profile ->
        AlertDialog(
            onDismissRequest = { deletingProfile = null },
            title = { Text("Excluir rota?") },
            text = { Text("Os horários associados também serão removidos. O histórico de viagens concluídas será preservado.") },
            confirmButton = {
                TextButton(onClick = { onDeleteProfile(profile); deletingProfile = null }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { deletingProfile = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun EmptyAutomationCard(message: String) {
    Surface(color = Raised, shape = RoundedCornerShape(14.dp)) {
        Text(message, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(14.dp))
    }
}

@Composable
private fun RouteProfileEditorDialog(
    profile: RouteProfile?,
    currentLocation: LocationPoint?,
    onDismiss: () -> Unit,
    onSave: (RouteProfile) -> Unit
) {
    val context = LocalContext.current
    var routeName by remember(profile?.id) { mutableStateOf(profile?.name.orEmpty()) }
    var arrivalRadiusText by remember(profile?.id) { mutableStateOf(profile?.arrivalRadiusMeters?.toString() ?: "500") }
    var mapPickerTarget by remember(profile?.id) { mutableStateOf<RouteLocationTarget?>(null) }
    var origin by remember(profile?.id) {
        mutableStateOf(profile?.let { PlaceResult(it.originName, it.originLatitude, it.originLongitude) })
    }
    var destination by remember(profile?.id) {
        mutableStateOf(profile?.let { PlaceResult(it.destinationName, it.destinationLatitude, it.destinationLongitude) })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (profile == null) "Nova rota" else "Editar rota") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = routeName,
                    onValueChange = { routeName = it },
                    label = { Text("Nome do perfil") },
                    placeholder = { Text("Ex.: Casa → Trabalho") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                RouteLocationField(
                    label = "Origem",
                    initialPlace = origin,
                    currentLocation = currentLocation,
                    allowCurrentLocation = true,
                    onSelectionChanged = { origin = it },
                    onPickOnMap = { mapPickerTarget = RouteLocationTarget.ORIGIN }
                )
                RouteLocationField(
                    label = "Destino",
                    initialPlace = destination,
                    currentLocation = currentLocation,
                    allowCurrentLocation = false,
                    onSelectionChanged = { destination = it },
                    onPickOnMap = { mapPickerTarget = RouteLocationTarget.DESTINATION }
                )
                OutlinedTextField(
                    value = arrivalRadiusText,
                    onValueChange = { arrivalRadiusText = it.filter(Char::isDigit).take(5) },
                    label = { Text("Raio de chegada (m)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = origin != null && destination != null,
                onClick = {
                    val from = origin ?: return@TextButton
                    val to = destination ?: return@TextButton
                    val name = routeName.trim().ifBlank { "${from.displayName} → ${to.displayName}" }
                    onSave(
                        RouteProfile(
                            id = profile?.id ?: 0L,
                            name = name,
                            originName = from.displayName,
                            originLatitude = from.latitude,
                            originLongitude = from.longitude,
                            destinationName = to.displayName,
                            destinationLatitude = to.latitude,
                            destinationLongitude = to.longitude,
                            arrivalRadiusMeters = (arrivalRadiusText.toIntOrNull() ?: 500).coerceIn(50, 50000)
                        )
                    )
                }
            ) { Text("Salvar rota") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )

    mapPickerTarget?.let { target ->
        val initialPlace = if (target == RouteLocationTarget.ORIGIN) origin else destination
        RouteLocationMapPickerDialog(
            targetLabel = target.label,
            initialPlace = initialPlace,
            currentLocation = currentLocation,
            onDismiss = { mapPickerTarget = null },
            onPlaceSelected = { place ->
                if (target == RouteLocationTarget.ORIGIN) origin = place else destination = place
                mapPickerTarget = null
            }
        )
    }
}

@Composable
private fun RouteLocationMapPickerDialog(
    targetLabel: String,
    initialPlace: PlaceResult?,
    currentLocation: LocationPoint?,
    onDismiss: () -> Unit,
    onPlaceSelected: (PlaceResult) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedPlace by remember(targetLabel, initialPlace) { mutableStateOf(initialPlace) }
    var resolvingAddress by remember(targetLabel, initialPlace) { mutableStateOf(false) }
    var selectionToken by remember(targetLabel, initialPlace) { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.98f).fillMaxHeight(0.94f),
            color = Background,
            shape = RoundedCornerShape(20.dp)
        ) {
            Box {
                MapPicker(
                    modifier = Modifier.fillMaxSize(),
                    selectedLatitude = selectedPlace?.latitude,
                    selectedLongitude = selectedPlace?.longitude,
                    arrivalRadiusMeters = 500,
                    currentLatitude = currentLocation?.latitude,
                    currentLongitude = currentLocation?.longitude,
                    centerRequest = 0,
                    mapLayer = MapLayerMode.MAP,
                    showArrivalRadius = false,
                    selectedMarkerTitle = "Ponto escolhido",
                    onLocationSelected = { latitude, longitude ->
                        selectionToken += 1
                        val thisSelection = selectionToken
                        selectedPlace = PlaceResult("Identificando endereço…", latitude, longitude)
                        resolvingAddress = true
                        scope.launch {
                            val address = withContext(Dispatchers.IO) {
                                reverseGeocode(context, latitude, longitude)
                            }
                            if (thisSelection == selectionToken) {
                                onPlaceSelected(PlaceResult(address, latitude, longitude))
                            }
                        }
                    }
                )

                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(12.dp),
                    color = SheetColor,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Marcar $targetLabel no mapa", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (resolvingAddress) "Local marcado. Identificando endereço…"
                            else "Toque no ponto desejado; o formulário será retomado automaticamente.",
                            color = Muted,
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
                    color = SheetColor,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            selectedPlace?.displayName ?: "Nenhum ponto selecionado",
                            color = if (selectedPlace == null) Muted else Color.White,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onDismiss, enabled = !resolvingAddress) { Text("Cancelar") }
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteLocationField(
    label: String,
    initialPlace: PlaceResult?,
    currentLocation: LocationPoint?,
    allowCurrentLocation: Boolean,
    onSelectionChanged: (PlaceResult?) -> Unit,
    onPickOnMap: () -> Unit
) {
    var query by remember(label) { mutableStateOf(initialPlace?.displayName.orEmpty()) }
    var results by remember(label) { mutableStateOf(emptyList<PlaceResult>()) }
    var error by remember(label) { mutableStateOf<String?>(null) }
    var searching by remember(label) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(initialPlace) {
        if (initialPlace != null) {
            query = initialPlace.displayName
            results = emptyList()
            error = null
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; onSelectionChanged(null); error = null },
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(
                enabled = !searching && query.isNotBlank(),
                onClick = {
                    searching = true
                    error = null
                    scope.launch {
                        runCatching { withContext(Dispatchers.IO) { SearchRepository.search(query) } }
                            .onSuccess { results = it; if (it.isEmpty()) error = "Nenhum local encontrado." }
                            .onFailure { error = "Não foi possível buscar esse local." }
                        searching = false
                    }
                }
            ) { Text(if (searching) "Buscando…" else "Buscar") }
            if (allowCurrentLocation && currentLocation != null) {
                TextButton(onClick = {
                    val place = PlaceResult("Minha localização", currentLocation.latitude, currentLocation.longitude)
                    query = place.displayName
                    results = emptyList()
                    onSelectionChanged(place)
                }) { Text("Usar GPS atual") }
            }
        }
        OutlinedButton(onClick = onPickOnMap, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text("Marcar no mapa")
        }
        error?.let { Text(it, color = Color(0xFFFFB4A5), fontSize = 10.sp) }
        results.take(4).forEach { place ->
            TextButton(
                onClick = {
                    query = place.displayName
                    results = emptyList()
                    onSelectionChanged(place)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(place.displayName, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun RouteScheduleEditorDialog(
    profiles: List<RouteProfile>,
    schedule: RouteSchedule?,
    onDismiss: () -> Unit,
    onSave: (RouteSchedule) -> Unit
) {
    val context = LocalContext.current
    var profileId by remember(schedule?.id, profiles) {
        mutableStateOf(schedule?.routeProfileId ?: profiles.firstOrNull()?.id ?: 0L)
    }
    var hour by remember(schedule?.id) { mutableIntStateOf(schedule?.hour ?: 7) }
    var minute by remember(schedule?.id) { mutableIntStateOf(schedule?.minute ?: 0) }
    var daysMask by remember(schedule?.id) { mutableIntStateOf(schedule?.daysMask ?: 0b1111111) }
    var announceStart by remember(schedule?.id) { mutableStateOf(schedule?.announceStart ?: true) }
    var enabled by remember(schedule?.id) { mutableStateOf(schedule?.enabled ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (schedule == null) "Novo início automático" else "Editar início automático") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text("Rota", fontWeight = FontWeight.SemiBold)
                profiles.forEach { profile ->
                    FilterChip(
                        selected = profileId == profile.id,
                        onClick = { profileId = profile.id },
                        label = { Text(profile.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
                OutlinedButton(
                    onClick = {
                        TimePickerDialog(context, { _, selectedHour, selectedMinute ->
                            hour = selectedHour
                            minute = selectedMinute
                        }, hour, minute, true).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Horário: %02d:%02d".format(hour, minute)) }
                Text("Dias", fontWeight = FontWeight.SemiBold)
                DaysSelector(daysMask, onChange = { daysMask = it })
                SettingSwitch(
                    "Anunciar por voz",
                    "Fala a rota ao iniciar automaticamente.",
                    announceStart,
                    { announceStart = it }
                )
                SettingSwitch(
                    "Rotina ativa",
                    "Quando chegar, o histórico dessa rota será atualizado.",
                    enabled,
                    { enabled = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = profileId > 0L && daysMask != 0,
                onClick = {
                    onSave(
                        RouteSchedule(
                            id = schedule?.id ?: 0L,
                            routeProfileId = profileId,
                            hour = hour,
                            minute = minute,
                            daysMask = daysMask,
                            enabled = enabled,
                            announceStart = announceStart
                        )
                    )
                }
            ) { Text("Salvar horário") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

private fun formatScheduleDays(mask: Int): String {
    if (mask == 0b1111111) return "Todos os dias"
    val labels = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")
    return labels.filterIndexed { index, _ -> mask and (1 shl index) != 0 }
        .joinToString(" · ")
        .ifBlank { "Nenhum dia selecionado" }
}

@Composable
private fun BottomDestinationSheet(
    modifier: Modifier,
    config: DestinationConfig,
    permissionGranted: Boolean,
    locationAvailable: Boolean,
    locationAccuracyMeters: Float?,
    distanceToDestinationMeters: Float?,
    estimatedArrivalSeconds: Long?,
    permissionMessage: String?,
    trackingActive: Boolean,
    collapseRequest: Int,
    onPermission: () -> Unit,
    onUseCurrent: () -> Unit,
    onSheetCollapsedChange: (Boolean) -> Unit,
    onStop: () -> Unit
) {
    val density = LocalDensity.current
    val collapsedVisiblePx = with(density) { 54.dp.toPx() }
    val initialSheetHeightPx = with(density) { 430.dp.roundToPx() }
    var sheetHeightPx by remember(density) { mutableIntStateOf(initialSheetHeightPx) }
    var sheetOffsetPx by remember(density, collapsedVisiblePx) {
        mutableStateOf((initialSheetHeightPx - collapsedVisiblePx).coerceAtLeast(0f))
    }
    val maxOffsetPx = (sheetHeightPx - collapsedVisiblePx).coerceAtLeast(0f)
    val animatedSheetOffsetPx by animateFloatAsState(
        targetValue = sheetOffsetPx,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "bottom_sheet_offset"
    )
    val sheetCollapsed = maxOffsetPx > 0f && animatedSheetOffsetPx > maxOffsetPx * 0.72f
    LaunchedEffect(sheetCollapsed) {
        onSheetCollapsedChange(sheetCollapsed)
    }
    LaunchedEffect(maxOffsetPx) {
        sheetOffsetPx = sheetOffsetPx.coerceIn(0f, maxOffsetPx)
    }
    LaunchedEffect(collapseRequest, maxOffsetPx) {
        if (collapseRequest > 0 && maxOffsetPx > 0f) sheetOffsetPx = maxOffsetPx
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            // Mantém a área do painel estável quando entram as informações de acompanhamento.
            // O conteúdo continua rolável, mas o topo do painel não muda ao ativar o alarme.
            .height(430.dp)
            .onSizeChanged { sheetHeightPx = it.height }
            .offset { IntOffset(0, animatedSheetOffsetPx.roundToInt()) }
            .navigationBarsPadding(),
        color = SheetColor,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 18.dp
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = if (sheetCollapsed) 0.dp else 14.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (sheetCollapsed) 54.dp else 30.dp)
                    .pointerInput(maxOffsetPx) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                sheetOffsetPx = (sheetOffsetPx + dragAmount).coerceIn(0f, maxOffsetPx)
                            },
                            onDragEnd = {
                                sheetOffsetPx = if (sheetOffsetPx > maxOffsetPx * 0.3f) maxOffsetPx else 0f
                            },
                            onDragCancel = {
                                sheetOffsetPx = if (sheetOffsetPx > maxOffsetPx * 0.3f) maxOffsetPx else 0f
                            }
                        )
                    }
                    .clickable {
                        sheetOffsetPx = if (sheetOffsetPx > 0f) 0f else maxOffsetPx
                    },
                contentAlignment = Alignment.Center
            ) {
                if (sheetCollapsed) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CollapsedMetric(
                            modifier = Modifier.weight(1f),
                            label = "Distância restante:",
                            value = formatRemainingDistance(distanceToDestinationMeters, config.arrivalRadiusMeters),
                            color = proximityColor(distanceToDestinationMeters, config.arrivalRadiusMeters)
                        )
                        Icon(
                            imageVector = Icons.Default.ExpandLess,
                            contentDescription = "Mostrar painel",
                            tint = Mint,
                            modifier = Modifier.size(30.dp)
                        )
                        CollapsedMetric(
                            modifier = Modifier.weight(1f),
                            label = "Tempo estimado:",
                            value = formatCollapsedTime(estimatedArrivalSeconds, distanceToDestinationMeters, config.arrivalRadiusMeters),
                            color = proximityColor(distanceToDestinationMeters, config.arrivalRadiusMeters)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Ocultar painel",
                        tint = Mint,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            if (!sheetCollapsed) Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (config.hasDestination()) "Resumo do destino" else "Escolha um destino",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (config.hasDestination()) "Raio de chegada: ${config.arrivalRadiusMeters} m" else "Toque no mapa ou pesquise um local acima",
                        color = Muted,
                        fontSize = 12.sp
                    )
                }
                Surface(color = Color(0x263DD7A9), shape = CircleShape, modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.LocationOn, null, tint = Mint)
                    }
                }
            }

            if (!permissionGranted) {
                PermissionRow(onPermission)
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (locationAvailable) Modifier else Modifier.clickable(onClick = onPermission)),
                    color = if (locationAvailable) Color(0x201E9B78) else Color(0x202A7FC1),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (locationAvailable) Icons.Default.Check else Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = if (locationAvailable) Mint else Blue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (locationAvailable) "Localização ativa" else "Localização disponível",
                                color = if (locationAvailable) Color(0xFFC4F5E4) else Color(0xFFCFE8FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (locationAvailable) {
                                    "GPS em tempo real · precisão ±${locationAccuracyMeters?.roundToInt() ?: 0} m"
                                } else {
                                    "Toque para obter a posição atual"
                                },
                                color = Muted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
            permissionMessage?.let {
                Text(it, color = Muted, fontSize = 10.sp, maxLines = 2, modifier = Modifier.padding(horizontal = 2.dp))
            }

            if (!config.hasDestination()) {
                OutlinedButton(onClick = onUseCurrent, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.MyLocation, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Usar minha localização atual como destino")
                }
            } else {
                distanceToDestinationMeters?.let { distance ->
                    val inside = distance <= config.arrivalRadiusMeters
                    Surface(
                        color = if (inside) Color(0x303DD7A9) else Raised,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SummaryMetric(
                                    modifier = Modifier.weight(1f),
                                    label = "DISTÂNCIA RESTANTE",
                                    value = formatRemainingDistance(distanceToDestinationMeters, config.arrivalRadiusMeters),
                                    color = proximityColor(distanceToDestinationMeters, config.arrivalRadiusMeters)
                                )
                                SummaryMetric(
                                    modifier = Modifier.weight(1f),
                                    label = "TEMPO ESTIMADO",
                                    value = when {
                                        inside -> "Chegou"
                                        estimatedArrivalSeconds != null -> formatEstimatedTime(estimatedArrivalSeconds)
                                        else -> "Aguardando"
                                    },
                                    color = if (inside) Mint else Blue
                                )
                            }
                            Text(
                                if (inside) {
                                    "Você já está dentro do raio de chegada."
                                } else {
                                    "Raio de chegada: ${config.arrivalRadiusMeters} m · até a borda"
                                },
                                color = if (inside) Mint else Muted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                if (trackingActive) {
                    Button(
                        onClick = onStop,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7D3F4B))
                    ) { Text("Desativar acompanhamento") }
                }
            }
        }
    }
}

@Composable
private fun MapTravelHud(
    modifier: Modifier,
    active: Boolean,
    elapsedSeconds: Long?,
    averageSeconds: Long?,
    shortestSeconds: Long?,
    longestSeconds: Long?,
    journeyCount: Int,
    speedMetersPerSecond: Float?
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier
            .animateContentSize(animationSpec = tween(260, easing = FastOutSlowInEasing))
            .width(if (expanded) 174.dp else 148.dp)
            .clickable { expanded = !expanded }
            .graphicsLayer { alpha = if (expanded) 0.70f else 0.50f },
        color = Color(0xEE151B21),
        shape = RoundedCornerShape(if (expanded) 16.dp else 13.dp),
        shadowElevation = 12.dp
    ) {
        AnimatedContent(
            targetState = expanded,
            transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(100)) },
            label = "travel_hud_expansion"
        ) { showDetails ->
            Column(
                Modifier.padding(
                    horizontal = if (showDetails) 12.dp else 10.dp,
                    vertical = if (showDetails) 10.dp else 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(if (showDetails) 7.dp else 4.dp)
            ) {
                if (showDetails) {
                    TravelHudMetric(
                        label = if (active) "TEMPO DO TRAJETO" else "ÚLTIMA VIAGEM",
                        value = elapsedSeconds?.let(::formatJourneyDuration) ?: "--:--:--",
                        valueColor = Color.White
                    )
                    TravelHudMetric(
                        label = if (journeyCount > 0) "MÉDIA · $journeyCount ${if (journeyCount == 1) "VIAGEM" else "VIAGENS"}" else "MÉDIA NESTA ROTA",
                        value = averageSeconds?.let(::formatJourneyDuration) ?: "Sem histórico",
                        valueColor = if (averageSeconds != null) Mint else Muted
                    )
                    if (shortestSeconds != null && longestSeconds != null) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("MENOR", color = Color(0xFFC3CDD4), fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                                Text(formatJourneyDuration(shortestSeconds), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("MAIOR", color = Color(0xFFC3CDD4), fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                                Text(formatJourneyDuration(longestSeconds), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(1.dp),
                        color = Color(0x405C6871)
                    ) {}
                    TravelHudMetric(
                        label = "VELOCIDADE",
                        value = formatSpeedKmH(speedMetersPerSecond),
                        valueColor = Blue
                    )
                } else {
                    TravelHudMetric(
                        label = "VELOCIDADE",
                        value = formatSpeedKmH(speedMetersPerSecond),
                        valueColor = Blue
                    )
                    TravelHudMetric(
                        label = if (active) "TEMPO DO TRAJETO" else "TEMPO",
                        value = elapsedSeconds?.let(::formatJourneyDuration) ?: "--:--:--",
                        valueColor = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun TravelHudMetric(label: String, value: String, valueColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            label,
            color = Color(0xFFC3CDD4),
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(
            value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun CollapsedMetric(
    modifier: Modifier,
    label: String,
    value: String,
    color: Color
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            label,
            color = color.copy(alpha = 0.78f),
            fontSize = 9.sp,
            lineHeight = 9.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        Text(
            value,
            color = color,
            fontSize = 15.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun SummaryMetric(
    modifier: Modifier,
    label: String,
    value: String,
    color: Color
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            label,
            color = Muted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        Text(
            value,
            color = color,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

private fun formatRemainingDistance(distanceToDestinationMeters: Float?, arrivalRadiusMeters: Int): String {
    val distance = distanceToDestinationMeters ?: return "—"
    val remaining = (distance - arrivalRadiusMeters).coerceAtLeast(0f)
    return if (remaining < 1_000f) {
        "${remaining.roundToInt()} m"
    } else {
        String.format(Locale("pt", "BR"), "%.1f km", remaining / 1_000f)
    }
}

private fun formatJourneyDuration(totalSeconds: Long): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0L)
    val hours = safeSeconds / 3_600L
    val minutes = (safeSeconds % 3_600L) / 60L
    val seconds = safeSeconds % 60L
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}

private fun formatSpeedKmH(speedMetersPerSecond: Float?): String {
    val speedKmH = (speedMetersPerSecond ?: 0f).coerceAtLeast(0f) * 3.6f
    return String.format(Locale("pt", "BR"), "%.1f km/h", speedKmH)
}

private suspend fun loadDestinationHistory(context: Context): List<RouteJourneyStats> =
    withContext(Dispatchers.IO) {
        val historyStore = DestinationHistoryStore(context)
        try {
            historyStore.loadCompletedStats()
        } finally {
            historyStore.close()
        }
    }

private suspend fun loadRouteAutomation(context: Context): Pair<List<RouteProfile>, List<RouteSchedule>> =
    withContext(Dispatchers.IO) {
        val store = DestinationHistoryStore(context)
        try {
            store.loadRouteProfiles() to store.loadRouteSchedules()
        } finally {
            store.close()
        }
    }

private fun formatCollapsedTime(
    estimatedArrivalSeconds: Long?,
    distanceToDestinationMeters: Float?,
    arrivalRadiusMeters: Int
): String {
    val distance = distanceToDestinationMeters ?: return "—"
    if (distance <= arrivalRadiusMeters) return "Chegou"
    return estimatedArrivalSeconds?.let(::formatEstimatedTime) ?: "—"
}

private fun formatCoordinates(latitude: Double, longitude: Double): String =
    String.format(Locale("pt", "BR"), "Origem · %.4f, %.4f", latitude, longitude)

private fun proximityColor(distanceToDestinationMeters: Float?, arrivalRadiusMeters: Int): Color {
    val distance = distanceToDestinationMeters ?: return Muted
    val remaining = (distance - arrivalRadiusMeters).coerceAtLeast(0f)
    // A cor começa a aquecer aos 500 m e chega ao abóbora completo aos 100 m.
    val fraction = ((500f - remaining) / 400f).coerceIn(0f, 1f)
    return Color(
        red = Muted.red + (WarmOrange.red - Muted.red) * fraction,
        green = Muted.green + (WarmOrange.green - Muted.green) * fraction,
        blue = Muted.blue + (WarmOrange.blue - Muted.blue) * fraction,
        alpha = 1f
    )
}

@Composable
private fun PermissionRow(onPermission: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF24333B)), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.MyLocation, null, tint = Blue)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Permita sua localização", fontWeight = FontWeight.SemiBold)
                Text("Necessária para mostrar você no mapa.", color = Muted, fontSize = 11.sp)
            }
            TextButton(onClick = onPermission) { Text("Permitir") }
        }
    }
}

@Composable
private fun CompactRadiusField(label: String, value: Int, onValueChange: (Int) -> Unit, modifier: Modifier) {
    Column(modifier) {
        Text(label, color = Muted, fontSize = 11.sp)
        OutlinedTextField(
            value = value.toString(),
            onValueChange = { it.toIntOrNull()?.let { parsed -> onValueChange(parsed.coerceIn(50, 100000)) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            suffix = { Text("m", color = Muted) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

@Composable
private fun ArrivalAlarmDurationField(value: Int, onValueChange: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    val focusManager = LocalFocusManager.current

    fun commit() {
        val seconds = (text.toIntOrNull() ?: value).coerceIn(1, 3600)
        text = seconds.toString()
        onValueChange(seconds)
    }

    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            val digits = input.filter(Char::isDigit).take(4)
            text = digits
            digits.toIntOrNull()?.takeIf { it in 1..3600 }?.let(onValueChange)
        },
        modifier = Modifier.width(112.dp).onFocusChanged { if (!it.isFocused) commit() },
        singleLine = true,
        suffix = { Text("s", color = Muted) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            commit()
            focusManager.clearFocus()
        })
    )
}

@Composable
private fun ModeSelector(selected: ActivationMode, onSelected: (ActivationMode) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ActivationMode.entries.forEach { mode ->
            val active = selected == mode
            Surface(
                modifier = Modifier.weight(1f).clickable { onSelected(mode) },
                color = if (active) Mint else Raised,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    mode.label,
                    color = if (active) Background else Color.White,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(vertical = 10.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DaysSelector(mask: Int, onChange: (Int) -> Unit) {
    val days = listOf("D", "S", "T", "Q", "Q", "S", "S")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEachIndexed { index, day ->
            val checked = mask and (1 shl index) != 0
            FilterChip(
                selected = checked,
                onClick = { onChange(if (checked) mask and (1 shl index).inv() else mask or (1 shl index)) },
                label = { Text(day) }
            )
        }
    }
}

@Composable
private fun SettingSwitch(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, icon: ImageVector? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon?.let {
            Icon(it, null, tint = Mint, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(7.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(description, color = Muted, fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun MapPicker(
    modifier: Modifier,
    selectedLatitude: Double?,
    selectedLongitude: Double?,
    arrivalRadiusMeters: Int,
    currentLatitude: Double?,
    currentLongitude: Double?,
    centerRequest: Int,
    mapLayer: MapLayerMode,
    onLocationSelected: (Double, Double) -> Unit,
    showArrivalRadius: Boolean = true,
    selectedMarkerTitle: String = "Destino"
) {
    val context = LocalContext.current
    val onSelectState = rememberUpdatedState(onLocationSelected)
    val mapView = remember {
        val mapCache = File(context.cacheDir, "osmdroid").apply { mkdirs() }
        val tileCache = File(mapCache, "tiles").apply { mkdirs() }
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            userAgentValue = context.packageName
            osmdroidBasePath = mapCache
            osmdroidTileCache = tileCache
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setUseDataConnection(true)
            minZoomLevel = 3.0
            maxZoomLevel = 20.0
            controller.setZoom(if (selectedLatitude != null || currentLatitude != null) 15.0 else 4.0)
            controller.setCenter(
                GeoPoint(
                    selectedLatitude ?: currentLatitude ?: -14.2350,
                    selectedLongitude ?: currentLongitude ?: -51.9253
                )
            )
        }
    }
    val destinationMarker = remember(mapView) {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_map_destination)
            title = selectedMarkerTitle
        }
    }
    val arrivalCircle = remember(mapView) {
        Polygon(mapView).apply {
            fillPaint.color = android.graphics.Color.argb(58, 102, 217, 181)
            outlinePaint.color = android.graphics.Color.rgb(102, 217, 181)
            outlinePaint.strokeWidth = 4f
            title = "Raio de chegada"
        }
    }
    val currentMarker = remember(mapView) {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_map_current)
            title = "Sua localização"
        }
    }
    val rotationOverlay = remember(mapView) {
        RotationGestureOverlay(mapView).apply { isEnabled = true }
    }
    val tapOverlay = remember(mapView) {
        MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(point: GeoPoint): Boolean {
                onSelectState.value(point.latitude, point.longitude)
                return true
            }

            override fun longPressHelper(point: GeoPoint): Boolean = false
        })
    }
    val copyrightOverlay = remember(mapView) { CopyrightOverlay(context) }

    LaunchedEffect(Unit) {
        if (!mapView.overlays.contains(tapOverlay)) mapView.overlays.add(tapOverlay)
        if (!mapView.overlays.contains(copyrightOverlay)) mapView.overlays.add(copyrightOverlay)
        if (!mapView.overlays.contains(rotationOverlay)) mapView.overlays.add(rotationOverlay)
        mapView.onResume()
    }
    LaunchedEffect(mapLayer) {
        mapView.setTileSource(
            when (mapLayer) {
                MapLayerMode.MAP -> TileSourceFactory.MAPNIK
                MapLayerMode.ROADS -> RoadsTileSource
                MapLayerMode.SATELLITE -> SatelliteTileSource
            }
        )
        mapView.invalidate()
    }
    LaunchedEffect(selectedLatitude, selectedLongitude, arrivalRadiusMeters, showArrivalRadius, selectedMarkerTitle) {
        if (selectedLatitude != null && selectedLongitude != null) {
            val destinationPoint = GeoPoint(selectedLatitude, selectedLongitude)
            arrivalCircle.points = Polygon.pointsAsCircle(destinationPoint, arrivalRadiusMeters.toDouble())
            arrivalCircle.title = "Raio de chegada: $arrivalRadiusMeters m"
            if (showArrivalRadius) {
                if (!mapView.overlays.contains(arrivalCircle)) mapView.overlays.add(arrivalCircle)
            } else {
                mapView.overlays.remove(arrivalCircle)
            }
            destinationMarker.position = destinationPoint
            destinationMarker.title = selectedMarkerTitle
            if (!mapView.overlays.contains(destinationMarker)) mapView.overlays.add(destinationMarker)
            mapView.controller.animateTo(destinationMarker.position)
            mapView.controller.setZoom(zoomForArrivalRadius(arrivalRadiusMeters))
        } else {
            mapView.overlays.remove(arrivalCircle)
            mapView.overlays.remove(destinationMarker)
        }
        mapView.invalidate()
    }
    LaunchedEffect(currentLatitude, currentLongitude) {
        if (currentLatitude != null && currentLongitude != null) {
            currentMarker.position = GeoPoint(currentLatitude, currentLongitude)
            if (!mapView.overlays.contains(currentMarker)) mapView.overlays.add(currentMarker)
        } else {
            mapView.overlays.remove(currentMarker)
        }
        mapView.invalidate()
    }
    LaunchedEffect(centerRequest) {
        if (centerRequest > 0 && currentLatitude != null && currentLongitude != null) {
            mapView.controller.animateTo(GeoPoint(currentLatitude, currentLongitude))
            mapView.controller.setZoom(16.0)
        }
    }
    DisposableEffect(mapView) {
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }
    AndroidView(factory = { mapView }, modifier = modifier)
}

private fun zoomForArrivalRadius(radiusMeters: Int): Double = when {
    radiusMeters <= 75 -> 18.0
    radiusMeters <= 150 -> 17.0
    radiusMeters <= 300 -> 16.0
    radiusMeters <= 600 -> 15.0
    radiusMeters <= 1_200 -> 14.0
    radiusMeters <= 3_000 -> 13.0
    radiusMeters <= 7_000 -> 12.0
    else -> 10.5
}

private fun distanceBetween(
    fromLatitude: Double?,
    fromLongitude: Double?,
    toLatitude: Double?,
    toLongitude: Double?
): Float? {
    if (fromLatitude == null || fromLongitude == null || toLatitude == null || toLongitude == null) return null
    val result = FloatArray(1)
    Location.distanceBetween(fromLatitude, fromLongitude, toLatitude, toLongitude, result)
    return result[0]
}

private fun smoothedMovementSpeed(location: Location, previousSpeed: Float?): Float? {
    if (!location.hasSpeed() || location.speed < 0.5f) return null
    val measured = location.speed.coerceAtMost(80f)
    return previousSpeed?.let { previous -> previous * 0.65f + measured * 0.35f } ?: measured
}

private fun estimateArrivalSeconds(
    distanceToDestinationMeters: Float?,
    arrivalRadiusMeters: Int,
    speedMetersPerSecond: Float?
): Long? {
    val distance = distanceToDestinationMeters ?: return null
    val remaining = (distance - arrivalRadiusMeters).coerceAtLeast(0f)
    if (remaining <= 0f) return 0L
    val speed = speedMetersPerSecond?.takeIf { it >= 0.5f } ?: return null
    return (remaining / speed).roundToLong().coerceAtLeast(1L)
}

private fun formatEstimatedTime(totalSeconds: Long): String {
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0L -> "$hours h $minutes min ${seconds.toString().padStart(2, '0')} s"
        minutes > 0L -> "$minutes min ${seconds.toString().padStart(2, '0')} s"
        else -> "$seconds s"
    }
}

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun hasPreciseLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

@Suppress("DEPRECATION")
private fun reverseGeocode(context: Context, latitude: Double, longitude: Double): String {
    return try {
        Geocoder(context, Locale("pt", "BR")).getFromLocation(latitude, longitude, 1)
            ?.firstOrNull()?.getAddressLine(0) ?: "Ponto selecionado"
    } catch (_: Exception) {
        "Ponto selecionado"
    }
}

data class PlaceResult(val displayName: String, val latitude: Double, val longitude: Double)

private object SearchRepository {
    fun search(query: String): List<PlaceResult> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val connection = (URL("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=5&q=$encoded").openConnection() as HttpURLConnection)
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "DespertaNoPonto/1.0 (Android)")
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        return try {
            if (connection.responseCode !in 200..299) return emptyList()
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val array = JSONArray(body)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        PlaceResult(
                            displayName = item.optString("display_name", "Local encontrado"),
                            latitude = item.optString("lat").toDoubleOrNull() ?: continue,
                            longitude = item.optString("lon").toDoubleOrNull() ?: continue
                        )
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
