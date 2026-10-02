package com.createatomic.effect;

import com.createatomic.registry.ModEffects;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Radiation: drains hunger at every level, and deals damage from amplifier 1 upwards.
 * Amplifier 0 = background (ore, raw materials), 1 = spent fuel, 2 = reactor meltdown.
 */
public class RadiationEffect extends MobEffect {
    public RadiationEffect() {
        super(MobEffectCategory.HARMFUL, 0x7CFC00);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return true;
        }
        if (entity instanceof Player player) {
            player.causeFoodExhaustion(0.5f * (amplifier + 1));
        }
        if (amplifier >= 1) {
            entity.hurt(entity.damageSources().magic(), (float) amplifier);
        }
        return true;
    }

    /** Applies radiation to every living entity in a sphere around a block. */
    public static void irradiate(Level level, BlockPos pos, double radius, int durationTicks, int amplifier) {
        Vec3 center = Vec3.atCenterOf(pos);
        AABB box = new AABB(pos).inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (entity instanceof Player p && (p.isCreative() || p.isSpectator())) {
                continue;
            }
            if (entity.distanceToSqr(center) <= radius * radius) {
                entity.addEffect(new MobEffectInstance(ModEffects.RADIATION, durationTicks, amplifier));
            }
        }
    }
}
