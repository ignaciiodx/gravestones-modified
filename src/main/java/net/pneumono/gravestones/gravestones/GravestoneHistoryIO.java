package net.pneumono.gravestones.gravestones;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.MinecraftServer;
import net.pneumono.gravestones.Gravestones;
import net.pneumono.gravestones.GravestoneHistoryEntry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * Handles saving and loading gravestone history to/from disk
 * Stores one file per world in: world/data/gravestones/history/
 */
public class GravestoneHistoryIO {
    private static final String HISTORY_FOLDER = "history";
    
    /**
     * Get or create the history folder for the given server
     */
    private static File getHistoryFolder(MinecraftServer server) {
        File gravestonesRoot = Gravestones.GRAVESTONES_ROOT.apply(server);
        File historyFolder = new File(gravestonesRoot, HISTORY_FOLDER);
        
        if (!historyFolder.exists()) {
            historyFolder.mkdirs();
            Gravestones.LOGGER.info("Created gravestone history folder at: {}", historyFolder.getAbsolutePath());
        }
        
        return historyFolder;
    }
    
    /**
     * Get the history file for a specific player
     */
    private static File getHistoryFile(MinecraftServer server, UUID playerUuid) {
        File historyFolder = getHistoryFolder(server);
        return new File(historyFolder, playerUuid.toString() + ".dat");
    }
    
    /**
     * Save the history for a specific player
     */
    public static void saveHistory(MinecraftServer server, UUID playerUuid, LinkedList<GravestoneHistoryEntry> history) {
        if (history == null || history.isEmpty()) {
            // Delete file if history is empty
            File file = getHistoryFile(server, playerUuid);
            if (file.exists()) {
                file.delete();
                Gravestones.LOGGER.debug("Deleted empty history file for player: {}", playerUuid);
            }
            return;
        }
        
        try {
            File file = getHistoryFile(server, playerUuid);
            
            NbtCompound root = new NbtCompound();
            root.putString("playerUuid", playerUuid.toString());
            root.putInt("version", 1); // For future compatibility
            
            NbtList entriesList = GravestoneHistoryEntry.toNbtList(history);
            root.put("entries", entriesList);
            
            // Write to temp file first, then rename (atomic operation)
            File tempFile = new File(file.getAbsolutePath() + ".tmp");
            NbtIo.writeCompressed(root, tempFile.toPath());
            
            // Atomic rename
            Files.move(tempFile.toPath(), file.toPath(), 
                      java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                      java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            
            Gravestones.LOGGER.debug("Saved {} history entries for player: {}", history.size(), playerUuid);
            
        } catch (IOException e) {
            Gravestones.LOGGER.error("Failed to save gravestone history for player {}: {}", playerUuid, e.getMessage());
        }
    }
    
    /**
     * Load the history for a specific player
     */
    public static LinkedList<GravestoneHistoryEntry> loadHistory(MinecraftServer server, UUID playerUuid) {
        File file = getHistoryFile(server, playerUuid);
        
        if (!file.exists()) {
            return new LinkedList<>();
        }
        
        try {
            NbtCompound root = NbtIo.readCompressed(file.toPath(), NbtSizeTracker.ofUnlimitedBytes());
            
            // Version check for future compatibility
            int version = root.getInt("version");
            if (version > 1) {
                Gravestones.LOGGER.warn("Unknown history file version {} for player {}, attempting to load anyway", version, playerUuid);
            }
            
            NbtList entriesList = root.getList("entries", 10); // 10 = NbtCompound type
            List<GravestoneHistoryEntry> entries = GravestoneHistoryEntry.fromNbtList(entriesList);
            
            Gravestones.LOGGER.debug("Loaded {} history entries for player: {}", entries.size(), playerUuid);
            
            return new LinkedList<>(entries);
            
        } catch (IOException e) {
            Gravestones.LOGGER.error("Failed to load gravestone history for player {}: {}", playerUuid, e.getMessage());
            return new LinkedList<>();
        }
    }
    
    /**
     * Load all histories from disk into the provided map
     */
    public static void loadAllHistories(MinecraftServer server, Map<UUID, LinkedList<GravestoneHistoryEntry>> historyMap) {
        File historyFolder = getHistoryFolder(server);
        File[] files = historyFolder.listFiles((dir, name) -> name.endsWith(".dat"));
        
        if (files == null || files.length == 0) {
            Gravestones.LOGGER.info("No gravestone history files found");
            return;
        }
        
        int loaded = 0;
        for (File file : files) {
            try {
                String filename = file.getName();
                String uuidString = filename.substring(0, filename.length() - 4); // Remove .dat
                UUID playerUuid = UUID.fromString(uuidString);
                
                LinkedList<GravestoneHistoryEntry> history = loadHistory(server, playerUuid);
                if (!history.isEmpty()) {
                    historyMap.put(playerUuid, history);
                    loaded++;
                }
                
            } catch (IllegalArgumentException e) {
                Gravestones.LOGGER.warn("Invalid history filename: {}", file.getName());
            }
        }
        
        Gravestones.LOGGER.info("Loaded gravestone history for {} players", loaded);
    }
    
    /**
     * Save all histories from the map to disk
     */
    public static void saveAllHistories(MinecraftServer server, Map<UUID, LinkedList<GravestoneHistoryEntry>> historyMap) {
        int saved = 0;
        for (Map.Entry<UUID, LinkedList<GravestoneHistoryEntry>> entry : historyMap.entrySet()) {
            saveHistory(server, entry.getKey(), entry.getValue());
            saved++;
        }
        
        Gravestones.LOGGER.info("Saved gravestone history for {} players", saved);
    }
}
