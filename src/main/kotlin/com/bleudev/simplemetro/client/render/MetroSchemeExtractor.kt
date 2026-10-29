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
import kotlin.math.abs
import kotlin.math.sqrt

@Environment(EnvType.CLIENT)
internal class MetroSchemeExtractor {
    private var renderState: HashMap<Identifier, SchemeRenderState> = hashMapOf()
    private var pathRenderState: HashMap<Identifier, SchemeRenderState> = hashMapOf()
    private var adjacencyMatrices: HashMap<Identifier, AdjacencyMatrix> = hashMapOf()

    private fun rebuild(dimMap: Map<Identifier, SMetroInfo>) {
        renderState.clear()
        adjacencyMatrices.clear()
        PathSearcher.getInstance().rebuild()
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

        // Build adjacency matrix
        val matrix = AdjacencyMatrix(stations.size)

        matrix.fill { i, j ->
            if (i == j) return@fill 0.0
            else {
                val si = stations[i]
                val sj = stations[j]
                if (si == null || sj == null) return@fill null

                val isTransfer = j in si.transfers || i in sj.transfers
                var bestSqr: Double? = if (isTransfer) si.pos.distSqr(sj.pos) else null
                // Check every line `i` has. `j` must be neighbor!
                for ((lineId) in si.lines) {
                    lmap[lineId]?.let { line ->
                        val ii = line.stations.indexOf(i)
                        val ji = line.stations.indexOf(j)
                        if (ii >= 0 && ji >= 0 && abs(ii-ji) == 1) {
                            val d = si.pos.distSqr(sj.pos)
                            if (bestSqr == null || d < bestSqr) {
                                bestSqr = d
                            }
                        }
                    }
                }
                return@fill bestSqr?.let(::sqrt)
            }
        }
        adjacencyMatrices[dim] = matrix
        PathSearcher.getInstance().rebuild(dim)
    }

    private fun rebuildPathSearch(dim: Identifier, from: Int?, to: Int?, path: List<Int>?) {
        val state = renderState[dim] ?: return

        val stations = hashMapOf<Int, StationRenderState>()
        val connections = arrayListOf<ConnectionRenderState>()

        fun selectedStation(id: Int) {
            val st = state.stations[id]
            if (st != null) {
                val col = if (from != null && to != null && path == null) SELECT_FAIL_COLOR else SELECT_COLOR
                stations[id] = StationRenderState(id, st.pos, "", listOf(StationLineRenderState(-1, "", col)), listOf(), MetroStation.Status.OPEN)
            }
        }

        if (from != null) selectedStation(from)
        if (to != null) selectedStation(to)
        if (path != null) {
            for ((i, j) in path.zipWithNext()) {
                val si = state.stations[i]
                val sj = state.stations[j]
                if (si != null && sj != null) {
                    connections.add(ConnectionRenderState(si.pos, sj.pos, SELECT_COLOR))
                }
                if (sj != null && j != from && j != to) selectedStation(j)
            }
        }

        pathRenderState[dim] = SchemeRenderState(stations, connections)
    }

    fun extractRenderState(dim: Identifier): SchemeRenderState {
        if (renderState[dim] == null) {
            rebuild(dim, SMetroInfo.getInstance(dim))
        }
        return renderState[dim]!!
    }
    fun extractPathRenderState(dim: Identifier): SchemeRenderState {
        return pathRenderState[dim] ?: SchemeRenderState(mapOf(), listOf())
    }
    fun extractAdjacencyMatrix(dim: Identifier): AdjacencyMatrix {
        if (adjacencyMatrices[dim] == null) {
            rebuild(dim, SMetroInfo.getInstance(dim))
        }
        return adjacencyMatrices[dim]!!
    }

    companion object {
        private const val SELECT_COLOR = 0xffffff00.toInt()
        private const val SELECT_FAIL_COLOR = 0xffff0000.toInt()

        private var instance: MetroSchemeExtractor? = MetroSchemeExtractor()
        fun getInstance(): MetroSchemeExtractor {
            if (instance == null) {
                instance = MetroSchemeExtractor()
            }
            return instance!!
        }
        fun rebuild(dimensionMap: Map<Identifier, SMetroInfo>) {
            getInstance().rebuild(dimensionMap)
            getInstance().pathRenderState.clear()
        }
        fun rebuild(dimension: Identifier, info: SMetroInfo) {
            getInstance().rebuild(dimension, info)
            getInstance().pathRenderState.remove(dimension)
        }
        fun rebuildPathSearch(dimension: Identifier, from: Int?, to: Int?, path: List<Int>?) {
            getInstance().rebuildPathSearch(dimension, from, to, path)
        }
    }


    data class StationLineRenderState(val id: Int, val name: String, val color: Int) {
        override fun equals(other: Any?): Boolean {
            return other is StationLineRenderState && other.id == id
        }
        override fun hashCode(): Int {
            var result = id
            result = 31 * result + color
            result = 31 * result + name.hashCode()
            return result
        }
    }

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
    
    class AdjacencyMatrix(initialSize: Int) {
        private var array = buildArray(initialSize)
        var size = initialSize
            private set
            
        private fun buildArray(size: Int): Array<Array<Double?>> {
            return Array(size) { Array(size) { null } }
        }

        operator fun get(i: Int, j: Int): Double? {
            return this.array[i-1][j-1]
        }
        operator fun set(i: Int, j: Int, value: Double?) {
            this.array[i-1][j-1] = value
            this.array[j-1][i-1] = value
        }

        fun fill(filler: (i: Int, j: Int) -> Double?) {
            for (i in 1..size) {
                for (j in i..size) {
                    this[i, j] = filler(i, j)
                }
            }
        }

        fun toList(): List<List<Double?>> {
            return this.array.toList().map { it.toList() }
        }

        override fun toString(): String = this.toList().toString()
    }
}