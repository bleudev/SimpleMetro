package com.bleudev.simplemetro.client.storage

import com.bleudev.simplemetro.config.SMetroInfo
import com.bleudev.simplemetro.config.obj.MetroStation
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

@Environment(EnvType.CLIENT)
class LineTableExtractor private constructor() {
    private var prevPos: BlockPos? = null
    private var nextPos: BlockPos? = null

    fun playerTick(storage: MetroClientStorage, player: LocalPlayer, level: ClientLevel) {
        val playerPos = player.position()
        val info = SMetroInfo.getInstance(level.dimension().identifier())
        var current: Int? = null
        var currentName: String? = null

        for ((id, pos1, name) in info.stations) {
            val pos = Vec3.atCenterOf(pos1)
            val d = pos.horizontal().distanceTo(playerPos.horizontal())
            val dy = abs(pos.y-playerPos.y)
            if (d <= STATION_CHECK_RADIUS && dy <= STATION_CHECK_HEIGHT) {
                current = id
                currentName = name
                break
            }
        }
        if (storage.currentStation != current) {
            storage.prevStation = storage.currentStation ?: storage.prevStation
            storage.currentStation = current
            if (current != null && storage.prevStation == null) storage.prevStation = current
            computeNextStation(storage.prevStation, info, playerPos).let { (l, r) ->
                storage.currentLine = l
                storage.nextStation = r
            }
            if (storage.currentStation == null && storage.currentLine != null && storage.prevStation != null && storage.nextStation != null) {
                for ((id, pos) in info.stations) {
                    if (id == storage.prevStation) {
                        this.prevPos = pos
                    }
                    if (id == storage.nextStation) {
                        this.nextPos = pos
                    }
                }
            }
            storage.stationUpdated = true
        }

        if (this.prevPos != null && this.nextPos != null && !isOnTrack(this.prevPos!!, this.nextPos!!, playerPos)) {
            storage.currentLine = null
            storage.prevStation = null
            storage.nextStation = null
        }

        if (storage.stationUpdated) {
            if (storage.currentLine != null) {
                if (storage.currentStation != null && currentName != null) {
                    player.sendOverlayMessage(Component.literal("Station: $currentName"))
                } else if (storage.nextStation != null) {
                    val st = info.stations.first { it.id == storage.nextStation }
                    player.sendOverlayMessage(Component.literal("Next station: ${st.name}"))
                }
            }
            storage.stationUpdated = false
        }
    }

    private fun computeNextStation(prev: Int?, info: SMetroInfo, playerPos: Vec3): Pair<Int?, Int?> {
        if (prev == null) return null to null
        val lines = info.lines.filter { prev in it.stations }
        val station = info.stations.first { it.id == prev }

        var best: Pair<Int, Int>? = null
        var bestd: Double? = null
        var bests: MetroStation? = null
        for ((id, _, _, stations) in lines) {
            val i = stations.indexOf(prev)

            // Check first
            if (i-1 >= 0) {
                val f = stations[i-1]
                val fs = info.stations.first { it.id == f }
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
                val ss = info.stations.first { it.id == s }
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