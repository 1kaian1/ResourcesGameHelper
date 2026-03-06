package at.uastw.resourcesgamehelper

import kotlin.math.*

data class Point(val x: Double, val y: Double, val lat: Double, val lon: Double)

object HqCalculator {

    private const val METERS_PER_DEGREE = 111111.0

    private fun project(lat: Double, lon: Double, refLat: Double, refLon: Double): Pair<Double, Double> {
        val dy = (lat - refLat) * METERS_PER_DEGREE
        val dx = (lon - refLon) * METERS_PER_DEGREE * cos(Math.toRadians(refLat))
        return Pair(dx, dy)
    }

    private fun unproject(x: Double, y: Double, refLat: Double, refLon: Double): Pair<Double, Double> {
        val lat = refLat + (y / METERS_PER_DEGREE)
        val lon = refLon + (x / (METERS_PER_DEGREE * cos(Math.toRadians(refLat))))
        return Pair(lat, lon)
    }

    fun circleCenters(a: Point, b: Point, r: Double, refLat: Double, refLon: Double): List<Point> {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val d2 = dx*dx + dy*dy
        val d = sqrt(d2)

        if (d > 2*r || d == 0.0) return emptyList()

        val mx = (a.x + b.x) / 2
        val my = (a.y + b.y) / 2

        val h = sqrt((r*r - (d/2)*(d/2)).coerceAtLeast(0.0))
        val px = -dy * (h/d)
        val py = dx * (h/d)

        return listOf(
            unproject(mx + px, my + py, refLat, refLon).let { Point(mx + px, my + py, it.first, it.second) },
            unproject(mx - px, my - py, refLat, refLon).let { Point(mx - px, my - py, it.first, it.second) }
        )
    }

    fun countPoints(center: Point, points: List<Point>, r: Double): Int {
        var count = 0
        val r2 = r * r
        for (p in points) {
            val dx = center.x - p.x
            val dy = center.y - p.y
            if (dx*dx + dy*dy <= r2 + 0.0001) count++
        }
        return count
    }

    fun findBestHqLocation(mines: List<Mine>, radiusMeters: Double = 145.0): Pair<Pair<Double, Double>?, Int> {
        if (mines.isEmpty()) return Pair(null, 0)
        
        val refLat = mines.map { it.lat }.average()
        val refLon = mines.map { it.lon }.average()
        
        val points = mines.map { 
            val proj = project(it.lat, it.lon, refLat, refLon)
            Point(proj.first, proj.second, it.lat, it.lon) 
        }

        var bestCenter: Point? = null
        var bestCount = 0
        val n = points.size

        for (i in 0 until n) {
            val cCountSelf = countPoints(points[i], points, radiusMeters)
            if (cCountSelf > bestCount) {
                bestCount = cCountSelf
                bestCenter = points[i]
            }
            for (j in i + 1 until n) {
                val centers = circleCenters(points[i], points[j], radiusMeters, refLat, refLon)
                for (c in centers) {
                    val cCount = countPoints(c, points, radiusMeters)
                    if (cCount > bestCount) {
                        bestCount = cCount
                        bestCenter = c
                    }
                }
            }
        }
        return Pair(bestCenter?.let { Pair(it.lat, it.lon) }, bestCount)
    }

    fun calculateMaxMinesInRadius(radius: Double = 145.0, minDist: Double = 30.5): Int {
        // Area-based approximation for hexagonal packing
        val areaCircle = PI * radius * radius
        val areaPerPoint = (sqrt(3.0) / 2.0) * minDist * minDist
        return (areaCircle / areaPerPoint).toInt()
    }
}
