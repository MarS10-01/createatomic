package com.createatomic.block.entity;

import java.util.List;
import java.util.Locale;

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
    public static final int TANK_CAPACITY = 16000;
    public static final float FUEL_UNITS = 1200f;
    public static final float HEAT_PER_MB = 0.5f;
    public static final float SCRAM_TEMP = 800f;
    public static final float MELT_TEMP = 1200f;
    public static final float SPEED = 64f;
    public static final float SU_PER_STEAM = 13.5f;

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

    private ReactorStructure structure = new ReactorStructure();
    private boolean scanned;

    // Structure info synced to the client (the multiblock scan only runs on the server). Used by the goggle tooltip.
    private boolean viewValid;
    private boolean viewRbmk;
    private int viewFuel;
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
        scanned = true;
    }

    private void reactorStep() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean valid = structure.valid;
        boolean rbmk = valid && structure.rbmk;

        // --- control rods
        int signal = level.getBestNeighborSignal(worldPosition);
        float target = (valid && rods > 0) ? signal / 15f : 0f;
        if (temp >= SCRAM_TEMP) {
            target = 0f; // automatic SCRAM
        }
        if (target == 0f && prevTarget > 0f && rbmk && rodPos > 0.6f) {
            spike = 0.9f; // graphite-tipped rods: SCRAM from high withdrawal gives a power spike
        }
        prevTarget = target;

        if (target > rodPos) {
            rodPos = Math.min(target, rodPos + 0.03f);
        } else if (target < rodPos) {
            rodPos = Math.max(target, rodPos - (target == 0f ? 0.12f : 0.06f));
        }

        // --- reactivity
        float bonus = 1f;
        float feedback;
        if (rbmk) {
            bonus = 1f + 0.25f * Math.min(3f, structure.graphite / (float) Math.max(1, structure.fuel));
            float coolantFraction = coolant / (float) TANK_CAPACITY;
            feedback = coolantFraction < 0.5f
                    ? 1f + 0.003f * Math.max(0f, temp - 280f)
                    : Mth.clamp(1f - 0.0004f * (temp - 280f), 0.5f, 1.2f);
        } else {
            feedback = Mth.clamp(1f - 0.0012f * (temp - 280f), 0.3f, 1.2f);
        }
        float targetPower = rods > 0 ? Mth.clamp(rodPos * bonus * feedback + spike, 0f, 3f) : 0f;
        power += (targetPower - power) * 0.35f;
        if (targetPower == 0f && power < 0.002f) {
            power = 0f;
        }
        spike *= 0.8f;
        if (spike < 0.01f) {
            spike = 0f;
        }

        // --- fuel burn-up
        if (power > 0.01f && rods > 0) {
            if (burnLeft <= 0f) {
                burnLeft = FUEL_UNITS;
                hotRods = rods;
            }
            burnLeft -= power;
            if (burnLeft <= 0f) {
                spent += rods;
                hotRods = Math.max(hotRods, rods);
                rods = 0;
                burnLeft = 0f;
                notifyUpdate();
            }
        }

        // --- decay heat keeps producing heat after shutdown
        decay = Math.max(decay * 0.9975f, power * 0.12f);
        if (decay < 0.002f && power < 0.01f) {
            decay = 0f;
            hotRods = rods;
        }

        // --- heat balance and cooling
        float multiplier = rbmk ? 1.2f : 1f;
        float thermal = (power * rods + decay * hotRods) * 10f * multiplier;
        float wanted = thermal * 0.95f + Math.max(0f, temp - 280f) * 0.6f;
        float removed = Math.min(wanted, coolant * HEAT_PER_MB);
        coolant = Math.max(0, coolant - Mth.ceil(removed / HEAT_PER_MB));
        steam = removed / HEAT_PER_MB;
        temp += (thermal - removed) * 0.3f - (temp - 20f) * 0.003f;
        if (temp < 20f) {
            temp = 20f;
        }

        // --- kinetic output from steam
        float capacity = steam * SU_PER_STEAM * (rbmk ? 1.25f : 1f);
        boolean nowGenerating = steam > 1f;
        if (nowGenerating != generating || Math.abs(capacity - outputCapacity) > Math.max(16f, outputCapacity * 0.03f)) {
            generating = nowGenerating;
            outputCapacity = capacity;
            updateGeneratedRotation();
        }

        emitRadiation();
        spawnEffects(serverLevel);

        if (temp >= MELT_TEMP) {
            meltdown(serverLevel);
            return;
        }
        if (level.getGameTime() % 20 == 0) {
            sendData(); // keeps the goggle tooltip up to date
        }
        setChanged();
    }

    private void emitRadiation() {
        Vec3 source = structure.valid ? structure.center : Vec3.atCenterOf(worldPosition);
        double multiplier = (structure.valid && structure.rbmk) ? 1.3 : 1.0;
        double hot = power * rods + decay * hotRods;
        double gamma = (0.004 * rods + 0.003 * spent + 0.06 * hot) * multiplier;
        double neutron = 0.02 * power * Math.max(1, rods) * multiplier;
        if (gamma < 1.0e-6 && neutron < 1.0e-6) {
            return;
        }
        Radiation.emit(level, worldPosition, source, new double[] {0.0, 0.0, gamma, neutron}, 40, false);
    }

    private void spawnEffects(ServerLevel serverLevel) {
        if (!structure.valid) {
            return;
        }
        RandomSource random = serverLevel.random;
        double minX = structure.min.getX();
        double minZ = structure.min.getZ();
        double sizeX = structure.max.getX() - minX + 1;
        double sizeZ = structure.max.getZ() - minZ + 1;
        double topY = structure.max.getY() + 2.2;

        // white steam plume
        int plume = steam > 5f ? 1 + (int) Math.min(6f, steam / 20f) : 0;
        for (int i = 0; i < plume; i++) {
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    minX + random.nextDouble() * sizeX, topY, minZ + random.nextDouble() * sizeZ,
                    0, 0.0, 0.12, 0.0, 1.0);
        }
        // dark smoke when overheating
        if (temp > 450f) {
            int smoke = temp > 700f ? 5 : 2;
            for (int i = 0; i < smoke; i++) {
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                        minX + random.nextDouble() * sizeX, topY, minZ + random.nextDouble() * sizeZ,
                        0, 0.0, 0.1, 0.0, 1.0);
            }
        }
        if (temp > 800f) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    minX + random.nextDouble() * sizeX, topY, minZ + random.nextDouble() * sizeZ,
                    0, 0.0, 0.1, 0.0, 1.0);
        }

        long time = serverLevel.getGameTime();
        if (steam > 20f && time % 20 == 0) {
            serverLevel.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4f, 0.6f);
        }
        if (temp > 600f && time % 40 == 0) {
            serverLevel.playSound(null, worldPosition, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0f, 0.5f);
        }
    }

    private void meltdown(ServerLevel serverLevel) {
        BlockPos pos = worldPosition;
        boolean rbmk = structure.valid && structure.rbmk;
        int size = Math.max(1, Math.max(rods, hotRods) + spent / 2);
        Vec3 center = structure.valid ? structure.center : Vec3.atCenterOf(pos);

        double k = Math.min(16.0, 1.0 + size * 0.35);
        // Long-lived exclusion zone: gamma reaches 160 blocks, neutron 150 blocks.
        Radiation.emit(serverLevel, pos, center, new double[] {0.02 * k, 0.15 * k, 45.0 * k, 2.5 * k},
                240000, true);

        // Consume all fuel/waste before the controller is destroyed. Fuel becomes molten corium.
        int coriumCount = Math.max(4, size * 3 + hotRods * 2);
        rods = 0;
        spent = 0;
        hotRods = 0;
        burnLeft = 0f;
        coolant = 0;
        power = 0f;
        decay = 0f;

        serverLevel.removeBlock(pos, false);
        float blast = rbmk ? Math.min(24f, 13f + size * 0.7f) : Math.min(20f, 10f + size * 0.55f);
        serverLevel.explode(null, center.x, center.y, center.z, blast, Level.ExplosionInteraction.BLOCK);

        RandomSource random = serverLevel.random;
        BlockPos origin = BlockPos.containing(center);

        // Dense corium field immediately around the reactor.
        int coriumPlaced = 0;
        int coriumRadius = Math.min(12, 5 + size / 2);
        for (int i = 0; i < coriumCount * 3 && coriumPlaced < coriumCount; i++) {
            int dx = random.nextInt(coriumRadius * 2 + 1) - coriumRadius;
            int dz = random.nextInt(coriumRadius * 2 + 1) - coriumRadius;
            if (dx * dx + dz * dz > coriumRadius * coriumRadius) continue;
            for (int dy = 8; dy >= -8; dy--) {
                BlockPos p = origin.offset(dx, dy, dz);
                if (serverLevel.getBlockState(p).isAir() && !serverLevel.getBlockState(p.below()).isAir()) {
                    serverLevel.setBlockAndUpdate(p, ModBlocks.CORIUM.get().defaultBlockState());
                    coriumPlaced++;
                    break;
                }
            }
        }

        // The whole 150-block area is visibly damaged without trying to rewrite every block.
        int zoneRadius = 150;
        int samples = Math.min(4500, 1200 + size * 80);
        for (int i = 0; i < samples; i++) {
            int dx = random.nextInt(zoneRadius * 2 + 1) - zoneRadius;
            int dz = random.nextInt(zoneRadius * 2 + 1) - zoneRadius;
            if (dx * dx + dz * dz > zoneRadius * zoneRadius) continue;
            int surfaceY = serverLevel.getHeight(Heightmap.Types.WORLD_SURFACE, origin.getX() + dx, origin.getZ() + dz);
            BlockPos column = new BlockPos(origin.getX() + dx, surfaceY, origin.getZ() + dz);
            for (int dy = 0; dy < 8; dy++) {
                BlockPos p = column.below(dy);
                BlockState state = serverLevel.getBlockState(p);
                if (state.isAir()) continue;
                BlockPos above = p.above();
                BlockState aboveState = serverLevel.getBlockState(above);
                if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.PODZOL)
                        || state.is(Blocks.COARSE_DIRT)) {
                    serverLevel.setBlockAndUpdate(p, Blocks.COARSE_DIRT.defaultBlockState());
                }
                if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT)) {
                    serverLevel.setBlockAndUpdate(p, Blocks.COARSE_DIRT.defaultBlockState());
                }
                if (state.is(Blocks.SHORT_GRASS) || state.is(Blocks.FERN) || state.is(Blocks.TALL_GRASS)) {
                    serverLevel.setBlockAndUpdate(p, Blocks.DEAD_BUSH.defaultBlockState());
                }
                if (state.getBlock().defaultBlockState().is(net.minecraft.tags.BlockTags.LEAVES)) {
                    serverLevel.setBlockAndUpdate(p, ModBlocks.IRRADIATED_LEAVES.get().defaultBlockState());
                }
                if (random.nextInt(18) == 0 && !aboveState.isAir()) {
                    break;
                }
                break;
            }
        }

        // Persistent hot spots at the surface.
        for (int i = 0; i < 180; i++) {
            int dx = random.nextInt(zoneRadius * 2 + 1) - zoneRadius;
            int dz = random.nextInt(zoneRadius * 2 + 1) - zoneRadius;
            if (dx * dx + dz * dz > zoneRadius * zoneRadius) continue;
            int surfaceY = serverLevel.getHeight(Heightmap.Types.WORLD_SURFACE, origin.getX() + dx, origin.getZ() + dz);
            BlockPos column = new BlockPos(origin.getX() + dx, surfaceY, origin.getZ() + dz);
            for (int dy = 0; dy < 8; dy++) {
                BlockPos p = column.below(dy);
                if (!serverLevel.getBlockState(p).isAir()) {
                    if (serverLevel.getBlockState(p).is(Blocks.GRASS_BLOCK)
                            || serverLevel.getBlockState(p).is(Blocks.DIRT)
                            || serverLevel.getBlockState(p).is(Blocks.COARSE_DIRT)) {
                        serverLevel.setBlockAndUpdate(p, ModBlocks.RADIOACTIVE_DEBRIS.get().defaultBlockState());
                    }
                    break;
                }
            }
        }
    }

    // ------------------------------------------------------------ player actions

    public void insertRod(Player player, ItemStack stack) {
        refreshStructure();
        if (!structure.valid) {
            sendInvalid(player);
            return;
        }
        if (burnLeft > 0f || power > 0.01f) {
            player.displayClientMessage(Component.translatable("message.createatomic.busy"), true);
            return;
        }
        if (rods >= structure.fuel) {
            player.displayClientMessage(Component.translatable("message.createatomic.full"), true);
            return;
        }
        rods++;
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        level.playSound(null, worldPosition, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1f, 1f);
        notifyUpdate();
        status(player);
    }

    public void addBucket(Player player, InteractionHand hand) {
        if (coolant + 1000 > TANK_CAPACITY) {
            player.displayClientMessage(Component.translatable("message.createatomic.tank_full"), true);
            return;
        }
        coolant += 1000;
        if (!player.isCreative()) {
            player.setItemInHand(hand, new ItemStack(Items.BUCKET));
        }
        level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1f, 1f);
        setChanged();
        status(player);
    }

    public void extract(Player player) {
        if (power > 0.01f || decay > 0.002f || temp > 150f) {
            player.displayClientMessage(Component.translatable("message.createatomic.too_hot"), true);
            return;
        }
        if (rods == 0 && spent == 0) {
            status(player);
            return;
        }
        for (ItemStack stack : takeOutputs()) {
            if (!stack.isEmpty() && !player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
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
        Component type = Component.translatable(structure.rbmk
                ? "message.createatomic.type_rbmk" : "message.createatomic.type_pwr");
        int fuelPercent = burnLeft > 0f ? Math.round(burnLeft / FUEL_UNITS * 100f) : 100;
        player.displayClientMessage(Component.translatable("message.createatomic.status",
                type, rods, structure.fuel, Math.round(power * 100f), Math.round(temp),
                String.format(Locale.ROOT, "%.1f", coolant / 1000f), Math.round(rodPos * 100f), fuelPercent), true);
    }

    private void sendInvalid(Player player) {
        player.displayClientMessage(Component.translatable("message.createatomic.invalid",
                Component.translatable("message.createatomic." + structure.error, structure.errorArgs)), true);
    }

    /** Called when the block is broken: spill whatever is inside. */
    public void dropContents() {
        if (level == null || level.isClientSide) {
            return;
        }
        for (ItemStack stack : takeOutputs()) {
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5, stack);
            }
        }
    }

    /** Empties the reactor. Partially burnt rods come back as spent rods. */
    private ItemStack[] takeOutputs() {
        int fuel = burnLeft > 0f ? 0 : rods;
        int waste = spent + (burnLeft > 0f ? rods : 0);
        rods = 0;
        spent = 0;
        burnLeft = 0f;
        hotRods = 0;
        return new ItemStack[] {
                new ItemStack(ModItems.FUEL_ROD.get(), fuel),
                new ItemStack(ModItems.SPENT_FUEL_ROD.get(), waste)
        };
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
        tag.putBoolean("StructValid", structure.valid);
        tag.putBoolean("StructRbmk", structure.rbmk);
        tag.putInt("StructFuel", structure.fuel);
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
        viewValid = tag.getBoolean("StructValid");
        viewRbmk = tag.getBoolean("StructRbmk");
        viewFuel = tag.getInt("StructFuel");
        viewError = tag.contains("StructError") ? tag.getString("StructError") : "err_interior";
        viewErrorArgs = tag.getIntArray("StructErrorArgs");
        scanned = false;
    }
}
