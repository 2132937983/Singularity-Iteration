---
navigation:
  title: "Armory Material Interface"
  icon: mio_icif:producer/block_material_interface
  parent: machines.md
  position: 11
item_ids:
  - mio_icif:producer/block_material_interface
---

# Armory Material Interface

<Row>
  <BlockImage id="mio_icif:producer/block_material_interface" scale="3" />
</Row>

## Function

The interface supplies building material to the drones of the [Blueprint Builder](producer_block_blueprint_builder.md).
It has 27 slots. It also reads the storages on its six sides.

## Rules

* Any storage that has an item handler works: chests, barrels, drawers, AE2 interfaces, Refined Storage interfaces and similar blocks.
* The drones take material from the own slots first, then from the adjacent storages.
* The drones bring unused material back to the interface.
* Hoppers and pipes can fill the 27 slots. RTS Building can link the interface as a storage.
* The builder uses the interfaces within 16 blocks.

## Recipe

<RecipesFor id="mio_icif:producer/block_material_interface" fallbackText="-" />
