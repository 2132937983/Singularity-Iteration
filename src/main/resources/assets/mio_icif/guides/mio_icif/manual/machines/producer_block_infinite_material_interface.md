---
navigation:
  title: "Infinite Armory Material Interface"
  icon: mio_icif:producer/block_infinite_material_interface
  parent: machines.md
  position: 11
item_ids:
  - mio_icif:producer/block_infinite_material_interface
---

# Infinite Armory Material Interface

<Row>
  <BlockImage id="mio_icif:producer/block_infinite_material_interface" scale="3" />
</Row>

## Function

A creative block, like the Infinite Generator: it has no recipe. It supplies every building material, in any amount, to
the drones of the [Blueprint Builder](producer_block_blueprint_builder.md).

## Rules

* Place it within 16 blocks of the builder. Nothing goes in: it has no screen and no slots.
* It gives every block the blueprint needs. Nothing is used up.
* Material the drones bring back is gone.
* Hoppers and pipes do not connect to it.
* A normal [Armory Material Interface](producer_block_material_interface.md) nearer to the builder is used first. A material it does not have comes from the infinite one.
