package net.pneumono.gravestones.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.entry.RegistryEntry;
import net.pneumono.gravestones.content.GravestonesRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntitySlownessMixin {
    
    /**
     * Modifies movement speed when the entity has Gravestone Curse effect.
     * Replicates Slowness III (-45% speed) without using attribute modifiers.
     */
    @Inject(method = "getAttributeValue", at = @At("RETURN"), cancellable = true)
    private void gravestones$applySlownessFromCurse(RegistryEntry<EntityAttribute> attribute, CallbackInfoReturnable<Double> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        
        // Only modify movement speed attribute
        if (attribute.equals(EntityAttributes.GENERIC_MOVEMENT_SPEED)) {
            // Check if entity has Gravestone Curse effect
            if (entity.hasStatusEffect(GravestonesRegistry.GRAVESTONE_CURSE)) {
                double currentSpeed = cir.getReturnValue();
                // Apply -45% speed reduction (Slowness III equivalent)
                double modifiedSpeed = currentSpeed * 0.55; // 55% of original speed = -45% reduction
                cir.setReturnValue(modifiedSpeed);
            }
        }
    }
}
