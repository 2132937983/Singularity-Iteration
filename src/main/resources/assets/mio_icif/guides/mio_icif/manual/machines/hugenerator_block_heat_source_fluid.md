---
navigation:
  title: "Heat Exchanger"
  icon: mio_icif:hugenerator/block_heat_source_fluid
  parent: generators.md
  position: 15
item_ids:
  - mio_icif:hugenerator/block_heat_source_fluid
---

# Heat Exchanger

<Row>
  <BlockImage id="mio_icif:hugenerator/block_heat_source_fluid" scale="3" />
</Row>

## Function

The machine converts lava or hot coolant into heat (HU).
Lava becomes pahoehoe lava. Hot coolant becomes coolant.
Each heat conductor increases the heat output. The machine gives heat only through its front face.

## Power data

This block uses no EU.

## Procedure

1. Place the machine with its front face against the heat consumer.
2. Put heat conductors into the conductor slots.
3. Put a lava bucket or a hot coolant cell into the hot fluid input slot.
4. Put empty containers into the cold fluid slot to collect the output fluid.

## Heat face

- The machine gives heat only through its front: the face with the copper heat plate. The plate turns red while the machine works.
- When you place the machine, the heat plate faces you.
- The heat input face of the heat user must touch the plate: a Stirling Generator takes heat through its back face and its front face (the copper plate, the face that looked at you when you placed it), a Kinetic Stirling Generator through every face except its shaft, a Steam Generator through every face except its front.
- Use a wrench on the face where you want the heat plate. Sneak and use the wrench to turn the plate to the opposite face. A wrench on the plate itself picks the machine up.
- The other five faces never give heat; use them for pipes, cells and automation.

## Notes

- The machine stops when the output tank is full.
- Without heat conductors, the machine makes no heat.

## Recipe

<RecipesFor id="mio_icif:hugenerator/block_heat_source_fluid" fallbackText="-" />
