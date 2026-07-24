package net.guizhanss.fastmachines.core.recipes

/**
 * One ingredient requirement for the allocation planner.
 *
 * [choices] contains every inventory key that may satisfy this ingredient, while [amount]
 * is the number of items needed for one craft.
 */
internal data class AllocationRequirement<K>(
    val choices: Set<K>,
    val amount: Int,
) {
    init {
        require(amount > 0) { "Allocation amount must be positive" }
    }
}

/**
 * Calculates an exact consumption plan as a small max-flow problem.
 *
 * This prevents overlapping alternatives (for example, "any plank" plus "oak plank")
 * from counting the same inventory items twice.
 */
internal fun <K> createAllocationPlan(
    availableItems: Map<K, Int>,
    requirements: List<AllocationRequirement<K>>,
    crafts: Int,
): Map<K, Int>? {
    if (crafts <= 0 || requirements.isEmpty()) return emptyMap()

    val items = availableItems.filterValues { it > 0 }.entries.toList()
    val requiredAmounts = requirements.map { it.amount * crafts }
    val totalRequired = requiredAmounts.sum()
    if (totalRequired <= 0) return emptyMap()

    val source = 0
    val itemStart = 1
    val requirementStart = itemStart + items.size
    val sink = requirementStart + requirements.size
    val graph = FlowGraph(sink + 1)

    items.forEachIndexed { index, entry ->
        graph.addEdge(source, itemStart + index, entry.value)
    }

    requirements.forEachIndexed { requirementIndex, requirement ->
        items.forEachIndexed { itemIndex, entry ->
            if (entry.key in requirement.choices) {
                graph.addEdge(itemStart + itemIndex, requirementStart + requirementIndex, entry.value)
            }
        }
        graph.addEdge(requirementStart + requirementIndex, sink, requiredAmounts[requirementIndex])
    }

    if (graph.maxFlow(source, sink) != totalRequired) return null

    return buildMap {
        items.forEachIndexed { index, entry ->
            val consumed = graph.flowFrom(source, itemStart + index)
            if (consumed > 0) put(entry.key, consumed)
        }
    }
}

/** Returns the maximum number of complete crafts that can be allocated. */
internal fun <K> maxAllocatableCrafts(
    availableItems: Map<K, Int>,
    requirements: List<AllocationRequirement<K>>,
): Int {
    if (requirements.isEmpty()) return 0

    var low = 0
    var high = requirements.minOf { requirement ->
        requirement.choices.sumOf { availableItems[it] ?: 0 } / requirement.amount
    }

    while (low < high) {
        val middle = low + (high - low + 1) / 2
        if (createAllocationPlan(availableItems, requirements, middle) != null) {
            low = middle
        } else {
            high = middle - 1
        }
    }

    return low
}

private class FlowGraph(size: Int) {
    private data class Edge(
        val to: Int,
        val reverse: Int,
        var capacity: Int,
        val original: Int,
    )

    private val adjacency = Array(size) { mutableListOf<Edge>() }

    fun addEdge(from: Int, to: Int, capacity: Int) {
        val forward = Edge(to, adjacency[to].size, capacity, capacity)
        val reverse = Edge(from, adjacency[from].size, 0, 0)
        adjacency[from].add(forward)
        adjacency[to].add(reverse)
    }

    fun maxFlow(source: Int, sink: Int): Int {
        var total = 0

        while (true) {
            val parentNode = IntArray(adjacency.size) { -1 }
            val parentEdge = IntArray(adjacency.size) { -1 }
            val queue = ArrayDeque<Int>()
            queue.add(source)
            parentNode[source] = source

            while (queue.isNotEmpty() && parentNode[sink] == -1) {
                val node = queue.removeFirst()
                adjacency[node].forEachIndexed { edgeIndex, edge ->
                    if (edge.capacity > 0 && parentNode[edge.to] == -1) {
                        parentNode[edge.to] = node
                        parentEdge[edge.to] = edgeIndex
                        queue.add(edge.to)
                    }
                }
            }

            if (parentNode[sink] == -1) break

            var amount = Int.MAX_VALUE
            var node = sink
            while (node != source) {
                val edge = adjacency[parentNode[node]][parentEdge[node]]
                amount = minOf(amount, edge.capacity)
                node = parentNode[node]
            }

            node = sink
            while (node != source) {
                val previous = parentNode[node]
                val edge = adjacency[previous][parentEdge[node]]
                edge.capacity -= amount
                adjacency[node][edge.reverse].capacity += amount
                node = previous
            }

            total += amount
        }

        return total
    }

    fun flowFrom(from: Int, to: Int): Int {
        val edge = adjacency[from].firstOrNull { it.to == to && it.original > 0 } ?: return 0
        return edge.original - edge.capacity
    }
}
