package net.guizhanss.fastmachines.core.recipes.choices

import net.guizhanss.fastmachines.core.items.ItemWrapper
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice as BukkitRecipeChoice

/**
 * Keeps the actual Bukkit/Paper ingredient predicate for addon-defined recipes.
 *
 * An example stack is only suitable for displaying the ingredient and carrying its required
 * amount. Matching by that stack would incorrectly reject other valid alternatives or
 * accept a generic vanilla item in place of a custom ItemsAdder ingredient.
 */
data class PredicateChoice(
    val predicate: BukkitRecipeChoice,
    val amount: Int = 1,
) : RecipeChoice {

    init {
        require(amount > 0) { "Recipe ingredient amount must be positive" }
    }

    // RecipeCrafting uses choices.values to read the required amount, but uses isValidItem
    // for actual inventory allocation. Do not use this representative item for matching.
    override val choices = mapOf(ItemWrapper.of(predicate.itemStack) to amount)

    override fun isValidItem(item: ItemStack) = !item.type.isAir && predicate.test(item)

    override fun maxCraftableAmount(availableItems: Map<ItemWrapper, Int>): Int {
        val available = availableItems.entries
            .filter { (item, _) -> isValidItem(item.baseItem) }
            .sumOf { it.value }
        return available / amount
    }
}
