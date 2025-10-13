package net.pneumono.gravestones.compat;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.impl.ExpandedSimpleContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.text.Text;
import net.pneumono.gravestones.api.GravestoneDataType;
import net.pneumono.gravestones.api.GravestonesApi;
import net.pneumono.gravestones.Gravestones;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class AccessoriesDataType extends GravestoneDataType {
    @Override
    public void writeData(NbtCompound view, PlayerEntity player) {
        Gravestones.LOGGER.info("[DEBUG] AccessoriesDataType.writeData called for player: {}", player.getName().getString());
        
        AccessoriesCapability capability = AccessoriesCapability.get(player);
        if (capability == null) {
            Gravestones.LOGGER.warn("[DEBUG] AccessoriesCapability is null for player: {}", player.getName().getString());
            return;
        }

        // Save regular accessories
        List<SlotReferencePrimitive> list = capability.getAllEquipped().stream()
                .filter(reference -> !GravestonesApi.shouldSkipItem(player, reference.stack()))
                .map(reference -> new SlotReferencePrimitive(reference.stack(), reference.reference(), false))
                .toList();

        Gravestones.LOGGER.info("[DEBUG] Found {} regular accessories to save for player: {}", list.size(), player.getName().getString());

        // Save cosmetic accessories
        List<SlotReferencePrimitive> cosmeticList = new ArrayList<>();
        for (var entry : capability.getContainers().entrySet()) {
            String slotName = entry.getKey();
            AccessoriesContainer container = entry.getValue();
            ExpandedSimpleContainer cosmeticAccessories = container.getCosmeticAccessories();
            
            for (int i = 0; i < cosmeticAccessories.size(); i++) {
                ItemStack stack = cosmeticAccessories.getStack(i);
                if (!stack.isEmpty() && !GravestonesApi.shouldSkipItem(player, stack)) {
                    cosmeticList.add(new SlotReferencePrimitive(stack, slotName, i, true));
                    Gravestones.LOGGER.info("[DEBUG] Found cosmetic accessory: {} in slot {} index {}", stack.getItem().toString(), slotName, i);
                }
            }
        }
        
        Gravestones.LOGGER.info("[DEBUG] Found {} cosmetic accessories to save for player: {}", cosmeticList.size(), player.getName().getString());

        // Combine both lists
        List<SlotReferencePrimitive> allAccessories = new ArrayList<>();
        allAccessories.addAll(list);
        allAccessories.addAll(cosmeticList);

        DataResult<NbtElement> result = SlotReferencePrimitive.CODEC.listOf().encodeStart(player.getRegistryManager().getOps(NbtOps.INSTANCE), allAccessories);
        if (result.isSuccess()) {
            view.put("accessories", result.getOrThrow());
            Gravestones.LOGGER.info("[DEBUG] Successfully saved {} total accessories (regular + cosmetic) to NBT for player: {}", allAccessories.size(), player.getName().getString());
            
            // Only remove items from player AFTER successfully saving to gravestone
            capability.getAllEquipped().stream()
                    .filter(reference -> !GravestonesApi.shouldSkipItem(player, reference.stack()))
                    .forEach(reference -> {
                        Gravestones.LOGGER.info("[DEBUG] Removing regular accessory: {} from player: {}", reference.stack().getItem().toString(), player.getName().getString());
                        reference.reference().setStack(ItemStack.EMPTY);
                    });
            
            // Remove cosmetic accessories
            for (var entry : capability.getContainers().entrySet()) {
                AccessoriesContainer container = entry.getValue();
                ExpandedSimpleContainer cosmeticAccessories = container.getCosmeticAccessories();
                
                for (int i = 0; i < cosmeticAccessories.size(); i++) {
                    ItemStack stack = cosmeticAccessories.getStack(i);
                    if (!stack.isEmpty() && !GravestonesApi.shouldSkipItem(player, stack)) {
                        Gravestones.LOGGER.info("[DEBUG] Removing cosmetic accessory: {} from player: {}", stack.getItem().toString(), player.getName().getString());
                        cosmeticAccessories.setStack(i, ItemStack.EMPTY);
                    }
                }
            }
        } else {
            Gravestones.LOGGER.error("[DEBUG] Failed to save accessories for player: {}", player.getName().getString());
        }
    }

    @Override
    public void onBreak(NbtCompound view, World world, BlockPos pos, int decay) {
        List<SlotReferencePrimitive> list = deserialize(view, world.getRegistryManager());
        if (list == null || list.isEmpty()) return;

        dropStacks(world, pos, list.stream().map(SlotReferencePrimitive::stack));
    }

    @Override
    public void onCollect(NbtCompound view, World world, BlockPos pos, PlayerEntity player, int decay) {
        List<SlotReferencePrimitive> list = deserialize(view, player.getRegistryManager());
        if (list == null || list.isEmpty()) return;

        AccessoriesCapability capability = AccessoriesCapability.get(player);
        if (capability == null) return;

        List<ItemStack> remaining = new ArrayList<>();

        for (SlotReferencePrimitive primitive : list) {
            ItemStack newStack = primitive.stack;
            if (newStack.isEmpty()) continue;
            int index = primitive.index;
            boolean isCosmetic = primitive.isCosmetic;

            AccessoriesContainer container = capability.getContainers().get(primitive.slotName);
            if (container == null || container.getSize() <= 0) {
                remaining.add(newStack);
                continue;
            }

            // Choose the correct inventory based on whether it's cosmetic
            ExpandedSimpleContainer targetInventory = isCosmetic ? container.getCosmeticAccessories() : container.getAccessories();

            // For regular accessories, check if the slot is valid
            if (!isCosmetic) {
                SlotReference slotReference = container.createReference(index);
                if (!AccessoriesAPI.canInsertIntoSlot(newStack, slotReference)) {
                    remaining.add(newStack);
                    continue;
                }
            }

            ItemStack oldStack = targetInventory.getStack(index);
            if (oldStack.isEmpty()) {
                // Slot is empty, we can insert the item directly
                if (isCosmetic) {
                    targetInventory.setStack(index, newStack);
                    Gravestones.LOGGER.info("[DEBUG] Restored cosmetic accessory: {} to slot {} index {}", newStack.getItem().toString(), primitive.slotName, index);
                } else {
                    SlotReference slotReference = container.createReference(index);
                    slotReference.setStack(newStack);
                    Gravestones.LOGGER.info("[DEBUG] Restored regular accessory: {} to slot {} index {}", newStack.getItem().toString(), primitive.slotName, index);
                }
            } else {
                // Slot is occupied, add to remaining items to be dropped
                remaining.add(newStack);
            }
        }

        dropStacks(world, pos, remaining.stream());
    }

    public List<SlotReferencePrimitive> deserialize(NbtCompound view, DynamicRegistryManager registryManager) {
        DataResult<Pair<List<SlotReferencePrimitive>, NbtElement>> result = SlotReferencePrimitive.CODEC.listOf()
                .decode(registryManager.getOps(NbtOps.INSTANCE), view.get("accessories"));
        if (result.isSuccess()) {
            return result.getOrThrow().getFirst();
  
        } else {
            return new ArrayList<>();
        }
    }

    public void dropStacks(World world, BlockPos pos, Stream<ItemStack> stream) {
        stream.filter(stack -> !stack.isEmpty())
                .forEach(stack -> ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), stack));
    }

    public record SlotReferencePrimitive(ItemStack stack, String slotName, int index, boolean isCosmetic) {
        public static final Codec<SlotReferencePrimitive> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.CODEC.fieldOf("newStack").forGetter(SlotReferencePrimitive::stack),
                Codec.STRING.fieldOf("slot_name").forGetter(SlotReferencePrimitive::slotName),
                Codec.INT.fieldOf("index").forGetter(SlotReferencePrimitive::index),
                Codec.BOOL.optionalFieldOf("is_cosmetic", false).forGetter(SlotReferencePrimitive::isCosmetic)
        ).apply(instance, SlotReferencePrimitive::new));

        public SlotReferencePrimitive(ItemStack stack, SlotReference reference, boolean isCosmetic) {
            this(
                    stack,
                    reference.slotName(),
                    reference.slot(),
                    isCosmetic
            );
        }
    }
}
