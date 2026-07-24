package net.guizhanss.fastmachines.core.recipes

import net.guizhanss.fastmachines.core.items.ItemWrapper
import net.guizhanss.fastmachines.core.recipes.choices.ExactChoice
import net.guizhanss.fastmachines.core.recipes.choices.MultipleChoice
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.inventory.ItemStack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RecipeCraftingTest {

    @Test
    fun `mixed alternatives are allocated without duplication`() {
        val oak = ItemWrapper.of(Material.OAK_PLANKS)
        val birch = ItemWrapper.of(Material.BIRCH_PLANKS)
        val recipe = TestRecipe(
            listOf(
                MultipleChoice(mapOf(oak to 1, birch to 1)),
                ExactChoice(oak, 1),
            )
        )
        val available = mapOf(oak to 1, birch to 1)
        val plan = assertNotNull(recipe.createConsumptionPlan(available, 1))
        assertEquals(2, plan.values.sum())
        assertEquals(1, recipe.maxCraftableAmount(available))
    }

    @Test
    fun `alternative stacks combine for one shaped ingredient`() {
        val oak = ItemWrapper.of(Material.OAK_PLANKS)
        val birch = ItemWrapper.of(Material.BIRCH_PLANKS)
        val recipe = TestRecipe(listOf(MultipleChoice(mapOf(oak to 4, birch to 4))))
        assertEquals(1, recipe.maxCraftableAmount(mapOf(oak to 2, birch to 2)))
    }

    private data class TestRecipe(override val inputs: List<net.guizhanss.fastmachines.core.recipes.choices.RecipeChoice>) : Recipe {
        override val outputs = listOf(ItemStack(Material.STICK))
        override fun getOutput(world: World) = outputs.first()
        override fun isDisabledIn(world: World) = false
    }
}
