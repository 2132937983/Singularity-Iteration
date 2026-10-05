---
navigation:
  title: "Semifluid Generator"
  icon: mio_icif:generator/block_semifluid_generator
  parent: generators.md
  position: 27
item_ids:
  - mio_icif:generator/block_semifluid_generator
---

# Semifluid Generator

<Row>
  <BlockImage id="mio_icif:generator/block_semifluid_generator" scale="3" />
</Row>

## Function

The machine burns semifluid fuel and makes EU.
The machine accepts biogas, biomass, crude oil and diesel oil.
Each fuel gives a different EU output.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | LV (32 EU) |
| Output | 16 EU/t |
| Energy storage | 32,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an LV cable to the generator.
2. Put a fuel bucket or a fuel cell into the input slot.
3. Remove the empty containers from the output slot.
4. Put a battery into the battery slot to charge it.

## Recipe

<RecipesFor id="mio_icif:generator/block_semifluid_generator" fallbackText="-" />
