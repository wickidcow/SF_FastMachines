package net.guizhanss.fastmachines.core.recipes

import net.guizhanss.fastmachines.core.items.ItemWrapper
import net.guizhanss.fastmachines.core.recipes.choices.ExactChoice
import net.guizhanss.fastmachines.core.recipes.choices.PredicateChoice
import net.guizhanss.fastmachines.utils.items.asFMRecipeChoice
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.bukkit.inventory.RecipeChoice as BukkitRecipeChoice

class PredicateRecipeChoiceTest {

    // Models an addon-defined predicate (the API is intentionally non-extendable for
    // production plugins, but this fake exercises compatibility on both Paper baselines).
    private class CustomIngredientChoice : BukkitRecipeChoice {
        override fun getItemStack(): ItemStack = ItemStack(Material.CHICKEN)

        override fun test(itemStack: ItemStack): Boolean =
            itemStack.type == Material.CHICKEN || itemStack.type == Material.BEEF

        public override fun clone(): BukkitRecipeChoice = CustomIngredientChoice()
    }

    @Test
    fun `unknown Bukkit choices preserve the original test predicate`() {
        val choice = assertIs<PredicateChoice>(CustomIngredientChoice().asFMRecipeChoice(2))

        assertEquals(2, choice.choices.values.single())
        assertTrue(choice.isValidItem(ItemStack(Material.CHICKEN)))
        // An example-stack-only conversion would reject this second valid alternative.
        assertTrue(choice.isValidItem(ItemStack(Material.BEEF)))
        assertFalse(choice.isValidItem(ItemStack(Material.COD)))
    }

    @Test
    fun `predicate requirements conserve inventory and do not double allocate`() {
        val recipe = StandardRecipe(
            listOf(
                CustomIngredientChoice().asFMRecipeChoice(),
                ExactChoice(ItemWrapper.of(Material.CHICKEN)),
            ),
            ItemStack(Material.FURNACE),
        )
        val available = mapOf(
            ItemWrapper.of(Material.CHICKEN) to 1,
            ItemWrapper.of(Material.BEEF) to 1,
        )

        assertEquals(1, recipe.maxCraftableAmount(available))
        val plan = assertNotNull(recipe.createConsumptionPlan(available, 1))
        assertEquals(2, plan.values.sum())
        assertEquals(1, plan[ItemWrapper.of(Material.CHICKEN)])
        assertEquals(1, plan[ItemWrapper.of(Material.BEEF)])
    }
}
