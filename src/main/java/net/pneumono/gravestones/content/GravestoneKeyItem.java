package net.pneumono.gravestones.content;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
import net.pneumono.gravestones.Gravestones;
import net.pneumono.gravestones.block.AbstractGravestoneBlock;
import net.pneumono.gravestones.gravestones.GravestoneDataSaving;
import net.pneumono.gravestones.gravestones.RecentGraveHistory;

import java.util.List;
import java.util.Optional;

public class GravestoneKeyItem extends Item {
    public static final String GRAVESTONE_POS_NBT = "GravestonePos";
    public static final String GRAVESTONE_DIM_NBT = "GravestoneDimension";
    
    public GravestoneKeyItem(Settings settings) {
        super(settings);
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        
        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            Gravestones.LOGGER.info("[DEBUG] Player {} is using gravestone key", user.getName().getString());
            
            // Try to find the player's most recent gravestone
            List<RecentGraveHistory> histories = GravestoneDataSaving.readData(serverPlayer.getServer());
            Gravestones.LOGGER.info("[DEBUG] Found {} grave histories total", histories.size());
            
            Optional<RecentGraveHistory> playerHistory = histories.stream()
                    .filter(history -> history.owner().equals(user.getUuid()))
                    .findFirst();
            
            if (playerHistory.isPresent()) {
                List<GlobalPos> graves = playerHistory.get().getList();
                Gravestones.LOGGER.info("[DEBUG] Player has {} graves in history", graves.size());
                
                if (!graves.isEmpty()) {
                    // Search for the most recent valid gravestone (from newest to oldest)
                    for (int i = graves.size() - 1; i >= 0; i--) {
                        GlobalPos gravePos = graves.get(i);
                        RegistryKey<World> dimension = gravePos.dimension();
                        BlockPos pos = gravePos.pos();
                        
                        Gravestones.LOGGER.info("[DEBUG] Checking grave {} at {} in dimension {}", 
                                i + 1, pos, dimension.getValue());
                        
                        // Try to get the world for this dimension
                        ServerWorld targetWorld = serverPlayer.getServer().getWorld(dimension);
                        
                        if (targetWorld != null) {
                            // Check if the gravestone block still exists at this position
                            if (targetWorld.getBlockState(pos).getBlock() instanceof AbstractGravestoneBlock) {
                                Gravestones.LOGGER.info("[DEBUG] Valid gravestone found! Teleporting...");
                                
                                // Teleport to the gravestone
                                double x = pos.getX() + 0.5;
                                double y = pos.getY() + 1.0;
                                double z = pos.getZ() + 0.5;
                                
                                serverPlayer.teleport(targetWorld, x, y, z, serverPlayer.getYaw(), serverPlayer.getPitch());
                                user.sendMessage(Text.translatable("item.gravestones.gravestone_key.teleported"), true);
                                
                                // Consume the key
                                stack.decrement(1);
                                
                                Gravestones.LOGGER.info("Player {} teleported to grave at {}", 
                                        user.getName().getString(), pos);
                                
                                return TypedActionResult.success(stack);
                            } else {
                                Gravestones.LOGGER.info("[DEBUG] Grave at {} was already collected, checking next...", pos);
                            }
                        } else {
                            Gravestones.LOGGER.warn("[DEBUG] Target world is null for dimension {}", dimension.getValue());
                        }
                    }
                    
                    // If we get here, all graves were already collected
                    Gravestones.LOGGER.info("[DEBUG] All graves were already collected");
                } else {
                    Gravestones.LOGGER.info("[DEBUG] Player history is empty");
                }
            } else {
                Gravestones.LOGGER.info("[DEBUG] No player history found for UUID {}", user.getUuid());
            }
            
            // No valid gravestone found
            user.sendMessage(Text.translatable("item.gravestones.gravestone_key.no_grave"), true);
            stack.decrement(1);
            return TypedActionResult.fail(stack);
        }
        
        return TypedActionResult.pass(stack);
    }
    
    /**
     * Makes the gravestone key have enchantment glint
     */
    @Override
    public boolean hasGlint(ItemStack stack) {
        // Add enchantment glint to make it look special/magical
        return true;
    }
    
    /**
     * Creates a gravestone key with the specified position and dimension
     */
    public static ItemStack createKeyForGravestone(BlockPos pos, String dimension) {
        ItemStack keyStack = new ItemStack(GravestonesRegistry.GRAVESTONE_KEY);
        
        // The key has the "skips_gravestones" tag to prevent it from going to gravestones
        // Players can now freely drop and manage the key like any other item
        
        return keyStack;
    }
}