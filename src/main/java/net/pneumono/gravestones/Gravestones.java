package net.pneumono.gravestones;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.GlobalPos;
import net.pneumono.gravestones.api.InsertGravestoneItemCallback;
import net.pneumono.gravestones.compat.AccessoriesCompat;
import net.pneumono.gravestones.compat.BackwardsCompat;
import net.pneumono.gravestones.compat.TrinketsCompat;
import net.pneumono.gravestones.content.GravestoneKeyItem;
import net.pneumono.gravestones.content.GravestonesCommands;
import net.pneumono.gravestones.content.GravestonesRegistry;
import net.pneumono.gravestones.events.GravestoneKeyDropHandler;
import net.pneumono.gravestones.gravestones.GravestoneDataSaving;
import net.pneumono.gravestones.gravestones.GravestoneManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.function.Function;

public class Gravestones implements ModInitializer {
	public static final String MOD_ID = "gravestones";
	public static final Function<MinecraftServer, File> GRAVESTONES_ROOT = GravestoneDataSaving::getOrCreateGravestonesFolder;
	public static final Logger LOGGER = LoggerFactory.getLogger("Gravestones");
	
	// Store the most recent gravestone position for each player
	private static final Map<UUID, GlobalPos> RECENT_GRAVESTONES = new HashMap<>();
	
	// Store the last 20 gravestones for each player (for recovery purposes)
	private static final Map<UUID, LinkedList<GravestoneHistoryEntry>> GRAVESTONE_HISTORY = new HashMap<>();
	private static final int MAX_HISTORY_SIZE = 20;

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Gravestones");
		GravestonesConfig.registerGravestonesConfigs();

		ServerLifecycleEvents.SERVER_STARTED.register(BackwardsCompat::convertOldFiles);

		GravestonesRegistry.registerModContent();
		GravestonesCommands.registerCommands();

		if (isModLoaded("spelunkery")) {
			InsertGravestoneItemCallback.EVENT.register((player, itemStack) -> itemStack.isOf(Items.RECOVERY_COMPASS));
		}
		
		// Register gravestone key to be skipped when creating graves to prevent duplicates
		InsertGravestoneItemCallback.EVENT.register((player, itemStack) -> itemStack.isOf(GravestonesRegistry.GRAVESTONE_KEY));

		// Register compatibility for available equipment mods
		LOGGER.info("Checking mod compatibility...");
		LOGGER.info("Trinkets loaded: " + isModLoaded("trinkets"));
		LOGGER.info("Accessories loaded: " + isModLoaded("accessories"));

		if (isModLoaded("trinkets")) {
			LOGGER.info("Registering Trinkets compatibility");
			TrinketsCompat.register();
		}

		if (isModLoaded("accessories")) {
			LOGGER.info("Registering Accessories compatibility");
			AccessoriesCompat.register();
		}
		
		// Register respawn event to give gravestone key
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			giveGravestoneKeyOnRespawn(newPlayer);
		});
		
		// Register entity tracker to prevent dropping gravestone keys
		GravestoneKeyDropHandler.register();
		LOGGER.info("Gravestone key drop prevention enabled via entity tracking");
	}

	private static boolean isModLoaded(String id) {
		return FabricLoader.getInstance().isModLoaded(id);
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
	
	/**
	 * Register the most recent gravestone position for a player
	 */
	public static void setRecentGravestone(UUID playerUuid, GlobalPos gravestonePos) {
		RECENT_GRAVESTONES.put(playerUuid, gravestonePos);
		LOGGER.info("[DEBUG] Registered recent gravestone for player {} at {}", playerUuid, gravestonePos);
	}
	
	/**
	 * Get the most recent gravestone position for a player
	 */
	public static GlobalPos getRecentGravestone(UUID playerUuid) {
		return RECENT_GRAVESTONES.get(playerUuid);
	}
	
	/**
	 * Add a gravestone to the player's history
	 * Stores the last 20 gravestones for recovery purposes
	 */
	public static void addToHistory(UUID playerUuid, GravestoneHistoryEntry entry) {
		LinkedList<GravestoneHistoryEntry> history = GRAVESTONE_HISTORY.computeIfAbsent(playerUuid, k -> new LinkedList<>());
		
		// Add to the front of the list (most recent first)
		history.addFirst(entry);
		
		// Remove oldest entries if we exceed the limit
		while (history.size() > MAX_HISTORY_SIZE) {
			history.removeLast();
		}
		
		LOGGER.info("[HISTORY] Added gravestone to history for player {}. Total entries: {}", playerUuid, history.size());
	}
	
	/**
	 * Get the gravestone history for a player
	 */
	public static List<GravestoneHistoryEntry> getHistory(UUID playerUuid) {
		return GRAVESTONE_HISTORY.getOrDefault(playerUuid, new LinkedList<>());
	}
	
	/**
	 * Get a specific gravestone from history by index (0 = most recent)
	 */
	public static GravestoneHistoryEntry getHistoryEntry(UUID playerUuid, int index) {
		List<GravestoneHistoryEntry> history = getHistory(playerUuid);
		if (index >= 0 && index < history.size()) {
			return history.get(index);
		}
		return null;
	}
	
	private static void giveGravestoneKeyOnRespawn(ServerPlayerEntity player) {
		// Check if gravestone key should be given
		if (!GravestonesConfig.GIVE_GRAVESTONE_KEY.getValue()) {
			return;
		}
		
		try {
			// Get the most recent gravestone position for this player
			GlobalPos recentGravestone = getRecentGravestone(player.getUuid());
			
			if (recentGravestone != null) {
				// Create key with the actual gravestone position
				ItemStack keyStack = GravestoneKeyItem.createKeyForGravestone(
					recentGravestone.pos(), 
					recentGravestone.dimension().getValue().toString()
				);
				
				// Try to add to player inventory, drop if full
				if (!player.getInventory().insertStack(keyStack)) {
					player.dropItem(keyStack, false);
				}
				
				// Send messages to player with colored coordinates (cyan like death message)
				player.sendMessage(Text.translatable("item.gravestones.gravestone_key.received"), false);
				
				// Build message: "PlayerName's grave is at (x, y, z)" with cyan coordinates
				Text message = Text.literal(player.getGameProfile().getName() + "'s grave is at ")
					.append(Text.literal(GravestoneManager.posToString(recentGravestone.pos()))
						.styled(style -> style.withColor(0x00FFFF))); // Cyan color
				
				player.sendMessage(message, false);
				
				LOGGER.info("Gave gravestone key to player {} for gravestone at {}", player.getName().getString(), recentGravestone);
			} else {
				LOGGER.info("No recent gravestone found for player {}, not giving key", player.getName().getString());
			}
		} catch (Exception e) {
			LOGGER.error("Failed to give gravestone key to player {}: {}", player.getName().getString(), e.getMessage());
		}
	}
}