package com.bleudev.simplemetro.client.storage

import com.bleudev.simplemetro.config.SMetroInfo
import com.bleudev.simplemetro.config.obj.MetroStation
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.resources.Identifier
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.sqrt

@Environment(EnvType.CLIENT)
class LinePanelExtractor private constructor() {
    private data class TrackPath(
        val lineId: Int,
        val fromStationId: Int,
        val toStationId: Int,
        val direction: Int,
        val points: List<Vec3>,
    )

    private data class Projection(
        val distanceHorizontal: Double,
        val distanceVertical: Double,
        val progress: Double,
        val tangent: Vec3,
    ) {
        val isNearTrack: Boolean
            get() = distanceHorizontal <= TRACK_CHECK_RADIUS && distanceVertical <= TRACK_CHECK_HEIGHT

        val distance: Double
            get() = sqrt(distanceHorizontal * distanceHorizontal + distanceVertical * distanceVertical)
    }

    private var activePath: TrackPath? = null
    private var lastPlayerPos: Vec3? = null
    private var lastDimension: Identifier? = null
    private var offTrackTicks = 0

    fun playerTick(storage: MetroClientStorage, player: LocalPlayer, level: ClientLevel) {
        val playerPos = player.position()
        val dimension = level.dimension().identifier()
        val info = SMetroInfo.getInstance(dimension)
        val stationsById = info.stations.associateBy { it.id }

        if (lastDimension != dimension) {
            reset(storage)
            lastDimension = dimension
            lastPlayerPos = playerPos
        }

        val motion = lastPlayerPos?.let { playerPos.subtract(it) } ?: ZERO
        lastPlayerPos = playerPos

        val previousCurrent = storage.currentStation

        activePath?.let { path ->
            if (stationsById[path.toStationId]?.status?.invisible == true) {
                val replacement = chooseOutgoingPath(
                    path.fromStationId,
                    info,
                    playerPos,
                    ZERO,
                    path.lineId,
                    path.direction,
                )
                setActivePath(storage, replacement, path.lineId)
            }
        }

        val current = detectVisibleStation(info.stations, stationsById, previousCurrent, playerPos)

        if (previousCurrent != current) {
            if (previousCurrent != null) {
                stationsById[previousCurrent]?.let { station ->
                    if (!station.status.invisible) {
                        storage.publishStationEvent(
                            MetroClientStorage.StationEventType.DEPARTURE,
                            station.id,
                            station.name,
                        )
                    }
                }
            }
            if (current != null) {
                stationsById[current]?.let { station ->
                    if (!station.status.invisible) {
                        storage.publishStationEvent(
                            MetroClientStorage.StationEventType.ARRIVAL,
                            station.id,
                            station.name,
                        )
                    }
                }
            }

            if (current == null && previousCurrent != null) {
                val previousStation = stationsById[previousCurrent]
                if (previousStation == null || previousStation.status.invisible) {
                    clearRoute(storage)
                } else {
                    storage.prevVisibleStation = previousCurrent
                    val selected = chooseOutgoingPath(
                        previousCurrent,
                        info,
                        playerPos,
                        motion,
                        storage.currentLine,
                        activePath?.direction,
                    )
                    setActivePath(storage, selected)
                }
            } else if (current != null) {
                val arrivedPath = activePath?.takeIf { it.toStationId == current }
                    ?: storage.prevVisibleStation?.let { from ->
                        findPathBetween(from, current, info, playerPos, motion, storage.currentLine)
                    }
                    ?: previousCurrent?.let { from ->
                        findPathBetween(from, current, info, playerPos, motion, storage.currentLine)
                    }

                storage.prevVisibleStation = current
                val continuation = arrivedPath?.let { arrived ->
                    outgoingPaths(current, info).firstOrNull {
                        it.lineId == arrived.lineId && it.direction == arrived.direction
                    }
                }

                val selected = continuation ?: if (arrivedPath == null) {
                    chooseOutgoingPath(current, info, playerPos, motion, storage.currentLine, null)
                } else {
                    null
                }
                setActivePath(storage, selected, arrivedPath?.lineId)
            }

            storage.currentStation = current
        }

        if (current != null) {
            if (storage.prevVisibleStation == null) {
                storage.prevVisibleStation = current
                setActivePath(storage, chooseOutgoingPath(current, info, playerPos, motion, storage.currentLine, null))
            }
            val stationPos = stationsById[current]?.pos?.let { Vec3.atCenterOf(it) }
            if (stationPos != null) {
                val radial = playerPos.subtract(stationPos)
                val horizontalMotion = Vec3(motion.x, 0.0, motion.z)
                val horizontalRadial = Vec3(radial.x, 0.0, radial.z)
                val isMovingAway = horizontalMotion.lengthSqr() > MOVEMENT_EPSILON_SQR &&
                    horizontalRadial.lengthSqr() > MOVEMENT_EPSILON_SQR &&
                    horizontalMotion.dot(horizontalRadial) > 0.0
                if (isMovingAway) {
                    selectBetterPath(storage, current, info, playerPos, motion)
                    val projection = activePath?.let { projectOnPath(it.points, playerPos, motion) }
                    storage.stationProgress = projection
                        ?.takeIf { it.isNearTrack }
                        ?.progress
                        ?.toFloat()
                        ?.coerceIn(0f, 1f)
                        ?: 0f
                } else {
                    storage.stationProgress = 0f
                }
            } else {
                storage.stationProgress = 0f
            }
            offTrackTicks = 0
            return
        }

        if (activePath == null && storage.prevVisibleStation != null) {
            setActivePath(
                storage,
                chooseOutgoingPath(storage.prevVisibleStation!!, info, playerPos, motion, storage.currentLine, null),
            )
        }

        activePath?.let { path ->
            selectBetterPath(storage, path.fromStationId, info, playerPos, motion)
        }

        val path = activePath
        if (path != null) {
            val projection = projectOnPath(path.points, playerPos, motion)
            if (projection != null && projection.isNearTrack) {
                storage.stationProgress = projection.progress.toFloat().coerceIn(0f, 1f)
                offTrackTicks = 0
            } else {
                offTrackTicks++
                if (offTrackTicks > OFF_TRACK_GRACE_TICKS) clearRoute(storage)
            }
        } else {
            storage.stationProgress = null
        }
    }

