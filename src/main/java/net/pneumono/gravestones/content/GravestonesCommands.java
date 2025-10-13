package net.pneumono.gravestones.content;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.pneumono.gravestones.Gravestones;
import net.pneumono.gravestones.api.GravestonesApi;
import net.pneumono.gravestones.block.TechnicalGravestoneBlockEntity;
import net.pneumono.gravestones.gravestones.GravestoneDataSaving;
import net.pneumono.gravestones.gravestones.GravestoneManager;
import net.pneumono.gravestones.gravestones.RecentGraveHistory;

import java.util.List;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class GravestonesCommands {
    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(literal("gravestones")
                        .requires(source -> source.hasPermissionLevel(4))
                        .then(literal("getdata")
                                .then(literal("gravestone")
                                        .then(argument("position", BlockPosArgumentType.blockPos())
                                                .executes(context -> {
                                                    ServerWorld world = context.getSource().getWorld();
                                                    BlockPos pos = BlockPosArgumentType.getBlockPos(context, "position");

                                                    if (!(world.getBlockState(pos).isOf(GravestonesRegistry.GRAVESTONE_TECHNICAL))) {
                                                        context.getSource().sendFeedback(() -> Text.translatable("commands.gravestones.getdata.gravestone.no_gravestone").formatted(Formatting.RED), false);
                                                    } else if (world.getBlockEntity(pos) instanceof TechnicalGravestoneBlockEntity entity) {
                                                        ProfileComponent owner = entity.getGraveOwner();
                                                        if (owner != null) {
                                                            context.getSource().sendFeedback(() -> Text.stringifiedTranslatable("commands.gravestones.getdata.gravestone.all_data", entity.getSpawnDateTime(), owner.name().orElse("???"), owner.id().orElse(null)).formatted(Formatting.GREEN), false);
                                                        } else {
                                                            context.getSource().sendFeedback(() -> Text.translatable("commands.gravestones.getdata.gravestone.no_grave_owner", entity.getSpawnDateTime()).formatted(Formatting.RED), false);
                                                        }

                                                        context.getSource().sendFeedback(() -> Text.translatable("commands.gravestones.getdata.gravestone.contents_data", NbtHelper.toPrettyPrintedText(entity.getContents())), false);
                                                    }
                                                    return 1;
                                                })
                                        )
                                )
                                .then(literal("player")
                                        .then(argument("player", EntityArgumentType.player())
                                                .executes(context -> {
                                                    List<RecentGraveHistory> histories = GravestoneDataSaving.readData(context.getSource().getServer());

                                                    UUID uuid = EntityArgumentType.getPlayer(context, "player").getGameProfile().getId();
                                                    List<GlobalPos> positions = null;
                                                    for (RecentGraveHistory history : histories) {
                                                        if (history.owner().equals(uuid)) {
                                                            positions = history.getList();
                                                            break;
                                                        }
                                                    }

                                                    if (positions == null) {
                                                        Gravestones.LOGGER.error("Could not find gravestone data file!");
                                                        context.getSource().sendFeedback(() -> Text.translatable("commands.gravestones.getdata.player.cannot_find").formatted(Formatting.RED), false);
                                                        return 0;
                                                    }

                                                    ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                                    Text first = GravestoneManager.posToText(positions.getFirst());
                                                    Text second = GravestoneManager.posToText(positions.get(1));
                                                    Text third = GravestoneManager.posToText(positions.get(2));
                                                    context.getSource().sendFeedback(() -> Text.translatable("commands.gravestones.getdata.player.grave_data",
                                                            player.getDisplayName(),
                                                            first, second, third
                                                    ), false);

                                                    return 1;
                                                })
                                        )
                                )
                        )
                        .then(literal("deaths")
                                .then(literal("view")
                                        .then(argument("death", DeathArgumentType.death())
                                                .executes(context -> {
                                                    ServerCommandSource source = context.getSource();

                                                    NbtCompound nbt = DeathArgumentType.getDeath(context, "death");

                                                    source.sendFeedback(() -> Text.translatable("commands.gravestones.deaths.view", NbtHelper.toPrettyPrintedText(nbt.getCompound("contents"))), false);
                                                    return 1;
                                                })
                                        )
                                )
                                .then(literal("recover")
                                        .then(argument("death", DeathArgumentType.death())
                                                .executes(context -> recoverDeath(context, DeathArgumentType.getDeath(context, "death"), context.getSource().getPlayerOrThrow()))
                                                .then(argument("player", EntityArgumentType.player())
                                                        .executes(context -> recoverDeath(
                                                                context,
                                                                DeathArgumentType.getDeath(context, "death"),
                                                                EntityArgumentType.getPlayer(context, "player")
                                                        ))
                                                )
                                        )
                                )
                        )
                        .then(literal("getuuid")
                                .then(argument("player", EntityArgumentType.player())
                                        .executes(
                                                context -> getUuid(context, EntityArgumentType.getPlayer(context, "player"))
                                        )
                                )
                                .executes(
                                        context -> getUuid(context, context.getSource().getPlayerOrThrow())
                                )
                        )
                        .then(literal("list")
                                .then(argument("player", EntityArgumentType.player())
                                        .executes(context -> listHistory(context, EntityArgumentType.getPlayer(context, "player")))
                                )
                                .executes(context -> listHistory(context, context.getSource().getPlayerOrThrow()))
                        )
                        .then(literal("spawn")
                                .then(argument("player", EntityArgumentType.player())
                                        .then(argument("index", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0))
                                                .executes(context -> spawnGravestoneFromHistory(
                                                        context,
                                                        EntityArgumentType.getPlayer(context, "player"),
                                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "index")
                                                ))
                                        )
                                )
                                .then(argument("index", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0))
                                        .executes(context -> spawnGravestoneFromHistory(
                                                context,
                                                context.getSource().getPlayerOrThrow(),
                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "index")
                                        ))
                                )
                        )
                )
        );
        
        // Register internal command for clicking on history items (not visible in autocomplete)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(literal("gsinternalview")
                        .requires(source -> source.hasPermissionLevel(0)) // Any player can use it
                        .then(argument("uuid", com.mojang.brigadier.arguments.StringArgumentType.string())
                                .then(argument("index", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0))
                                        .executes(context -> viewHistoryInventory(
                                                context,
                                                com.mojang.brigadier.arguments.StringArgumentType.getString(context, "uuid"),
                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "index")
                                        ))
                                )
                        )
                )
        );
    }

    private static int recoverDeath(CommandContext<ServerCommandSource> context, NbtCompound nbt, ServerPlayerEntity player) {
        GravestonesApi.onCollect(context.getSource().getWorld(), player.getBlockPos(), player, 0, nbt.getCompound("contents"));
        context.getSource().sendFeedback(() -> Text.translatable("commands.gravestones.deaths.recover"), true);
        return 1;
    }

    private static int getUuid(CommandContext<ServerCommandSource> context, ServerPlayerEntity player) {
        context.getSource().sendFeedback(() -> Text.translatable("commands.gravestones.getuuid", player.getDisplayName(), player.getUuidAsString()), false);
        return 1;
    }
    
    private static int listHistory(CommandContext<ServerCommandSource> context, ServerPlayerEntity player) {
        List<net.pneumono.gravestones.GravestoneHistoryEntry> history = Gravestones.getHistory(player.getUuid());
        
        if (history.isEmpty()) {
            context.getSource().sendFeedback(() -> Text.literal("No gravestone history found for " + player.getDisplayName().getString()).formatted(Formatting.YELLOW), false);
            return 0;
        }
        
        context.getSource().sendFeedback(() -> Text.literal("=== Gravestone History for " + player.getDisplayName().getString() + " ===").formatted(Formatting.GOLD), false);
        
        for (int i = 0; i < history.size(); i++) {
            final int index = i;
            net.pneumono.gravestones.GravestoneHistoryEntry entry = history.get(i);
            BlockPos pos = entry.getPosition();
            String dimension = entry.getDimension().getValue().toString();
            long timeAgo = System.currentTimeMillis() - entry.getTimestamp();
            int minutesAgo = (int) (timeAgo / 60000);
            
            // Create clickable buttons for view and spawn (includes player name to avoid confusion)
            Text message = Text.literal(String.format("[%d] ", index))
                    .formatted(Formatting.GREEN)
                    .append(Text.literal("[View] ").formatted(Formatting.YELLOW)
                            .styled(style -> style
                                    .withClickEvent(new net.minecraft.text.ClickEvent(
                                            net.minecraft.text.ClickEvent.Action.RUN_COMMAND,
                                            "/gsinternalview " + player.getUuidAsString() + " " + index))
                                    .withHoverEvent(new net.minecraft.text.HoverEvent(
                                            net.minecraft.text.HoverEvent.Action.SHOW_TEXT,
                                            Text.literal("Click to view inventory").formatted(Formatting.AQUA)))))
                    .append(Text.literal("[Spawn] ").formatted(Formatting.LIGHT_PURPLE)
                            .styled(style -> style
                                    .withClickEvent(new net.minecraft.text.ClickEvent(
                                            net.minecraft.text.ClickEvent.Action.RUN_COMMAND,
                                            "/gravestones spawn " + player.getName().getString() + " " + index))
                                    .withHoverEvent(new net.minecraft.text.HoverEvent(
                                            net.minecraft.text.HoverEvent.Action.SHOW_TEXT,
                                            Text.literal("Click to spawn gravestone at your location").formatted(Formatting.LIGHT_PURPLE)))))
                    .append(Text.literal(String.format("%s at (%d, %d, %d) - %d minutes ago",
                            dimension, pos.getX(), pos.getY(), pos.getZ(), minutesAgo))
                            .formatted(Formatting.GREEN));
            
            context.getSource().sendFeedback(() -> message, false);
        }
        
        context.getSource().sendFeedback(() -> Text.literal("Click [View] to preview | Click [Spawn] to restore at your location").formatted(Formatting.AQUA), false);
        
        return history.size();
    }
    
    private static int spawnGravestoneFromHistory(CommandContext<ServerCommandSource> context, ServerPlayerEntity player, int index) {
        net.pneumono.gravestones.GravestoneHistoryEntry entry = Gravestones.getHistoryEntry(player.getUuid(), index);
        
        if (entry == null) {
            context.getSource().sendFeedback(() -> Text.literal("Invalid history index: " + index).formatted(Formatting.RED), false);
            return 0;
        }
        
        try {
            ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
            ServerWorld world = executor.getServerWorld();
            BlockPos spawnPos = executor.getBlockPos();
            
            // Place the gravestone block at the executor's position
            world.setBlockState(spawnPos, GravestonesRegistry.GRAVESTONE_TECHNICAL.getDefaultState());
            
            // Set the block entity data
            if (world.getBlockEntity(spawnPos) instanceof TechnicalGravestoneBlockEntity gravestone) {
                NbtCompound data = entry.getGravestoneData();
                gravestone.setContents(data);
                gravestone.setGraveOwner(new ProfileComponent(player.getGameProfile()));
                gravestone.setSpawnDate("Restored", world.getTime());
                
                world.updateListeners(spawnPos, gravestone.getCachedState(), gravestone.getCachedState(), net.minecraft.block.Block.NOTIFY_LISTENERS);
                
                BlockPos originalPos = entry.getPosition();
                String originalDim = entry.getDimension().getValue().toString();
                
                context.getSource().sendFeedback(() -> Text.literal(String.format("Spawned gravestone #%d for %s at your location. Original location: %s at (%d, %d, %d)",
                        index, player.getDisplayName().getString(), originalDim, originalPos.getX(), originalPos.getY(), originalPos.getZ()))
                        .formatted(Formatting.GREEN), true);
                
                Gravestones.LOGGER.info("Spawned gravestone #{} for player {} at executor's location {}", 
                        index, player.getDisplayName().getString(), spawnPos);
                
                return 1;
            }
            
            context.getSource().sendFeedback(() -> Text.literal("Failed to spawn gravestone - could not create block entity").formatted(Formatting.RED), false);
            return 0;
        } catch (Exception e) {
            context.getSource().sendFeedback(() -> Text.literal("Error spawning gravestone: " + e.getMessage()).formatted(Formatting.RED), false);
            Gravestones.LOGGER.error("Error spawning gravestone from history", e);
            return 0;
        }
    }
    
    private static int viewHistoryInventory(CommandContext<ServerCommandSource> context, String uuidString, int index) {
        try {
            UUID playerUuid = UUID.fromString(uuidString);
            net.pneumono.gravestones.GravestoneHistoryEntry entry = Gravestones.getHistoryEntry(playerUuid, index);
            
            if (entry == null) {
                context.getSource().sendFeedback(() -> Text.literal("Invalid history index: " + index).formatted(Formatting.RED), false);
                return 0;
            }
            
            ServerPlayerEntity viewer = context.getSource().getPlayerOrThrow();
            NbtCompound data = entry.getGravestoneData();
            
            // Create an inventory with 6 rows (54 slots) like a double chest
            SimpleInventory inventory = new SimpleInventory(54);
            
            int currentSlot = 0;
            
            // Add regular inventory items
            if (data.contains("gravestones:inventory")) {
                NbtCompound inventoryData = data.getCompound("gravestones:inventory");
                if (inventoryData.contains("inventory")) {
                    NbtList inventoryList = inventoryData.getList("inventory", 10); // 10 = compound type
                    
                    // Use the same decoding logic as PlayerInventoryDataType.onBreak
                    currentSlot = addItemsToInventory(inventory, inventoryList, viewer, currentSlot);
                }
            }
            
            // Add Accessories items if present
            if (data.contains("gravestones:accessories")) {
                NbtCompound accessoriesData = data.getCompound("gravestones:accessories");
                if (accessoriesData.contains("accessories")) {
                    NbtElement accessoriesList = accessoriesData.get("accessories");
                    currentSlot = addAccessoriesToInventory(inventory, accessoriesList, viewer, currentSlot);
                }
            }
            
            // Add Trinkets items if present
            if (data.contains("pneumonocore:trinkets")) {
                NbtCompound trinketsData = data.getCompound("pneumonocore:trinkets");
                if (trinketsData.contains("trinkets")) {
                    NbtElement trinketsList = trinketsData.get("trinkets");
                    currentSlot = addTrinketsToInventory(inventory, trinketsList, viewer, currentSlot);
                }
            }
            
            BlockPos pos = entry.getPosition();
            String dimension = entry.getDimension().getValue().toString();
            
            // Open the GUI
            viewer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, playerInventory, player) -> 
                        GenericContainerScreenHandler.createGeneric9x6(syncId, playerInventory, inventory),
                    Text.literal(String.format("Gravestone #%d - %s at (%d, %d, %d)", 
                            index, dimension, pos.getX(), pos.getY(), pos.getZ()))
                            .formatted(Formatting.GOLD)
            ));
            
            return 1;
        } catch (IllegalArgumentException e) {
            context.getSource().sendFeedback(() -> Text.literal("Invalid UUID: " + uuidString).formatted(Formatting.RED), false);
            return 0;
        } catch (Exception e) {
            context.getSource().sendFeedback(() -> Text.literal("Error opening inventory: " + e.getMessage()).formatted(Formatting.RED), false);
            Gravestones.LOGGER.error("Error opening gravestone history inventory", e);
            return 0;
        }
    }
    
    private static int addItemsToInventory(SimpleInventory inventory, NbtList itemList, ServerPlayerEntity viewer, int startSlot) {
        final int[] currentSlot = {startSlot}; // Use array to make it effectively final
        
        itemList.stream()
                .map(element -> PlayerInventoryDataType.StackWithSlot.CODEC.decode(
                        viewer.getRegistryManager().getOps(net.minecraft.nbt.NbtOps.INSTANCE), element))
                .filter(com.mojang.serialization.DataResult::isSuccess)
                .map(result -> result.getOrThrow().getFirst())
                .forEach(stackWithSlot -> {
                    ItemStack stack = stackWithSlot.stack();
                    if (!stack.isEmpty() && currentSlot[0] < 54) {
                        inventory.setStack(currentSlot[0], stack.copy());
                        currentSlot[0]++;
                    }
                });
        
        return currentSlot[0];
    }
    
    private static int addAccessoriesToInventory(SimpleInventory inventory, NbtElement accessoriesElement, ServerPlayerEntity viewer, int startSlot) {
        try {
            int currentSlot = startSlot;
            
            // Decode the accessories list using the same codec as AccessoriesDataType
            com.mojang.serialization.DataResult<com.mojang.datafixers.util.Pair<java.util.List<net.pneumono.gravestones.compat.AccessoriesDataType.SlotReferencePrimitive>, NbtElement>> result = 
                    net.pneumono.gravestones.compat.AccessoriesDataType.SlotReferencePrimitive.CODEC.listOf()
                            .decode(viewer.getRegistryManager().getOps(net.minecraft.nbt.NbtOps.INSTANCE), accessoriesElement);
            
            if (result.isSuccess()) {
                java.util.List<net.pneumono.gravestones.compat.AccessoriesDataType.SlotReferencePrimitive> accessories = result.getOrThrow().getFirst();
                for (net.pneumono.gravestones.compat.AccessoriesDataType.SlotReferencePrimitive primitive : accessories) {
                    ItemStack stack = primitive.stack();
                    if (!stack.isEmpty() && currentSlot < 54) {
                        inventory.setStack(currentSlot, stack.copy());
                        currentSlot++;
                    }
                }
            }
            
            return currentSlot;
        } catch (Exception e) {
            Gravestones.LOGGER.warn("Failed to load accessories for history view", e);
            return startSlot;
        }
    }
    
    private static int addTrinketsToInventory(SimpleInventory inventory, NbtElement trinketsElement, ServerPlayerEntity viewer, int startSlot) {
        try {
            int currentSlot = startSlot;
            
            // Decode the trinkets list using the same codec as TrinketsDataType - returns Pair<SlotReferencePrimitive, ItemStack>
            com.mojang.serialization.DataResult<com.mojang.datafixers.util.Pair<java.util.List<com.mojang.datafixers.util.Pair<net.pneumono.gravestones.compat.TrinketsDataType.SlotReferencePrimitive, ItemStack>>, NbtElement>> result = 
                    net.pneumono.gravestones.compat.TrinketsDataType.SLOT_CODEC.listOf()
                            .decode(viewer.getRegistryManager().getOps(net.minecraft.nbt.NbtOps.INSTANCE), trinketsElement);
            
            if (result.isSuccess()) {
                java.util.List<com.mojang.datafixers.util.Pair<net.pneumono.gravestones.compat.TrinketsDataType.SlotReferencePrimitive, ItemStack>> trinkets = result.getOrThrow().getFirst();
                for (com.mojang.datafixers.util.Pair<net.pneumono.gravestones.compat.TrinketsDataType.SlotReferencePrimitive, ItemStack> pair : trinkets) {
                    ItemStack stack = pair.getSecond(); // Get the ItemStack from the Pair
                    if (!stack.isEmpty() && currentSlot < 54) {
                        inventory.setStack(currentSlot, stack.copy());
                        currentSlot++;
                    }
                }
            }
            
            return currentSlot;
        } catch (Exception e) {
            Gravestones.LOGGER.warn("Failed to load trinkets for history view", e);
            return startSlot;
        }
    }
}
