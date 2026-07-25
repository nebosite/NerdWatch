package com.nerdwatch.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.Instant

/**
 * Schedules the alarm broadcast with [AlarmManager]. Exact if the app is allowed
 * to (USE_EXACT_ALARM covers this for an alarm/timer app), else a best-effort
 * inexact wake so it still fires.
 */
object AlarmScheduler {

    fun schedule(context: Context, at: Instant) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pending = pendingIntent(context, at)
        val triggerAt = at.toEpochMilli()
        if (manager.canScheduleExactAlarms()) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancel(context: Context, at: Instant) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pending = pendingIntent(context, at)
        manager.cancel(pending)
        pending.cancel()
    }

    /** Same request code for a given instant, so schedule and cancel match up. */
    private fun pendingIntent(context: Context, at: Instant): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
        val requestCode = (at.epochSecond and 0x7FFFFFFF).toInt()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
