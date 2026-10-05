---
navigation:
  title: "Advanced Fluid Distributor"
  icon: mio_icif:producer/block_weighted_fluid_distributor_elc
  parent: machines.md
  position: 1
item_ids:
  - mio_icif:producer/block_weighted_fluid_distributor_elc
---

# Advanced Fluid Distributor

<Row>
  <BlockImage id="mio_icif:producer/block_weighted_fluid_distributor_elc" scale="3" />
</Row>

## Function

The Advanced Fluid Distributor takes fluid at the front face and outputs the fluid to the selected faces in a priority order.
Faces that are not in the list do not connect. The block uses no EU.

## Power data

This block uses no EU.

## Procedure

1. Place the distributor with the front face toward the fluid source.
2. Open the GUI and add the output faces in the order of priority.
3. Connect pipes or tanks to the output faces.

## Recipe

<RecipesFor id="mio_icif:producer/block_weighted_fluid_distributor_elc" fallbackText="-" />
