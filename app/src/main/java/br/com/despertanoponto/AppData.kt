package br.com.despertanoponto

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.location.Geocoder
import java.util.Calendar
import java.util.Locale
import java.text.Normalizer
import kotlin.math.roundToLong

object OnboardingStore {
    private const val PREFS = "app_preferences"
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

    fun isCompleted(context: Context): Boolean = context
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_ONBOARDING_COMPLETED, false)

    fun complete(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, true)
            .apply()
    }
}

data class SavedLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val savedAtMillis: Long,
    val speedMetersPerSecond: Float? = null
)

object CurrentLocationStore {
    private const val PREFS = "current_location"
    private const val LATITUDE = "latitude"
    private const val LONGITUDE = "longitude"
    private const val ACCURACY = "accuracy"
    private const val SAVED_AT = "saved_at"
    private const val SPEED = "speed_meters_per_second"

    fun save(
        context: Context,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float,
        savedAtMillis: Long = System.currentTimeMillis(),
        speedMetersPerSecond: Float? = null
    ) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(LATITUDE, latitude.toString())
            .putString(LONGITUDE, longitude.toString())
            .putFloat(ACCURACY, accuracyMeters)
            .putLong(SAVED_AT, savedAtMillis)
            .putFloat(SPEED, speedMetersPerSecond ?: 0f)
            .apply()
    }

    fun load(context: Context): SavedLocation? {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val latitude = preferences.getString(LATITUDE, null)?.toDoubleOrNull() ?: return null
        val longitude = preferences.getString(LONGITUDE, null)?.toDoubleOrNull() ?: return null
        return SavedLocation(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = preferences.getFloat(ACCURACY, 0f),
            savedAtMillis = preferences.getLong(SAVED_AT, 0L),
            speedMetersPerSecond = preferences.getFloat(SPEED, 0f)
        )
    }
}

enum class ActivationMode(val label: String) {
    MANUAL("Manual"),
    RECURRING("Recorrente"),
    BOTH("Ambos")
}

data class DestinationConfig(
    val name: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val arrivalRadiusMeters: Int = 500,
    val departureRadiusMeters: Int = 1000,
    val departureAlertEnabled: Boolean = true,
    val activationMode: ActivationMode = ActivationMode.BOTH,
    val scheduleHour: Int = 7,
    val scheduleMinute: Int = 0,
    val daysMask: Int = 0b1111111,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val arrivalAlarmDurationSeconds: Int = 30
) {
    fun hasDestination(): Boolean = latitude != null && longitude != null && name.isNotBlank()

    fun normalized(): DestinationConfig {
        val arrival = arrivalRadiusMeters.coerceIn(50, 50000)
        return copy(
            arrivalRadiusMeters = arrival,
            departureRadiusMeters = departureRadiusMeters.coerceAtLeast(arrival + 100).coerceAtMost(100000),
            scheduleHour = scheduleHour.coerceIn(0, 23),
            scheduleMinute = scheduleMinute.coerceIn(0, 59),
            daysMask = daysMask and 0b1111111,
            arrivalAlarmDurationSeconds = arrivalAlarmDurationSeconds.coerceIn(1, 3600)
        )
    }
}

data class RouteProfile(
    val id: Long = 0L,
    val name: String,
    val originName: String,
    val originLatitude: Double,
    val originLongitude: Double,
    val destinationName: String,
    val destinationLatitude: Double,
    val destinationLongitude: Double,
    val arrivalRadiusMeters: Int = 500
) {
    val originKey: String
        get() = "coordinates:${DestinationHistoryStore.destinationKey(originLatitude, originLongitude)}"

    val destinationKey: String
        get() = DestinationHistoryStore.destinationKey(destinationLatitude, destinationLongitude)

    fun destinationConfig(base: DestinationConfig = DestinationConfig()): DestinationConfig = base.copy(
        name = destinationName,
        latitude = destinationLatitude,
        longitude = destinationLongitude,
        arrivalRadiusMeters = arrivalRadiusMeters.coerceIn(50, 50000)
    ).normalized()
}

