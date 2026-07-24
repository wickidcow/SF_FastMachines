package net.guizhanss.fastmachines.core.recipes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RecipeCraftingTest {

    @Test
    fun `mixed alternatives are allocated without duplication`() {
        val requirements = listOf(
            AllocationRequirement(setOf("oak", "birch"), 1),
            AllocationRequirement(setOf("oak"), 1),
        )
        val available = mapOf("oak" to 1, "birch" to 1)

        val plan = assertNotNull(createAllocationPlan(available, requirements, 1))

        assertEquals(2, plan.values.sum())
        assertEquals(1, plan["oak"])
        assertEquals(1, plan["birch"])
        assertEquals(1, maxAllocatableCrafts(available, requirements))
    }

    @Test
    fun `alternative stacks combine for one ingredient`() {
        val requirements = listOf(AllocationRequirement(setOf("oak", "birch"), 4))
        val available = mapOf("oak" to 2, "birch" to 2)

        assertEquals(1, maxAllocatableCrafts(available, requirements))
        assertEquals(4, assertNotNull(createAllocationPlan(available, requirements, 1)).values.sum())
    }

    @Test
    fun `insufficient overlapping items are rejected`() {
        val requirements = listOf(
            AllocationRequirement(setOf("oak", "birch"), 2),
            AllocationRequirement(setOf("oak"), 1),
        )
        val available = mapOf("oak" to 1, "birch" to 1)

        assertNull(createAllocationPlan(available, requirements, 1))
        assertEquals(0, maxAllocatableCrafts(available, requirements))
    }

    @Test
    fun `multiple crafts produce an exact plan`() {
        val requirements = listOf(
            AllocationRequirement(setOf("iron"), 2),
            AllocationRequirement(setOf("coal", "charcoal"), 1),
        )
        val available = mapOf("iron" to 6, "coal" to 1, "charcoal" to 2)

        val plan = assertNotNull(createAllocationPlan(available, requirements, 3))

        assertEquals(mapOf("iron" to 6, "coal" to 1, "charcoal" to 2), plan)
        assertEquals(3, maxAllocatableCrafts(available, requirements))
    }
}
