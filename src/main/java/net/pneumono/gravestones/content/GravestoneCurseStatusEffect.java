package net.pneumono.gravestones.content;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.pneumono.gravestones.Gravestones;

public class GravestoneCurseStatusEffect extends StatusEffect {
    public GravestoneCurseStatusEffect() {
        super(StatusEffectCategory.HARMFUL, 0x2C1B47); // Dark purple color
        
        // Replicate Slowness III effect by directly modifying movement speed
        // Slowness I = -15%, II = -30%, III = -45%
        this.addAttributeModifier(
            EntityAttributes.GENERIC_MOVEMENT_SPEED,
            Gravestones.id("gravestone_curse_slowness"),
            -0.15, // -15% per amplifier
            EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }
    
    // Blindness visuals are replicated via client-side mixin in BackgroundRendererMixin
}