data class RouteSchedule(
    val id: Long = 0L,
    val routeProfileId: Long,
    val hour: Int,
    val minute: Int,
    val daysMask: Int = 0b1111111,
    val enabled: Boolean = true,
    val announceStart: Boolean = true
)

class DestinationStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): DestinationConfig {
        val mode = runCatching {
            ActivationMode.valueOf(preferences.getString(KEY_MODE, ActivationMode.BOTH.name)!!)
        }.getOrDefault(ActivationMode.BOTH)
        return DestinationConfig(
            name = preferences.getString(KEY_NAME, "") ?: "",
            latitude = if (preferences.contains(KEY_LATITUDE)) preferences.getString(KEY_LATITUDE, null)?.toDoubleOrNull() else null,
            longitude = if (preferences.contains(KEY_LONGITUDE)) preferences.getString(KEY_LONGITUDE, null)?.toDoubleOrNull() else null,
            arrivalRadiusMeters = preferences.getInt(KEY_ARRIVAL, 500),
            departureRadiusMeters = preferences.getInt(KEY_DEPARTURE, 1000),
            departureAlertEnabled = preferences.getBoolean(KEY_DEPARTURE_ALERT, true),
            activationMode = mode,
            scheduleHour = preferences.getInt(KEY_HOUR, 7),
            scheduleMinute = preferences.getInt(KEY_MINUTE, 0),
            daysMask = preferences.getInt(KEY_DAYS, 0b1111111),
            soundEnabled = preferences.getBoolean(KEY_SOUND, true),
            vibrationEnabled = preferences.getBoolean(KEY_VIBRATION, true),
            arrivalAlarmDurationSeconds = preferences.getInt(KEY_ALARM_DURATION, 30)
        ).normalized()
    }

    fun save(config: DestinationConfig) {
        val value = config.normalized()
        preferences.edit()
            .putString(KEY_NAME, value.name)
            .putString(KEY_LATITUDE, value.latitude?.toString())
            .putString(KEY_LONGITUDE, value.longitude?.toString())
            .putInt(KEY_ARRIVAL, value.arrivalRadiusMeters)
            .putInt(KEY_DEPARTURE, value.departureRadiusMeters)
            .putBoolean(KEY_DEPARTURE_ALERT, value.departureAlertEnabled)
            .putString(KEY_MODE, value.activationMode.name)
            .putInt(KEY_HOUR, value.scheduleHour)
            .putInt(KEY_MINUTE, value.scheduleMinute)
            .putInt(KEY_DAYS, value.daysMask)
            .putBoolean(KEY_SOUND, value.soundEnabled)
            .putBoolean(KEY_VIBRATION, value.vibrationEnabled)
            .putInt(KEY_ALARM_DURATION, value.arrivalAlarmDurationSeconds)
            .apply()
    }

    private companion object {
        const val PREFS = "destination_config"
        const val KEY_NAME = "name"
        const val KEY_LATITUDE = "latitude"
        const val KEY_LONGITUDE = "longitude"
        const val KEY_ARRIVAL = "arrival_radius"
        const val KEY_DEPARTURE = "departure_radius"
        const val KEY_DEPARTURE_ALERT = "departure_alert"
        const val KEY_MODE = "activation_mode"
        const val KEY_HOUR = "schedule_hour"
        const val KEY_MINUTE = "schedule_minute"
        const val KEY_DAYS = "days_mask"
        const val KEY_SOUND = "sound"
        const val KEY_VIBRATION = "vibration"
        const val KEY_ALARM_DURATION = "arrival_alarm_duration_seconds"
    }
}

object TrackingStateStore {
    private const val PREFS = "tracking_state"
    private const val ACTIVE = "active"
    private const val ARRIVED = "arrived"
    private const val DEPARTURE_SENT = "departure_sent"
    private const val CLOSEST_DISTANCE = "closest_distance_meters"
    private const val STARTED_AT = "started_at_millis"
    private const val RUN_ORIGIN_KEY = "run_origin_key"
    private const val ACTIVE_ROUTE_PROFILE_ID = "active_route_profile_id"
    private const val LAST_COMPLETED_STARTED_AT = "last_completed_started_at"
    private const val LAST_COMPLETED_DURATION = "last_completed_duration_seconds"

