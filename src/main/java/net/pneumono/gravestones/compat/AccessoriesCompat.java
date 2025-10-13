package net.pneumono.gravestones.compat;

import io.wispforest.accessories.api.events.OnDeathCallback;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.entity.player.PlayerEntity;
import net.pneumono.gravestones.Gravestones;
import net.pneumono.gravestones.api.GravestonesApi;

public class AccessoriesCompat {
    public static void register() {
        Gravestones.LOGGER.info("Registering AccessoriesDataType...");
        GravestonesApi.registerDataType(Gravestones.id("accessories"), new AccessoriesDataType());
        Gravestones.LOGGER.info("AccessoriesDataType registered successfully!");
        
        // Register callback to disable Accessories death drops for players
        // This ensures Gravestones handles all accessory items instead of Accessories dropping them
        Gravestones.LOGGER.info("Registering Accessories death callback to disable item drops...");
        OnDeathCallback.EVENT.register((currentState, entity, capability, damageSource, droppedStacks) -> {
            if (entity instanceof PlayerEntity) {
                Gravestones.LOGGER.debug("Preventing Accessories from dropping items for player: " + entity.getName().getString());
                return TriState.FALSE; // Prevent Accessories from dropping items
            }
            return currentState; // Allow normal behavior for non-players
        });
        Gravestones.LOGGER.info("Accessories death callback registered successfully!");
    }
}
