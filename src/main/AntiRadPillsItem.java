package com.createatomic.item;

import com.createatomic.radiation.Radiation;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Iodine / radioprotector tablets: removes part of the accumulated dose. */
public class AntiRadPillsItem extends Item {
    private static final float DOSE_REMOVED = 0.75f;

    public AntiRadPillsItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            float dose = Radiation.getDose(player);
            if (dose <= 0.01f) {
                player.displayClientMessage(Component.translatable("message.createatomic.pills_not_needed"), true);
                return InteractionResultHolder.fail(stack);
            }
            Radiation.setDose(player, dose - DOSE_REMOVED);
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            player.getCooldowns().addCooldown(this, 60);
            level.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 1.0f, 1.4f);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
