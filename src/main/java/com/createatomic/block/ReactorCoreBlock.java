package com.createatomic.block;

import com.createatomic.block.entity.ReactorCoreBlockEntity;
import com.createatomic.registry.ModBlockEntities;
import com.createatomic.registry.ModItems;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Nuclear reactor core. A kinetic generator: shafts attach on both ends of its axis,
 * the four remaining sides should touch water source blocks for cooling.
 *
 * Controls:
 *  - right-click with a fuel rod        : load a rod (while idle)
 *  - right-click with an empty hand      : start / SCRAM
 *  - sneak + right-click, empty hand     : unload rods
 */
public class ReactorCoreBlock extends RotatedPillarKineticBlock implements IBE<ReactorCoreBlockEntity> {

    public ReactorCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(AXIS);
    }

    @Override
    public Class<ReactorCoreBlockEntity> getBlockEntityClass() {
        return ReactorCoreBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ReactorCoreBlockEntity> getBlockEntityType() {
        return ModBlockEntities.REACTOR_CORE.get();
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                           Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.FUEL_ROD.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        withBlockEntityDo(level, pos, be -> be.insertRod(player, stack));
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                            BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        withBlockEntityDo(level, pos, be -> {
            if (player.isShiftKeyDown()) {
                be.extract(player);
            } else {
                be.toggle(player);
            }
        });
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, ReactorCoreBlockEntity::dropContents);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
