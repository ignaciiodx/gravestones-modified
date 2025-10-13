package net.pneumono.gravestones.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.pneumono.gravestones.content.GravestonesRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityCurseEffectFixMixin {
    @Inject(method = "onStatusEffectRemoved", at = @At("HEAD"))
    private void gravestones$onCurseRemoved(StatusEffectInstance effect, CallbackInfo ci) {
        if (effect.getEffectType() == GravestonesRegistry.GRAVESTONE_CURSE) {
            // Forzar recálculo de velocidad: aplicar Speed 0 durante 1 tick
            ((LivingEntity)(Object)this).addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 1, 0, false, false, false));
        }
    }
}
