package net.guizhanss.fastmachines.utils

import net.guizhanss.fastmachines.core.recipes.choices.ExactChoice
import net.guizhanss.fastmachines.core.recipes.choices.MultipleChoice
import net.guizhanss.fastmachines.core.recipes.choices.RecipeChoice

/** Consolidates repeated exact ingredients and repeated alternative groups. */
fun List<RecipeChoice>.consolidate(): List<RecipeChoice> {
    val exactChoices = filterIsInstance<ExactChoice>()
    val multipleChoices = filterIsInstance<MultipleChoice>()
    val unknownChoices = filterNot { it is ExactChoice || it is MultipleChoice }

    val mergedExact = exactChoices.groupBy { it.item }.map { (item, entries) ->
        ExactChoice(item, entries.sumOf { it.amount })
    }

    val mergedMultiple = multipleChoices
        .groupBy { choice -> choice.choices.keys.toSet() }
        .map { (items, entries) ->
            val amount = entries.sumOf { it.amount }
            MultipleChoice(items.associateWith { amount })
        }

    return mergedExact + mergedMultiple + unknownChoices
}
