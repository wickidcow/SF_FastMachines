package net.guizhanss.fastmachines.core.items

import io.github.thebusybiscuit.slimefun4.utils.itemstack.ItemStackWrapper
import net.guizhanss.guizhanlib.kt.slimefun.extensions.getSlimefunItem
import net.guizhanss.guizhanlib.kt.slimefun.extensions.isSlimefunItem
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * Immutable amount-one identity used as a map key.
 *
 * Equality intentionally uses Bukkit's strict [ItemStack.isSimilar] contract so it remains
 * consistent with [hashCode]. Configurable recipe matching is handled by RecipeChoice instead.
 */
@ConsistentCopyVisibility
data class ItemWrapper private constructor(
    val baseItem: ItemStack,
) {

    companion object {
        fun of(item: ItemStack): ItemWrapper {
            val baseItem = if (item is ItemStackWrapper) {
                ItemStack(item.type).apply {
                    if (item.hasItemMeta()) itemMeta = item.itemMeta
                    amount = 1
                }
            } else {
                item.clone().apply { amount = 1 }
            }
            return ItemWrapper(baseItem)
        }

        fun of(material: Material) = ItemWrapper(ItemStack(material))
    }

    val baseItemMeta = if (baseItem.hasItemMeta()) baseItem.itemMeta else null
    private val itemHash = baseItem.hashCode()

    override fun equals(other: Any?) = other is ItemWrapper && baseItem.isSimilar(other.baseItem)

    override fun hashCode() = itemHash

    override fun toString() = if (baseItem.isSlimefunItem()) {
        "ItemWrapper(slimefunId=${baseItem.getSlimefunItem().id})"
    } else {
        "ItemWrapper(type=${baseItem.type}${if (baseItemMeta != null) ", meta=$baseItemMeta" else ""})"
    }
}
