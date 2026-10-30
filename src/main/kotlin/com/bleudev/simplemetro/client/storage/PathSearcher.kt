package com.bleudev.simplemetro.client.storage

import com.bleudev.simplemetro.config.SMetroInfo
import net.minecraft.resources.Identifier

class PathSearcher {
    private var cached: HashMap<Identifier, HashMap<Pair<Int, Int>, List<Int>?>> = hashMapOf()
    private var selectedFrom: HashMap<Identifier, Int?> = hashMapOf()
    private var selectedTo: HashMap<Identifier, Int?> = hashMapOf()

    fun rebuild(dim: Identifier) {
        cached[dim] = hashMapOf()
        selectedFrom.remove(dim)
        selectedTo.remove(dim)
    }

    fun rebuild() {
        cached.clear()
        selectedFrom.clear()
        selectedTo.clear()
    }

    fun search(dim: Identifier, from: Int, to: Int): List<Int>? {
        if (dim !in cached) {
            cached[dim] = hashMapOf()
        }
        if (from to to in cached[dim]!!) {
            return cached[dim]!![from to to]
        }

        val ans = computeSearch(dim, from, to)
        cached[dim]!![from to to] = ans
        return ans
    }

    private fun computeSearch(dim: Identifier, from: Int, to: Int): List<Int>? {
        val matrix = MetroSchemeExtractor.getInstance().extractAdjacencyMatrix(dim)

        require(from in 1..matrix.size) { "Invalid start station: $from" }
        require(to in 1..matrix.size) { "Invalid destination station: $to" }

        val stations = SMetroInfo.getInstance(dim).stations.associateBy { it.id }
        if (stations[from]?.status?.invisible != false || stations[to]?.status?.invisible != false) {
            return null
        }

        val distances = DoubleArray(matrix.size) { Double.POSITIVE_INFINITY }
        val previous = IntArray(matrix.size) { -1 }
        val visited = BooleanArray(matrix.size)

        distances[from - 1] = 0.0

        @Suppress("unused")
        for (ignored in 0..<matrix.size) {
            var current = -1
            var minDistance = Double.POSITIVE_INFINITY

            for (station in 1..matrix.size) {
                val index = station - 1

                if (!visited[index] && distances[index] < minDistance) {
                    minDistance = distances[index]
                    current = station
                }
            }

            if (current == -1) return null

            visited[current - 1] = true
            if (current == to) break

            for (next in 1..matrix.size) {
                val nextIndex = next - 1

                if (visited[nextIndex]) continue

                val weight = matrix[current, next] ?: continue
                require(weight >= 0.0) { "Edge weights must be non-negative" }

                val newDistance = distances[current - 1] + weight

                if (newDistance < distances[nextIndex]) {
                    distances[nextIndex] = newDistance
                    previous[nextIndex] = current
                }
            }
        }

        if (distances[to - 1] == Double.POSITIVE_INFINITY) {
            return null
        }

        val path = mutableListOf<Int>()
        var current = to

        while (current != -1) {
            path.add(current)

            if (current == from) break

            current = previous[current - 1]
        }

        if (path.last() != from) return null

        return path.asReversed()
    }

    fun select(dim: Identifier, id: Int) {
        if (dim !in this.selectedFrom) {
            this.selectedFrom[dim] = null
        }
        if (dim !in this.selectedTo) {
            this.selectedTo[dim] = null
        }

        var from = this.selectedFrom[dim]
        var to = this.selectedTo[dim]
        if (from != null && to != null) {
            from = id
            to = null
        } else if (from != null) {
            if (from == id) from = null
            else to = id
        } else {
            from = id
        }

        this.selectedFrom[dim] = from
        this.selectedTo[dim] = to


        MetroSchemeExtractor.rebuildPathSearch(dim, from, to, if (from != null && to != null) search(dim, from, to) else null)
    }

    companion object {
        private var instance: PathSearcher? = null

        fun getInstance(): PathSearcher {
            if (instance == null) {
                instance = PathSearcher()
            }
            return instance!!
        }
    }
}