package com.bleudev.simplemetro.client.storage

import com.bleudev.simplemetro.config.SMetroInfo
import com.bleudev.simplemetro.config.obj.MetroStation
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

@Environment(EnvType.CLIENT)
class LineTableExtractor private constructor() {
    private var prevPos: BlockPos? = null
    private var nextPos: BlockPos? = null

    fun playerTick(storage: MetroClientStorage, player: LocalPlayer, level: ClientLevel) {
        val playerPos = player.position()
        val info = SMetroInfo.getInstance(level.dimension().identifier())
        val stationsById = info.stations.associateBy { it.id }

        var current: Int? = null
        var closestStationDistance = Double.POSITIVE_INFINITY

        for ((id, pos1, _, _, status) in info.stations) {
//            if (status.invisible) continue
            val pos = Vec3.atCenterOf(pos1)
            val d = pos.horizontal().distanceTo(playerPos.horizontal())
            val dy = abs(pos.y - playerPos.y)
            if (d <= STATION_CHECK_RADIUS && dy <= STATION_CHECK_HEIGHT && d < closestStationDistance) {
                current = id
                closestStationDistance = d
            }
        }
        if (storage.currentStation != current) {
            val previousCurrent = storage.currentStation

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

            storage.prevStation = previousCurrent ?: storage.prevStation
            storage.currentStation = current
            if (current != null && storage.prevStation == null) storage.prevStation = current
            computeNextStation(storage.prevStation, info, playerPos).let { (line, next) ->
                storage.currentLine = line
                storage.nextStation = next
            }
            if (storage.currentStation == null && storage.currentLine != null && storage.prevStation != null && storage.nextStation != null) {
                for ((id, pos) in info.stations) {
                    if (id == storage.prevStation) this.prevPos = pos
                    if (id == storage.nextStation) this.nextPos = pos
                }
            }
        }

        if (this.prevPos != null && this.nextPos != null) {
            if (isOnTrack(this.prevPos!!, this.nextPos!!, playerPos)) {
                storage.stationProgress = getLineProgress(Vec3.atCenterOf(this.prevPos!!), Vec3.atCenterOf(this.nextPos!!), playerPos).toFloat()
            } else {
                storage.currentLine = null
                storage.prevStation = null
                storage.nextStation = null
                storage.stationProgress = null
                this.prevPos = null
                this.nextPos = null
            }
        }

    }

    private fun computeNextStation(prev: Int?, info: SMetroInfo, playerPos: Vec3): Pair<Int?, Int?> {
        if (prev == null) return null to null
        val lines = info.lines.filter { prev in it.stations }
        val allStations = info.stations.associateBy { it.id }
        val station = allStations[prev] ?: return null to null

        var best: Pair<Int, Int>? = null
        var bestd: Double? = null
        var bests: MetroStation? = null
        for ((id, _, _, stations) in lines) {
            val i = stations.indexOf(prev)

            // Check first
            if (i-1 >= 0) {
                val f = stations[i-1]
                val fs = allStations[f] ?: continue
                val fd = playerPos.distanceToSqr(Vec3.atCenterOf(fs.pos))
                if (best == null || bestd == null || bestd > fd) {
                    best = id to f
                    bestd = fd
                    bests = fs
                }
            }

            // Check second
            if (i+1 < stations.size) {
                val s = stations[i+1]
                val ss = allStations[s] ?: continue
                val sd = playerPos.distanceToSqr(Vec3.atCenterOf(ss.pos))
                if (best == null || bestd == null || bestd > sd) {
                    best = id to s
                    bestd = sd
                    bests = ss
                }
            }
        }

        if (bests != null) {
            if (isOnTrack(station.pos, bests.pos, playerPos)) {
                return best ?: (null to null)
            }
        }
        return null to null
    }

    private fun getLineProgress(a: Vec3, b: Vec3, p: Vec3): Double {
        val ab = b.subtract(a)
        val ap = p.subtract(a)
        return ((ap.dot(ab)) / (ab.dot(ab))).coerceIn(0.0, 1.0)
    }

    private fun distanceToLine(a: Vec3, b: Vec3, p: Vec3): Double {
        val ab = b.subtract(a)
        val t = getLineProgress(a, b, p)
        val q = a.add(ab.scale(t))
        return p.distanceTo(q)
    }

    private fun isOnTrack(a: Vec3, b: Vec3, p: Vec3): Boolean {
        val t = getLineProgress(a, b, p)
        val ab = b.subtract(a)
        val q = a.add(ab.scale(t))
        return distanceToLine(a.horizontal(), b.horizontal(), p.horizontal()) <= STATION_CHECK_RADIUS
            && abs(q.y-p.y) <= STATION_CHECK_HEIGHT
    }


    private fun isOnTrack(a: BlockPos, b: BlockPos, p: Vec3): Boolean {
        return isOnTrack(Vec3.atCenterOf(a), Vec3.atCenterOf(b), p)
    }

    companion object {
        private const val STATION_CHECK_RADIUS = 7
        private const val STATION_CHECK_HEIGHT = 5

        private var instance: LineTableExtractor? = null
        fun getInstance(): LineTableExtractor {
            if (instance == null) {
                instance = LineTableExtractor()
            }
            return instance!!
        }
    }
}