package br.com.despertanoponto

import android.annotation.SuppressLint
import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import java.util.Locale

class TrackingService : Service() {
    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var config = DestinationConfig()
    private var activeRouteProfile: RouteProfile? = null
    private var speechEngine: TextToSpeech? = null
    private var lastStatusNotificationAt = 0L
    private var originGeocodeKey: String? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val historyStoreDelegate = lazy(LazyThreadSafetyMode.NONE) { DestinationHistoryStore(this) }
    private val historyStore by historyStoreDelegate

    override fun onCreate() {
        super.onCreate()
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let(::processLocation)
            }
        }
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopTracking()
            return START_NOT_STICKY
        }
        val action = intent?.action
        val announceScheduledStart = action == ACTION_START_SCHEDULED_ROUTE &&
            intent?.getBooleanExtra(EXTRA_ANNOUNCE_START, true) == true
        val startsSavedRoute = action == ACTION_START_SAVED_ROUTE || action == ACTION_START_SCHEDULED_ROUTE
        if (startsSavedRoute) {
            val profileId = intent?.getLongExtra(EXTRA_ROUTE_PROFILE_ID, 0L)?.takeIf { it > 0L }
                ?: return START_NOT_STICKY
            if (TrackingStateStore.isActive(this) && !TrackingStateStore.hasArrived(this)) {
                return START_STICKY
            }
            val profile = historyStore.loadRouteProfile(profileId) ?: run {
                stopSelf()
                return START_NOT_STICKY
            }
            if (TrackingStateStore.isActive(this)) TrackingStateStore.stopRun(this)
            TrackingStateStore.startRun(this, profileId)
            activeRouteProfile = profile
            config = profile.destinationConfig(DestinationStore(this).load())
            originGeocodeKey = null
        } else {
            if (action == ACTION_START_MANUAL || action == ACTION_START_RECURRING) {
                TrackingStateStore.startRun(this)
            }
            activeRouteProfile = TrackingStateStore.activeRouteProfileId(this)?.let(historyStore::loadRouteProfile)
            config = activeRouteProfile?.destinationConfig(DestinationStore(this).load())
                ?: DestinationStore(this).load()
        }
        if (!config.hasDestination() || !TrackingStateStore.isActive(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!hasLocationPermission()) {
            TrackingStateStore.stopRun(this)
            stopSelf()
            return START_NOT_STICKY
        }
        val journeyStartedAt = TrackingStateStore.ensureStartedAt(this)
        val now = System.currentTimeMillis()
        val recentOrigin = activeRouteProfile?.let { profile ->
            SavedLocation(profile.originLatitude, profile.originLongitude, 0f, now)
        } ?: CurrentLocationStore.load(this)?.takeIf {
            now - it.savedAtMillis in 0L..180_000L && it.accuracyMeters <= 150f
        }
        ensureJourneyOrigin(journeyStartedAt, recentOrigin)
        try {
            startAsForeground()
            requestLocationUpdates()
            if (announceScheduledStart) activeRouteProfile?.let(::announceRouteStart)
            RouteAlarmWidgetProvider.updateAll(this)
        } catch (_: SecurityException) {
            TrackingStateStore.stopRun(this)
            RouteAlarmWidgetProvider.updateAll(this)
            stopSelf()
        }
        return START_STICKY
    }

    private fun startAsForeground() {
        val notification = buildStatusNotification()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        if (!hasLocationPermission()) {
            TrackingStateStore.stopRun(this)
            stopSelf()
            return
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500L)
            .setMinUpdateIntervalMillis(250L)
            .setMaxUpdateDelayMillis(500L)
            .setMinUpdateDistanceMeters(0f)
            .setWaitForAccurateLocation(false)
            .build()
        locationClient.removeLocationUpdates(locationCallback)
        locationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            .addOnFailureListener { stopSelf() }
    }

    private fun processLocation(location: Location) {
        CurrentLocationStore.save(
            this,
            location.latitude,
            location.longitude,
            location.accuracy,
            location.time.takeIf { it > 0L } ?: System.currentTimeMillis(),
            location.speed.takeIf { location.hasSpeed() }
        )
        val startedAt = TrackingStateStore.ensureStartedAt(this)
        val originCandidate = activeRouteProfile?.let { profile ->
            SavedLocation(profile.originLatitude, profile.originLongitude, 0f, System.currentTimeMillis())
        } ?: location.takeIf { it.hasAccuracy() && it.accuracy <= 150f }?.let {
            SavedLocation(
                latitude = it.latitude,
                longitude = it.longitude,
                accuracyMeters = it.accuracy,
                savedAtMillis = it.time.takeIf { time -> time > 0L } ?: System.currentTimeMillis(),
                speedMetersPerSecond = location.speed.takeIf { location.hasSpeed() }
            )
        }
        ensureJourneyOrigin(startedAt, originCandidate)
        val latitude = config.latitude ?: return
        val longitude = config.longitude ?: return
        val distance = FloatArray(1)
        Location.distanceBetween(location.latitude, location.longitude, latitude, longitude, distance)
        val meters = distance[0]
        updateStatusNotification(meters, location.speed.takeIf { location.hasSpeed() })
        val acceptableArrivalAccuracy = max(20f, config.arrivalRadiusMeters * 0.5f).coerceAtMost(50f)
        if (location.hasAccuracy() && location.accuracy > acceptableArrivalAccuracy) return
        val closestMeters = TrackingStateStore.updateClosestDistance(this, meters)
        val arrived = TrackingStateStore.hasArrived(this)

        if (
            !arrived &&
            meters <= config.arrivalRadiusMeters
        ) {
            val journeyStartedAt = TrackingStateStore.ensureStartedAt(this)
            val completedAt = System.currentTimeMillis()
            val durationSeconds = historyStore.completeJourney(journeyStartedAt, completedAt)
                ?: ((completedAt - journeyStartedAt) / 1_000L).coerceAtLeast(0L)
            TrackingStateStore.markArrived(this, journeyStartedAt, durationSeconds)
            RouteAlarmWidgetProvider.updateAll(this)
            updateStatusNotification(meters, location.speed.takeIf { location.hasSpeed() }, force = true)
            triggerAlarm(AlarmType.ARRIVAL)
            return
        }

        if (
            config.departureAlertEnabled &&
            !TrackingStateStore.departureAlertSent(this) &&
            meters >= closestMeters + config.departureRadiusMeters
        ) {
            TrackingStateStore.markDepartureAlertSent(this)
            triggerAlarm(AlarmType.DEPARTURE)
            stopTracking()
        }
    }

    private fun ensureJourneyOrigin(startedAtMillis: Long, candidate: SavedLocation?) {
        val profile = activeRouteProfile
        val origin = historyStore.ensureJourney(
            config = config,
            startedAtMillis = startedAtMillis,
            originCandidate = candidate,
            originLabel = profile?.originName,
            fixedOriginKey = profile?.originKey
        ) ?: return
        TrackingStateStore.setRunOriginKey(this, startedAtMillis, origin.key)
        if (profile != null || origin.name != null || originGeocodeKey == origin.key) return
        originGeocodeKey = origin.key
        serviceScope.launch {
            val locality = reverseGeocodeLocality(applicationContext, origin.latitude, origin.longitude)
            if (locality != null) {
                runCatching {
                    historyStore.updateOriginLocality(startedAtMillis, locality)
                }.getOrNull()?.let { key ->
                    TrackingStateStore.setRunOriginKey(applicationContext, startedAtMillis, key)
                }
            }
        }
    }

    private fun announceRouteStart(profile: RouteProfile) {
        runCatching { speechEngine?.shutdown() }
        speechEngine = TextToSpeech(applicationContext) { status ->
            val engine = speechEngine ?: return@TextToSpeech
            if (status == TextToSpeech.SUCCESS) {
                engine.setLanguage(Locale("pt", "BR"))
                engine.speak(
                    "Iniciando o trajeto de ${profile.originName} para ${profile.destinationName}.",
                    TextToSpeech.QUEUE_FLUSH,
                    Bundle(),
                    "route-start-${profile.id}-${System.currentTimeMillis()}"
                )
                Handler(Looper.getMainLooper()).postDelayed({
                    if (speechEngine === engine) {
                        runCatching { engine.shutdown() }
                        speechEngine = null
                    }
                }, 15_000L)
            } else {
                runCatching { engine.shutdown() }
                speechEngine = null
            }
        }
    }

    private fun triggerAlarm(type: AlarmType) {
        AlarmStateStore.set(this, type, config.name)
        val intent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        // Quando o app ainda está visível, abrir diretamente garante que o
        // som não dependa do comportamento de heads-up da notificação.
        runCatching { startActivity(intent) }
        val pendingIntent = PendingIntent.getActivity(
            this,
            if (type == AlarmType.ARRIVAL) 5101 else 5102,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(if (type == AlarmType.ARRIVAL) "Você chegou ao ponto" else "Você se afastou do ponto")
            .setContentText(config.name)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(pendingIntent, true)
            .build()
        try {
            NotificationManagerCompat.from(this).notify(ALARM_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // A tela de alarme ainda pode ser aberta pelo toque na notificação de acompanhamento.
        }
    }

    private fun updateStatusNotification(distanceMeters: Float, speedMetersPerSecond: Float?, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastStatusNotificationAt < 1_000L) return
        lastStatusNotificationAt = now
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return
        try {
            NotificationManagerCompat.from(this)
                .notify(NOTIFICATION_ID, buildStatusNotification(distanceMeters, speedMetersPerSecond))
        } catch (_: SecurityException) {
            // A notificação é opcional; o acompanhamento continua pelo serviço.
        }
    }

    private fun buildStatusNotification(
        distanceMeters: Float? = null,
        speedMetersPerSecond: Float? = null
    ): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            5201,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val remainingMeters = distanceMeters?.let {
            (it - config.arrivalRadiusMeters).coerceAtLeast(0f)
        }
        val distanceText = remainingMeters?.let(::formatNotificationDistance) ?: "calculando..."
        val arrived = remainingMeters?.let { it <= 0f } == true
        val etaSeconds = remainingMeters?.let { remaining ->
            if (remaining <= 0f) 0L
            else speedMetersPerSecond?.takeIf { it >= 0.5f }
                ?.let { (remaining / it).roundToLong().coerceAtLeast(1L) }
        }
        val timeText = when {
            arrived -> "chegada alcançada"
            etaSeconds != null -> formatNotificationDuration(etaSeconds)
            else -> "aguardando movimento"
        }
        val summary = "Distância estimada: $distanceText · Tempo estimado: $timeText"
        return NotificationCompat.Builder(this, STATUS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Desperta no Ponto ativo")
            .setContentText(summary)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$summary\nDestino: ${config.name}"
                )
            )
            .build()
    }

    private fun formatNotificationDistance(meters: Float): String = if (meters < 1_000f) {
        "${meters.roundToInt()} m"
    } else {
        String.format(java.util.Locale("pt", "BR"), "%.1f km", meters / 1_000f)
    }

    private fun formatNotificationDuration(totalSeconds: Long): String {
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        return when {
            hours > 0L -> "$hours h $minutes min ${seconds.toString().padStart(2, '0')} s"
            minutes > 0L -> "$minutes min ${seconds.toString().padStart(2, '0')} s"
            else -> "$seconds s"
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                STATUS_CHANNEL_ID,
                "Acompanhamento de localização",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Mostra quando o destino está sendo monitorado." }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                ALARM_CHANNEL_ID,
                "Despertador de chegada",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerta persistente quando você chega ou se afasta do destino."
                setSound(null, null)
                enableVibration(false)
            }
        )
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun stopTracking() {
        runCatching { locationClient.removeLocationUpdates(locationCallback) }
        TrackingStateStore.stopRun(this)
        NotificationManagerCompat.from(this).cancel(ALARM_NOTIFICATION_ID)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        runCatching { locationClient.removeLocationUpdates(locationCallback) }
        runCatching { speechEngine?.shutdown() }
        speechEngine = null
        serviceScope.cancel()
        if (historyStoreDelegate.isInitialized()) historyStore.close()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // O serviço é START_STICKY e a notificação permanece ativa. Assim,
        // remover a tela do app da lista de recentes não cancela o acompanhamento.
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_MANUAL = "br.com.despertanoponto.START_MANUAL"
        const val ACTION_START_RECURRING = "br.com.despertanoponto.START_RECURRING"
        const val ACTION_START_SAVED_ROUTE = "br.com.despertanoponto.START_SAVED_ROUTE"
        const val ACTION_START_SCHEDULED_ROUTE = "br.com.despertanoponto.START_SCHEDULED_ROUTE"
        const val ACTION_STOP = "br.com.despertanoponto.STOP"
        const val EXTRA_ROUTE_PROFILE_ID = "route_profile_id"
        const val EXTRA_ANNOUNCE_START = "announce_start"
        const val STATUS_CHANNEL_ID = "tracking_status"
        const val ALARM_CHANNEL_ID = "arrival_alarm"
        const val NOTIFICATION_ID = 1001
        const val ALARM_NOTIFICATION_ID = 1002
    }
}
