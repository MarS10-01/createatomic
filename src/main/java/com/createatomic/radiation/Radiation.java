package com.createatomic.radiation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import com.createatomic.item.RadioactiveItem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Radiation manager.
 *
 * Units: dose rates are in Sv/s. The player accumulates dose; the body recovers RECOVERY Sv/s.
 * Sources: radioactive blocks near the player (6 block scan), long-range emitters (reactors, meltdown bursts),
 * and radioactive items in the inventory.
 */
public final class Radiation {
    public static final String DOSE_KEY = "createatomic_dose";
    public static final double RECOVERY = 0.00008;
    private static final int SCAN = 10;

    private record Emitter(Vec3 pos, double[] strength, long start, long end) {}

    private static final class Store {
        final Map<Long, Emitter> continuous = new HashMap<>();
        final Map<Long, Emitter> bursts = new HashMap<>();
    }

    private static final Map<Level, Store> STORES = new WeakHashMap<>();

    private Radiation() {}

    // ------------------------------------------------------------ emitters

    /**
     * Registers a long-range emitter. Continuous emitters (reactors) must be refreshed regularly;
     * decaying ones (meltdown bursts) fade out linearly over durationTicks.
     */
    public static void emit(Level level, BlockPos key, Vec3 pos, double[] strength, int durationTicks, boolean decaying) {
        Store store = STORES.computeIfAbsent(level, l -> new Store());
        long now = level.getGameTime();
        Emitter emitter = new Emitter(pos, strength.clone(), now, now + durationTicks);
        (decaying ? store.bursts : store.continuous).put(key.asLong(), emitter);
    }

    // ------------------------------------------------------------ measuring

    /** Radiation at a point in the world, per type (Gy/s), ignoring armor. */
    public static double[] ambient(Level level, Vec3 point) {
        double[] out = new double[RadiationType.VALUES.length];
        BlockPos center = BlockPos.containing(point);
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();

        for (int dx = -SCAN; dx <= SCAN; dx++) {
            for (int dy = -SCAN; dy <= SCAN; dy++) {
                for (int dz = -SCAN; dz <= SCAN; dz++) {
                    mp.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (!level.isLoaded(mp)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(mp);
                    if (state.getBlock() instanceof IRadioactive source) {
                        addSource(level, point, Vec3.atCenterOf(mp), mp.immutable(), source.emission(state), out, 1.0);
                    }
                }
            }
        }

        Store store = STORES.get(level);
        if (store != null) {
            long now = level.getGameTime();
            processEmitters(store.continuous, level, point, now, false, out);
            processEmitters(store.bursts, level, point, now, true, out);
        }
        return out;
    }

    private static void processEmitters(Map<Long, Emitter> map, Level level, Vec3 point, long now,
                                        boolean decaying, double[] out) {
        Iterator<Map.Entry<Long, Emitter>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Emitter e = it.next().getValue();
            if (now > e.end()) {
                it.remove();
                continue;
            }
            double scale = 1.0;
            if (decaying) {
                scale = Math.max(0.0, (double) (e.end() - now) / Math.max(1L, e.end() - e.start()));
            }
            addSource(level, point, e.pos(), BlockPos.containing(e.pos()), e.strength(), out, scale);
        }
    }

    private static void addSource(Level level, Vec3 target, Vec3 sourcePos, BlockPos sourceBlock,
                                  double[] emission, double[] out, double scale) {
        double dist = Math.max(1.0, sourcePos.distanceTo(target));
        for (RadiationType type : RadiationType.VALUES) {
            double e = emission[type.ordinal()] * scale;
            if (e <= 0.0 || dist > type.range) {
                continue;
            }
            double transmission = RadiationShielding.transmission(level, sourcePos, target, type, sourceBlock);
            if (transmission <= 0.0005) {
                continue;
            }
            out[type.ordinal()] += e / (dist * dist) * transmission;
        }
    }

    /** Radioactive items in the inventory irradiate their owner from point-blank range. */
    public static double[] carried(Player player) {
        double[] out = new double[RadiationType.VALUES.length];
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof RadioactiveItem item) {
                double factor = 1.0 * Math.sqrt(stack.getCount());
                for (RadiationType type : RadiationType.VALUES) {
                    out[type.ordinal()] += item.emission(type) * factor;
                }
            }
        }
        return out;
    }

    /** Everything that hits the player before armor. */
    public static double[] rawExposure(Player player) {
        double[] out = ambient(player.level(), player.getEyePosition());
        double[] carried = carried(player);
        for (int i = 0; i < out.length; i++) {
            out[i] += carried[i];
        }
        return out;
    }

    /** Applies the protection of worn armor. */
    public static double[] protect(Player player, double[] raw) {
        double[] out = new double[raw.length];
        for (RadiationType type : RadiationType.VALUES) {
            out[type.ordinal()] = raw[type.ordinal()] * (1.0 - HazmatProtection.fraction(player, type));
        }
        return out;
    }

    /** Effective dose rate (Sv/s) from per-type absorbed dose rates. */
    public static double effective(double[] rates) {
        double sum = 0.0;
        for (RadiationType type : RadiationType.VALUES) {
            sum += rates[type.ordinal()] * type.weight;
        }
        return sum;
    }

    // ------------------------------------------------------------ player dose

    public static float getDose(Player player) {
        return player.getPersistentData().getFloat(DOSE_KEY);
    }

    public static void setDose(Player player, float dose) {
        player.getPersistentData().putFloat(DOSE_KEY, Math.max(0f, dose));
    }
}
