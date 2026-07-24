package net.guizhanss.fastmachines.core.recipes

import net.guizhanss.fastmachines.core.items.ItemWrapper
import net.guizhanss.fastmachines.core.recipes.choices.RecipeChoice

/**
 * Calculates exact recipe consumption without allowing overlapping alternatives to count the
 * same inventory items twice.
 */
fun Recipe.createConsumptionPlan(
    availableItems: Map<ItemWrapper, Int>,
    crafts: Int,
): Map<ItemWrapper, Int>? {
    val requirements = createAllocationRequirements(availableItems.keys)
    return createAllocationPlan(availableItems, requirements, crafts)
}

fun Recipe.maxCraftableAmount(availableItems: Map<ItemWrapper, Int>): Int {
    val requirements = createAllocationRequirements(availableItems.keys)
    return maxAllocatableCrafts(availableItems, requirements)
}

private fun Recipe.createAllocationRequirements(
    availableItems: Set<ItemWrapper>,
): List<AllocationRequirement<ItemWrapper>> = inputs.map { choice ->
    AllocationRequirement(
        choices = availableItems.filterTo(mutableSetOf()) { choice.isValidItem(it.baseItem) },
        amount = choice.requiredAmount(),
    )
}

private fun RecipeChoice.requiredAmount(): Int {
    val amounts = choices.values.distinct()
    require(amounts.size == 1 && amounts.first() > 0) {
        "Recipe alternatives must all have one positive required amount: $this"
    }
    return amounts.first()
}
