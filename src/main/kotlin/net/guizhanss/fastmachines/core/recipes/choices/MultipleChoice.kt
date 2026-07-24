package net.guizhanss.fastmachines.core.recipes.choices

import net.guizhanss.fastmachines.core.items.ItemWrapper
import net.guizhanss.fastmachines.utils.items.isSimilarTo
import org.bukkit.inventory.ItemStack

/** Alternatives for one ingredient requirement. Every alternative must use the same amount. */
data class MultipleChoice(
    override val choices: Map<ItemWrapper, Int>,
) : RecipeChoice {

    init {
        require(choices.isNotEmpty()) { "MultipleChoice requires at least one alternative" }
        require(choices.values.all { it > 0 }) { "Recipe ingredient amount must be positive" }
        require(choices.values.distinct().size == 1) { "All alternatives must require the same amount" }
    }

    val amount: Int = choices.values.first()

    override fun isValidItem(item: ItemStack) = choices.keys.any { it.isSimilarTo(item) }

    override fun maxCraftableAmount(availableItems: Map<ItemWrapper, Int>): Int {
        val available = availableItems.entries
            .filter { (wrapper, _) -> isValidItem(wrapper.baseItem) }
            .sumOf { it.value }
        return available / amount
    }
}