    private fun detectVisibleStation(
        stations: List<MetroStation>,
        stationsById: Map<Int, MetroStation>,
        previousCurrent: Int?,
        playerPos: Vec3,
    ): Int? {
        var bestId: Int? = null
        var bestDistance = Double.POSITIVE_INFINITY

        for (station in stations) {
            if (station.status.invisible) continue
            val pos = Vec3.atCenterOf(station.pos)
            val horizontalDistance = pos.horizontal().distanceTo(playerPos.horizontal())
            val verticalDistance = abs(pos.y - playerPos.y)
            if (horizontalDistance <= STATION_ENTER_RADIUS &&
                verticalDistance <= STATION_CHECK_HEIGHT &&
                horizontalDistance < bestDistance
            ) {
                bestId = station.id
                bestDistance = horizontalDistance
            }
        }

        if (previousCurrent != null) {
            val previous = stationsById[previousCurrent]
            if (previous != null && !previous.status.invisible) {
                val pos = Vec3.atCenterOf(previous.pos)
                val oldDistance = pos.horizontal().distanceTo(playerPos.horizontal())
                val oldHeight = abs(pos.y - playerPos.y)
                if (oldDistance <= STATION_EXIT_RADIUS && oldHeight <= STATION_CHECK_HEIGHT &&
                    (bestId == null || bestId == previousCurrent || bestDistance + STATION_SWITCH_MARGIN >= oldDistance)
                ) {
                    return previousCurrent
                }
            }
        }

        return bestId
    }

