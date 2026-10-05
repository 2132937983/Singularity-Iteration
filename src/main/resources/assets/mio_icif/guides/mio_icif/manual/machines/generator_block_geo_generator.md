---
navigation:
  title: "Geothermal Generator"
  icon: mio_icif:generator/block_geo_generator
  parent: generators.md
  position: 14
item_ids:
  - mio_icif:generator/block_geo_generator
---

# Geothermal Generator

<Row>
  <BlockImage id="mio_icif:generator/block_geo_generator" scale="3" />
</Row>

## Function

The machine converts lava into EU.
The machine moves lava from lava buckets and lava cells into an internal tank.
Each lava block next to the machine adds extra EU output.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | LV (32 EU) |
| Output | 20 EU/t |
| Energy storage | 2,400 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an LV cable to the generator.
2. Put a lava bucket into the input slot.
3. Remove the empty buckets from the output slot.
4. Put a battery into the battery slot to charge it.

## Notes

- The machine does not empty a bucket when the output slot is full.

## Recipe

<RecipesFor id="mio_icif:generator/block_geo_generator" fallbackText="-" />
