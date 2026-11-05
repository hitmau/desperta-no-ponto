package br.com.despertanoponto

import android.content.Context
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationManagerCompat

class AlarmActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var alarmType: AlarmType = AlarmType.ARRIVAL
    private val autoDismissHandler = Handler(Looper.getMainLooper())
    private val autoDismissRunnable = Runnable {
        if (alarmType == AlarmType.ARRIVAL && !isFinishing) dismissAlarm(stopTracking = true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) setShowWhenLocked(true)
        if (Build.VERSION.SDK_INT >= 27) setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        alarmType = AlarmStateStore.getType(this) ?: run {
            finish()
            return
        }
        val label = AlarmStateStore.getLabel(this)
        val config = DestinationStore(this).load()
        startAlarm(config)
        val descriptionText = if (alarmType == AlarmType.ARRIVAL) {
            "Você está no raio de chegada de:\n$label\n\nO alarme será encerrado automaticamente em ${config.arrivalAlarmDurationSeconds} s."
        } else {
            "Você se afastou ${config.departureRadiusMeters} m do ponto mais próximo registrado no trajeto de:\n$label"
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.rgb(11, 15, 20))
        }
        val icon = TextView(this).apply {
            text = if (alarmType == AlarmType.ARRIVAL) "●" else "↗"
            textSize = 62f
            setTextColor(Color.rgb(98, 215, 178))
            gravity = Gravity.CENTER
        }
        val title = TextView(this).apply {
            text = if (alarmType == AlarmType.ARRIVAL) "Você chegou!" else "Afastamento detectado"
            textSize = 30f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        val description = TextView(this).apply {
            text = descriptionText
            textSize = 17f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 18, 0, 36)
        }
        val dismiss = Button(this).apply {
            text = "Dispensar alarme"
            setOnClickListener { dismissAlarm(stopTracking = false) }
        }
        val disable = Button(this).apply {
            text = "Dispensar e desativar"
            setOnClickListener { dismissAlarm(stopTracking = true) }
        }
        root.addView(icon, LinearLayout.LayoutParams(-1, 100))
        root.addView(title, LinearLayout.LayoutParams(-1, -2))
        root.addView(description, LinearLayout.LayoutParams(-1, -2))
        root.addView(dismiss, LinearLayout.LayoutParams(-1, 56))
        root.addView(disable, LinearLayout.LayoutParams(-1, 56))
        setContentView(root)
        if (alarmType == AlarmType.ARRIVAL) {
            autoDismissHandler.postDelayed(
                autoDismissRunnable,
                config.arrivalAlarmDurationSeconds.coerceIn(1, 3600) * 1_000L
            )
        }
    }

    private fun startAlarm(config: DestinationConfig) {
        if (config.soundEnabled) {
            runCatching {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(this@AlarmActivity, uri)
                    isLooping = true
                    prepare()
                    start()
                }
            }
        }
        if (config.vibrationEnabled) {
            runCatching {
                vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= 26) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 350), 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 700, 350), 0)
                }
            }.onFailure {
                // O despertador continua funcionando com som mesmo se o fabricante
                // bloquear a vibração ou a permissão não estiver disponível.
                vibrator = null
            }
        }
    }

    private fun dismissAlarm(stopTracking: Boolean) {
        autoDismissHandler.removeCallbacks(autoDismissRunnable)
        mediaPlayer?.runCatching { stop(); release() }
        mediaPlayer = null
        vibrator?.cancel()
        AlarmStateStore.clear(this)
        NotificationManagerCompat.from(this).cancel(TrackingService.ALARM_NOTIFICATION_ID)
        if (stopTracking) {
            stopService(android.content.Intent(this, TrackingService::class.java))
            TrackingStateStore.stopRun(this)
        }
        finish()
    }

    override fun onDestroy() {
        autoDismissHandler.removeCallbacks(autoDismissRunnable)
        mediaPlayer?.runCatching { stop(); release() }
        vibrator?.cancel()
        super.onDestroy()
    }
}