    private fun preferences(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isActive(context: Context): Boolean = preferences(context).getBoolean(ACTIVE, false)

    fun startRun(context: Context, routeProfileId: Long? = null) {
        val prefs = preferences(context)
        val editor = prefs.edit()
            .putBoolean(ACTIVE, true)
            .putBoolean(ARRIVED, false)
            .putBoolean(DEPARTURE_SENT, false)
        if (!prefs.getBoolean(ACTIVE, false) || !prefs.contains(STARTED_AT)) {
            editor.putLong(STARTED_AT, System.currentTimeMillis())
                .remove(RUN_ORIGIN_KEY)
                .remove(ACTIVE_ROUTE_PROFILE_ID)
                .remove(CLOSEST_DISTANCE)
        }
        if (routeProfileId != null && routeProfileId > 0L) {
            editor.putLong(ACTIVE_ROUTE_PROFILE_ID, routeProfileId)
        }
        editor.apply()
    }

    fun stopRun(context: Context) {
        preferences(context).edit()
            .putBoolean(ACTIVE, false)
            .putBoolean(ARRIVED, false)
            .putBoolean(DEPARTURE_SENT, false)
            .remove(CLOSEST_DISTANCE)
            .remove(STARTED_AT)
            .remove(RUN_ORIGIN_KEY)
            .remove(ACTIVE_ROUTE_PROFILE_ID)
            .apply()
    }

    fun startedAt(context: Context): Long? = preferences(context)
        .getLong(STARTED_AT, 0L)
        .takeIf { it > 0L }

    fun runOriginKey(context: Context): String? = preferences(context)
        .getString(RUN_ORIGIN_KEY, null)

    fun setRunOriginKey(context: Context, startedAtMillis: Long, key: String) {
        val prefs = preferences(context)
        if (prefs.getLong(STARTED_AT, 0L) == startedAtMillis && prefs.getBoolean(ACTIVE, false)) {
            prefs.edit().putString(RUN_ORIGIN_KEY, key).apply()
        }
    }

    fun activeRouteProfileId(context: Context): Long? = preferences(context)
        .getLong(ACTIVE_ROUTE_PROFILE_ID, 0L)
        .takeIf { it > 0L }

    fun ensureStartedAt(context: Context): Long {
        val prefs = preferences(context)
        val existing = prefs.getLong(STARTED_AT, 0L)
        if (existing > 0L) return existing
        val startedAt = System.currentTimeMillis()
        prefs.edit().putLong(STARTED_AT, startedAt).apply()
        return startedAt
    }

    fun hasArrived(context: Context): Boolean = preferences(context).getBoolean(ARRIVED, false)

    fun markArrived(context: Context, startedAtMillis: Long, durationSeconds: Long) {
        preferences(context).edit()
            .putBoolean(ARRIVED, true)
            .putLong(LAST_COMPLETED_STARTED_AT, startedAtMillis)
            .putLong(LAST_COMPLETED_DURATION, durationSeconds)
            .apply()
    }

    fun lastCompletedStartedAt(context: Context): Long = preferences(context)
        .getLong(LAST_COMPLETED_STARTED_AT, 0L)

    fun lastCompletedDurationSeconds(context: Context): Long? = preferences(context)
        .getLong(LAST_COMPLETED_DURATION, -1L)
        .takeIf { it >= 0L }

    fun departureAlertSent(context: Context): Boolean = preferences(context).getBoolean(DEPARTURE_SENT, false)

    fun markDepartureAlertSent(context: Context) {
        preferences(context).edit().putBoolean(DEPARTURE_SENT, true).apply()
    }

    /**
     * Guarda a menor distância observada neste acompanhamento.
     * O afastamento passa a ser medido a partir deste melhor ponto do trajeto.
     */
    fun updateClosestDistance(context: Context, distanceMeters: Float): Float {
        val prefs = preferences(context)
        val previous = prefs.getFloat(CLOSEST_DISTANCE, Float.MAX_VALUE)
        val closest = minOf(previous, distanceMeters)
        if (closest != previous) {
            prefs.edit().putFloat(CLOSEST_DISTANCE, closest).apply()
        }
        return closest
    }

    fun closestDistance(context: Context): Float? {
        val value = preferences(context).getFloat(CLOSEST_DISTANCE, Float.MAX_VALUE)
        return value.takeUnless { it == Float.MAX_VALUE }
    }
}

enum class AlarmType { ARRIVAL, DEPARTURE }

object AlarmStateStore {
    private const val PREFS = "alarm_state"
    private const val TYPE = "type"
    private const val LABEL = "label"

