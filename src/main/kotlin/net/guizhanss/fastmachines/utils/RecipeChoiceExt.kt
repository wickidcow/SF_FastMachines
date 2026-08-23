package net.guizhanss.fastmachines.utils

import net.guizhanss.fastmachines.core.recipes.choices.ExactChoice
import net.guizhanss.fastmachines.core.recipes.choices.MultipleChoice
import net.guizhanss.fastmachines.core.recipes.choices.RecipeChoice

/** Consolidates repeated exact ingredients and repeated alternative groups. */
fun List<RecipeChoice>.consolidate(): List<RecipeChoice> {
    val exactChoices = filterIsInstance<ExactChoice>()
    val multipleChoices = filterIsInstance<MultipleChoice>()
    val unknownChoices = filterNot { it is ExactChoice || it is MultipleChoice }

    // Keep consolidated requirements representable by real inventory stacks. Recipes such as
    // repeated low-stack-size ingredients can otherwise produce one impossible ExactChoice.
    val mergedExact = exactChoices.groupBy { it.item }.flatMap { (item, entries) ->
        val totalAmount = entries.sumOf { it.amount }
        val maxStackSize = item.baseItem.maxStackSize.coerceAtLeast(1)
        buildList {
            var remaining = totalAmount
            while (remaining > 0) {
                val amount = minOf(remaining, maxStackSize)
                add(ExactChoice(item, amount))
                remaining -= amount
            }
        }
    }

    val mergedMultiple = multipleChoices
        .groupBy { choice -> choice.choices.keys.toSet() }
        .map { (items, entries) ->
            val amount = entries.sumOf { it.amount }
            MultipleChoice(items.associateWith { amount })
        }

    return mergedExact + mergedMultiple + unknownChoices
}
