---
navigation:
  title: "Fluid Distributor"
  icon: mio_icif:producer/block_fluid_distributor_elc
  parent: machines.md
  position: 27
item_ids:
  - mio_icif:producer/block_fluid_distributor_elc
---

# Fluid Distributor

<Row>
  <BlockImage id="mio_icif:producer/block_fluid_distributor_elc" scale="3" />
</Row>

## Function

The Fluid Distributor moves fluid in two modes. The block uses no EU.
Distribute mode takes fluid at the front face and splits the fluid to the other five faces.
Concentrate mode takes fluid at the other five faces and outputs the fluid at the front face.

## Power data

This block uses no EU.

## Procedure

1. Place the distributor with the front face toward the source or the target.
2. Click Toggle Mode in the GUI to select the mode.
3. Connect pipes or tanks to the faces.
4. Put an empty container into the container slot to fill the container from the tank.

## Recipe

<RecipesFor id="mio_icif:producer/block_fluid_distributor_elc" fallbackText="-" />
