package com.nerdwatch.data

import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Opens the system app behind each face element. Everything is wrapped so a
 * missing app on a given watch simply does nothing rather than crashing.
 */
object SubAppLauncher {

    fun openBattery(context: Context) = launch(context) {
        Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
    }

    fun openSteps(context: Context) = launchApp(context, category = Intent.CATEGORY_APP_FITNESS)

    fun openWeather(context: Context) = launch(context) {
        Intent(Intent.ACTION_VIEW).setData(android.net.Uri.parse("dynact://velour/weather/ProxyActivity"))
    }

    fun openCalendar(context: Context) = launchApp(context, category = Intent.CATEGORY_APP_CALENDAR)

    private inline fun launch(context: Context, build: () -> Intent) {
        runCatching {
            context.startActivity(build().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun launchApp(context: Context, category: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
