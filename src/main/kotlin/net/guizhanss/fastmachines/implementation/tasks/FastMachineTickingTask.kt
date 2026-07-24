package net.guizhanss.fastmachines.implementation.tasks

import net.guizhanss.fastmachines.FastMachines
import net.guizhanss.fastmachines.core.FMRegistry
import org.bukkit.Bukkit
import java.util.logging.Level

/** Inventory and BlockMenu APIs must only be accessed from the primary server thread. */
class FastMachineTickingTask {

    init {
        val period = FastMachines.configService.fmTickRate.value.toLong()
        Bukkit.getScheduler().runTaskTimer(FastMachines.instance, Runnable {
            FMRegistry.enabledFastMachines.toList().forEach { machine ->
                machine.caches.entries.toList().forEach { (pos, cache) ->
                    try {
                        cache.tick()
                    } catch (ex: Exception) {
                        FastMachines.log(
                            Level.SEVERE,
                            ex,
                            "An error occurred while ticking ${machine.javaClass.simpleName} " +
                                "at ${pos.world.name} ${pos.x} ${pos.y} ${pos.z}",
                        )
                    }
                }
            }
        }, period, period)
    }
}
