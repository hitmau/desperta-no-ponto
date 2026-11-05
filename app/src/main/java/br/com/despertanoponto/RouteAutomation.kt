package br.com.despertanoponto

import android.annotation.SuppressLint
import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.RemoteViews
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

object RouteScheduleManager {
    private const val ACTION_START = "br.com.despertanoponto.ROUTE_SCHEDULE_ALARM"
    private const val DATA_PREFIX = "despertanoponto://route-schedule/"

    fun exactAlarmPermissionGranted(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun syncAll(context: Context) {
        val store = DestinationHistoryStore(context)
        val schedules = try {
            store.loadRouteSchedules()
        } finally {
            store.close()
        }
        schedules.forEach { schedule ->
            cancel(context, schedule.id)
            if (schedule.enabled && schedule.daysMask != 0) setNext(context, schedule)
        }
    }

    fun cancel(context: Context, scheduleId: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(pendingIntent(context, scheduleId))
    }

    fun rescheduleNext(context: Context, scheduleId: Long) {
        val store = DestinationHistoryStore(context)
        val schedule = try {
            store.loadRouteSchedule(scheduleId)
        } finally {
            store.close()
        }
        cancel(context, scheduleId)
        if (schedule != null && schedule.enabled && schedule.daysMask != 0) setNext(context, schedule)
    }

    private fun setNext(context: Context, schedule: RouteSchedule) {
        val triggerAt = nextTriggerMillis(schedule, System.currentTimeMillis()) ?: return
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val operation = pendingIntent(context, schedule.id)
        if (exactAlarmPermissionGranted(context)) {
            runCatching { alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation) }
                .onFailure { alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation) }
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        }
    }

    private fun nextTriggerMillis(schedule: RouteSchedule, now: Long): Long? {
        for (offset in 0..7) {
            val candidate = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, schedule.hour)
                set(Calendar.MINUTE, schedule.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayBit = 1 shl (candidate.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY)
            if (schedule.daysMask and dayBit != 0 && candidate.timeInMillis > now) {
                return candidate.timeInMillis
            }
        }
        return null
    }

    private fun pendingIntent(context: Context, scheduleId: Long): PendingIntent {
        val intent = Intent(context, RouteScheduleReceiver::class.java)
            .setAction(ACTION_START)
            .setData(Uri.parse("$DATA_PREFIX$scheduleId"))
            .putExtra(EXTRA_SCHEDULE_ID, scheduleId)
        return PendingIntent.getBroadcast(
            context,
            scheduleId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    const val EXTRA_SCHEDULE_ID = "route_schedule_id"
}

class RouteScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val scheduleId = intent?.getLongExtra(RouteScheduleManager.EXTRA_SCHEDULE_ID, 0L)?.takeIf { it > 0L } ?: return
        RouteScheduleManager.rescheduleNext(context, scheduleId)

        val store = DestinationHistoryStore(context)
        val (schedule, profile) = try {
            val schedule = store.loadRouteSchedule(scheduleId)
            schedule to schedule?.let { store.loadRouteProfile(it.routeProfileId) }
        } finally {
            store.close()
        }
        if (schedule == null || profile == null || !schedule.enabled) return
        if (TrackingStateStore.isActive(context) && !TrackingStateStore.hasArrived(context)) {
            RouteAutomationNotifier.postAlreadyTracking(context, profile)
            return
        }
        if (Build.VERSION.SDK_INT >= 29 && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            RouteAutomationNotifier.postBackgroundLocationNeeded(context, profile)
            return
        }

        val serviceIntent = Intent(context, TrackingService::class.java)
            .setAction(TrackingService.ACTION_START_SCHEDULED_ROUTE)
            .putExtra(TrackingService.EXTRA_ROUTE_PROFILE_ID, profile.id)
            .putExtra(TrackingService.EXTRA_ANNOUNCE_START, schedule.announceStart)
        runCatching { ContextCompat.startForegroundService(context, serviceIntent) }
            .onFailure { RouteAutomationNotifier.postStartFailed(context, profile) }
    }
}

private object RouteAutomationNotifier {
    private const val CHANNEL_ID = "route_automation"

    fun postBackgroundLocationNeeded(context: Context, profile: RouteProfile) {
        post(context, profile, "Ative a permissão de localização “Sempre” para iniciar esta rota automaticamente.")
    }

    fun postStartFailed(context: Context, profile: RouteProfile) {
        post(context, profile, "Não foi possível iniciar o acompanhamento. Abra o app e confira as permissões.")
    }

