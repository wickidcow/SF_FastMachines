package net.guizhanss.fastmachines.implementation.listeners

import me.mrCookieSlime.Slimefun.api.BlockStorage
import net.guizhanss.fastmachines.FastMachines
import net.guizhanss.fastmachines.core.items.attributes.NotAnAnvil
import org.bukkit.Tag
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPhysicsEvent

class GravityListener(plugin: FastMachines) : Listener {

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler
    fun onAnvilFall(e: BlockPhysicsEvent) {
        val affectedBlock = e.block
        if (!Tag.ANVIL.isTagged(affectedBlock.type)) return

        val sfItem = BlockStorage.check(affectedBlock) ?: return
        if (sfItem is NotAnAnvil) {
            e.isCancelled = true
        }
    }
}
