---
navigation:
  title: "Solid Heat Generator"
  icon: mio_icif:hugenerator/block_solid_heat_generator
  parent: generators.md
  position: 29
item_ids:
  - mio_icif:hugenerator/block_solid_heat_generator
---

# Solid Heat Generator

<Row>
  <BlockImage id="mio_icif:hugenerator/block_solid_heat_generator" scale="3" />
</Row>

## Function

The machine burns furnace fuel and makes heat (HU).
Each burned fuel item gives one ash in the ash slot.
The machine gives heat only through its front face.

## Power data

| Item | Value |
|---|---|
| Heat output | 20 HU/t while the fuel burns |
| Burn time | furnace burn time ÷ 4 (Coal: 400 t, 8,000 HU) |

This block uses no EU.

## Procedure

1. Place the machine with its front face against the heat consumer.
2. Put coal into the fuel slot.
3. Remove the ash from the ash slot.

## Notes

- The machine does not burn lava.
- The machine stops when the ash slot is full.

## Recipe

<RecipesFor id="mio_icif:hugenerator/block_solid_heat_generator" fallbackText="-" />
