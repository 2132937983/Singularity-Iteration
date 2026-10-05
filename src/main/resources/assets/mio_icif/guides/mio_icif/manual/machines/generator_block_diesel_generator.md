---
navigation:
  title: "Diesel Generator"
  icon: mio_icif:generator/block_diesel_generator
  parent: generators.md
  position: 5
item_ids:
  - mio_icif:generator/block_diesel_generator
---

# Diesel Generator

<Row>
  <BlockImage id="mio_icif:generator/block_diesel_generator" scale="3" />
</Row>

## Function

The machine burns diesel oil and makes EU.
The machine accepts diesel oil buckets and diesel oil cells only.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | EV (2,048 EU) |
| Output | 120 EU/t |
| Energy storage | 1,000,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an HV cable to the generator.
2. Put a diesel oil bucket into the input slot.
3. Remove the empty containers from the output slot.
4. Put a battery into the battery slot to charge it.

## Recipe

<RecipesFor id="mio_icif:generator/block_diesel_generator" fallbackText="-" />
