package com.createatomic.effect;

import com.createatomic.CreateAtomic;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Radiation sickness. Stages (amplifier 0..3) are applied by RadiationEvents from the accumulated dose:
 * weakness and slowness, shrinking maximum health, hunger, and finally organ failure damage.
 */
public class RadiationEffect extends MobEffect {

    public RadiationEffect() {
        super(MobEffectCategory.HARMFUL, 0x9ACD32);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath(CreateAtomic.MODID, "effect.radiation_sickness.speed"),
                -0.08, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.ATTACK_DAMAGE,
                ResourceLocation.fromNamespaceAndPath(CreateAtomic.MODID, "effect.radiation_sickness.damage"),
                -0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.MAX_HEALTH,
                ResourceLocation.fromNamespaceAndPath(CreateAtomic.MODID, "effect.radiation_sickness.health"),
                -2.0, AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 40 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return true;
        }
        if (entity instanceof Player player) {
            player.causeFoodExhaustion(1.0f * (amplifier + 1));
        }
        if (amplifier >= 2) {
            entity.hurt(entity.damageSources().magic(), amplifier >= 3 ? 2.0f : 1.0f);
        }
        return true;
    }
}
