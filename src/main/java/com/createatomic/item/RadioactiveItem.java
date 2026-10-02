package com.createatomic.item;

import com.createatomic.registry.ModEffects;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** An item that irradiates whoever carries it. */
public class RadioactiveItem extends Item {
    private final int amplifier;

    public RadioactiveItem(Properties properties, int amplifier) {
        super(properties);
        this.amplifier = amplifier;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide || !(entity instanceof LivingEntity living) || level.getGameTime() % 40 != 0) {
            return;
        }
        if (entity instanceof Player p && (p.isCreative() || p.isSpectator())) {
            return;
        }
        living.addEffect(new MobEffectInstance(ModEffects.RADIATION, 100, amplifier));
    }
}
