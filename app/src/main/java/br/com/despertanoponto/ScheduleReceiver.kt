package br.com.despertanoponto

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import java.util.Calendar

class ScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val config = DestinationStore(context).load()
        if (!config.hasDestination() || config.activationMode == ActivationMode.MANUAL) return
        val dayIndex = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
        if (config.daysMask and (1 shl dayIndex) == 0) return
        if (TrackingStateStore.isActive(context) && !TrackingStateStore.hasArrived(context)) return

        TrackingStateStore.startRun(context)
        val serviceIntent = Intent(context, TrackingService::class.java)
            .setAction(TrackingService.ACTION_START_RECURRING)
        runCatching { ContextCompat.startForegroundService(context, serviceIntent) }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val config = DestinationStore(context).load()
        if (config.activationMode != ActivationMode.MANUAL && config.hasDestination()) {
            ScheduleManager.sync(context, config)
        }
        RouteScheduleManager.syncAll(context)
    }
}
