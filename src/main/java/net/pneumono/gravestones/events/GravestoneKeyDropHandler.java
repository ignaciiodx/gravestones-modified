package net.pneumono.gravestones.events;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.server.world.ServerWorld;
import net.pneumono.gravestones.content.GravestonesRegistry;

/**
 * Handles prevention of gravestone key dropping using entity tracking
 */
public class GravestoneKeyDropHandler {
    
    public static void register() {
        // Track when entities are loaded and remove any gravestone key item entities
        ServerEntityEvents.ENTITY_LOAD.register(GravestoneKeyDropHandler::onEntityLoad);
    }
    
    private static void onEntityLoad(Entity entity, ServerWorld world) {
        // Check if it's an ItemEntity containing a gravestone key
        if (entity instanceof ItemEntity itemEntity) {
            if (itemEntity.getStack().isOf(GravestonesRegistry.GRAVESTONE_KEY)) {
                // Get the owner if available
                if (itemEntity.getOwner() instanceof net.minecraft.server.network.ServerPlayerEntity player) {
                    // Add the key back to the player's inventory
                    player.getInventory().insertStack(itemEntity.getStack().copy());
                }
                // Remove the dropped key entity from the world
                itemEntity.discard();
            }
        }
    }
}
