package com.bleudev.simplemetro.client.render

import com.bleudev.simplemetro.config.SMetroInfo
import com.bleudev.simplemetro.obj.MetroLine
import com.bleudev.simplemetro.obj.MetroStation
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.TextColor
import net.minecraft.util.ARGB

@Environment(EnvType.CLIENT)
internal class MetroSchemeExtractor {
    private var renderState: SchemeRenderState? = null

    private fun rebuild(info: SMetroInfo) {
        val stations = hashMapOf<Int, StationRenderState>()
        val connections = arrayListOf<ConnectionRenderState>()

        val lmap = info.lines.associateBy { it.id }
        for ((id, pos, name, transfers, status) in info.stations) {
            stations[id] = StationRenderState(id, pos, name, listOf(), transfers, status)
        }
        for (line in lmap.values) {
            for ((cr, nx) in line.stations.zipWithNext()) {
                connections.add(ConnectionRenderState(stations[cr]!!.pos, stations[nx]!!.pos, line.color))

                for (tr in stations[cr]!!.transfers) {
                    connections.add(ConnectionRenderState(stations[cr]!!.pos, stations[tr]!!.pos, 0x555555))
                }

                stations[cr] = stations[cr]!!.addLine(line)
                stations[nx] = stations[nx]!!.addLine(line)
            }
        }

        renderState = SchemeRenderState(stations, connections)
    }

    fun extractRenderState(): SchemeRenderState {
        if (renderState == null) {
            rebuild(SMetroInfo.getInstance())
        }
        return renderState!!
    }

    companion object {
        private var instance: MetroSchemeExtractor? = MetroSchemeExtractor()
        fun getInstance(): MetroSchemeExtractor {
            if (instance == null) {
                instance = MetroSchemeExtractor()
            }
            return instance!!
        }
        fun rebuild(info: SMetroInfo) {
            getInstance().rebuild(info)
        }
    }


    data class StationLineRenderState(val id: Int, val name: String, val color: Int)

    data class StationRenderState(
        val id: Int,
        val pos: BlockPos,
        val name: String,
        val lines: List<StationLineRenderState>,
        val transfers: List<Int>,
        val status: MetroStation.Status
    ) {
        val schemeColors: List<Int> get() = if (lines.isEmpty()) listOf(ARGB.white(.9f)) else lines.map { ARGB.color(.9f, it.color) }
        val whiteColor: Int get() = ARGB.color(.9f, 0xffffff)
        val grayColor: Int get() = ARGB.color(.9f, TextColor.GRAY.value)

        fun addLine(line: MetroLine): StationRenderState = StationRenderState(
            id, pos, name,
            HashSet(lines).apply { add(StationLineRenderState(line.id, line.name, line.color)) }.toList(),
            transfers, status
        )
    }
    data class ConnectionRenderState(val pos1: BlockPos, val pos2: BlockPos, val color: Int) {
        val argbColor: Int get() = ARGB.color(.8f, color)
    }

    data class SchemeRenderState(val stations: Map<Int, StationRenderState>, val connections: List<ConnectionRenderState>)
}