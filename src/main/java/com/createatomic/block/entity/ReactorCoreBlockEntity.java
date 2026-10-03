package com.createatomic.block.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.createatomic.radiation.Radiation;
import com.createatomic.registry.ModBlockEntities;
import com.createatomic.registry.ModBlocks;
import com.createatomic.registry.ModItems;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Reactor physics (one simulation step every 10 ticks).
 *
 *  control rods : redstone signal sets the target rod withdrawal; rods withdraw slowly and SCRAM quickly
 *  power        : follows rod position, with temperature feedback
 *                   PWR  - negative coefficient: hotter core = less power (self-stabilising)
 *                   RBMK - positive void coefficient: with low coolant, hotter core = MORE power (runaway),
 *                          and a SCRAM from high withdrawal gives a short power spike (graphite-tipped rods)
 *  heat         : produced by power and by decay heat (which keeps going after shutdown!)
 *  coolant      : water in the controller tank evaporates to remove heat; the steam drives the turbine shaft
 *  meltdown     : core temperature reaches MELT_TEMP
 */
public class ReactorCoreBlockEntity extends GeneratingKineticBlockEntity {
    public static final int TANK_CAPACITY = 64000;
    public static final float FUEL_UNITS = 1500f;
    public static final float HEAT_PER_MB = 0.45f;
    public static final float SCRAM_TEMP = 850f;
    public static final float MELT_TEMP = 1350f;
    public static final float SPEED = 96f;
    public static final float SU_PER_STEAM = 11.0f;

    private int rods;
    private int spent;
    private int hotRods;
    private int coolant;
    private float burnLeft;
    private float temp = 20f;
    private float power;
    private float decay;
    private float rodPos;
    private float spike;
    private float steam;
    private float prevTarget;
    private float outputCapacity;
    private boolean generating;

    private final Set<Long> loadedChannels = new HashSet<>();

    private ReactorStructure structure = new ReactorStructure();
    private boolean scanned;

    // Structure info synced to the client (the multiblock scan only runs on the server). Used by the goggle tooltip.
    private boolean viewValid;
    private boolean viewRbmk;
    private int viewFuel;
    private int viewWidth;
    private int viewHeight;
    private int viewDepth;
    private String viewError = "err_interior";
    private int[] viewErrorArgs = new int[0];