    fun set(context: Context, type: AlarmType, label: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(TYPE, type.name)
            .putString(LABEL, label)
            .apply()
    }

    fun getType(context: Context): AlarmType? = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(TYPE, null)?.let { runCatching { AlarmType.valueOf(it) }.getOrNull() }

    fun getLabel(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(LABEL, "Seu destino") ?: "Seu destino"

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}

object ScheduleManager {
    private const val REQUEST_CODE = 4201

    fun sync(context: Context, config: DestinationConfig) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, ScheduleReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        if (!config.hasDestination() || config.activationMode == ActivationMode.MANUAL || config.daysMask == 0) return

        val first = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, config.scheduleHour)
            set(Calendar.MINUTE, config.scheduleMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            first.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}

data class JourneyOrigin(
    val key: String,
    val name: String?,
    val latitude: Double,
    val longitude: Double
)

data class RouteJourneyStats(
    val originKey: String,
    val originName: String?,
    val originLatitude: Double,
    val originLongitude: Double,
    val destinationKey: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val journeyCount: Int,
    val averageDurationSeconds: Long,
    val shortestDurationSeconds: Long,
    val longestDurationSeconds: Long,
    val lastDurationSeconds: Long,
    val lastCompletedAtMillis: Long
)

data class RouteJourneyRecord(
    val id: Long,
    val startedAtMillis: Long,
    val completedAtMillis: Long?,
    val durationSeconds: Long
)

class DestinationHistoryStore(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE destinations (
                destination_key TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE journeys (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                run_started_at INTEGER NOT NULL UNIQUE,
                destination_key TEXT NOT NULL,
                started_at INTEGER NOT NULL,
                completed_at INTEGER,
                duration_seconds INTEGER,
                origin_key TEXT,
                origin_name TEXT,
                origin_latitude REAL,
                origin_longitude REAL,
                FOREIGN KEY(destination_key) REFERENCES destinations(destination_key)
            )"""
        )
        db.execSQL("CREATE INDEX journeys_destination_idx ON journeys(destination_key, completed_at)")
        db.execSQL("CREATE INDEX journeys_route_idx ON journeys(origin_key, destination_key, completed_at)")
        createRouteAutomationTables(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE journeys ADD COLUMN origin_key TEXT")
            db.execSQL("ALTER TABLE journeys ADD COLUMN origin_name TEXT")
            db.execSQL("ALTER TABLE journeys ADD COLUMN origin_latitude REAL")
            db.execSQL("ALTER TABLE journeys ADD COLUMN origin_longitude REAL")
            db.execSQL("CREATE INDEX journeys_route_idx ON journeys(origin_key, destination_key, completed_at)")
        }
        if (oldVersion < 3) createRouteAutomationTables(db)
    }

    private fun createRouteAutomationTables(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE route_profiles (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                origin_name TEXT NOT NULL,
                origin_latitude REAL NOT NULL,
                origin_longitude REAL NOT NULL,
                destination_name TEXT NOT NULL,
                destination_latitude REAL NOT NULL,
                destination_longitude REAL NOT NULL,
                arrival_radius_meters INTEGER NOT NULL DEFAULT 500
            )"""
        )
        db.execSQL(
            """CREATE TABLE route_schedules (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                route_profile_id INTEGER NOT NULL,
                hour INTEGER NOT NULL,
                minute INTEGER NOT NULL,
                days_mask INTEGER NOT NULL DEFAULT 127,
                enabled INTEGER NOT NULL DEFAULT 1,
                announce_start INTEGER NOT NULL DEFAULT 1,
                FOREIGN KEY(route_profile_id) REFERENCES route_profiles(id) ON DELETE CASCADE
            )"""
        )
        db.execSQL("CREATE INDEX route_schedules_enabled_idx ON route_schedules(enabled, hour, minute)")
    }

