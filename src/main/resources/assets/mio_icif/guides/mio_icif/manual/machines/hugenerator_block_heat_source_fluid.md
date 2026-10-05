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

## Notes

- The machine stops when the output tank is full.
- Without heat conductors, the machine makes no heat.

## Recipe

<RecipesFor id="mio_icif:hugenerator/block_heat_source_fluid" fallbackText="-" />
