package com.createatomic.block;

import com.createatomic.block.entity.ReactorCoreBlockEntity;
import com.createatomic.block.entity.ReactorStructure;
import com.createatomic.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Internal fuel channel. A loaded channel gets a visible fuel-rod state. */
public class FuelChannelBlock extends ReactorComponentBlock {
    public static final BooleanProperty LOADED = BooleanProperty.create("loaded");

    public FuelChannelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LOADED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LOADED);
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(ModItems.FUEL_ROD.get())) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            BlockPos controller = ReactorStructure.findController(level, pos);
            if (controller == null || !(level.getBlockEntity(controller) instanceof ReactorCoreBlockEntity be)) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            be.insertRodIntoChannel(player, stack, pos);
            return ItemInteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockPos controller = ReactorStructure.findController(level, pos);
        if (controller != null && level.getBlockEntity(controller) instanceof ReactorCoreBlockEntity be) {
            if (player.isShiftKeyDown()) {
                be.removeRodFromChannel(player, pos);
            } else {
                be.status(player);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
