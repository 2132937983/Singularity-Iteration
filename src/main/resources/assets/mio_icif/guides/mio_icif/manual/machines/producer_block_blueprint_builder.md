---
navigation:
  title: "Blueprint Builder"
  icon: mio_icif:producer/block_blueprint_builder
  parent: machines.md
  position: 10
item_ids:
  - mio_icif:producer/block_blueprint_builder
---

# Blueprint Builder

<Row>
  <BlockImage id="mio_icif:producer/block_blueprint_builder" scale="3" />
</Row>

## Function

The Blueprint Builder has four builder drones. The drones place the blocks of a blueprint, one block at a time.
The drones take the building material from an [Armory Material Interface](producer_block_material_interface.md). In creative play an [Infinite Armory Material Interface](producer_block_infinite_material_interface.md) supplies everything.

## Blueprint sources

* A deployed Create schematic in the schematic slot. The builder uses the anchor, the rotation and the mirror of the schematic.
* A file in the `schematics` folder of the game. On a server, the folder is in the server folder. The builder reads `.nbt` structure files (also Create schematics) and `.litematic` files (Litematica).
* A file from your own computer, on a server: click "Upload" and pick a file of your `schematics` folder. It goes to the server (as with Create's Schematic Table) and the builder loads it at once. The server keeps it in `schematics/uploads/<your UUID>/`; the same file again is not sent a second time. In single player this is not needed: your files are already in the list.
* A multiblock structure of this mod: click "Catalog" and pick one on the left (the fusion tokamak, the TESU, the steam tunnel, the reactors and more: the build plans of JEI's "Multiblock Structures"). On the right it turns with a drag, zooms with the wheel and shows one layer at a time as in JEI, with the blocks it needs below; then click "Build this". Where a position takes several blocks, the suggested one is placed; the fusion reactor gets each port in its slot, and the TESU's port slots take an input port and an output port in turn. Spaces the structure needs empty (the fusion plasma channel, the inside of a TESU or a steam tunnel) are not cleared.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | MV (128 EU) |
| Energy storage | 200,000 EU |
| Energy for each block | 40 EU |
| Range | No limit (the blueprint offset up to 1,024 blocks per axis); blocks in chunks that are not loaded wait until they are (a Chunk Loader helps for very large builds) |
| Blueprint size | No block limit; only the server's memory limits it (the status says "too large" when it cannot hold the blueprint) |
| Drones | 4 |

## Procedure

1. Put an Armory Material Interface within 16 blocks of the builder.
2. Put the building material into the interface, or into a storage next to it.
3. Put a deployed Create schematic into the slot, or select a file with the `<` and `>` buttons. On a server, "Upload" sends a file from your computer instead (it is loaded at once; go on with step 4).
4. For files, place the blueprint: click "Place in world", then look at the hologram. The mouse wheel moves it one block along where you look (with Ctrl five), R turns it, M mirrors it; right-click or Esc when done. The R and M buttons on the screen also turn and mirror it. Esc ends placing without the pause menu.
5. Click "Load". The hologram shows the work area, with every block still to place as a translucent ghost of itself. The area button goes round: "blocks" (every ghost), "layer" (only the lowest layer still to build), "frame" (the box only) and "hidden". The ghosts show for you only; the box shows for everyone. Large blueprints stay smooth: the ghosts are built once and only the changed parts are rebuilt.
6. Click "Start".

## Rules

* The drones place the bottom layer first, then the layers above.
* The drones do not break blocks. A position with a different block is skipped and counts as "obstructed".
* The drones use only plain items. They do not use an item with contents, a custom name or enchantments.
* The upper half of a door or a tall plant and the head of a bed need no item.
* The drones place waterlogged blocks dry. They do not place fluids.
* Each placement fires the block place event as the owner of the builder. A protection mod can stop it.

**CAUTION:** Do a check of the work area hologram before you click "Start". The drones fill every free position in the area. A block that stands where the blueprint has another one is taken out (its drops go to the nearest Armory Material Interface) and the blueprint's block goes in; blocks with a block entity (machines, chests) and unbreakable blocks stay, and claim protection is asked first. A server can turn this off with the `clearObstructions` option.

When the last block is in, the builder waits 2 seconds and checks the whole blueprint again: blocks that fell for want of support (a scaffold of the Dimension Oil Rig placed before its support) or were broken meanwhile are built again, up to 3 checks. Clicking "Start" again after the build checks it once more and fills the gaps.

## Recipe

<RecipesFor id="mio_icif:producer/block_blueprint_builder" fallbackText="-" />
