// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.client.ponder;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The ponder storyboards (0.1.8.0). Text rules (ASD-STE100): short sentences, active voice,
 * one instruction per sentence, approved words; a warning starts with "WARNING". The default
 * texts here are the English source; the language files hold the same keys
 * ({@code mio_icif.ponder.<scene>.header}, {@code .text_1}, ...).
 */
@OnlyIn(Dist.CLIENT)
final class SiPonderScenes {
    private SiPonderScenes() {}

    // ------------------------------------------------------------------ Armory
    static void armory(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("armory", "Store and summon suits with the Armory");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos armory = util.grid().at(2, 1, 2);
        scene.world().showSection(util.select().position(armory), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(70).text("The Armory stores six suits. Each suit has six pieces: four armor pieces and two hand items.")
            .pointAt(util.vector().topOf(armory)).placeNearTarget().attachKeyFrame();
        scene.idle(80);
        BlockPos power = util.grid().at(1, 1, 2);
        scene.world().showSection(util.select().position(power), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(70).text("Supply power to the Armory. The first player who opens a powered Armory becomes its owner.")
            .pointAt(util.vector().blockSurface(armory, Direction.WEST)).placeNearTarget().attachKeyFrame();
        scene.idle(80);
        BlockPos projector = util.grid().at(3, 1, 2);
        scene.world().showSection(util.select().position(projector), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(70).text("A Holo Display Projector next to the Armory adds suit positions. It shows its suit as a hologram.")
            .pointAt(util.vector().topOf(projector)).placeNearTarget().attachKeyFrame();
        scene.idle(80);
        scene.overlay().showText(80).text("Pair an Armory Remote with the Armory. Then use the Singularity Management Panel to summon a suit, to deploy a Wingman or to equip a companion.")
            .independent(40).attachKeyFrame();
        scene.idle(90);
        scene.markAsFinished();
    }

    // ------------------------------------------------------------------ Nuclear reactor
    static void nuclearReactor(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("nuclear_reactor", "Build a nuclear reactor");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos core = util.grid().at(2, 1, 2);
        scene.world().showSection(util.select().position(core), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(70).text("The Nuclear Reactor Generator makes EU and heat from fuel rods. Without chambers, it has three columns of slots.")
            .pointAt(util.vector().topOf(core)).placeNearTarget().attachKeyFrame();
        scene.idle(80);
        BlockPos[] chambers = {util.grid().at(1, 1, 2), util.grid().at(3, 1, 2), util.grid().at(2, 1, 1), util.grid().at(2, 1, 3), util.grid().at(2, 2, 2)};
        for (BlockPos c : chambers) {
            scene.world().showSection(util.select().position(c), Direction.DOWN);
            scene.idle(6);
        }
        scene.overlay().showText(70).text("Each Reactor Chamber on a side of the generator adds one column of slots. The maximum is six chambers.")
            .pointAt(util.vector().topOf(chambers[4])).placeNearTarget().attachKeyFrame();
        scene.idle(80);
        scene.overlay().showText(70).text("Do a check of each design in the Reactor Simulator page of the manual before you put fuel rods in the reactor.")
            .independent(40).attachKeyFrame();
        scene.idle(80);
        Selection all = util.select().fromTo(1, 1, 1, 3, 2, 3);
        scene.overlay().showOutline(PonderPalette.RED, "heat", all, 70);
        scene.overlay().showText(70).colored(PonderPalette.RED).text("WARNING: A reactor with too much hull heat explodes. The explosion destroys the blocks near it.")
            .pointAt(util.vector().topOf(core)).placeNearTarget().attachKeyFrame();
        scene.idle(80);
        scene.markAsFinished();
    }

    // ------------------------------------------------------------------ Blueprint Builder
    static void blueprintBuilder(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("blueprint_builder", "Build a blueprint with drones");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos builder = util.grid().at(1, 1, 1);
        scene.world().showSection(util.select().position(builder), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(70).text("The Blueprint Builder has four drones. The drones place the blocks of a blueprint, one block at a time.")
            .pointAt(util.vector().topOf(builder)).placeNearTarget().attachKeyFrame();
        scene.idle(80);
        BlockPos iface = util.grid().at(5, 1, 1);
        scene.world().showSection(util.select().fromTo(5, 1, 1, 5, 1, 2), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80).text("Put an Armory Material Interface within 16 blocks. The drones take material from it and from the storages next to it.")
            .pointAt(util.vector().topOf(iface)).placeNearTarget().attachKeyFrame();
        scene.idle(90);
        Selection area = util.select().fromTo(2, 1, 3, 4, 3, 5);
        scene.overlay().showOutline(PonderPalette.BLUE, "area", area, 60);
        scene.overlay().showText(60).text("Load a Create schematic or a file of the schematics folder. The hologram shows the work area.")
            .pointAt(util.vector().centerOf(3, 2, 4)).placeNearTarget().attachKeyFrame();
        scene.idle(70);
        // the drones build the house: bottom layer first, then the layers above
        for (int y = 1; y <= 3; y++) {
            for (int z = 3; z <= 5; z++) {
                for (int x = 2; x <= 4; x++) {
                    if (y == 2 && x == 3 && z == 4) continue;
                    scene.world().showSection(util.select().position(x, y, z), Direction.DOWN);
                    scene.idle(2);
                }
            }
        }
        scene.overlay().showText(70).text("The drones place the bottom layer first. They do not break blocks: a position with a different block is skipped.")
            .pointAt(util.vector().topOf(3, 3, 4)).placeNearTarget().attachKeyFrame();
        scene.idle(80);
        scene.markAsFinished();
    }

    // ------------------------------------------------------------------ Ejector Pipe
    static void ejectorPipe(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("ejector_pipe", "Eject items and fluids into the world");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos chest = util.grid().at(1, 1, 2), pipe = util.grid().at(2, 1, 2);
        scene.world().showSection(util.select().position(chest), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().position(pipe), Direction.WEST);
        scene.idle(15);
        scene.overlay().showText(70).text("Put the Ejector Pipe on a container. The nozzle points away from the container.")
            .pointAt(util.vector().topOf(pipe)).placeNearTarget().attachKeyFrame();
        scene.idle(80);
        scene.overlay().showLine(PonderPalette.OUTPUT, util.vector().centerOf(pipe), util.vector().centerOf(4, 1, 2), 60);
        scene.overlay().showText(70).text("The pipe takes up to 16 items from the container and throws them out of the nozzle every 8 ticks.")
            .pointAt(util.vector().centerOf(3, 1, 2)).placeNearTarget().attachKeyFrame();
        scene.idle(80);
        scene.overlay().showText(70).text("With one full bucket of fluid, the pipe puts a source block in front of the nozzle. A redstone signal stops the pipe.")
            .independent(40).attachKeyFrame();
        scene.idle(80);
        scene.markAsFinished();
    }
}
