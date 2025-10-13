package net.pneumono.gravestones;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single gravestone entry in the history
 */
public class GravestoneHistoryEntry {
    private final BlockPos position;
    private final RegistryKey<World> dimension;
    private final long timestamp;
    private final NbtCompound gravestoneData; // Stores all gravestone data (inventory, xp, etc.)
    
    public GravestoneHistoryEntry(BlockPos position, RegistryKey<World> dimension, long timestamp, NbtCompound gravestoneData) {
        this.position = position;
        this.dimension = dimension;
        this.timestamp = timestamp;
        this.gravestoneData = gravestoneData;
    }
    
    public BlockPos getPosition() {
        return position;
    }
    
    public RegistryKey<World> getDimension() {
        return dimension;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public NbtCompound getGravestoneData() {
        return gravestoneData;
    }
    
    /**
     * Serialize this entry to NBT
     */
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        
        nbt.putInt("x", position.getX());
        nbt.putInt("y", position.getY());
        nbt.putInt("z", position.getZ());
        nbt.putString("dimension", dimension.getValue().toString());
        nbt.putLong("timestamp", timestamp);
        nbt.put("gravestoneData", gravestoneData.copy());
        
        return nbt;
    }
    
    /**
     * Deserialize an entry from NBT
     */
    public static GravestoneHistoryEntry fromNbt(NbtCompound nbt) {
        BlockPos pos = new BlockPos(
            nbt.getInt("x"),
            nbt.getInt("y"),
            nbt.getInt("z")
        );
        
        RegistryKey<World> dimension = RegistryKey.of(
            RegistryKeys.WORLD,
            Identifier.of(nbt.getString("dimension"))
        );
        
        long timestamp = nbt.getLong("timestamp");
        NbtCompound gravestoneData = nbt.getCompound("gravestoneData");
        
        return new GravestoneHistoryEntry(pos, dimension, timestamp, gravestoneData);
    }
    
    /**
     * Serialize a list of entries to NBT
     */
    public static NbtList toNbtList(List<GravestoneHistoryEntry> entries) {
        NbtList list = new NbtList();
        for (GravestoneHistoryEntry entry : entries) {
            list.add(entry.toNbt());
        }
        return list;
    }
    
    /**
     * Deserialize a list of entries from NBT
     */
    public static List<GravestoneHistoryEntry> fromNbtList(NbtList list) {
        List<GravestoneHistoryEntry> entries = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            entries.add(fromNbt(list.getCompound(i)));
        }
        return entries;
    }
}
