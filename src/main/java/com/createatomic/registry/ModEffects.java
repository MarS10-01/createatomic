package com.createatomic.registry;

import com.createatomic.CreateAtomic;
import com.createatomic.effect.RadiationEffect;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, CreateAtomic.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> RADIATION =
            EFFECTS.register("radiation", () -> new RadiationEffect());
}