    private final IFluidHandler coolantHandler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return coolant > 0 ? new FluidStack(Fluids.WATER, coolant) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return TANK_CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return stack.getFluid().isSame(Fluids.WATER);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !resource.getFluid().isSame(Fluids.WATER)) {
                return 0;
            }
            int accepted = Math.min(TANK_CAPACITY - coolant, resource.getAmount());
            if (accepted > 0 && action.execute()) {
                coolant += accepted;
                setChanged();
            }
            return accepted;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    };

    public ReactorCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REACTOR_CORE.get(), pos, state);
    }

    public IFluidHandler getFluidHandler() {
        return coolantHandler;
    }

    /** Comparator output: core temperature scaled to 0-15. */
    public int getAnalogSignal() {
        return Mth.clamp((int) (temp / MELT_TEMP * 15f), 0, 15);
    }

    // ------------------------------------------------------------ kinetics

    @Override
    public float getGeneratedSpeed() {
        return generating ? SPEED : 0f;
    }

    @Override
    public float calculateAddedStressCapacity() {
        float capacity = generating ? outputCapacity : 0f;
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
        long time = level.getGameTime();
        if (!scanned || time % 40 == 0) {
            refreshStructure();
        }
        if (time % 10 == 0) {
            reactorStep();
        }
    }

    private void refreshStructure() {
        structure = ReactorStructure.scan(level, worldPosition);
        if (structure.valid) {
            loadedChannels.removeIf(packed -> {
                BlockPos channel = BlockPos.of(packed);
                return !structure.containsInterior(channel)
                        || !level.getBlockState(channel).is(ModBlocks.FUEL_CHANNEL.get());
            });
            rods = loadedChannels.size();
            // Keep the physical channel state synchronized with the reactor inventory.
            for (long packed : new HashSet<>(loadedChannels)) {
                BlockPos channel = BlockPos.of(packed);
                BlockState state = level.getBlockState(channel);
                if (!state.getValue(com.createatomic.block.FuelChannelBlock.LOADED)) {
                    level.setBlock(channel, state.setValue(com.createatomic.block.FuelChannelBlock.LOADED, true), 3);
                }
            }
            for (int y = structure.min.getY(); y <= structure.max.getY(); y++) {
                for (int x = structure.min.getX(); x <= structure.max.getX(); x++) {
                    for (int z = structure.min.getZ(); z <= structure.max.getZ(); z++) {
                        BlockPos channel = new BlockPos(x, y, z);
                        BlockState state = level.getBlockState(channel);
                        if (state.is(ModBlocks.FUEL_CHANNEL.get())
                                && state.getValue(com.createatomic.block.FuelChannelBlock.LOADED)
                                && !loadedChannels.contains(channel.asLong())) {
                            level.setBlock(channel, state.setValue(com.createatomic.block.FuelChannelBlock.LOADED, false), 3);
                        }
                    }
                }
            }
        }
        scanned = true;
        viewValid = structure.valid;
        viewRbmk = structure.rbmk;
        viewFuel = structure.fuel;
        viewWidth = structure.width;
        viewHeight = structure.height;
        viewDepth = structure.depth;
        viewError = structure.error;
        viewErrorArgs = new int[structure.errorArgs.length];
        for (int i = 0; i < structure.errorArgs.length; i++) {
            viewErrorArgs[i] = structure.errorArgs[i] instanceof Integer value ? value : 0;
        }
    }

    private void reactorStep() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean valid = structure.valid;
        boolean rbmk = valid && structure.rbmk;

        int signal = level.getBestNeighborSignal(worldPosition);
        float target = (valid && rods > 0 && structure.control > 0) ? signal / 15f : 0f;
        if (temp >= SCRAM_TEMP) {
            target = 0f;
        }
        if (target == 0f && prevTarget > 0f && rbmk && rodPos > 0.65f) {
            spike = 1.15f;
        }
        prevTarget = target;
        if (target > rodPos) {
            rodPos = Math.min(target, rodPos + 0.025f);
        } else if (target < rodPos) {
            rodPos = Math.max(target, rodPos - (target == 0f ? 0.14f : 0.06f));
        }

        float controlCoverage = valid ? Mth.clamp(structure.control / (float) Math.max(1, rods), 0.25f, 1f) : 0.25f;
        float bonus = valid ? 1f + (rbmk ? 0.35f : 0.08f) : 0f;
        float coolantFraction = coolant / (float) TANK_CAPACITY;
        float feedback;
        if (rbmk) {
            feedback = coolantFraction < 0.35f
                    ? 1f + 0.005f * Math.max(0f, temp - 260f)
                    : Mth.clamp(1f - 0.00055f * (temp - 260f), 0.40f, 1.2f);
        } else {
            feedback = Mth.clamp(1f - 0.0014f * (temp - 300f), 0.25f, 1.15f);
        }

        float targetPower = rods > 0 ? Mth.clamp(rodPos * bonus * feedback * controlCoverage + spike, 0f, 3.5f) : 0f;
        power += (targetPower - power) * 0.33f;
        spike *= 0.78f;
        if (spike < 0.01f) {
            spike = 0f;
        }

        if (power > 0.01f && rods > 0) {
            if (burnLeft <= 0f) {
                burnLeft = FUEL_UNITS;
                hotRods = rods;
            }
            burnLeft -= power;
            if (burnLeft <= 0f) {
                spent += rods;
                rods = 0;
                loadedChannels.clear();
                hotRods = Math.max(hotRods, spent);
                burnLeft = 0f;
                notifyUpdate();
            }
        }

        decay = Math.max(decay * 0.9975f, power * 0.14f);
        if (decay < 0.002f && power < 0.01f) {
            decay = 0f;
            hotRods = Math.max(hotRods, spent);
        }

        float thermal = (power * Math.max(1, rods) + decay * Math.max(1, hotRods)) * 11f * (rbmk ? 1.25f : 1f);
        float wanted = thermal * 0.94f + Math.max(0f, temp - 280f) * 0.65f;
        float removed = Math.min(wanted, coolant * HEAT_PER_MB);
        int consumed = Mth.ceil(removed / HEAT_PER_MB);
        coolant = Math.max(0, coolant - consumed);
        steam = removed / HEAT_PER_MB;
        temp += (thermal - removed) * 0.28f - (temp - 20f) * 0.0025f;
        if (temp < 20f) {
            temp = 20f;
        }

        float capacity = steam * SU_PER_STEAM * (rbmk ? 1.35f : 1f);
        boolean nowGenerating = steam > 1f;
        if (nowGenerating != generating || Math.abs(capacity - outputCapacity) > Math.max(24f, outputCapacity * 0.03f)) {
            generating = nowGenerating;
            outputCapacity = capacity;
            updateGeneratedRotation();
        }

        if (!valid) {
            power *= 0.92f;
            rodPos = Math.max(0f, rodPos - 0.08f);
        }

        emitRadiation();
        spawnEffects(serverLevel);
        if (temp >= MELT_TEMP) {
            meltdown(serverLevel);
            return;
        }
        if (level.getGameTime() % 20 == 0) {
            sendData();
        }
        setChanged();
    }

    private void emitRadiation() {
        Vec3 source = structure.valid ? structure.center : Vec3.atCenterOf(worldPosition);
        double hot = power * Math.max(1, rods) + decay * Math.max(1, hotRods);
        double multiplier = structure.valid && structure.rbmk ? 1.45 : 1.0;
        double gamma = (0.012 * rods + 0.009 * spent + 0.10 * hot) * multiplier;
        double neutron = 0.035 * power * Math.max(1, rods) * multiplier;
        if (gamma < 1.0e-6 && neutron < 1.0e-6) {
            return;
        }
        Radiation.emit(level, worldPosition, source, new double[] {0.0, 0.0, gamma, neutron}, 60, false);
    }

    private void spawnEffects(ServerLevel serverLevel) {
        if (!structure.valid) {
            return;
        }
        RandomSource random = serverLevel.random;
        double centerX = structure.center.x;
        double centerZ = structure.center.z;
        double topY = structure.max.getY() + 1.5;
        int steamCount = steam > 5f ? 1 + (int) Math.min(8f, steam / 24f) : 0;
        for (int i = 0; i < steamCount; i++) {
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    centerX + (random.nextDouble() - 0.5) * structure.width * 0.7, topY,
                    centerZ + (random.nextDouble() - 0.5) * structure.depth * 0.7,
                    0, 0, 0.10, 0, 1.0);
        }
        if (temp > 420f) {
            int smoke = temp > 700f ? 6 : 2;
            for (int i = 0; i < smoke; i++) {
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, centerX + (random.nextDouble() - 0.5) * 4.0,
                        topY + random.nextDouble() * 2.0, centerZ + (random.nextDouble() - 0.5) * 4.0,
                        0, (random.nextDouble() - 0.5) * 0.03, 0.12, (random.nextDouble() - 0.5) * 0.03, 1.0);
            }
        }
        if (temp > 850f) {
            serverLevel.sendParticles(ParticleTypes.FLAME, centerX, topY, centerZ, 2, 0.8, 0.2, 0.8, 0.02);
        }
    }

    private void meltdown(ServerLevel serverLevel) {
        BlockPos pos = worldPosition;
        Vec3 center = structure.valid ? structure.center : Vec3.atCenterOf(pos);
        int fuelBefore = Math.max(1, rods + spent);
        boolean rbmk = structure.valid && structure.rbmk;

        double severity = Math.min(5.0, 1.0 + fuelBefore * 0.18 + (rbmk ? 0.55 : 0.0));
        Radiation.emit(serverLevel, pos, center,
                new double[] {0.15 * severity, 0.55 * severity, 140.0 * severity, 8.0 * severity},
                24_192_000, true);

        rods = 0;
        spent = 0;
        hotRods = 0;
        loadedChannels.clear();
        burnLeft = 0f;
        coolant = 0;
        power = 0f;
        decay = 0f;
        steam = 0f;
        generating = false;
        outputCapacity = 0f;

        serverLevel.removeBlock(pos, false);
        float blast = Math.min(24f, 17.5f + fuelBefore * 0.42f + (rbmk ? 1.5f : 0f));
        serverLevel.explode(null, center.x, center.y, center.z, blast, Level.ExplosionInteraction.BLOCK);

        BlockPos origin = BlockPos.containing(center);
        RandomSource random = serverLevel.random;
        scorchCrater(serverLevel, origin, random);
        createCorium(serverLevel, origin, fuelBefore, random);
        igniteNearCrater(serverLevel, origin, random);
        contaminateZone(serverLevel, origin, random);
        falloutPatches(serverLevel, origin, random);

        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y + 5, center.z,
                120, 3.2, 6.0, 3.2, 0.16);
        serverLevel.sendParticles(ParticleTypes.ASH, center.x, center.y + 3, center.z,
                60, 4.0, 2.0, 4.0, 0.04);
        serverLevel.playSound(null, origin, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0f, 0.45f);
    }

    private void scorchCrater(ServerLevel level, BlockPos origin, RandomSource random) {
        int radius = 30;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                BlockPos probe = new BlockPos(x, origin.getY(), z);
                if (!level.isLoaded(probe)) continue;
                int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                for (int d = 0; d < 4; d++) {
                    BlockPos p = new BlockPos(x, surfaceY - d, z);
                    BlockState state = level.getBlockState(p);
                    if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.PODZOL)
                            || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)) {
                        level.setBlockAndUpdate(p, ModBlocks.SCORCHED_EARTH.get().defaultBlockState());
                    } else if (state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.GRAVEL)
                            || state.is(Blocks.ANDESITE) || state.is(Blocks.DIORITE) || state.is(Blocks.GRANITE)) {
                        if (d < 2 || random.nextInt(3) == 0) {
                            level.setBlockAndUpdate(p, ModBlocks.CHARRED_STONE.get().defaultBlockState());
                        }
                    }
                }
                for (int dy = 1; dy <= 12; dy++) {
                    BlockPos foliage = new BlockPos(x, surfaceY + dy, z);
                    if (level.getBlockState(foliage).is(net.minecraft.tags.BlockTags.LEAVES)) {
                        level.setBlockAndUpdate(foliage, ModBlocks.IRRADIATED_LEAVES.get().defaultBlockState());
                    }
                }
            }
        }
    }

    private void createCorium(ServerLevel level, BlockPos origin, int fuelBefore, RandomSource random) {
        int count = Mth.clamp(8 + fuelBefore * 2, 8, 36);
        int radius = 5 + Math.min(7, fuelBefore / 3);
        int placed = 0;
        for (int i = 0; i < count * 3 && placed < count; i++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            if (dx * dx + dz * dz > radius * radius) continue;
            int x = origin.getX() + dx;
            int z = origin.getZ() + dz;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            BlockPos ground = new BlockPos(x, y, z);
            BlockState groundState = level.getBlockState(ground);
            if (!level.isLoaded(ground) || groundState.isAir() || !groundState.isSolid()) continue;
            BlockPos pool = ground.above();
            if (level.getBlockState(pool).isAir()) {
                placeCorium(level, pool);
                placed++;
            }
        }
        for (int stream = 0; stream < Math.min(6, 2 + fuelBefore / 3); stream++) {
            int x = origin.getX() + random.nextInt(9) - 4;
            int z = origin.getZ() + random.nextInt(9) - 4;
            for (int step = 0; step < 5; step++) {
                if ((x - origin.getX()) * (x - origin.getX()) + (z - origin.getZ()) * (z - origin.getZ()) > 144) break;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                BlockPos p = new BlockPos(x, y + 1, z);
                if (level.isLoaded(p) && level.getBlockState(p).isAir()) placeCorium(level, p);
                x = Mth.clamp(x + random.nextInt(3) - 1, origin.getX() - 12, origin.getX() + 12);
                z = Mth.clamp(z + random.nextInt(3) - 1, origin.getZ() - 12, origin.getZ() + 12);
            }
        }
    }

    private void placeCorium(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir()) return;
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (belowState.is(Blocks.GRASS_BLOCK) || belowState.is(Blocks.DIRT) || belowState.is(Blocks.COARSE_DIRT)
                || belowState.is(Blocks.PODZOL) || belowState.is(Blocks.ROOTED_DIRT)
                || belowState.is(ModBlocks.IRRADIATED_SOIL.get())) {
            level.setBlockAndUpdate(below, ModBlocks.SCORCHED_EARTH.get().defaultBlockState());
        } else if (belowState.is(Blocks.STONE) || belowState.is(Blocks.DEEPSLATE)) {
            level.setBlockAndUpdate(below, ModBlocks.CHARRED_STONE.get().defaultBlockState());
        }
        level.setBlockAndUpdate(pos, ModBlocks.CORIUM.get().defaultBlockState());
    }

    private void igniteNearCrater(ServerLevel level, BlockPos origin, RandomSource random) {
        for (int i = 0; i < 42; i++) {
            int dx = random.nextInt(35) - 17;
            int dz = random.nextInt(35) - 17;
            if (dx * dx + dz * dz > 18 * 18) continue;
            int x = origin.getX() + dx;
            int z = origin.getZ() + dz;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            BlockPos ground = new BlockPos(x, y, z);
            BlockPos fire = ground.above();
            if (level.isLoaded(fire) && level.getBlockState(ground).isSolid() && level.getBlockState(fire).isAir()) {
                level.setBlockAndUpdate(fire, Blocks.FIRE.defaultBlockState());
            }
        }
    }

    private void contaminateZone(ServerLevel level, BlockPos origin, RandomSource random) {
        final int radius = 150;
        final int samples = 7500;
        for (int i = 0; i < samples; i++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            int distanceSq = dx * dx + dz * dz;
            if (distanceSq > radius * radius || distanceSq < 28 * 28) continue;
            int x = origin.getX() + dx;
            int z = origin.getZ() + dz;
            BlockPos probe = new BlockPos(x, origin.getY(), z);
            if (!level.isLoaded(probe)) continue;
            double distance = Math.sqrt(distanceSq);
            double density = 0.78 - (distance / radius) * 0.52;
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            BlockPos soil = new BlockPos(x, surfaceY, z);
            BlockState soilState = level.getBlockState(soil);
            if ((soilState.is(Blocks.GRASS_BLOCK) || soilState.is(Blocks.DIRT) || soilState.is(Blocks.PODZOL)
                    || soilState.is(Blocks.COARSE_DIRT) || soilState.is(Blocks.ROOTED_DIRT)) && random.nextDouble() < density) {
                level.setBlockAndUpdate(soil, ModBlocks.IRRADIATED_SOIL.get().defaultBlockState());
            }
            double leafChance = Mth.clamp(density + 0.12, 0.15, 0.92);
            for (int dy = 1; dy <= 14; dy++) {
                BlockPos foliage = new BlockPos(x, surfaceY + dy, z);
                BlockState foliageState = level.getBlockState(foliage);
                if (foliageState.is(net.minecraft.tags.BlockTags.LEAVES) && random.nextDouble() < leafChance) {
                    level.setBlockAndUpdate(foliage, ModBlocks.IRRADIATED_LEAVES.get().defaultBlockState());
                } else if ((foliageState.is(Blocks.SHORT_GRASS) || foliageState.is(Blocks.FERN)
                        || foliageState.is(Blocks.TALL_GRASS) || foliageState.is(Blocks.DEAD_BUSH)) && random.nextDouble() < density) {
                    level.setBlockAndUpdate(foliage, Blocks.DEAD_BUSH.defaultBlockState());
                }
            }
        }
    }

    private void falloutPatches(ServerLevel level, BlockPos origin, RandomSource random) {
        for (int i = 0; i < 32; i++) {
            int dx = random.nextInt(65) - 32;
            int dz = random.nextInt(65) - 32;
            if (dx * dx + dz * dz > 36 * 36) continue;
            int x = origin.getX() + dx;
            int z = origin.getZ() + dz;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            BlockPos patch = new BlockPos(x, y, z);
            if (level.isLoaded(patch) && (level.getBlockState(patch).is(ModBlocks.SCORCHED_EARTH.get())
                    || level.getBlockState(patch).is(ModBlocks.IRRADIATED_SOIL.get()))) {
                level.setBlockAndUpdate(patch, ModBlocks.RADIOACTIVE_DEBRIS.get().defaultBlockState());
            }
        }
    }

    // ------------------------------------------------------------ player actions

    public boolean canInteractFromInside(Player player, BlockPos component) {
        // Interactions can happen immediately after the player finishes the multiblock.
        // Refresh here so the 40-tick scan interval does not make the interior look unusable.
        if (!structure.valid) {
            refreshStructure();
        }
        if (!structure.valid || level == null) return false;
        return structure.containsInterior(player.blockPosition())
                && (structure.containsInterior(component) || player.distanceToSqr(component.getX() + 0.5,
                        component.getY() + 0.5, component.getZ() + 0.5) < 12.25);
    }

    public void insertRodIntoChannel(Player player, ItemStack stack, BlockPos channel) {
        refreshStructure();
        if (!structure.valid || !structure.containsInterior(channel)
                || !level.getBlockState(channel).is(ModBlocks.FUEL_CHANNEL.get())) {
            sendInvalid(player);
            return;
        }
        if (!canInteractFromInside(player, channel)) {
            player.displayClientMessage(Component.translatable("message.createatomic.inside_only"), true);
            return;
        }
        if (burnLeft > 0f || power > 0.01f || decay > 0.01f) {
            player.displayClientMessage(Component.translatable("message.createatomic.busy"), true);
            return;
        }
        if (loadedChannels.contains(channel.asLong())) {
            player.displayClientMessage(Component.translatable("message.createatomic.channel_loaded"), true);
            return;
        }
        loadedChannels.add(channel.asLong());
        rods = loadedChannels.size();
        BlockState channelState = level.getBlockState(channel);
        if (channelState.is(ModBlocks.FUEL_CHANNEL.get())) {
            level.setBlock(channel, channelState.setValue(com.createatomic.block.FuelChannelBlock.LOADED, true), 3);
        }
        if (!player.isCreative()) stack.shrink(1);
        level.playSound(null, channel, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.8f, 0.8f);
        notifyUpdate();
        status(player);
    }

    public void removeRodFromChannel(Player player, BlockPos channel) {
        refreshStructure();
        if (!structure.valid || !loadedChannels.contains(channel.asLong()) || !canInteractFromInside(player, channel)) return;
        if (burnLeft > 0f || power > 0.01f || decay > 0.01f || temp > 120f) {
            player.displayClientMessage(Component.translatable("message.createatomic.too_hot"), true);
            return;
        }
        loadedChannels.remove(channel.asLong());
        rods = loadedChannels.size();
        BlockState channelState = level.getBlockState(channel);
        if (channelState.is(ModBlocks.FUEL_CHANNEL.get())) {
            level.setBlock(channel, channelState.setValue(com.createatomic.block.FuelChannelBlock.LOADED, false), 3);
        }
        ItemStack fuel = new ItemStack(ModItems.FUEL_ROD.get());
        if (!player.getInventory().add(fuel)) player.drop(fuel, false);
        notifyUpdate();
        status(player);
    }

    public void addWaterFromInside(Player player, InteractionHand hand) {
        refreshStructure();
        if (!structure.valid || !structure.containsInterior(player.blockPosition())) {
            player.displayClientMessage(Component.translatable("message.createatomic.inside_only"), true);
            return;
        }
        addWater(player, hand);
    }

    public void addWaterFromExternal(Player player, InteractionHand hand) {
        addWater(player, hand);
    }

    public void addBucket(Player player, InteractionHand hand) {
        addWaterFromExternal(player, hand);
    }

    private void addWater(Player player, InteractionHand hand) {
        if (coolant + 1000 > TANK_CAPACITY) {
            player.displayClientMessage(Component.translatable("message.createatomic.tank_full"), true);
            return;
        }
        coolant += 1000;
        if (!player.isCreative()) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
        level.playSound(null, player.blockPosition(), SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1f, 1f);
        setChanged();
        status(player);
    }

    public void extract(Player player) {
        refreshStructure();
        if (power > 0.01f || decay > 0.002f || temp > 120f) {
            player.displayClientMessage(Component.translatable("message.createatomic.too_hot"), true);
            return;
        }
        if (rods == 0 && spent == 0) {
            status(player);
            return;
        }
        for (ItemStack stack : takeOutputs()) {
            if (!stack.isEmpty() && !player.getInventory().add(stack)) player.drop(stack, false);
        }
        notifyUpdate();
        status(player);
    }

    public void status(Player player) {
        refreshStructure();
        if (!structure.valid) {
            sendInvalid(player);
            return;
        }
        Component type = Component.translatable(structure.rbmk ? "message.createatomic.type_rbmk" : "message.createatomic.type_pwr");
        int fuelPercent = burnLeft > 0f ? Math.round(burnLeft / FUEL_UNITS * 100f) : 100;
        player.displayClientMessage(Component.translatable("message.createatomic.status", type, rods, structure.fuel,
                Math.round(power * 100f), Math.round(temp), String.format(Locale.ROOT, "%.1f", coolant / 1000f),
                Math.round(rodPos * 100f), fuelPercent), true);
    }

    private void sendInvalid(Player player) {
        player.displayClientMessage(Component.translatable("message.createatomic.invalid",
                Component.translatable("message.createatomic." + structure.error, structure.errorArgs)), true);
    }

    public void dropContents() {
        if (level == null || level.isClientSide) return;
        for (ItemStack stack : takeOutputs()) {
            if (!stack.isEmpty()) Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5, stack);
        }
    }

    private ItemStack[] takeOutputs() {
        int fuel = burnLeft > 0f ? 0 : rods;
        int waste = spent + (burnLeft > 0f ? rods : 0);
        rods = 0;
        spent = 0;
        hotRods = 0;
        burnLeft = 0f;
        loadedChannels.clear();
        return new ItemStack[] {new ItemStack(ModItems.FUEL_ROD.get(), fuel), new ItemStack(ModItems.SPENT_FUEL_ROD.get(), waste)};
    }

    // ------------------------------------------------------------ goggles

    private static final String PAD = "    ";

    /** Shown while the player wears Create's Engineer's Goggles and looks at the controller. */
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        tooltip.add(Component.literal(PAD).append(
                Component.translatable("createatomic.goggles.header").withStyle(ChatFormatting.GOLD)));

        if (!viewValid) {
            tooltip.add(gogglesLine("createatomic.goggles.structure",
                    Component.translatable("createatomic.goggles.invalid").withStyle(ChatFormatting.RED)));
            Object[] args = new Object[viewErrorArgs.length];
            for (int i = 0; i < args.length; i++) {
                args[i] = viewErrorArgs[i];
            }
            tooltip.add(Component.literal(PAD + PAD).append(
                    Component.translatable("message.createatomic." + viewError, args)
                            .withStyle(ChatFormatting.DARK_RED)));
            return true;
        }

        Component type = Component.translatable(viewRbmk
                ? "message.createatomic.type_rbmk" : "message.createatomic.type_pwr");
        tooltip.add(gogglesLine("createatomic.goggles.type", type.copy().withStyle(ChatFormatting.AQUA)));
        tooltip.add(gogglesLine("createatomic.goggles.size", Component.literal(viewWidth + "x" + viewHeight + "x" + viewDepth)
                .withStyle(ChatFormatting.GRAY)));

        ChatFormatting tempColor = temp >= SCRAM_TEMP ? ChatFormatting.RED
                : temp >= 450f ? ChatFormatting.YELLOW : ChatFormatting.GREEN;
        tooltip.add(gogglesLine("createatomic.goggles.temperature",
                Component.literal(Math.round(temp) + " \u00b0C").withStyle(tempColor)));
        tooltip.add(gogglesLine("createatomic.goggles.power",
                Component.literal(Math.round(power * 100f) + "%").withStyle(ChatFormatting.AQUA)));
        tooltip.add(gogglesLine("createatomic.goggles.control",
                Component.literal(Math.round(rodPos * 100f) + "%").withStyle(ChatFormatting.AQUA)));

        int fuelPercent = burnLeft > 0f ? Math.round(burnLeft / FUEL_UNITS * 100f) : 100;
        tooltip.add(gogglesLine("createatomic.goggles.fuel",
                Component.literal(rods + "/" + viewFuel + " (" + fuelPercent + "%)").withStyle(ChatFormatting.AQUA)));
        if (spent > 0) {
            tooltip.add(gogglesLine("createatomic.goggles.spent",
                    Component.literal(String.valueOf(spent)).withStyle(ChatFormatting.GOLD)));
        }

        boolean lowCoolant = coolant < TANK_CAPACITY * 0.15f;
        tooltip.add(gogglesLine("createatomic.goggles.coolant",
                Component.literal(String.format(Locale.ROOT, "%.1f / %.0f B", coolant / 1000f, TANK_CAPACITY / 1000f))
                        .withStyle(lowCoolant ? ChatFormatting.RED : ChatFormatting.AQUA)));

        if (temp >= SCRAM_TEMP) {
            tooltip.add(Component.literal(PAD).append(
                    Component.translatable("createatomic.goggles.scram").withStyle(ChatFormatting.RED)));
        } else if (lowCoolant && (power > 0.01f || decay > 0.002f)) {
            tooltip.add(Component.literal(PAD).append(
                    Component.translatable("createatomic.goggles.low_coolant").withStyle(ChatFormatting.RED)));
        }
        return true;
    }

    private static Component gogglesLine(String labelKey, Component value) {
        return Component.literal(PAD)
                .append(Component.translatable(labelKey).withStyle(ChatFormatting.GRAY))
                .append(": ")
                .append(value);
    }

    // ------------------------------------------------------------ persistence

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Rods", rods);
        tag.putInt("Spent", spent);
        tag.putInt("HotRods", hotRods);
        tag.putInt("Coolant", coolant);
        tag.putFloat("BurnLeft", burnLeft);
        tag.putFloat("Temp", temp);
        tag.putFloat("Power", power);
        tag.putFloat("Decay", decay);
        tag.putFloat("RodPos", rodPos);
        tag.putFloat("Spike", spike);
        tag.putFloat("Steam", steam);
        tag.putFloat("OutputCapacity", outputCapacity);
        tag.putBoolean("Generating", generating);
        long[] channels = new long[loadedChannels.size()];
        int channelIndex = 0;
        for (long channel : loadedChannels) channels[channelIndex++] = channel;
        tag.putLongArray("LoadedChannels", channels);
        tag.putBoolean("StructValid", structure.valid);
        tag.putBoolean("StructRbmk", structure.rbmk);
        tag.putInt("StructFuel", structure.fuel);
        tag.putInt("StructWidth", structure.width);
        tag.putInt("StructHeight", structure.height);
        tag.putInt("StructDepth", structure.depth);
        tag.putString("StructError", structure.error);
        int[] errorArgs = new int[structure.errorArgs.length];
        for (int i = 0; i < errorArgs.length; i++) {
            errorArgs[i] = structure.errorArgs[i] instanceof Integer value ? value : 0;
        }
        tag.putIntArray("StructErrorArgs", errorArgs);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        rods = tag.getInt("Rods");
        spent = tag.getInt("Spent");
        hotRods = tag.getInt("HotRods");
        coolant = tag.getInt("Coolant");
        burnLeft = tag.getFloat("BurnLeft");
        temp = tag.contains("Temp") ? tag.getFloat("Temp") : 20f;
        power = tag.getFloat("Power");
        decay = tag.getFloat("Decay");
        rodPos = tag.getFloat("RodPos");
        spike = tag.getFloat("Spike");
        steam = tag.getFloat("Steam");
        outputCapacity = tag.getFloat("OutputCapacity");
        generating = tag.getBoolean("Generating");
        loadedChannels.clear();
        for (long channel : tag.getLongArray("LoadedChannels")) loadedChannels.add(channel);
        viewValid = tag.getBoolean("StructValid");
        viewRbmk = tag.getBoolean("StructRbmk");
        viewFuel = tag.getInt("StructFuel");
        viewWidth = tag.getInt("StructWidth");
        viewHeight = tag.getInt("StructHeight");
        viewDepth = tag.getInt("StructDepth");
        viewError = tag.contains("StructError") ? tag.getString("StructError") : "err_interior";
        viewErrorArgs = tag.getIntArray("StructErrorArgs");
        scanned = false;
    }
}
