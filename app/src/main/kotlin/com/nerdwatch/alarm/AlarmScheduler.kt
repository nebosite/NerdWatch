package com.nerdwatch.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.Instant

/**
 * Schedules the alarm/timer broadcast with [AlarmManager] using
 * [AlarmManager.setAlarmClock].
 *
 * setAlarmClock (not setExactAndAllowWhileIdle) is deliberate: only alarm-clock
 * alarms are exempt from Doze **and** App Standby **and** Battery Saver deferral.
 * setExactAndAllowWhileIdle escapes Doze but is still held for minutes once the
 * app is killed into a restricted standby bucket — which made a backgrounded
 * timer fire late. setAlarmClock is the user-facing "this will wake you" contract
 * and fires on time regardless of the app's state.
 */
object AlarmScheduler {

    fun schedule(context: Context, at: Instant, message: String = "Alarm") {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pending = pendingIntent(context, at, message)
        val triggerAt = at.toEpochMilli()
        val info = AlarmManager.AlarmClockInfo(triggerAt, showIntent(context))
        manager.setAlarmClock(info, pending)
    }

    /** Opens the app if the user taps the system alarm indicator. */
    private fun showIntent(context: Context): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return null
        return PendingIntent.getActivity(
            context,
            0,
            launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    fun cancel(context: Context, at: Instant) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        // Extras are ignored when matching a PendingIntent, so the message is
        // irrelevant here — the request code (the instant) is what matches.
        val pending = pendingIntent(context, at, "")
        manager.cancel(pending)
        pending.cancel()
    }

    /** Same request code for a given instant, so schedule and cancel match up. */
    private fun pendingIntent(context: Context, at: Instant, message: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_MESSAGE, message)
        val requestCode = (at.epochSecond and 0x7FFFFFFF).toInt()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
