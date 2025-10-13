package net.pneumono.gravestones.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.pneumono.gravestones.content.GravestonesRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    
    @Shadow @Final private MinecraftClient client;
    
    /**
     * Render darkness overlay for Gravestone Curse effect, replicating vanilla blindness.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void gravestones$renderBlindnessOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (this.client.player == null) {
            return;
        }
        
        StatusEffectInstance curseEffect = this.client.player.getStatusEffect(GravestonesRegistry.GRAVESTONE_CURSE);
        
        if (curseEffect != null) {
            // Render darkness overlay like vanilla blindness does
            renderBlindnessOverlay(context);
        }
    }
    
    private void renderBlindnessOverlay(DrawContext context) {
        int width = this.client.getWindow().getScaledWidth();
        int height = this.client.getWindow().getScaledHeight();
        
        // Vanilla blindness renders a black overlay with reduced opacity
        // This creates the "darkening" effect that combines with the fog
        context.fill(0, 0, width, height, 0x80000000); // 50% black overlay (ARGB: 80 = ~50% alpha)
    }
}