    private fun outgoingPaths(fromStationId: Int, info: SMetroInfo): List<TrackPath> {
        val stationsById = info.stations.associateBy { it.id }
        val paths = ArrayList<TrackPath>()
        val seen = hashSetOf<Triple<Int, Int, Int>>()

        for (line in info.lines) {
            for (startIndex in line.stations.indices.filter { line.stations[it] == fromStationId }) {
                for (direction in intArrayOf(-1, 1)) {
                    var index = startIndex + direction
                    val points = ArrayList<Vec3>()
                    val source = stationsById[fromStationId] ?: continue
                    points.add(Vec3.atCenterOf(source.pos))

                    while (index in line.stations.indices) {
                        val stationId = line.stations[index]
                        val station = stationsById[stationId] ?: break
                        points.add(Vec3.atCenterOf(station.pos))

                        if (!station.status.invisible) {
                            val key = Triple(line.id, stationId, direction)
                            if (stationId != fromStationId && seen.add(key)) {
                                paths.add(
                                    TrackPath(
                                        line.id,
                                        fromStationId,
                                        stationId,
                                        direction,
                                        points.toList(),
                                    )
                                )
                            }
                            break
                        }
                        index += direction
                    }
                }
            }
        }
        return paths
    }

    private fun findPathBetween(
        from: Int,
        to: Int,
        info: SMetroInfo,
        playerPos: Vec3,
        motion: Vec3,
        preferredLine: Int?,
    ): TrackPath? {
        val candidates = outgoingPaths(from, info).filter { it.toStationId == to }
        return chooseBestPath(candidates, playerPos, motion, preferredLine, null)
    }

    private fun chooseOutgoingPath(
        from: Int,
        info: SMetroInfo,
        playerPos: Vec3,
        motion: Vec3,
        preferredLine: Int?,
        preferredDirection: Int?,
    ): TrackPath? {
        return chooseBestPath(
            outgoingPaths(from, info),
            playerPos,
            motion,
            preferredLine,
            preferredDirection,
        )
    }

    private fun chooseBestPath(
        candidates: List<TrackPath>,
        playerPos: Vec3,
        motion: Vec3,
        preferredLine: Int?,
        preferredDirection: Int?,
    ): TrackPath? {
        if (candidates.isEmpty()) return null

        val horizontalMotion = Vec3(motion.x, 0.0, motion.z)
        val hasMotion = horizontalMotion.lengthSqr() > MOVEMENT_EPSILON_SQR
        val normalizedMotion = if (hasMotion) horizontalMotion.normalize() else ZERO

        return candidates.minByOrNull { path ->
            val projection = projectOnPath(path.points, playerPos, motion)
            val distance = projection?.distance ?: Double.POSITIVE_INFINITY
            val tangent = projection?.tangent?.let { Vec3(it.x, 0.0, it.z) } ?: ZERO
            val alignment = if (hasMotion && tangent.lengthSqr() > MOVEMENT_EPSILON_SQR) {
                normalizedMotion.dot(tangent.normalize()).coerceIn(-1.0, 1.0)
            } else {
                0.0
            }
            val directionPenalty = if (hasMotion) (1.0 - alignment) * DIRECTION_WEIGHT else 0.0
            val linePreference = if (path.lineId == preferredLine) LINE_PREFERENCE else 0.0
            val directionPreference = if (path.lineId == preferredLine && path.direction == preferredDirection) {
                DIRECTION_PREFERENCE
            } else {
                0.0
            }
            distance + directionPenalty - linePreference - directionPreference
        }
    }

