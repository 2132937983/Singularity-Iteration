---
navigation:
  title: "Advanced Semifluid Generator"
  icon: mio_icif:generator/block_advanced_semifluid_generator
  parent: generators.md
  position: 2
item_ids:
  - mio_icif:generator/block_advanced_semifluid_generator
---

# Advanced Semifluid Generator

<Row>
  <BlockImage id="mio_icif:generator/block_advanced_semifluid_generator" scale="3" />
</Row>

## Function

The machine burns biogas and makes EU.
The machine accepts biogas buckets and biogas cells only.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | MV (128 EU) |
| Output | 36 EU/t |
| Energy storage | 100,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an MV cable to the generator.
2. Put a biogas bucket into the input slot.
3. Remove the empty containers from the output slot.
4. Put a battery into the battery slot to charge it.

## Recipe

<RecipesFor id="mio_icif:generator/block_advanced_semifluid_generator" fallbackText="-" />