    fun loadRouteProfiles(): List<RouteProfile> = readableDatabase.rawQuery(
        "SELECT id, name, origin_name, origin_latitude, origin_longitude, destination_name, destination_latitude, destination_longitude, arrival_radius_meters FROM route_profiles ORDER BY name COLLATE NOCASE",
        null
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(cursor.toRouteProfile())
        }
    }

    fun loadRouteProfile(id: Long): RouteProfile? = readableDatabase.rawQuery(
        "SELECT id, name, origin_name, origin_latitude, origin_longitude, destination_name, destination_latitude, destination_longitude, arrival_radius_meters FROM route_profiles WHERE id = ?",
        arrayOf(id.toString())
    ).use { cursor ->
        if (cursor.moveToFirst()) cursor.toRouteProfile() else null
    }

    fun saveRouteProfile(profile: RouteProfile): Long {
        val values = ContentValues().apply {
            put("name", profile.name.trim())
            put("origin_name", profile.originName.trim())
            put("origin_latitude", profile.originLatitude)
            put("origin_longitude", profile.originLongitude)
            put("destination_name", profile.destinationName.trim())
            put("destination_latitude", profile.destinationLatitude)
            put("destination_longitude", profile.destinationLongitude)
            put("arrival_radius_meters", profile.arrivalRadiusMeters.coerceIn(50, 50000))
        }
        return if (profile.id > 0L) {
            writableDatabase.update("route_profiles", values, "id = ?", arrayOf(profile.id.toString()))
            profile.id
        } else {
            writableDatabase.insertOrThrow("route_profiles", null, values)
        }
    }

    fun deleteRouteProfile(id: Long) {
        writableDatabase.delete("route_profiles", "id = ?", arrayOf(id.toString()))
    }

