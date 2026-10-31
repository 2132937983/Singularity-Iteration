---
navigation:
  title: "Multiblock Structures"
  icon: mio_icif:producer/block_large_fabricator_core
  parent: heavy.md
  position: 1
---

# Multiblock Structures

These are the build plans of the multiblock structures and of the machines that need a neighbour block in a fixed position. The structure checks itself in the world; the plans show a correct build.

Drag a view to turn it. In JEI, the category "Multiblock Structures" shows the same plans: drag to turn, scroll to zoom, and click the layer label to see one layer at a time. Look up the uses of a block in JEI to find the structures that contain it.

## Nuclear Reactor with chambers

<GameScene zoom="4" interactive={true}>
  <SiStructure id="mio_icif:nuclear_reactor" />
</GameScene>

<SiStructureInfo id="mio_icif:nuclear_reactor" />

Page: [Nuclear Reactor with chambers](generator_block_nuclear_reactor_generator.md)

## Fluid Reactor (5×5×5)

<GameScene zoom="2.5" interactive={true}>
  <SiStructure id="mio_icif:fluid_reactor" />
</GameScene>

<SiStructureInfo id="mio_icif:fluid_reactor" />

Page: [Fluid Reactor (5×5×5)](reactor_block_reactor_vessel.md)

## GESU

<GameScene zoom="4" interactive={true}>
  <SiStructure id="mio_icif:gesu" />
</GameScene>

<SiStructureInfo id="mio_icif:gesu" />

Page: [GESU](wiring_block_gesu_core.md)

## Large Matter Fabricator

<GameScene zoom="4" interactive={true}>
  <SiStructure id="mio_icif:large_fabricator" />
</GameScene>

<SiStructureInfo id="mio_icif:large_fabricator" />

Page: [Large Matter Fabricator](producer_block_large_fabricator_core.md)

## Oil Rig

<GameScene zoom="3" interactive={true}>
  <SiStructure id="mio_icif:oil_rig" />
</GameScene>

<SiStructureInfo id="mio_icif:oil_rig" />

Page: [Oil Rig](oilrig_block_oil_rig_core.md)

## Dimension Oil Rig

<GameScene zoom="2" interactive={true}>
  <SiStructure id="mio_icif:dimension_oil_rig" />
</GameScene>

<SiStructureInfo id="mio_icif:dimension_oil_rig" />

Page: [Dimension Oil Rig](oilrig_block_dimension_oil_rig_core.md)

## Blast Furnace and heat source

<GameScene zoom="5" interactive={true}>
  <SiStructure id="mio_icif:blast_furnace" />
</GameScene>

<SiStructureInfo id="mio_icif:blast_furnace" />

Page: [Blast Furnace and heat source](producer_block_blast_furnace.md)

## Fermenter and heat source

<GameScene zoom="5" interactive={true}>
  <SiStructure id="mio_icif:fermenter" />
</GameScene>

<SiStructureInfo id="mio_icif:fermenter" />

Page: [Fermenter and heat source](producer_block_fermenter_elc.md)

## Steam Tunnel

<GameScene zoom="4" interactive={true}>
  <SiStructure id="mio_icif:steam_tunnel" />
</GameScene>

<SiStructureInfo id="mio_icif:steam_tunnel" />

Page: [Steam Tunnel](generator_block_steam_tunnel.md)

## For addon authors

Register a plan with `StructureBlueprintRegistry.register(...)` (package `core.api.multiblock`). JEI then shows it, and a page of this manual can show it with `<SiStructure id="mymod:plan" />` inside a `GameScene` and `<SiStructureInfo id="mymod:plan" />`. See the API guide, section 20.
