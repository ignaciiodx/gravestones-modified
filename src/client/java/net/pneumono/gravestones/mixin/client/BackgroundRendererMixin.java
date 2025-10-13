package net.pneumono.gravestones.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.FogShape;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.pneumono.gravestones.content.GravestonesRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {
    
    /**
     * Replicate vanilla blindness fog rendering for Gravestone Curse effect.
     * Injected at TAIL to override fog settings after vanilla calculations.
     */
    @Inject(method = "applyFog", at = @At("TAIL"))
    private static void gravestones$applyBlindnessFog(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
        Entity entity = camera.getFocusedEntity();
        
        if (entity instanceof LivingEntity livingEntity) {
            StatusEffectInstance curseEffect = livingEntity.getStatusEffect(GravestonesRegistry.GRAVESTONE_CURSE);
            
            if (curseEffect != null) {
                // Replicate exact vanilla blindness fog calculation
                float fogStart = -4.0F;
                float fogEnd = fogStart * 0.5F; // = -2.0F (this creates the close fog effect)
                
                RenderSystem.setShaderFogStart(fogStart);
                RenderSystem.setShaderFogEnd(fogEnd);
                RenderSystem.setShaderFogShape(FogShape.SPHERE);
            }
        }
    }
}
