package com.createatomic.block;

import com.createatomic.block.entity.ReactorCoreBlockEntity;
import com.createatomic.block.entity.ReactorStructure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.ItemInteractionResult;

/** Common interaction for blocks placed inside the reactor vessel. */
public class ReactorComponentBlock extends Block {
    public ReactorComponentBlock(Properties properties) {
        super(properties);
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                           Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.WATER_BUCKET)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        BlockPos controller = ReactorStructure.findController(level, pos);
        if (controller == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        ReactorCoreBlockEntity be = (ReactorCoreBlockEntity) level.getBlockEntity(controller);
        if (be == null || !be.canInteractFromInside(player, pos)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        be.addWaterFromInside(player, hand);
        return ItemInteractionResult.SUCCESS;
    }
}
