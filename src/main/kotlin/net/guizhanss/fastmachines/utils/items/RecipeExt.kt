package net.guizhanss.fastmachines.utils.items

import net.guizhanss.fastmachines.core.items.ItemWrapper
import net.guizhanss.fastmachines.core.recipes.choices.ExactChoice
import net.guizhanss.fastmachines.core.recipes.choices.MultipleChoice
import net.guizhanss.fastmachines.core.recipes.choices.PredicateChoice
import org.bukkit.inventory.RecipeChoice as BukkitRecipeChoice

fun BukkitRecipeChoice.asFMRecipeChoice(amount: Int = 1) =
    when (this) {
        is BukkitRecipeChoice.MaterialChoice -> {
            if (choices.size == 1) {
                ExactChoice(ItemWrapper.of(choices.first()), amount)
            } else {
                MultipleChoice(choices.associate { ItemWrapper.of(it) to amount })
            }
        }

        is BukkitRecipeChoice.ExactChoice -> {
            if (choices.size == 1) {
                ExactChoice(ItemWrapper.of(choices.first()), amount)
            } else {
                MultipleChoice(choices.associate { ItemWrapper.of(it) to amount })
            }
        }

        else -> {
            // Modern Paper and addon registries may supply PredicateChoice or ItemTypeChoice.
            // Preserve their test(ItemStack) semantics; the example stack is not a matcher.
            PredicateChoice(this.clone(), amount)
        }
    }
