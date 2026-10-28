package com.bleudev.simplemetro.client.render

import com.bleudev.simplemetro.config.SMetroInfo
import com.bleudev.simplemetro.config.obj.MetroLine
import com.bleudev.simplemetro.config.obj.MetroStation
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.TextColor
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB

@Environment(EnvType.CLIENT)
internal class MetroSchemeExtractor {
    private var renderState: HashMap<Identifier, SchemeRenderState> = hashMapOf()

    private fun rebuild(dimMap: Map<Identifier, SMetroInfo>) {
        renderState.clear()
        for ((dim, info) in dimMap) rebuild(dim, info)
    }

    private fun rebuild(dim: Identifier, info: SMetroInfo) {
        val stations = hashMapOf<Int, StationRenderState>()
        val connections = arrayListOf<ConnectionRenderState>()

        // Extract stations
        for ((id, pos, name, transfers, status) in info.stations) {
            stations[id] = StationRenderState(id, pos, name, listOf(), transfers, status)
        }

        val lmap = info.lines.associateBy { it.id }
        for (line in lmap.values) {
            for ((cr, nx) in line.stations.zipWithNext()) {
                // Extract connections
                connections.add(ConnectionRenderState(stations[cr]!!.pos, stations[nx]!!.pos, line.color))
                // Extract station lines
                stations[cr] = stations[cr]!!.addLine(line)
                stations[nx] = stations[nx]!!.addLine(line)
            }
        }

        // Extract transfers
        val transferPairs = hashSetOf<Pair<Int, Int>>()
        for ((id, pos, _, _, transfers) in stations.values) {
            for (transferId in transfers) {
                val other = stations[transferId] ?: continue
                val pair = minOf(id, transferId) to maxOf(id, transferId)
                if (transferPairs.add(pair)) {
                    connections.add(
                        ConnectionRenderState(
                            pos,
                            other.pos,
                            0x555555,
                        )
                    )
                }
            }
        }

        renderState[dim] = SchemeRenderState(stations, connections)
    }

    fun extractRenderState(dim: Identifier): SchemeRenderState {
        if (renderState[dim] == null) {
            rebuild(dim, SMetroInfo.getInstance(dim))
        }
        return renderState[dim]!!
    }

    companion object {
        private var instance: MetroSchemeExtractor? = MetroSchemeExtractor()
        fun getInstance(): MetroSchemeExtractor {
            if (instance == null) {
                instance = MetroSchemeExtractor()
            }
            return instance!!
        }
        fun rebuild(dimensionMap: Map<Identifier, SMetroInfo>) {
            getInstance().rebuild(dimensionMap)
        }
        fun rebuild(dimension: Identifier, info: SMetroInfo) {
            getInstance().rebuild(dimension, info)
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