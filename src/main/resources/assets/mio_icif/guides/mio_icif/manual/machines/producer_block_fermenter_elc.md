---
navigation:
  title: "Fermenter"
  icon: mio_icif:producer/block_fermenter_elc
  parent: machines.md
  position: 26
item_ids:
  - mio_icif:producer/block_fermenter_elc
---

# Fermenter

<Row>
  <BlockImage id="mio_icif:producer/block_fermenter_elc" scale="3" />
</Row>

## Function

The Fermenter uses heat (HU) to convert Biomass into Biogas.
The machine takes heat only from a heat source on its front face.
The machine also produces Fertilizer from the processed Biomass.

## Power data

This block uses no EU.

## Procedure

1. Place a heat generator against the front face of the Fermenter.
2. Supply Biomass with Biomass Cells or with a pipe.
3. Put Empty Cells into the cell input slot to collect Biogas.
4. Take the Fertilizer from the fertilizer slot.

## Notes

- A redstone signal has no effect on the Fermenter.
- The upgrade slots accept item and fluid ejector and pulling upgrades only.

## Heat source placement

Drag the view to turn it. The list gives the blocks and their rules. JEI shows the same plan in the category "Multiblock Structures".

<GameScene zoom="5" interactive={true}>
  <SiStructure id="mio_icif:fermenter" />
</GameScene>

<SiStructureInfo id="mio_icif:fermenter" />

## Recipe

<RecipesFor id="mio_icif:producer/block_fermenter_elc" fallbackText="-" />
