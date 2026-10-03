package com.createatomic.client.ponder;

import com.createatomic.CreateAtomic;
import com.createatomic.registry.ModItems;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/**
 * Create's built-in guide (Ponder). Hold W while pointing at one of the items below (inventory, JEI, creative tab)
 * to open the scenes, exactly like for Create's own blocks.
 */
public class AtomicPonderPlugin implements PonderPlugin {

    @Override
    public String getModId() {
        return CreateAtomic.MODID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        // Scene 1: how to build the multiblock
        helper.forComponents(
                        CreateAtomic.id("reactor_core"),
                        CreateAtomic.id("reactor_casing"),
                        CreateAtomic.id("fuel_channel"),
                        CreateAtomic.id("control_rod"),
                        CreateAtomic.id("graphite_block"))
                .addStoryBoard("reactor/structure", AtomicScenes::structure, AtomicPonderTags.ATOMIC);

        // Scene 2: how to run it
        helper.forComponents(
                        CreateAtomic.id("reactor_core"),
                        CreateAtomic.id("radicia_fuel_rod"),
                        CreateAtomic.id("spent_fuel_rod"))
                .addStoryBoard("reactor/structure", AtomicScenes::operation, AtomicPonderTags.ATOMIC);

        // Scene 3: radiation and shielding
        helper.forComponents(
                        CreateAtomic.id("geiger_counter"),
                        CreateAtomic.id("hazmat_helmet"),
                        CreateAtomic.id("hazmat_chestplate"),
                        CreateAtomic.id("hazmat_leggings"),
                        CreateAtomic.id("hazmat_boots"),
                        CreateAtomic.id("anti_rad_pills"),
                        CreateAtomic.id("reinforced_concrete"),
                        CreateAtomic.id("borated_concrete"),
                        CreateAtomic.id("radicia_block"),
                        CreateAtomic.id("radicia_ore"),
                        CreateAtomic.id("deepslate_radicia_ore"))
                .addStoryBoard("radiation/shielding", AtomicScenes::shielding, AtomicPonderTags.ATOMIC);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(AtomicPonderTags.ATOMIC)
                .addToIndex()
                .item(ModItems.REACTOR_CORE_ITEM.get(), true, false)
                .title("Atomic Energy")
                .description("Nuclear reactors, radiation and the equipment to survive them")
                .register();

        helper.addToTag(AtomicPonderTags.ATOMIC)
                .add(CreateAtomic.id("reactor_core"))
                .add(CreateAtomic.id("reactor_casing"))
                .add(CreateAtomic.id("fuel_channel"))
                .add(CreateAtomic.id("control_rod"))
                .add(CreateAtomic.id("graphite_block"))
                .add(CreateAtomic.id("radicia_fuel_rod"))
                .add(CreateAtomic.id("geiger_counter"))
                .add(CreateAtomic.id("reinforced_concrete"))
                .add(CreateAtomic.id("borated_concrete"));
    }
}
