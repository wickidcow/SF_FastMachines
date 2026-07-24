package net.guizhanss.fastmachines.core.recipes

import net.guizhanss.fastmachines.core.items.ItemWrapper
import net.guizhanss.fastmachines.core.recipes.choices.RecipeChoice

/**
 * Calculates recipe consumption as a small max-flow problem. This prevents overlapping choices
 * (for example "any plank" plus "oak plank") from overestimating crafts or consuming too little.
 */
fun Recipe.createConsumptionPlan(
    availableItems: Map<ItemWrapper, Int>,
    crafts: Int,
): Map<ItemWrapper, Int>? {
    if (crafts <= 0) return emptyMap()
    if (inputs.isEmpty()) return emptyMap()

    val items = availableItems.filterValues { it > 0 }.entries.toList()
    val requirements = inputs.map { it.requiredAmount() * crafts }
    val totalRequired = requirements.sum()
    if (totalRequired <= 0) return emptyMap()

    val source = 0
    val itemStart = 1
    val choiceStart = itemStart + items.size
    val sink = choiceStart + inputs.size
    val graph = FlowGraph(sink + 1)

    items.forEachIndexed { index, entry ->
        graph.addEdge(source, itemStart + index, entry.value)
    }

    inputs.forEachIndexed { choiceIndex, choice ->
        items.forEachIndexed { itemIndex, entry ->
            if (choice.isValidItem(entry.key.baseItem)) {
                graph.addEdge(itemStart + itemIndex, choiceStart + choiceIndex, entry.value)
            }
        }
        graph.addEdge(choiceStart + choiceIndex, sink, requirements[choiceIndex])
    }

    if (graph.maxFlow(source, sink) != totalRequired) return null

    return buildMap {
        items.forEachIndexed { index, entry ->
            val consumed = graph.flowFrom(source, itemStart + index)
            if (consumed > 0) put(entry.key, consumed)
        }
    }
}

fun Recipe.maxCraftableAmount(availableItems: Map<ItemWrapper, Int>): Int {
    if (inputs.isEmpty()) return 0
    var low = 0
    var high = inputs.minOf { it.maxCraftableAmount(availableItems) }
    while (low < high) {
        val middle = low + (high - low + 1) / 2
        if (createConsumptionPlan(availableItems, middle) != null) low = middle else high = middle - 1
    }
    return low
}

private fun RecipeChoice.requiredAmount(): Int {
    val amounts = choices.values.distinct()
    require(amounts.size == 1 && amounts.first() > 0) {
        "Recipe alternatives must all have one positive required amount: $this"
    }
    return amounts.first()
}

private class FlowGraph(size: Int) {
    private data class Edge(val to: Int, val reverse: Int, var capacity: Int, val original: Int)
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
