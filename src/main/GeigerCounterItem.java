package com.createatomic.item;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.createatomic.radiation.Radiation;
import com.createatomic.radiation.RadiationType;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Hold it (main hand or off hand): clicks get faster with radiation, and the action bar shows the dose rate,
 * your accumulated dose and which radiation types are present.
 */
public class GeigerCounterItem extends Item {
    private static final Map<UUID, double[]> CACHE = new HashMap<>();

    public GeigerCounterItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }
        if (!isSelected && player.getOffhandItem() != stack) {
            return;
        }

        double[] raw = CACHE.get(player.getUUID());
        if (raw == null || player.tickCount % 5 == 0) {
            raw = Radiation.rawExposure(player);
            CACHE.put(player.getUUID(), raw);
        }
        double rate = Radiation.effective(raw);

        double clickChance = Math.min(0.9, 0.02 + rate * 400.0);
        if (player.getRandom().nextDouble() < clickChance) {
            player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.25f, 2.0f);
        }

        if (player.tickCount % 10 == 0) {
            StringBuilder types = new StringBuilder();
            for (RadiationType type : RadiationType.VALUES) {
                if (raw[type.ordinal()] > 1.0e-6) {
                    types.append(type.symbol).append(' ');
                }
            }
            player.displayClientMessage(Component.translatable("message.createatomic.geiger",
                    String.format(Locale.ROOT, "%.2f", rate * 1000.0),
                    String.format(Locale.ROOT, "%.2f", Radiation.getDose(player)),
                    types.length() == 0 ? "-" : types.toString().trim()), true);
        }
    }
}
