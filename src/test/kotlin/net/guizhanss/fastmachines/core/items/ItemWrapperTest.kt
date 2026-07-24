package net.guizhanss.fastmachines.core.items

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import kotlin.test.Test
import kotlin.test.assertEquals

class ItemWrapperTest {

    @Test
    fun testConstructor() {
        val wrapper1 = ItemWrapper.of(ItemStack(Material.DIAMOND, 32))

        assertEquals(1, wrapper1.baseItem.amount)
    }

    @Test
    fun testEquals() {
        val wrapper1 = ItemWrapper.of(ItemStack(Material.DIAMOND, 32))
        val wrapper2 = ItemWrapper.of(ItemStack(Material.DIAMOND, 32))

        assertEquals(wrapper1, wrapper2)

        val wrapper3 = ItemWrapper.of(ItemStack(Material.GOLD_INGOT, 32))
        val wrapper4 = ItemWrapper.of(ItemStack(Material.GOLD_INGOT, 16))

        assertEquals(wrapper3, wrapper4)
    }
}
