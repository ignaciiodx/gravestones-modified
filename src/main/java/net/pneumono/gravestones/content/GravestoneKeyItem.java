package net.pneumono.gravestones.content;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
import net.pneumono.gravestones.Gravestones;
import net.pneumono.gravestones.GravestonesConfig;
import net.pneumono.gravestones.block.AbstractGravestoneBlock;

public class GravestoneKeyItem extends Item {
    
    public GravestoneKeyItem(Settings settings) {
        super(settings);
    }
    
    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        // Get delay from config (in seconds) and convert to ticks (20 ticks = 1 second)
        return (int) (GravestonesConfig.KEY_TELEPORT_DELAY.getValue() * 20);
    }
    
    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW; // Uses bow animation (pulling back)
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        
        if (!world.isClient) {
            // Play initial sound
            world.playSound(null, user.getX(), user.getY(), user.getZ(), 
                SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.PLAYERS, 0.1f, 2f);
        }
        
        // Start using the item (begins the delay)
        return ItemUsage.consumeHeldItem(world, user, hand);
    }
    
    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (world.isClient && user instanceof PlayerEntity) {
            int maxUseTime = getMaxUseTime(stack, user);
            // Calculate progress (0.0 to 1.0)
            float progress = (maxUseTime - remainingUseTicks) / (float) maxUseTime;
            
            // Spawn particles around the player based on progress
            if (remainingUseTicks % 2 == 0) {
                int particleCount = (int) (progress * 8);
                for (int i = 0; i < Math.max(2, particleCount); i++) {
                    double offsetX = (world.random.nextDouble() - 0.5) * 1.5;
                    double offsetY = world.random.nextDouble() * 1.5;
                    double offsetZ = (world.random.nextDouble() - 0.5) * 1.5;
                    
                    world.addParticle(ParticleTypes.SOUL,
                        user.getX() + offsetX,
                        user.getY() + offsetY,
                        user.getZ() + offsetZ,
                        0, 0.05, 0);
                }
                
                // Add more particles as we get closer to teleporting
                if (progress >= 0.5f) {
                    for (int i = 0; i < particleCount / 2; i++) {
                        double offsetX = (world.random.nextDouble() - 0.5) * 1.5;
                        double offsetY = world.random.nextDouble() * 1.5;
                        double offsetZ = (world.random.nextDouble() - 0.5) * 1.5;
                        
                        world.addParticle(ParticleTypes.REVERSE_PORTAL,
                            user.getX() + offsetX,
                            user.getY() + offsetY,
                            user.getZ() + offsetZ,
                            0, world.random.nextDouble() * 0.5, 0);
                    }
                }
            }
        }
    }
    
    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            Gravestones.LOGGER.info("[DEBUG] Player {} finished using gravestone key", user.getName().getString());
            
            // Get the most recent gravestone position for this player
            GlobalPos recentGravestone = Gravestones.getRecentGravestone(user.getUuid());
            
            if (recentGravestone != null) {
                RegistryKey<World> dimension = recentGravestone.dimension();
                BlockPos pos = recentGravestone.pos();
                
                Gravestones.LOGGER.info("[DEBUG] Found recent gravestone at {} in dimension {}", pos, dimension.getValue());
                
                // Try to get the world for this dimension
                ServerWorld targetWorld = serverPlayer.getServer().getWorld(dimension);
                
                if (targetWorld != null) {
                    // Check if the gravestone block still exists at this position
                    if (targetWorld.getBlockState(pos).getBlock() instanceof AbstractGravestoneBlock) {
                        Gravestones.LOGGER.info("[DEBUG] Valid gravestone found! Teleporting...");
                        
                        // Play teleport sound
                        world.playSound(null, user.getX(), user.getY(), user.getZ(), 
                            SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
                        
                        // Spawn particles at departure
                        if (world instanceof ServerWorld serverWorld) {
                            for (int i = 0; i < 32; i++) {
                                double offsetX = (world.random.nextDouble() - 0.5) * 2.0;
                                double offsetY = world.random.nextDouble() * 2.0;
                                double offsetZ = (world.random.nextDouble() - 0.5) * 2.0;
                                
                                serverWorld.spawnParticles(ParticleTypes.PORTAL,
                                    user.getX() + offsetX,
                                    user.getY() + offsetY,
                                    user.getZ() + offsetZ,
                                    1, 0, 0, 0, 0);
                            }
                        }
                        
                        // Teleport to the gravestone
                        double x = pos.getX() + 0.5;
                        double y = pos.getY() + 1.0;
                        double z = pos.getZ() + 0.5;
                        
                        serverPlayer.teleport(targetWorld, x, y, z, serverPlayer.getYaw(), serverPlayer.getPitch());
                        
                        // Apply positive effects after teleportation
                        // Absorption X (40 HP / 20 hearts) - amplifier 9 = level 10
                        serverPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 500, 9)); // 25 seconds, level 10
                        // Regeneration V
                        serverPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 500, 7)); // 25 seconds, level 8
                        // Resistance III - reduces damage by 60%
                        serverPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 500, 4)); // 25 seconds, level 5
                        // Speed I - 20% movement speed boost
                        serverPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 500, 1)); // 25 seconds, level 1
                        // Jump Boost I - jump 0.5 blocks higher
                        serverPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 500, 0)); // 25 seconds, level 1
                        
                        // Spawn particles at arrival
                        for (int i = 0; i < 32; i++) {
                            double offsetX = (world.random.nextDouble() - 0.5) * 2.0;
                            double offsetY = world.random.nextDouble() * 2.0;
                            double offsetZ = (world.random.nextDouble() - 0.5) * 2.0;
                            
                            targetWorld.spawnParticles(ParticleTypes.PORTAL,
                                x + offsetX,
                                y + offsetY,
                                z + offsetZ,
                                1, 0, 0, 0, 0);
                        }
                        
                        serverPlayer.sendMessage(Text.translatable("item.gravestones.gravestone_key.teleported"), true);
                        
                        // Consume the key
                        stack.decrement(1);
                        
                        Gravestones.LOGGER.info("Player {} teleported to grave at {}", 
                                user.getName().getString(), pos);
                        
                        return stack;
                    } else {
                        Gravestones.LOGGER.info("[DEBUG] Grave at {} was already collected or doesn't exist", pos);
                    }
                } else {
                    Gravestones.LOGGER.warn("[DEBUG] Target world is null for dimension {}", dimension.getValue());
                }
            } else {
                Gravestones.LOGGER.info("[DEBUG] No recent gravestone found for player {}", user.getUuid());
            }
            
            // No valid gravestone found
            serverPlayer.sendMessage(Text.translatable("item.gravestones.gravestone_key.no_grave"), true);
            stack.decrement(1);
        }
        
        return stack;
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
        
        Gravestones.LOGGER.info("[DEBUG] Created gravestone key for position {} in dimension {}", pos, dimension);
        
        return keyStack;
    }
}