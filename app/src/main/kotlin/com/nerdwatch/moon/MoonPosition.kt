package com.nerdwatch.moon

import java.time.Instant
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Where the moon sits in the sky, mapped onto the widget's ring.
 *
 * The ring is the full 360° diurnal circle of the moon's hour angle, drawn from
 * the perspective of a Northern-hemisphere observer facing south: the moon at
 * upper transit ("overhead") is the top, rising in the east is the LEFT, nadir
 * ("below your feet") is the bottom, setting in the west is the RIGHT. Facing
 * south puts east on the left, so ring angle φ (clockwise from top) = +H, the
 * hour angle measured west from the meridian.
 *
 * Lunar right ascension uses Schlyter's low-precision method (a degree or two —
 * fine for a marker). Latitude is not needed; only longitude, via sidereal time.
 */
object MoonPosition {

    /** Ring angle in degrees, clockwise from the top, for the moon at [instant]. */
    fun ringAngleDegrees(instant: Instant, longitudeEastDeg: Double): Double {
        val raDeg = rightAscensionDeg(instant)
        val lstDeg = norm360(gmstDeg(instant) + longitudeEastDeg)
        val hourAngleDeg = lstDeg - raDeg
        return ringAngleFromHourAngle(hourAngleDeg)
    }

    /**
     * Pure mapping, facing south: transit(0)→top, +6h/90° (setting/west)→right,
     * −6h (rising/east)→left, ±12h→bottom.
     */
    fun ringAngleFromHourAngle(hourAngleDeg: Double): Double = norm360(hourAngleDeg)

    /** Geocentric apparent right ascension of the moon, degrees [0,360). */
    fun rightAscensionDeg(instant: Instant): Double {
        val d = daysSince2000(instant)

        // Moon orbital elements (degrees / Earth radii).
        val n = rad(norm360(125.1228 - 0.0529538083 * d))
        val i = rad(5.1454)
        val w = rad(norm360(318.0634 + 0.1643573223 * d))
        val a = 60.2666
        val e = 0.054900
        val m = rad(norm360(115.3654 + 13.0649929509 * d))

        // Eccentric anomaly (a few iterations; e is small).
        var ecc = m + e * sin(m) * (1.0 + e * cos(m))
        repeat(3) { ecc = ecc - (ecc - e * sin(ecc) - m) / (1.0 - e * cos(ecc)) }

        val xv = a * (cos(ecc) - e)
        val yv = a * (sqrt(1.0 - e * e) * sin(ecc))
        val v = atan2(yv, xv)
        val r = sqrt(xv * xv + yv * yv)

        // Geocentric ecliptic rectangular coordinates.
        val xh = r * (cos(n) * cos(v + w) - sin(n) * sin(v + w) * cos(i))
        val yh = r * (sin(n) * cos(v + w) + cos(n) * sin(v + w) * cos(i))
        val zh = r * (sin(v + w) * sin(i))

        // Rotate ecliptic → equatorial by the obliquity.
        val ecl = rad(23.4393 - 3.563e-7 * d)
        val xe = xh
        val ye = yh * cos(ecl) - zh * sin(ecl)

        return norm360(deg(atan2(ye, xe)))
    }

    /** Greenwich mean sidereal time, degrees [0,360). */
    fun gmstDeg(instant: Instant): Double {
        val du = julianDay(instant) - 2451545.0
        return norm360(280.46061837 + 360.98564736629 * du)
    }

    private fun julianDay(instant: Instant): Double =
        instant.toEpochMilli() / 86_400_000.0 + 2440587.5

    private fun daysSince2000(instant: Instant): Double = julianDay(instant) - 2451543.5

    private fun rad(deg: Double) = deg * Math.PI / 180.0
    private fun deg(rad: Double) = rad * 180.0 / Math.PI

    private fun norm360(deg: Double): Double {
        var x = deg % 360.0
        if (x < 0.0) x += 360.0
        return x
    }
}
