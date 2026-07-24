package net.guizhanss.fastmachines.implementation.items.machines.base

import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu
import net.guizhanss.fastmachines.FastMachines
import net.guizhanss.fastmachines.core.items.ItemWrapper
import net.guizhanss.fastmachines.core.recipes.Recipe
import net.guizhanss.fastmachines.core.recipes.createConsumptionPlan
import net.guizhanss.fastmachines.core.recipes.maxCraftableAmount
import net.guizhanss.fastmachines.utils.consumeItems
import net.guizhanss.fastmachines.utils.countItems
import net.guizhanss.fastmachines.utils.items.toDisplayItem
import net.guizhanss.guizhanlib.kt.slimefun.extensions.getSlimefunItem
import net.guizhanss.guizhanlib.kt.slimefun.extensions.isSlimefunItem
import net.guizhanss.guizhanlib.kt.slimefun.extensions.location
import net.guizhanss.guizhanlib.kt.slimefun.extensions.position
import net.guizhanss.guizhanlib.minecraft.utils.InventoryUtil
import org.bukkit.entity.Player

class FastMachineCache(
    private val machine: BaseFastMachine,
    private val menu: BlockMenu,
) {

    private val pos = menu.location.position
    private var page = -1
    private var lastInputs = emptyMap<ItemWrapper, Int>()
    private var needUpdateMenu = true
    private var availableRecipes: Map<Recipe, Int> = emptyMap()
    private var selectedRecipe: Recipe? = null

    init {
        menu.addMenuClickHandler(BaseFastMachine.SCROLL_UP_SLOT) { _, _, _, _ ->
            page--
            needUpdateMenu = true
            false
        }
        menu.addMenuClickHandler(BaseFastMachine.SCROLL_DOWN_SLOT) { _, _, _, _ ->
            page++
            needUpdateMenu = true
            false
        }
        menu.addMenuClickHandler(BaseFastMachine.CRAFT_SLOT) { p, _, _, action ->
            val amount = when {
                action.isShiftClicked && action.isRightClicked -> Int.MAX_VALUE
                action.isShiftClicked -> 64
                action.isRightClicked -> 16
                else -> 1
            }
            craft(p, amount)
            false
        }
    }

    fun tick() {
        if (!menu.hasViewer()) return

        generateOutputs()
        if (needUpdateMenu) updateMenu()
    }

    private fun generateOutputs() {
        val inputs = menu.countItems(*BaseFastMachine.INPUT_SLOTS)

        // Compare the complete item map instead of relying on a potentially colliding hash.
        if (inputs == lastInputs) return
        lastInputs = inputs

        FastMachines.debug("Checking outputs for ${machine.javaClass.simpleName} at $pos")
        FastMachines.debug("Inputs: $inputs")
        FastMachines.debug("Recipe count: ${machine.recipes.size}")

        val possibleRecipes = machine.recipes.filter { recipe ->
            !recipe.isDisabledIn(pos.world) && recipe.inputs.all { choice ->
                inputs.keys.any { inputItem -> choice.isValidItem(inputItem.baseItem) }
            }
        }

        FastMachines.debug("Possible recipes: $possibleRecipes")

        // check amount
        availableRecipes = possibleRecipes.associateWith { recipe ->
            recipe.maxCraftableAmount(inputs)
        }.filterValues { it > 0 }

        if (selectedRecipe !in availableRecipes) {
            selectedRecipe = null
        }

        FastMachines.debug("Available recipes: $availableRecipes")
        needUpdateMenu = true
    }

    private fun updateMenu() {
        needUpdateMenu = false

        // no available recipes, clear preview
        if (availableRecipes.isEmpty()) {
            BaseFastMachine.PREVIEW_SLOTS.forEach { slot ->
                menu.replaceExistingItem(slot, ChestMenuUtils.getBackground())
                menu.addMenuClickHandler(slot, ChestMenuUtils.getEmptyClickHandler())
            }
            updateSelectedRecipe()
            return
        }

        val recipesList = availableRecipes.entries.toList()
        val totalPages = (recipesList.size + BaseFastMachine.ITEMS_PER_PAGE - 1) / BaseFastMachine.ITEMS_PER_PAGE
        page = page.coerceIn(1, totalPages)
        val startIndex = (page - 1) * BaseFastMachine.ITEMS_PER_PAGE

        BaseFastMachine.PREVIEW_SLOTS.forEachIndexed { index, slot ->
            val recipeIndex = startIndex + index
            if (recipeIndex >= recipesList.size) {
                menu.replaceExistingItem(slot, ChestMenuUtils.getBackground())
                menu.addMenuClickHandler(slot, ChestMenuUtils.getEmptyClickHandler())
                return@forEachIndexed
            }

            val (recipe, _) = recipesList[recipeIndex]
            menu.replaceExistingItem(slot, recipe.getOutput(pos.world).toDisplayItem())
            menu.addMenuClickHandler(slot) { _, _, _, _ ->
                selectedRecipe = recipe
                updateSelectedRecipe()
                false
            }
        }

        updateSelectedRecipe()
    }

    private fun updateSelectedRecipe() {
        val displayItem = selectedRecipe?.getOutput(pos.world)?.toDisplayItem()
            ?: BaseFastMachine.NO_ITEM
        menu.replaceExistingItem(BaseFastMachine.CHOICE_SLOT, displayItem)
    }

    private fun craft(p: Player, expectCrafts: Int) {
        val inputs = menu.countItems(*BaseFastMachine.INPUT_SLOTS)

        // check if a recipe is selected
        val recipe = selectedRecipe ?: return

        // calculate and check craftable times
        val maxCraftable = recipe.maxCraftableAmount(inputs)
        var actualCrafts = maxCraftable.coerceAtMost(expectCrafts)
        if (actualCrafts <= 0) {
            FastMachines.localization.sendMessage(p, "not-enough-materials")
            return
        }

        // check if recipe is available for the player
        if (FastMachines.configService.fmRequireSfResearch.value) {
            val researches = recipe.outputs
                .filter { it.isSlimefunItem() }
                .map { it.getSlimefunItem() }
                .mapNotNull { it.research }
                .toSet()

            if (researches.isNotEmpty()) {
                val pp = PlayerProfile.find(p)
                if (pp.isEmpty) {
                    FastMachines.localization.sendMessage(p, "profile-not-loaded")
                    PlayerProfile.request(p)
                    return
                }

                val profile = pp.get()
                if (researches.any { !profile.hasUnlocked(it) }) {
                    FastMachines.localization.sendMessage(p, "no-research")
                    return
                }
            }
        }

        val useEnergy = FastMachines.configService.fmUseEnergy.value
        val energyPerCraft = machine.energyPerUse
        val currentEnergy = if (useEnergy && energyPerCraft > 0) machine.getCharge(pos.location) else 0
        if (useEnergy && energyPerCraft > 0) {
            actualCrafts = actualCrafts.coerceAtMost(currentEnergy / energyPerCraft)
            if (actualCrafts <= 0) {
                FastMachines.localization.sendMessage(p, "not-enough-energy")
                return
            }
        }

        val consumptionPlan = recipe.createConsumptionPlan(inputs, actualCrafts)
        if (consumptionPlan == null) {
            FastMachines.localization.sendMessage(p, "not-enough-materials")
            needUpdateMenu = true
            return
        }

        // Inputs and energy are changed only after the final synchronous validation.
        menu.consumeItems(consumptionPlan, *BaseFastMachine.INPUT_SLOTS)
        if (useEnergy && energyPerCraft > 0) {
            machine.setCharge(pos.location, currentEnergy - actualCrafts * energyPerCraft)
        }

        // add outputs
        repeat(actualCrafts) {
            val outputItem = recipe.getOutput(pos.world).clone()

            if (menu.fits(outputItem, *BaseFastMachine.OUTPUT_SLOTS)) {
                menu.pushItem(outputItem, *BaseFastMachine.OUTPUT_SLOTS)
            } else {
                InventoryUtil.push(p, outputItem)
            }
        }

        needUpdateMenu = true
    }
}