    fun loadRouteSchedules(): List<RouteSchedule> = readableDatabase.rawQuery(
        "SELECT id, route_profile_id, hour, minute, days_mask, enabled, announce_start FROM route_schedules ORDER BY hour, minute",
        null
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(
                    RouteSchedule(
                        id = cursor.getLong(0),
                        routeProfileId = cursor.getLong(1),
                        hour = cursor.getInt(2),
                        minute = cursor.getInt(3),
                        daysMask = cursor.getInt(4),
                        enabled = cursor.getInt(5) != 0,
                        announceStart = cursor.getInt(6) != 0
                    )
                )
            }
        }
    }

    fun loadRouteSchedule(id: Long): RouteSchedule? = readableDatabase.rawQuery(
        "SELECT id, route_profile_id, hour, minute, days_mask, enabled, announce_start FROM route_schedules WHERE id = ?",
        arrayOf(id.toString())
    ).use { cursor ->
        if (!cursor.moveToFirst()) null else RouteSchedule(
            id = cursor.getLong(0),
            routeProfileId = cursor.getLong(1),
            hour = cursor.getInt(2),
            minute = cursor.getInt(3),
            daysMask = cursor.getInt(4),
            enabled = cursor.getInt(5) != 0,
            announceStart = cursor.getInt(6) != 0
        )
    }

    fun saveRouteSchedule(schedule: RouteSchedule): Long {
        val values = ContentValues().apply {
            put("route_profile_id", schedule.routeProfileId)
            put("hour", schedule.hour.coerceIn(0, 23))
            put("minute", schedule.minute.coerceIn(0, 59))
            put("days_mask", schedule.daysMask and 0b1111111)
            put("enabled", if (schedule.enabled) 1 else 0)
            put("announce_start", if (schedule.announceStart) 1 else 0)
        }
        return if (schedule.id > 0L) {
            writableDatabase.update("route_schedules", values, "id = ?", arrayOf(schedule.id.toString()))
            schedule.id
        } else {
            writableDatabase.insertOrThrow("route_schedules", null, values)
        }
    }

    fun deleteRouteSchedule(id: Long) {
        writableDatabase.delete("route_schedules", "id = ?", arrayOf(id.toString()))
    }

    private fun android.database.Cursor.toRouteProfile() = RouteProfile(
        id = getLong(0),
        name = getString(1),
        originName = getString(2),
        originLatitude = getDouble(3),
        originLongitude = getDouble(4),
        destinationName = getString(5),
        destinationLatitude = getDouble(6),
        destinationLongitude = getDouble(7),
        arrivalRadiusMeters = getInt(8)
    )

    fun ensureJourney(
        config: DestinationConfig,
        startedAtMillis: Long,
        originCandidate: SavedLocation? = null,
        originLabel: String? = null,
        fixedOriginKey: String? = null
    ): JourneyOrigin? {
        val latitude = config.latitude ?: return null
        val longitude = config.longitude ?: return null
        val key = destinationKey(latitude, longitude)
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(
                "journeys",
                "duration_seconds IS NULL AND run_started_at <> ?",
                arrayOf(startedAtMillis.toString())
            )
            val destination = ContentValues().apply {
                put("destination_key", key)
                put("name", config.name)
                put("latitude", latitude)
                put("longitude", longitude)
            }
            if (db.update("destinations", destination, "destination_key = ?", arrayOf(key)) == 0) {
                db.insertWithOnConflict("destinations", null, destination, SQLiteDatabase.CONFLICT_IGNORE)
            }
            val journey = ContentValues().apply {
                put("run_started_at", startedAtMillis)
                put("destination_key", key)
                put("started_at", startedAtMillis)
            }
            db.insertWithOnConflict("journeys", null, journey, SQLiteDatabase.CONFLICT_IGNORE)
            if (originCandidate != null && originCandidate.accuracyMeters <= 150f) {
                val originValues = ContentValues().apply {
                    put("origin_key", fixedOriginKey ?: "coordinates:${destinationKey(originCandidate.latitude, originCandidate.longitude)}")
                    if (!originLabel.isNullOrBlank()) put("origin_name", originLabel.trim())
                    put("origin_latitude", originCandidate.latitude)
                    put("origin_longitude", originCandidate.longitude)
                }
                db.update(
                    "journeys",
                    originValues,
                    "run_started_at = ? AND origin_key IS NULL",
                    arrayOf(startedAtMillis.toString())
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return loadJourneyOrigin(startedAtMillis)
    }

    fun updateOriginLocality(startedAtMillis: Long, locality: String): String? {
        val cleanLocality = locality.trim().takeIf { it.isNotEmpty() } ?: return null
        val normalizedLocality = Normalizer.normalize(cleanLocality.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
        val key = "locality:$normalizedLocality"
        val values = ContentValues().apply {
            put("origin_key", key)
            put("origin_name", cleanLocality)
        }
        val updated = writableDatabase.update(
            "journeys",
            values,
            "run_started_at = ? AND origin_latitude IS NOT NULL",
            arrayOf(startedAtMillis.toString())
        )
        return key.takeIf { updated > 0 }
    }

    private fun loadJourneyOrigin(startedAtMillis: Long): JourneyOrigin? {
        return readableDatabase.rawQuery(
            "SELECT origin_key, origin_name, origin_latitude, origin_longitude FROM journeys WHERE run_started_at = ?",
            arrayOf(startedAtMillis.toString())
        ).use { cursor ->
            if (!cursor.moveToFirst() || cursor.isNull(0) || cursor.isNull(2) || cursor.isNull(3)) return null
            JourneyOrigin(
                key = cursor.getString(0),
                name = if (cursor.isNull(1)) null else cursor.getString(1),
                latitude = cursor.getDouble(2),
                longitude = cursor.getDouble(3)
            )
        }
    }

    fun completeJourney(startedAtMillis: Long, completedAtMillis: Long): Long? {
        val db = writableDatabase
        val cursor = db.rawQuery(
            "SELECT started_at, duration_seconds FROM journeys WHERE run_started_at = ?",
            arrayOf(startedAtMillis.toString())
        )
        val startedAt: Long
        cursor.use {
            if (!it.moveToFirst()) return null
            if (!it.isNull(1)) return it.getLong(1)
            startedAt = it.getLong(0)
        }
        val durationSeconds = ((completedAtMillis - startedAt) / 1_000L).coerceAtLeast(0L)
        val values = ContentValues().apply {
            put("completed_at", completedAtMillis)
            put("duration_seconds", durationSeconds)
        }
        val updated = db.update(
            "journeys",
            values,
            "run_started_at = ? AND duration_seconds IS NULL",
            arrayOf(startedAtMillis.toString())
        )
        return if (updated > 0) durationSeconds else null
    }

    fun loadCompletedStats(): List<RouteJourneyStats> {
        val query = """SELECT j.origin_key, j.origin_name, j.origin_latitude, j.origin_longitude,
                d.destination_key, d.name, d.latitude, d.longitude,
                COUNT(j.id), AVG(j.duration_seconds), MIN(j.duration_seconds), MAX(j.duration_seconds), MAX(j.completed_at),
                (SELECT latest.duration_seconds FROM journeys latest
                    WHERE latest.destination_key = j.destination_key
                      AND latest.origin_key = j.origin_key
                      AND latest.duration_seconds IS NOT NULL
                    ORDER BY latest.completed_at DESC LIMIT 1)
            FROM journeys j
            JOIN destinations d ON j.destination_key = d.destination_key
            WHERE j.duration_seconds IS NOT NULL AND j.origin_key IS NOT NULL
            GROUP BY j.origin_key, j.destination_key
            ORDER BY MAX(j.completed_at) DESC"""
        return readableDatabase.rawQuery(query, null).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        RouteJourneyStats(
                            originKey = cursor.getString(0),
                            originName = if (cursor.isNull(1)) null else cursor.getString(1),
                            originLatitude = cursor.getDouble(2),
                            originLongitude = cursor.getDouble(3),
                            destinationKey = cursor.getString(4),
                            name = cursor.getString(5),
                            latitude = cursor.getDouble(6),
                            longitude = cursor.getDouble(7),
                            journeyCount = cursor.getInt(8),
                            averageDurationSeconds = cursor.getDouble(9).roundToLong(),
                            shortestDurationSeconds = cursor.getLong(10),
                            longestDurationSeconds = cursor.getLong(11),
                            lastCompletedAtMillis = cursor.getLong(12),
                            lastDurationSeconds = cursor.getLong(13)
                        )
                    )
                }
            }
        }
    }

    fun loadJourneysForRoute(originKey: String, destinationKey: String): List<RouteJourneyRecord> =
        readableDatabase.rawQuery(
            """SELECT id, started_at, completed_at, duration_seconds
                FROM journeys
                WHERE origin_key = ? AND destination_key = ? AND duration_seconds IS NOT NULL
                ORDER BY started_at DESC, id DESC""",
            arrayOf(originKey, destinationKey)
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        RouteJourneyRecord(
                            id = cursor.getLong(0),
                            startedAtMillis = cursor.getLong(1),
                            completedAtMillis = if (cursor.isNull(2)) null else cursor.getLong(2),
                            durationSeconds = cursor.getLong(3)
                        )
                    )
                }
            }
        }

    companion object {
        private const val DATABASE_NAME = "destination_journey_history.db"
        private const val DATABASE_VERSION = 3

        fun destinationKey(latitude: Double, longitude: Double): String =
            String.format(Locale.US, "%.5f,%.5f", latitude, longitude)
    }
}

@Suppress("DEPRECATION")
fun reverseGeocodeLocality(context: Context, latitude: Double, longitude: Double): String? = runCatching {
    Geocoder(context, Locale("pt", "BR")).getFromLocation(latitude, longitude, 1)
        ?.firstOrNull()
        ?.let { address ->
            listOfNotNull(address.subLocality, address.locality, address.subAdminArea)
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinct()
                .joinToString(", ")
                .ifBlank { address.getAddressLine(0).orEmpty() }
                .takeIf(String::isNotBlank)
        }
}.getOrNull()
