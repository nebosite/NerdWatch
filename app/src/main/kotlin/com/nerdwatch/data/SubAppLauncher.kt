package com.nerdwatch.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Opens the system app behind each face element.
 *
 * Each target is a prioritized list: the specific app expected on a Galaxy Watch
 * first (Samsung Health for steps, a weather app for temp), then generic
 * fallbacks. The first intent that actually resolves is launched; if none do,
 * nothing happens rather than crashing. The specific packages are declared in
 * the manifest's <queries> so they are visible on Android 11+.
 */
object SubAppLauncher {

    fun openBattery(context: Context) = launchFirst(
        context,
        actionIntent(Settings.ACTION_BATTERY_SAVER_SETTINGS),
    )

    /** Steps detail: Samsung Health on a Galaxy Watch, else the system fitness app. */
    fun openSteps(context: Context) = launchFirst(
        context,
        packageIntent(context, "com.samsung.android.wear.shealth"),
        categoryIntent(Intent.CATEGORY_APP_FITNESS),
        packageIntent(context, "com.google.android.apps.fitness"),
    )

    /** The weather app: Samsung's on a Galaxy Watch, else a Google weather target. */
    fun openWeather(context: Context) = launchFirst(
        context,
        packageIntent(context, "com.samsung.android.watch.weather"),
        packageIntent(context, "com.samsung.android.weather"),
        viewIntent("dynact://velour/weather/ProxyActivity"),
    )

    fun openCalendar(context: Context) = launchFirst(
        context,
        categoryIntent(Intent.CATEGORY_APP_CALENDAR),
    )

    /** Launch the first intent that resolves to something on this device. */
    private fun launchFirst(context: Context, vararg intents: Intent?) {
        val intent = intents.filterNotNull().firstOrNull {
            it.resolveActivity(context.packageManager) != null
        } ?: return
        runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun packageIntent(context: Context, pkg: String): Intent? =
        context.packageManager.getLaunchIntentForPackage(pkg)

    private fun actionIntent(action: String): Intent = Intent(action)

    private fun categoryIntent(category: String): Intent =
        Intent(Intent.ACTION_MAIN).addCategory(category)

    private fun viewIntent(uri: String): Intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
}
