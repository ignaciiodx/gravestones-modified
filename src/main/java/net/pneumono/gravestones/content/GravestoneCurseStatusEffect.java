package net.pneumono.gravestones.content;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;



public class GravestoneCurseStatusEffect extends StatusEffect {
    public GravestoneCurseStatusEffect() {
        super(StatusEffectCategory.HARMFUL, 0x2C1B47); // Dark purple color
        // No attribute modifiers to avoid accumulation issues
        // Slowness is applied separately in TechnicalGravestoneBlock
    }


    // Blindness visuals are replicated via client-side mixin in BackgroundRendererMixin
    // Hunger effect is applied separately in TechnicalGravestoneBlock
}
