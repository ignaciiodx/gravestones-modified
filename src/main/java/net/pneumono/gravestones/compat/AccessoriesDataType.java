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

        List<SlotReferencePrimitive> list = capability.getAllEquipped().stream()
                .filter(reference -> !GravestonesApi.shouldSkipItem(player, reference.stack()))
                .map(reference -> new SlotReferencePrimitive(reference.stack(), reference.reference()))
                .toList();

        Gravestones.LOGGER.info("[DEBUG] Found {} accessories to save for player: {}", list.size(), player.getName().getString());

        DataResult<NbtElement> result = SlotReferencePrimitive.CODEC.listOf().encodeStart(player.getRegistryManager().getOps(NbtOps.INSTANCE), list);
        if (result.isSuccess()) {
            view.put("accessories", result.getOrThrow());
            Gravestones.LOGGER.info("[DEBUG] Successfully saved {} accessories to NBT for player: {}", list.size(), player.getName().getString());
            
            // Only remove items from player AFTER successfully saving to gravestone
            capability.getAllEquipped().stream()
                    .filter(reference -> !GravestonesApi.shouldSkipItem(player, reference.stack()))
                    .forEach(reference -> {
                        Gravestones.LOGGER.info("[DEBUG] Removing accessory: {} from player: {}", reference.stack().getItem().toString(), player.getName().getString());
                        reference.reference().setStack(ItemStack.EMPTY);
                    });
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

            AccessoriesContainer container = capability.getContainers().get(primitive.slotName);
            if (container == null || container.getSize() <= 0) {
                remaining.add(newStack);
                continue;
            }

            SlotReference slotReference = container.createReference(index);
            if (!AccessoriesAPI.canInsertIntoSlot(newStack, slotReference)) {
                remaining.add(newStack);
                continue;
            }

            ExpandedSimpleContainer accessories = container.getAccessories();

            ItemStack oldStack = accessories.getStack(index);
            if (oldStack.isEmpty()) {
                // Slot is empty, we can insert the item directly
                slotReference.setStack(newStack);
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

    public record SlotReferencePrimitive(ItemStack stack, String slotName, int index) {
        public static final Codec<SlotReferencePrimitive> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.CODEC.fieldOf("newStack").forGetter(SlotReferencePrimitive::stack),
                Codec.STRING.fieldOf("slot_name").forGetter(SlotReferencePrimitive::slotName),
                Codec.INT.fieldOf("index").forGetter(SlotReferencePrimitive::index)
        ).apply(instance, SlotReferencePrimitive::new));

        public SlotReferencePrimitive(ItemStack stack, SlotReference reference) {
            this(
                    stack,
                    reference.slotName(),
                    reference.slot()
            );
        }
    }
}
