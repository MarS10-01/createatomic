package com.createatomic.block.entity;

import com.createatomic.effect.RadiationEffect;
import com.createatomic.registry.ModBlockEntities;
import com.createatomic.registry.ModItems;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * Reactor logic.
 *
 *  - Up to 4 fuel rods can be loaded. All loaded rods burn together for FUEL_TICKS and become spent rods.
 *  - Every rod produces heat; every adjacent water SOURCE block removes heat.
 *    Rule of thumb: you need at least as many water sources around the core as loaded rods.
 *  - At MAX_HEAT the core melts down: huge explosion + lethal radiation in a wide radius.
 *  - Output: SPEED rpm, and CAPACITY_PER_ROD stress units per rpm for every loaded rod while running.
 */
public class ReactorCoreBlockEntity extends GeneratingKineticBlockEntity {
    public static final int MAX_RODS = 4;
    public static final int FUEL_TICKS = 6000;      // 5 minutes per fuel load
    public static final int MAX_HEAT = 1000;
    public static final float SPEED = 64f;           // rpm
    public static final float CAPACITY_PER_ROD = 256f; // su per rpm per rod (4 rods = 65 536 su at 64 rpm)

    private int rods;
    private int spent;
    private int burnLeft;
    private int heat;
    private boolean running;

    public ReactorCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REACTOR_CORE.get(), pos, state);
    }

    // ------------------------------------------------------------ kinetics

    @Override
    public float getGeneratedSpeed() {
        return running ? SPEED : 0f;
    }

    @Override
    public float calculateAddedStressCapacity() {
        float capacity = running ? rods * CAPACITY_PER_ROD : 0f;
        this.lastCapacityProvided = capacity;
        return capacity;
    }

    // ------------------------------------------------------------ simulation

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }

        if (running && --burnLeft <= 0) {
            spent += rods;
            rods = 0;
            burnLeft = 0;
            running = false;
            updateGeneratedRotation();
            notifyUpdate();
        }

        if (level.getGameTime() % 10 != 0) {
            return;
        }
        if (!running && heat == 0) {
            return;
        }

        int gain = running ? rods * 2 : 0;
        int cooling = countWaterSources() * 2 + (heat > 0 ? 1 : 0);
        int newHeat = Mth.clamp(heat + gain - cooling, 0, MAX_HEAT);
        if (newHeat != heat) {
            heat = newHeat;
            setChanged();
        }

        if (heat >= MAX_HEAT) {
            meltdown();
            return;
        }

        if (heat > MAX_HEAT * 7 / 10 && level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5,
                    3, 0.3, 0.1, 0.3, 0.02);
            if (heat > MAX_HEAT * 85 / 100) {
                level.playSound(null, worldPosition, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.5f, 0.6f);
            }
        }
    }

    private int countWaterSources() {
        int count = 0;
        for (Direction direction : Direction.values()) {
            FluidState fluid = level.getFluidState(worldPosition.relative(direction));
            if (fluid.is(FluidTags.WATER) && fluid.isSource()) {
                count++;
            }
        }
        return count;
    }

    private void meltdown() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos pos = worldPosition;
        RadiationEffect.irradiate(serverLevel, pos, 32, 1200, 2);
        serverLevel.removeBlock(pos, false);
        serverLevel.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 7.0f,
                Level.ExplosionInteraction.BLOCK);
    }

    // ------------------------------------------------------------ player actions

    public void insertRod(Player player, ItemStack stack) {
        if (running || burnLeft > 0) {
            player.displayClientMessage(Component.translatable("message.createatomic.busy"), true);
            return;
        }
        if (rods >= MAX_RODS) {
            player.displayClientMessage(Component.translatable("message.createatomic.full"), true);
            return;
        }
        rods++;
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        level.playSound(null, worldPosition, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1f, 1f);
        updateGeneratedRotation();
        notifyUpdate();
        status(player);
    }

    public void toggle(Player player) {
        if (running) {
            running = false;
        } else if (rods > 0) {
            running = true;
            if (burnLeft <= 0) {
                burnLeft = FUEL_TICKS;
            }
        } else {
            player.displayClientMessage(Component.translatable("message.createatomic.no_fuel"), true);
            return;
        }
        level.playSound(null, worldPosition, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1f, running ? 1.2f : 0.8f);
        updateGeneratedRotation();
        notifyUpdate();
        status(player);
    }

    public void extract(Player player) {
        if (running) {
            player.displayClientMessage(Component.translatable("message.createatomic.busy"), true);
            return;
        }
        if (rods == 0 && spent == 0) {
            status(player);
            return;
        }
        give(player, takeOutputs());
        updateGeneratedRotation();
        notifyUpdate();
        status(player);
    }

    /** Called when the block is broken: spill whatever is inside. */
    public void dropContents() {
        if (level == null || level.isClientSide) {
            return;
        }
        for (ItemStack stack : takeOutputs()) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5, stack);
        }
    }

    /** Empties the reactor. Partially burnt rods come back as spent rods. */
    private ItemStack[] takeOutputs() {
        int fuel = burnLeft > 0 ? 0 : rods;
        int waste = spent + (burnLeft > 0 ? rods : 0);
        rods = 0;
        spent = 0;
        burnLeft = 0;
        return new ItemStack[] {
                new ItemStack(ModItems.FUEL_ROD.get(), fuel),
                new ItemStack(ModItems.SPENT_FUEL_ROD.get(), waste)
        };
    }

    private static void give(Player player, ItemStack[] stacks) {
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    private void status(Player player) {
        Component state = Component.translatable(running
                ? "message.createatomic.state_on" : "message.createatomic.state_off");
        player.displayClientMessage(Component.translatable("message.createatomic.status",
                state, rods, MAX_RODS, spent, heat * 100 / MAX_HEAT, Math.max(burnLeft, 0) / 20), true);
    }

    // ------------------------------------------------------------ persistence

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Rods", rods);
        tag.putInt("Spent", spent);
        tag.putInt("BurnLeft", burnLeft);
        tag.putInt("Heat", heat);
        tag.putBoolean("Running", running);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        rods = tag.getInt("Rods");
        spent = tag.getInt("Spent");
        burnLeft = tag.getInt("BurnLeft");
        heat = tag.getInt("Heat");
        running = tag.getBoolean("Running");
    }
}
