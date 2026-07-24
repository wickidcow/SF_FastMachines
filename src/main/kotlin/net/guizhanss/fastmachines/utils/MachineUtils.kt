package net.guizhanss.fastmachines.utils

import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu
import net.guizhanss.fastmachines.FastMachines
import net.guizhanss.fastmachines.core.items.ItemWrapper
import net.guizhanss.fastmachines.core.recipes.choices.RecipeChoice
import net.guizhanss.fastmachines.utils.items.countItems
import org.bukkit.inventory.ItemStack

fun BlockMenu.getItems(vararg slots: Int): List<ItemStack> = getItems(slots.toList())

fun BlockMenu.getItems(slots: List<Int>): List<ItemStack> = slots.mapNotNull { getItemInSlot(it) }

fun BlockMenu.countItems(vararg slots: Int): Map<ItemWrapper, Int> = countItems(slots.toList())

fun BlockMenu.countItems(slots: List<Int>): Map<ItemWrapper, Int> = getItems(slots).countItems()

/** Consumes an exact precomputed plan. Must be called synchronously. */
fun BlockMenu.consumeItems(plan: Map<ItemWrapper, Int>, slots: List<Int>) {
    plan.forEach { (wrapper, required) ->
        var remaining = required
        for (slot in slots) {
            if (remaining <= 0) break
            val stack = getItemInSlot(slot) ?: continue
            if (!wrapper.baseItem.isSimilar(stack)) continue
            val consumeNow = minOf(stack.amount, remaining)
            consumeItem(slot, consumeNow)
            remaining -= consumeNow
        }
        check(remaining == 0) { "Inventory changed while consuming $wrapper; $remaining item(s) missing" }
    }
}

fun BlockMenu.consumeItems(plan: Map<ItemWrapper, Int>, vararg slots: Int) = consumeItems(plan, slots.toList())

/** Compatibility helper for callers that consume one recipe choice. */
fun BlockMenu.consumeChoice(choice: RecipeChoice, amount: Int, slots: List<Int>) {
    if (amount <= 0) return
    val requiredPerCraft = choice.choices.values.distinct().single()
    var remaining = requiredPerCraft * amount
    FastMachines.debug("Consuming $remaining item(s) for choice $choice")

    for (slot in slots) {
        if (remaining <= 0) break
        val stack = getItemInSlot(slot) ?: continue
        if (!choice.isValidItem(stack)) continue
        val consumeNow = minOf(stack.amount, remaining)
        consumeItem(slot, consumeNow)
        remaining -= consumeNow
    }
    check(remaining == 0) { "Inventory changed while consuming choice $choice" }
}

fun BlockMenu.consumeChoice(choice: RecipeChoice, amount: Int, vararg slots: Int) =
    consumeChoice(choice, amount, slots.toList())
