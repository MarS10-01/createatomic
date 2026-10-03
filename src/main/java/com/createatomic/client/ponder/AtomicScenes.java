package com.createatomic.client.ponder;

import com.createatomic.registry.ModItems;
import com.simibubi.create.AllItems;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Create Ponder scenes for the accessible reactor.
 *
 * The reactor vessel is deliberately OPEN at the top. The player builds from inside, places fuel channels
 * and control rods by hand, fills the internal coolant tank with buckets, and can automate coolant through
 * any external casing block with a Create pipe/pump.
 */
public class AtomicScenes {

    public static void structure(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("reactor_structure", "Building the Reactor Vessel");
        scene.scaleSceneView(0.58f);
        scene.showBasePlate();
        scene.idle(10);

        // Demo schematic: 11 x 9 x 11 internal chamber, open top.
        scene.world().showSection(util.select().fromTo(1, 1, 1, 13, 1, 13), net.minecraft.core.Direction.DOWN);
        scene.idle(8);
        scene.world().showSection(util.select().fromTo(1, 2, 1, 1, 10, 6), net.minecraft.core.Direction.DOWN);
        scene.world().showSection(util.select().fromTo(1, 2, 8, 1, 10, 13), net.minecraft.core.Direction.DOWN);
        scene.world().showSection(util.select().fromTo(13, 2, 1, 13, 10, 13), net.minecraft.core.Direction.DOWN);
        scene.world().showSection(util.select().fromTo(2, 2, 1, 12, 10, 1), net.minecraft.core.Direction.DOWN);
        scene.world().showSection(util.select().fromTo(2, 2, 13, 12, 10, 13), net.minecraft.core.Direction.DOWN);
        scene.idle(15);

        scene.overlay().showText(90)
                .text("createatomic.ponder.reactor_structure.text_1");
        scene.idle(95);

        // Controller in the west wall, with its shaft outside the vessel.
        scene.world().showSection(util.select().position(1, 6, 7), net.minecraft.core.Direction.EAST);
        scene.world().showSection(util.select().position(0, 6, 7), net.minecraft.core.Direction.EAST);
        scene.idle(12);
        scene.overlay().showText(100)
                .text("createatomic.ponder.reactor_structure.text_2")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().centerOf(1, 6, 7));
        scene.idle(105);

        // Internal components.
        int[][] fuel = {{4, 4, 4}, {10, 4, 4}, {4, 4, 10}, {10, 4, 10}};
        for (int[] p : fuel) {
            scene.world().showSection(util.select().position(p[0], p[1], p[2]), net.minecraft.core.Direction.DOWN);
            scene.idle(4);
        }
        scene.overlay().showText(90)
                .text("createatomic.ponder.reactor_structure.text_3")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().centerOf(4, 4, 4));
        scene.idle(95);

        int[][] control = {{7, 4, 7}, {7, 4, 9}};
        for (int[] p : control) {
            scene.world().showSection(util.select().position(p[0], p[1], p[2]), net.minecraft.core.Direction.DOWN);
            scene.idle(4);
        }
        scene.overlay().showText(90)
                .text("createatomic.ponder.reactor_structure.text_4");
        scene.idle(95);

        // Water layer is intentionally visible in the Ponder scene.
        int[][] water = {{2, 2, 2}, {3, 2, 2}, {2, 2, 3}, {3, 2, 3}, {11, 2, 2}, {12, 2, 2}};
        for (int[] p : water) {
            scene.world().showSection(util.select().position(p[0], p[1], p[2]), net.minecraft.core.Direction.DOWN);
            scene.idle(2);
        }
        scene.overlay().showText(100)
                .text("createatomic.ponder.reactor_structure.text_6");
        scene.idle(105);

        scene.overlay().showText(95)
                .text("createatomic.ponder.reactor_structure.text_5")
                .attachKeyFrame()
                .colored(PonderPalette.BLUE)
                .placeNearTarget()
                .pointAt(util.vector().topOf(7, 10, 7));
        scene.idle(100);

        scene.overlay().showText(100)
                .text("createatomic.ponder.reactor_structure.text_7");
        scene.idle(105);
        scene.markAsFinished();
    }

    public static void operation(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("reactor_operation", "Loading and Operating the Reactor");
        scene.scaleSceneView(0.62f);
        scene.showBasePlate();
        scene.world().showSection(util.select().fromTo(1, 1, 1, 13, 10, 13), net.minecraft.core.Direction.DOWN);
        scene.world().showSection(util.select().position(0, 6, 7), net.minecraft.core.Direction.EAST);
        scene.idle(20);

        scene.overlay().showControls(util.vector().centerOf(4, 4, 4), Pointing.DOWN, 60)
                .rightClick()
                .withItem(new ItemStack(ModItems.FUEL_ROD.get()));
        scene.overlay().showText(85)
                .text("createatomic.ponder.reactor_operation.text_1");
        scene.idle(95);

        scene.overlay().showControls(util.vector().centerOf(4, 2, 2), Pointing.DOWN, 60)
                .rightClick()
                .withItem(new ItemStack(Items.WATER_BUCKET));
        scene.overlay().showText(90)
                .text("createatomic.ponder.reactor_operation.text_2");
        scene.idle(100);

        scene.overlay().showText(95)
                .text("createatomic.ponder.reactor_operation.text_3");
        scene.idle(105);

        scene.overlay().showText(95)
                .text("createatomic.ponder.reactor_operation.text_4");
        scene.idle(100);

        scene.overlay().showControls(util.vector().centerOf(1, 6, 7), Pointing.RIGHT, 60)
                .withItem(AllItems.GOGGLES.asStack());
        scene.overlay().showText(90)
                .text("createatomic.ponder.reactor_operation.text_5");
        scene.idle(100);

        scene.overlay().showText(100)
                .text("createatomic.ponder.reactor_operation.text_6")
                .attachKeyFrame()
                .colored(PonderPalette.RED)
                .placeNearTarget()
                .pointAt(util.vector().topOf(7, 10, 7));
        scene.idle(110);

        scene.overlay().showControls(util.vector().centerOf(4, 4, 4), Pointing.DOWN, 60)
                .rightClick();
        scene.overlay().showText(95)
                .text("createatomic.ponder.reactor_operation.text_7");
        scene.idle(100);
        scene.markAsFinished();
    }

    public static void shielding(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("radiation_shielding", "Radiation and Shielding");
        scene.scaleSceneView(0.8f);
        scene.showBasePlate();
        scene.idle(10);

        scene.world().showSection(util.select().position(1, 1, 2), net.minecraft.core.Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(90)
                .text("createatomic.ponder.radiation_shielding.text_1");
        scene.idle(100);

        scene.world().showSection(util.select().fromTo(4, 1, 0, 4, 3, 4), net.minecraft.core.Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80)
                .text("createatomic.ponder.radiation_shielding.text_2");
        scene.idle(90);

        scene.world().showSection(util.select().fromTo(5, 1, 0, 5, 3, 4), net.minecraft.core.Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80)
                .text("createatomic.ponder.radiation_shielding.text_3");
        scene.idle(90);

        scene.overlay().showControls(util.vector().topOf(7, 0, 2), Pointing.DOWN, 70)
                .withItem(new ItemStack(ModItems.GEIGER_COUNTER.get()));
        scene.overlay().showText(90)
                .text("createatomic.ponder.radiation_shielding.text_4");
        scene.idle(95);

        scene.overlay().showText(95)
                .text("createatomic.ponder.radiation_shielding.text_5");
        scene.idle(105);
        scene.markAsFinished();
    }

    private AtomicScenes() {
    }
}
