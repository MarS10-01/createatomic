package com.createatomic.radiation;

import com.createatomic.CreateAtomic;
import com.createatomic.registry.ModEffects;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Accumulates dose once per second and turns it into radiation sickness. */
@EventBusSubscriber(modid = CreateAtomic.MODID)
public class RadiationEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        double[] exposure = Radiation.protect(player, Radiation.rawExposure(player));
        double rate = Radiation.effective(exposure);

        float dose = Radiation.getDose(player);
        dose = (float) Math.max(0.0, dose + rate - Radiation.RECOVERY);
        Radiation.setDose(player, dose);

        int stage = dose >= 5.0f ? 4 : dose >= 3.0f ? 3 : dose >= 1.5f ? 2 : dose >= 0.5f ? 1 : 0;
        if (stage > 0) {
            player.addEffect(new MobEffectInstance(ModEffects.RADIATION, 100, stage - 1, false, false, true));
        }
        if (dose >= 10.0f) {
            player.hurt(player.damageSources().magic(), 1000.0f);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Radiation.setDose(event.getEntity(), 0f);
    }
}
