package com.createatomic.client.ponder;

import com.createatomic.registry.ModItems;
import com.simibubi.create.AllItems;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LeverBlock;

/**
 * Ponder scenes. Schematics live in assets/createatomic/ponder/ (reactor/structure.nbt, radiation/shielding.nbt).
 * Layout of reactor/structure.nbt: base plate at y=0, casing box x,z = 1..5, y = 1..5 (3x3x3 inside),
 * controller at 3,3,1 (axis z) with a shaft at 3,3,0, a lever at 2,3,0, fuel channels at the four
 * corners of the middle layer and one control rod in the centre.
 */
public class AtomicScenes {

    public static void structure(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("reactor_structure", "Building a Nuclear Reactor");
        scene.scaleSceneView(0.75f);
        scene.showBasePlate();
        scene.idle(10);

        // floor and walls (everything except the controller position 3,3,1)
        scene.world().showSection(util.select().fromTo(1, 1, 1, 5, 1, 5), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(1, 2, 5, 5, 4, 5), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(1, 2, 2, 1, 4, 4), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(5, 2, 2, 5, 4, 4), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(1, 2, 1, 5, 2, 1), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(1, 4, 1, 5, 4, 1), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(1, 3, 1, 2, 3, 1), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(4, 3, 1, 5, 3, 1), Direction.DOWN);
        scene.idle(15);

        scene.overlay().showText(80)
                .text("The walls of the chamber are built from Reactor Casing only")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().centerOf(1, 3, 3));
        scene.idle(90);

        // controller and shaft
        scene.world().showSection(util.select().position(3, 3, 1), Direction.SOUTH);
        scene.idle(5);
        scene.world().showSection(util.select().position(3, 3, 0), Direction.SOUTH);
        scene.idle(15);
        scene.overlay().showText(90)
                .text("Replace one wall block with the Reactor Controller. Its shaft side connects to your Create network")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().centerOf(3, 3, 1));
        scene.idle(100);

        // interior
        for (BlockPos fuel : new BlockPos[] {
                util.grid().at(2, 3, 2), util.grid().at(4, 3, 2), util.grid().at(2, 3, 4), util.grid().at(4, 3, 4) }) {
            scene.world().showSection(util.select().position(fuel), Direction.DOWN);
            scene.idle(4);
        }
        scene.overlay().showText(80)
                .text("Fuel Channels hold the fuel rods: one channel takes one Fuel Rod")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().centerOf(2, 3, 2));
        scene.idle(90);

        scene.world().showSection(util.select().position(3, 3, 3), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80)
                .text("Control Rods are required at a ratio of at least 1 per 4 Fuel Channels")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().centerOf(3, 3, 3));
        scene.idle(90);

        // roof
        scene.world().showSection(util.select().fromTo(1, 5, 1, 5, 5, 5), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(90)
                .text("Close the chamber completely, from 3x3x3 up to 7x7x7 inside. A gap or a foreign block makes the structure invalid")
                .attachKeyFrame()
                .colored(PonderPalette.RED)
                .placeNearTarget()
                .pointAt(util.vector().topOf(3, 5, 3));
        scene.idle(100);

        scene.overlay().showText(100)
                .text("Add Graphite Moderators, at least one per Fuel Channel, for a graphite-moderated RBMK reactor. Without graphite it is a water-moderated PWR")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().topOf(3, 5, 3));
        scene.idle(110);
        scene.markAsFinished();
    }

    public static void operation(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("reactor_operation", "Operating the Reactor");
        scene.scaleSceneView(0.75f);
        scene.showBasePlate();
        scene.world().showSection(util.select().fromTo(1, 1, 0, 5, 5, 5), Direction.DOWN);
        scene.idle(20);

        BlockPos lever = util.grid().at(2, 3, 0);

        scene.overlay().showControls(util.vector().topOf(3, 5, 3), Pointing.DOWN, 60)
                .rightClick()
                .withItem(new ItemStack(ModItems.FUEL_ROD.get()));
        scene.overlay().showText(70)
                .text("Right-click the controller with a Fuel Rod to load it. The reactor must be idle")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().centerOf(3, 3, 1));
        scene.idle(80);

        scene.overlay().showControls(util.vector().topOf(3, 5, 3), Pointing.DOWN, 60)
                .rightClick()
                .withItem(new ItemStack(Items.WATER_BUCKET));
        scene.overlay().showText(70)
                .text("Fill the coolant tank with water: use a Water Bucket or feed it with a Create pipe or pump")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().centerOf(3, 3, 1));
        scene.idle(80);

        scene.world().modifyBlock(lever, s -> s.setValue(LeverBlock.POWERED, true), false);
        scene.effects().indicateRedstone(lever);
        scene.overlay().showText(80)
                .text("A redstone signal sets how far the control rods are withdrawn. No signal means fully inserted: the reactor is off")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().centerOf(lever));
        scene.idle(90);

        scene.overlay().showText(80)
                .text("The coolant boils into steam that spins the shaft. The more water boils off, the more stress capacity you get")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().topOf(3, 3, 0));
        scene.idle(90);

        scene.overlay().showControls(util.vector().centerOf(3, 3, 1), Pointing.RIGHT, 60)
                .withItem(AllItems.GOGGLES.asStack());
        scene.overlay().showText(80)
                .text("Wear Engineer's Goggles and look at the controller to read temperature, power, coolant and fuel")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().centerOf(3, 3, 1));
        scene.idle(90);

        scene.overlay().showText(100)
                .text("Never cut the cooling: decay heat continues after shutdown. At 800 C the reactor scrams, at 1200 C it melts down")
                .attachKeyFrame()
                .colored(PonderPalette.RED)
                .placeNearTarget()
                .pointAt(util.vector().topOf(3, 5, 3));
        scene.idle(110);

        scene.overlay().showText(90)
                .text("A comparator reads the core temperature. Sneak and right-click with an empty hand to unload a cold reactor")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().centerOf(3, 3, 1));
        scene.idle(100);
        scene.markAsFinished();
    }

    public static void shielding(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("radiation_shielding", "Radiation and Shielding");
        scene.scaleSceneView(0.8f);
        scene.showBasePlate();
        scene.idle(10);

        scene.world().showSection(util.select().position(1, 1, 2), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(90)
                .text("Uranium ore, ingots and especially fuel emit alpha, beta, gamma and neutron radiation")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().topOf(1, 1, 2));
        scene.idle(100);

        scene.world().showSection(util.select().fromTo(4, 1, 0, 4, 3, 4), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80)
                .text("Reinforced Concrete and metal stop gamma rays")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().centerOf(4, 2, 2));
        scene.idle(90);

        scene.world().showSection(util.select().fromTo(5, 1, 0, 5, 3, 4), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80)
                .text("Neutrons need Borated Concrete or a lot of water")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().centerOf(5, 2, 2));
        scene.idle(90);

        scene.overlay().showControls(util.vector().topOf(7, 0, 2), Pointing.DOWN, 70)
                .withItem(new ItemStack(ModItems.GEIGER_COUNTER.get()));
        scene.overlay().showText(80)
                .text("Hold a Geiger Counter to see the dose rate and your accumulated dose")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().topOf(7, 0, 2));
        scene.idle(90);

        scene.overlay().showText(100)
                .text("The Hazmat Suit stops alpha and most beta radiation, but gamma and neutrons are best stopped by walls")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().topOf(7, 0, 2));
        scene.idle(110);
        scene.markAsFinished();
    }

    private AtomicScenes() {
    }
}
