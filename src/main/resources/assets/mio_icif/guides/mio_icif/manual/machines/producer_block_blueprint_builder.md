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
The drones take the building material from an [Armory Material Interface](producer_block_material_interface.md).

## Blueprint sources

* A deployed Create schematic in the schematic slot. The builder uses the anchor, the rotation and the mirror of the schematic.
* A file in the `schematics` folder of the game. On a server, the folder is in the server folder. The builder reads `.nbt` structure files (also Create schematics) and `.litematic` files (Litematica).

## Power data

| Item | Value |
|---|---|
| Input voltage tier | MV (128 EU) |
| Energy storage | 200,000 EU |
| Energy for each block | 40 EU |
| Range | 64 blocks from the builder (all blocks of the blueprint) |
| Drones | 4 |

## Procedure

1. Put an Armory Material Interface within 16 blocks of the builder.
2. Put the building material into the interface, or into a storage next to it.
3. Put a deployed Create schematic into the slot, or select a file with the `<` and `>` buttons.
4. For a file, set the turn (R), the mirror (M) and the offset (X, Y, Z). The offset is from the builder to the minimum corner of the blueprint.
5. Click "Load". The hologram shows the work area.
6. Click "Start".

## Rules

* The drones place the bottom layer first, then the layers above.
* The drones do not break blocks. A position with a different block is skipped and counts as "obstructed".
* The drones use only plain items. They do not use an item with contents, a custom name or enchantments.
* The upper half of a door or a tall plant and the head of a bed need no item.
* The drones place waterlogged blocks dry. They do not place fluids.
* Each placement fires the block place event as the owner of the builder. A protection mod can stop it.

**CAUTION:** Do a check of the work area hologram before you click "Start". The drones fill every free position in the area.

## Recipe

<RecipesFor id="mio_icif:producer/block_blueprint_builder" fallbackText="-" />
