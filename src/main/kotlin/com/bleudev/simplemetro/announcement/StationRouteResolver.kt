package com.bleudev.simplemetro.announcement

import com.bleudev.simplemetro.config.SMetroInfo
import com.bleudev.simplemetro.config.obj.MetroStation
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.sqrt

object StationRouteResolver {
    data class Route(
        val lineId: Int,
        val direction: Int,
        val nextStationId: Int,
    )

    private data class Candidate(
        val route: Route,
        val points: List<Vec3>,
    )

    private data class Projection(
        val distance: Double,
        val tangent: Vec3,
    )

    fun detectVisibleStation(
        stations: List<MetroStation>,
        previousCurrent: Int?,
        playerPos: Vec3,
    ): Int? {
        var bestId: Int? = null
        var bestDistance = Double.POSITIVE_INFINITY

        for (station in stations) {
            if (station.status.invisible) continue
            val stationPos = Vec3.atCenterOf(station.pos)
            val horizontalDistance = stationPos.horizontal().distanceTo(playerPos.horizontal())
            val verticalDistance = abs(stationPos.y - playerPos.y)
            if (horizontalDistance <= STATION_ENTER_RADIUS &&
                verticalDistance <= STATION_CHECK_HEIGHT &&
                horizontalDistance < bestDistance
            ) {
                bestId = station.id
                bestDistance = horizontalDistance
            }
        }

        if (previousCurrent != null) {
            val previous = stations.firstOrNull { it.id == previousCurrent }
            if (previous != null && !previous.status.invisible) {
                val stationPos = Vec3.atCenterOf(previous.pos)
                val oldDistance = stationPos.horizontal().distanceTo(playerPos.horizontal())
                val oldHeight = abs(stationPos.y - playerPos.y)
                if (oldDistance <= STATION_EXIT_RADIUS && oldHeight <= STATION_CHECK_HEIGHT &&
                    (bestId == null || bestId == previousCurrent || bestDistance + STATION_SWITCH_MARGIN >= oldDistance)
                ) {
                    return previousCurrent
                }
            }
        }

        return bestId
    }

    /**
     * Finds the visible station immediately ahead on the most likely outgoing route.
     * Invisible stations remain part of the route geometry but are skipped as stops.
     */
    fun resolveNextStation(
        info: SMetroInfo,
        fromStationId: Int,
        playerPos: Vec3,
        motion: Vec3,
        preferredLine: Int? = null,
        preferredDirection: Int? = null,
    ): Route? {
        val stationsById = info.stations.associateBy { it.id }
        val candidates = ArrayList<Candidate>()
        val seen = hashSetOf<Triple<Int, Int, Int>>()
        val source = stationsById[fromStationId] ?: return null
        val sourcePos = Vec3.atCenterOf(source.pos)

        for (line in info.lines) {
            for (startIndex in line.stations.indices.filter { line.stations[it] == fromStationId }) {
                for (direction in intArrayOf(-1, 1)) {
                    var index = startIndex + direction
                    val points = ArrayList<Vec3>()
                    points.add(sourcePos)

                    while (index in line.stations.indices) {
                        val stationId = line.stations[index]
                        val station = stationsById[stationId] ?: break
                        points.add(Vec3.atCenterOf(station.pos))

                        if (!station.status.invisible) {
                            val key = Triple(line.id, stationId, direction)
                            if (stationId != fromStationId && seen.add(key)) {
                                candidates.add(
                                    Candidate(Route(line.id, direction, stationId), points.toList()),
                                )
                            }
                            break
                        }
                        index += direction
                    }
                }
            }
        }

        if (candidates.isEmpty()) return null

        val horizontalMotion = Vec3(motion.x, 0.0, motion.z)
        val hasMotion = horizontalMotion.lengthSqr() > MOVEMENT_EPSILON_SQR
        val normalizedMotion = if (hasMotion) horizontalMotion.normalize() else ZERO

        return candidates.minByOrNull { candidate ->
            val projection = project(candidate.points, playerPos, motion)
            val tangent = projection?.tangent?.let { Vec3(it.x, 0.0, it.z) } ?: ZERO
            val alignment = if (hasMotion && tangent.lengthSqr() > MOVEMENT_EPSILON_SQR) {
                normalizedMotion.dot(tangent.normalize()).coerceIn(-1.0, 1.0)
            } else {
                0.0
            }
            val directionPenalty = if (hasMotion) (1.0 - alignment) * DIRECTION_WEIGHT else 0.0
            val linePreference = if (candidate.route.lineId == preferredLine) LINE_PREFERENCE else 0.0
            val directionPreference = if (
                candidate.route.lineId == preferredLine && candidate.route.direction == preferredDirection
            ) DIRECTION_PREFERENCE else 0.0

            (projection?.distance ?: Double.POSITIVE_INFINITY) + directionPenalty - linePreference - directionPreference
        }?.route
    }

    private fun project(points: List<Vec3>, playerPos: Vec3, motion: Vec3): Projection? {
        if (points.size < 2) return null

        val horizontalMotion = Vec3(motion.x, 0.0, motion.z)
        val useMotionForTieBreak = horizontalMotion.lengthSqr() > MOVEMENT_EPSILON_SQR
        val normalizedMotion = if (useMotionForTieBreak) horizontalMotion.normalize() else ZERO

        var bestMetric = Double.POSITIVE_INFINITY
        var best: Projection? = null

        for (index in 0 until points.lastIndex) {
            val a = points[index]
            val b = points[index + 1]
            val ab = b.subtract(a)
            val segmentLengthSqr = ab.lengthSqr()
            if (segmentLengthSqr <= 1.0e-9) continue

            val segmentLength = sqrt(segmentLengthSqr)
            val t = (playerPos.subtract(a).dot(ab) / segmentLengthSqr).coerceIn(0.0, 1.0)
            val closest = a.add(ab.scale(t))
            val horizontalDistance = closest.horizontal().distanceTo(playerPos.horizontal())
            val verticalDistance = abs(closest.y - playerPos.y)
            val tangent = ab.scale(1.0 / segmentLength)

            var metric = horizontalDistance * horizontalDistance + verticalDistance * verticalDistance
            if (useMotionForTieBreak) {
                val horizontalTangent = Vec3(tangent.x, 0.0, tangent.z)
                if (horizontalTangent.lengthSqr() > MOVEMENT_EPSILON_SQR) {
                    val alignment = normalizedMotion.dot(horizontalTangent.normalize()).coerceIn(-1.0, 1.0)
                    metric += (1.0 - alignment) * PROJECTION_DIRECTION_TIE_WEIGHT
                }
            }

            if (metric < bestMetric) {
                bestMetric = metric
                best = Projection(sqrt(horizontalDistance * horizontalDistance + verticalDistance * verticalDistance), tangent)
            }
        }

        return best
    }

    private const val STATION_ENTER_RADIUS = 7.0
    private const val STATION_EXIT_RADIUS = 9.0
    private const val STATION_SWITCH_MARGIN = 1.0
    private const val STATION_CHECK_HEIGHT = 5.0
    private const val MOVEMENT_EPSILON_SQR = 0.0004
    private const val DIRECTION_WEIGHT = 4.0
    private const val LINE_PREFERENCE = 0.75
    private const val DIRECTION_PREFERENCE = 0.75
    private const val PROJECTION_DIRECTION_TIE_WEIGHT = 0.0001
    private val ZERO = Vec3(0.0, 0.0, 0.0)
}