    fun postAlreadyTracking(context: Context, profile: RouteProfile) {
        post(context, profile, "Esta rotina não foi iniciada porque já existe outro trajeto em acompanhamento.")
    }

    private fun post(context: Context, profile: RouteProfile, message: String) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = context.getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(
                android.app.NotificationChannel(
                    CHANNEL_ID,
                    "Rotinas de trajeto",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
        val openApp = PendingIntent.getActivity(
            context,
            (profile.id % Int.MAX_VALUE).toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Rotina não iniciada: ${profile.name}")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context).notify((profile.id % Int.MAX_VALUE).toInt(), notification)
    }
}

class RouteAlarmWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_WIDGET_START) {
            startSavedRoute(context, intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID))
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { updateWidget(context, manager, it) }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val preferences = context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE).edit()
        appWidgetIds.forEach { preferences.remove(widgetKey(it)) }
        preferences.apply()
    }

    companion object {
        private const val WIDGET_PREFS = "route_widget_profiles"
        private const val ACTION_WIDGET_START = "br.com.despertanoponto.WIDGET_START_ROUTE"

        fun setProfileForWidget(context: Context, widgetId: Int, profileId: Long) {
            context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE).edit()
                .putLong(widgetKey(widgetId), profileId)
                .apply()
            updateWidget(context, AppWidgetManager.getInstance(context), widgetId)
        }

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(android.content.ComponentName(context, RouteAlarmWidgetProvider::class.java))
            ids.forEach { updateWidget(context, manager, it) }
        }

        private fun widgetKey(widgetId: Int) = "profile_for_widget_$widgetId"

        private fun startSavedRoute(context: Context, widgetId: Int) {
            if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
            val profileId = context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
                .getLong(widgetKey(widgetId), 0L)
            if (profileId <= 0L || !routeProfileExists(context, profileId)) {
                openWidgetConfiguration(context, widgetId)
                return
            }
            if (!hasLocationPermission(context)) {
                context.startActivity(
                    Intent(context, WidgetRouteStartActivity::class.java)
                        .putExtra(TrackingService.EXTRA_ROUTE_PROFILE_ID, profileId)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                return
            }
            if (TrackingStateStore.isActive(context) && !TrackingStateStore.hasArrived(context)) {
                Toast.makeText(context, "Já existe um trajeto em acompanhamento.", Toast.LENGTH_SHORT).show()
                return
            }
            val serviceIntent = Intent(context, TrackingService::class.java)
                .setAction(TrackingService.ACTION_START_SAVED_ROUTE)
                .putExtra(TrackingService.EXTRA_ROUTE_PROFILE_ID, profileId)
            runCatching { ContextCompat.startForegroundService(context, serviceIntent) }
        }

        private fun routeProfileExists(context: Context, profileId: Long): Boolean {
            val store = DestinationHistoryStore(context)
            return try {
                store.loadRouteProfile(profileId) != null
            } finally {
                store.close()
            }
        }

        private fun openWidgetConfiguration(context: Context, widgetId: Int) {
            context.startActivity(
                Intent(context, RouteWidgetConfigActivity::class.java)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_route_alarm)
            val profileId = context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
                .getLong(widgetKey(widgetId), 0L)
            if (profileId <= 0L) {
                views.setTextViewText(R.id.widget_route_title, "Configure uma rota")
                views.setTextViewText(R.id.widget_route_distance, "Abra Widgets em Configurações")
                views.setTextViewText(R.id.widget_route_stats, "Toque para iniciar")
                views.setTextViewText(R.id.widget_route_action, "TOQUE PARA CONFIGURAR")
            } else {
                val store = DestinationHistoryStore(context)
                val profile: RouteProfile?
                val stats: RouteJourneyStats?
                try {
                    profile = store.loadRouteProfile(profileId)
                    stats = profile?.let { route ->
                        store.loadCompletedStats().firstOrNull {
                            it.originKey == route.originKey && it.destinationKey == route.destinationKey
                        }
                    }
                } finally {
                    store.close()
                }
                if (profile == null) {
                    views.setTextViewText(R.id.widget_route_title, "Rota removida")
                    views.setTextViewText(R.id.widget_route_distance, "Escolha outra rota nas configurações")
                    views.setTextViewText(R.id.widget_route_stats, "")
                    views.setTextViewText(R.id.widget_route_action, "ABRIR CONFIGURAÇÕES")
                } else {
                    val distanceMeters = FloatArray(1)
                    android.location.Location.distanceBetween(
                        profile.originLatitude,
                        profile.originLongitude,
                        profile.destinationLatitude,
                        profile.destinationLongitude,
                        distanceMeters
                    )
                    views.setTextViewText(R.id.widget_route_title, profile.name)
                    views.setTextViewText(R.id.widget_route_distance, "${formatDistance(distanceMeters[0])} · distância direta estimada")
                    views.setTextViewText(
                        R.id.widget_route_stats,
                        if (stats == null) "Média: sem histórico"
                        else "Média ${formatDuration(stats.averageDurationSeconds)} · última ${formatDuration(stats.lastDurationSeconds)}"
                    )
                    views.setTextViewText(R.id.widget_route_action, "TOQUE PARA INICIAR")
                }
            }
            val clickIntent = Intent(context, RouteAlarmWidgetProvider::class.java)
                .setAction(ACTION_WIDGET_START)
                .setData(Uri.parse("despertanoponto://widget/$widgetId"))
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            val clickOperation = PendingIntent.getBroadcast(
                context,
                70000 + widgetId,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_route_root, clickOperation)
            manager.updateAppWidget(widgetId, views)
        }

        private fun hasLocationPermission(context: Context) =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        private fun formatDistance(meters: Float): String = if (meters < 1_000f) {
            "${meters.roundToInt()} m"
        } else {
            String.format(Locale("pt", "BR"), "%.1f km", meters / 1_000f)
        }

        private fun formatDuration(seconds: Long): String = String.format(
            Locale.US,
            "%02d:%02d:%02d",
            seconds.coerceAtLeast(0L) / 3600L,
            (seconds.coerceAtLeast(0L) % 3600L) / 60L,
            seconds.coerceAtLeast(0L) % 60L
        )
    }
}

