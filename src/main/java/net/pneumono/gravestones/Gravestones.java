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
import net.minecraft.util.math.BlockPos;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.function.Function;

public class Gravestones implements ModInitializer {
	public static final String MOD_ID = "gravestones";
	public static final Function<MinecraftServer, File> GRAVESTONES_ROOT = GravestoneDataSaving::getOrCreateGravestonesFolder;
	public static final Logger LOGGER = LoggerFactory.getLogger("Gravestones");

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
	
	private static void giveGravestoneKeyOnRespawn(ServerPlayerEntity player) {
		try {
			// Check if player has any gravestones
			// For now, just give a key unconditionally as a basic implementation
			ItemStack keyStack = GravestoneKeyItem.createKeyForGravestone(BlockPos.ORIGIN, "minecraft:overworld");
			
			// Try to add to player inventory, drop if full
			if (!player.getInventory().insertStack(keyStack)) {
				player.dropItem(keyStack, false);
			}
			
			// Send message to player
			player.sendMessage(Text.translatable("item.gravestones.gravestone_key.received"), false);
			
			LOGGER.info("Gave gravestone key to player {} on respawn", player.getName().getString());
		} catch (Exception e) {
			LOGGER.error("Failed to give gravestone key to player {}: {}", player.getName().getString(), e.getMessage());
		}
	}
}