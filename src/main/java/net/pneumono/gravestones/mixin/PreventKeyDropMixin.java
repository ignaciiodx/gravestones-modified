package net.pneumono.gravestones.mixin;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.pneumono.gravestones.content.GravestonesRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents gravestone keys from being dropped by the player.
 * Cancels the drop and returns the key to inventory.
 */
@Mixin(PlayerEntity.class)
public class PreventKeyDropMixin {
    
    /**
     * Prevents gravestone keys from being dropped
     * This intercepts at HEAD before anything happens
     */
    @Inject(method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;", 
            at = @At("HEAD"), 
            cancellable = true)
    private void preventGravestoneKeyDrop(ItemStack stack, boolean throwRandomly, boolean retainOwnership, 
                                         CallbackInfoReturnable<ItemEntity> cir) {
        if (stack != null && !stack.isEmpty() && stack.isOf(GravestonesRegistry.GRAVESTONE_KEY)) {
            // Cancel the drop completely and add item back to inventory
            PlayerEntity player = (PlayerEntity) (Object) this;
            
            // Create a copy to add back to inventory
            ItemStack copy = stack.copy();
            
            // Try to insert the stack back into inventory
            if (!player.getInventory().insertStack(copy)) {
                // If inventory is full, just cancel the drop but keep the original
                // This prevents the item from being lost
            }
            
            // Cancel the drop operation
            cir.setReturnValue(null);
        }
    }
}