    private fun selectBetterPath(
        storage: MetroClientStorage,
        fromStationId: Int,
        info: SMetroInfo,
        playerPos: Vec3,
        motion: Vec3,
    ) {
        val currentPath = activePath
        val alternative = chooseOutgoingPath(
            fromStationId,
            info,
            playerPos,
            motion,
            storage.currentLine,
            currentPath?.direction,
        ) ?: return

        if (currentPath != null && alternative.lineId == currentPath.lineId &&
            alternative.toStationId == currentPath.toStationId && alternative.direction == currentPath.direction
        ) return

        val alternativeProjection = projectOnPath(alternative.points, playerPos, motion)
        if (alternativeProjection == null || !alternativeProjection.isNearTrack) return

        val currentProjection = currentPath?.let { projectOnPath(it.points, playerPos, motion) }
        if (currentProjection == null || !currentProjection.isNearTrack ||
            alternativeProjection.distance + PATH_SWITCH_MARGIN < currentProjection.distance
        ) {
            setActivePath(storage, alternative)
        }
    }

    private fun projectOnPath(points: List<Vec3>, playerPos: Vec3, motion: Vec3): Projection? {
        if (points.size < 2) return null

        var totalLength = 0.0
        for (index in 0 until points.lastIndex) {
            totalLength += points[index].distanceTo(points[index + 1])
        }
        if (totalLength <= 1.0e-6) return null

        val horizontalMotion = Vec3(motion.x, 0.0, motion.z)
        val useMotionForTieBreak = horizontalMotion.lengthSqr() > MOVEMENT_EPSILON_SQR
        val normalizedMotion = if (useMotionForTieBreak) horizontalMotion.normalize() else ZERO

        var lengthBefore = 0.0
        var bestMetric = Double.POSITIVE_INFINITY
        var best: Projection? = null

        for (index in 0 until points.lastIndex) {
            val a = points[index]
            val b = points[index + 1]
            val ab = b.subtract(a)
            val segmentLengthSqr = ab.lengthSqr()
            val segmentLength = sqrt(segmentLengthSqr)
            if (segmentLengthSqr <= 1.0e-9) continue

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
                val progress = (lengthBefore + segmentLength * t) / totalLength
                bestMetric = metric
                best = Projection(horizontalDistance, verticalDistance, progress, tangent)
            }
            lengthBefore += segmentLength
        }

        return best
    }

    private fun setActivePath(storage: MetroClientStorage, path: TrackPath?, fallbackLineId: Int? = null) {
        activePath = path
        offTrackTicks = 0
        storage.currentLine = path?.lineId ?: fallbackLineId
        storage.nextVisibleStation = path?.toStationId
        storage.stationProgress = if (path != null) 0f else null
    }

    private fun clearRoute(storage: MetroClientStorage) {
        activePath = null
        offTrackTicks = 0
        storage.prevVisibleStation = null
        storage.nextVisibleStation = null
        storage.currentLine = null
        storage.stationProgress = null
    }

    private fun reset(storage: MetroClientStorage) {
        activePath = null
        offTrackTicks = 0
        storage.currentStation = null
        storage.prevVisibleStation = null
        storage.nextVisibleStation = null
        storage.currentLine = null
        storage.stationProgress = null
    }

    companion object {
        private const val STATION_ENTER_RADIUS = 7.0
        private const val STATION_EXIT_RADIUS = 9.0
        private const val STATION_SWITCH_MARGIN = 1.0
        private const val STATION_CHECK_HEIGHT = 5.0
        private const val TRACK_CHECK_RADIUS = 7.0
        private const val TRACK_CHECK_HEIGHT = 5.0
        private const val OFF_TRACK_GRACE_TICKS = 8
        private const val PATH_SWITCH_MARGIN = 1.0
        private const val MOVEMENT_EPSILON_SQR = 0.0004
        private const val DIRECTION_WEIGHT = 4.0
        private const val LINE_PREFERENCE = 0.75
        private const val DIRECTION_PREFERENCE = 0.75
        private const val PROJECTION_DIRECTION_TIE_WEIGHT = 0.0001
        private val ZERO = Vec3(0.0, 0.0, 0.0)

        private var instance: LinePanelExtractor? = null
        fun getInstance(): LinePanelExtractor {
            if (instance == null) {
                instance = LinePanelExtractor()
            }
            return instance!!
        }
    }
}
