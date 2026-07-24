package net.guizhanss.fastmachines.core.recipes.choices

import net.guizhanss.fastmachines.core.items.ItemWrapper
import net.guizhanss.fastmachines.utils.items.isSimilarTo
import org.bukkit.inventory.ItemStack

data class ExactChoice(
    val item: ItemWrapper,
    val amount: Int = 1,
) : RecipeChoice {

    init {
        require(amount > 0) { "Recipe ingredient amount must be positive" }
    }

    override val choices = mapOf(item to amount)

    override fun isValidItem(item: ItemStack) = this.item.isSimilarTo(item)

    override fun maxCraftableAmount(availableItems: Map<ItemWrapper, Int>): Int {
        val available = availableItems.entries
            .filter { this.item.isSimilarTo(it.key.baseItem) }
            .sumOf { it.value }
        return available / amount
    }
}
