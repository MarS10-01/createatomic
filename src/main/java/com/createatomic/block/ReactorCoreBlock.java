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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Reactor Controller: the brain of the multiblock reactor and a Create kinetic generator.
 *
 * Put it IN the wall of the reactor chamber. Its shaft axis connects to your Create network.
 *
 *  - redstone signal (0-15) = how far the control rods are withdrawn (no signal = rods fully inserted = off)
 *  - comparator output     = core temperature (0-15), use it to build your own safety systems
 *  - right-click, fuel rod  : load a rod (reactor must be idle)
 *  - right-click, water bucket or a Create pipe/pump on ANY exposed face : coolant
 *  - right-click, empty hand: status / structure check
 *  - sneak + right-click, empty hand: unload (reactor must be cold)
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
        boolean fuel = stack.is(ModItems.FUEL_ROD.get());
        boolean water = stack.is(Items.WATER_BUCKET);
        if (!fuel && !water) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (fuel) {
            withBlockEntityDo(level, pos, be -> be.insertRod(player, stack));
        } else {
            withBlockEntityDo(level, pos, be -> be.addBucket(player, hand));
        }
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
                be.status(player);
            }
        });
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return getBlockEntityOptional(level, pos).map(ReactorCoreBlockEntity::getAnalogSignal).orElse(0);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, ReactorCoreBlockEntity::dropContents);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
