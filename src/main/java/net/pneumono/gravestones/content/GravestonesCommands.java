package net.pneumono.gravestones.content;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
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
                        .then(literal("history")
                                .then(literal("list")
                                        .then(argument("player", EntityArgumentType.player())
                                                .executes(context -> listHistory(context, EntityArgumentType.getPlayer(context, "player")))
                                        )
                                        .executes(context -> listHistory(context, context.getSource().getPlayerOrThrow()))
                                )
                                .then(literal("view")
                                        .then(argument("uuid", com.mojang.brigadier.arguments.StringArgumentType.string())
                                                .then(argument("index", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 19))
                                                        .executes(context -> viewHistoryInventory(
                                                                context,
                                                                com.mojang.brigadier.arguments.StringArgumentType.getString(context, "uuid"),
                                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "index")
                                                        ))
                                                )
                                        )
                                )
                                .then(literal("restore")
                                        .then(argument("player", EntityArgumentType.player())
                                                .then(argument("index", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 19))
                                                        .executes(context -> restoreFromHistory(
                                                                context,
                                                                EntityArgumentType.getPlayer(context, "player"),
                                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "index")
                                                        ))
                                                )
                                        )
                                        .then(argument("index", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 19))
                                                .executes(context -> restoreFromHistory(
                                                        context,
                                                        context.getSource().getPlayerOrThrow(),
                                                        com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "index")
                                                ))
                                        )
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
            
            // Create clickable text that opens the inventory GUI
            Text message = Text.literal(String.format("[%d] ", index))
                    .formatted(Formatting.GREEN)
                    .append(Text.literal("📦 ").formatted(Formatting.YELLOW)
                            .styled(style -> style
                                    .withClickEvent(new net.minecraft.text.ClickEvent(
                                            net.minecraft.text.ClickEvent.Action.RUN_COMMAND,
                                            "/gravestones history view " + player.getUuidAsString() + " " + index))
                                    .withHoverEvent(new net.minecraft.text.HoverEvent(
                                            net.minecraft.text.HoverEvent.Action.SHOW_TEXT,
                                            Text.literal("Click to view inventory").formatted(Formatting.AQUA)))))
                    .append(Text.literal(String.format("%s at (%d, %d, %d) - %d minutes ago",
                            dimension, pos.getX(), pos.getY(), pos.getZ(), minutesAgo))
                            .formatted(Formatting.GREEN));
            
            context.getSource().sendFeedback(() -> message, false);
        }
        
        context.getSource().sendFeedback(() -> Text.literal("Click 📦 to view inventory | Use /gravestones history restore <player> <index> to restore").formatted(Formatting.AQUA), false);
        
        return history.size();
    }
    
    private static int restoreFromHistory(CommandContext<ServerCommandSource> context, ServerPlayerEntity player, int index) {
        net.pneumono.gravestones.GravestoneHistoryEntry entry = Gravestones.getHistoryEntry(player.getUuid(), index);
        
        if (entry == null) {
            context.getSource().sendFeedback(() -> Text.literal("Invalid history index: " + index).formatted(Formatting.RED), false);
            return 0;
        }
        
        ServerWorld world = context.getSource().getServer().getWorld(entry.getDimension());
        if (world == null) {
            context.getSource().sendFeedback(() -> Text.literal("Dimension not found: " + entry.getDimension().getValue()).formatted(Formatting.RED), false);
            return 0;
        }
        
        BlockPos pos = entry.getPosition();
        
        // Place the gravestone block
        world.setBlockState(pos, GravestonesRegistry.GRAVESTONE_TECHNICAL.getDefaultState());
        
        // Set the block entity data
        if (world.getBlockEntity(pos) instanceof TechnicalGravestoneBlockEntity gravestone) {
            NbtCompound data = entry.getGravestoneData();
            gravestone.setContents(data);
            gravestone.setGraveOwner(new ProfileComponent(player.getGameProfile()));
            gravestone.setSpawnDate("Restored", world.getTime());
            
            world.updateListeners(pos, gravestone.getCachedState(), gravestone.getCachedState(), net.minecraft.block.Block.NOTIFY_LISTENERS);
            
            context.getSource().sendFeedback(() -> Text.literal(String.format("Restored gravestone #%d for %s at (%d, %d, %d) in %s",
                    index, player.getDisplayName().getString(), pos.getX(), pos.getY(), pos.getZ(), entry.getDimension().getValue()))
                    .formatted(Formatting.GREEN), true);
            
            Gravestones.LOGGER.info("Restored gravestone #{} for player {} at {} in {}", 
                    index, player.getDisplayName().getString(), pos, entry.getDimension().getValue());
            
            return 1;
        }
        
        context.getSource().sendFeedback(() -> Text.literal("Failed to restore gravestone - could not create block entity").formatted(Formatting.RED), false);
        return 0;
    }
}