class RouteWidgetConfigActivity : ComponentActivity() {
    private var widgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        setResult(Activity.RESULT_CANCELED)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        setContent {
            var profiles by remember { mutableStateOf<List<RouteProfile>>(emptyList()) }
            LaunchedEffect(Unit) {
                profiles = withContext(Dispatchers.IO) {
                    val store = DestinationHistoryStore(this@RouteWidgetConfigActivity)
                    try { store.loadRouteProfiles() } finally { store.close() }
                }
            }
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF081016)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Escolha a rota deste widget", color = Color.White, style = MaterialTheme.typography.titleLarge)
                        if (profiles.isEmpty()) {
                            Text("Crie primeiro uma rota em Configurações → Widgets e rotinas.", color = Color.White)
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(profiles, key = { it.id }) { profile ->
                                    Surface(
                                        color = Color(0xFF252B31),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().clickable { finishConfiguration(profile.id) }
                                    ) {
                                        Column(Modifier.padding(14.dp)) {
                                            Text(profile.name, color = Color.White)
                                            Text("${profile.originName} → ${profile.destinationName}", color = Color(0xFF66D9B5))
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { finish() }, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
                    }
                }
            }
        }
    }

    private fun finishConfiguration(profileId: Long) {
        RouteAlarmWidgetProvider.setProfileForWidget(this, widgetId, profileId)
        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
    }
}

@SuppressLint("InvalidFragmentVersionForActivityResult")
class WidgetRouteStartActivity : ComponentActivity() {
    private var profileId = 0L
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) startSavedRoute() else finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profileId = intent.getLongExtra(TrackingService.EXTRA_ROUTE_PROFILE_ID, 0L)
        if (profileId <= 0L) {
            finish()
            return
        }
        if (hasLocationPermission()) {
            startSavedRoute()
            return
        }
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF081016)) {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Permita a localização para iniciar essa rota pelo widget.", color = Color.White)
                        Button(
                            onClick = {
                                locationPermissionRequest.launch(
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                )
                            },
                            modifier = Modifier.padding(top = 16.dp).fillMaxWidth()
                        ) { Text("Continuar") }
                    }
                }
            }
        }
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun startSavedRoute() {
        if (TrackingStateStore.isActive(this) && !TrackingStateStore.hasArrived(this)) {
            Toast.makeText(this, "Já existe um trajeto em acompanhamento.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val store = DestinationHistoryStore(this)
        val profileExists = try {
            store.loadRouteProfile(profileId) != null
        } finally {
            store.close()
        }
        if (!profileExists) {
            Toast.makeText(this, "Esta rota foi removida. Configure o widget novamente.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val serviceIntent = Intent(this, TrackingService::class.java)
            .setAction(TrackingService.ACTION_START_SAVED_ROUTE)
            .putExtra(TrackingService.EXTRA_ROUTE_PROFILE_ID, profileId)
        runCatching { ContextCompat.startForegroundService(this, serviceIntent) }
        finish()
    }
}
