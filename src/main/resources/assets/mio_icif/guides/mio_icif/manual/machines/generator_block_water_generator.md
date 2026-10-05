---
navigation:
  title: "Water Generator"
  icon: mio_icif:generator/block_water_generator
  parent: generators.md
  position: 37
item_ids:
  - mio_icif:generator/block_water_generator
---

# Water Generator

<Row>
  <BlockImage id="mio_icif:generator/block_water_generator" scale="3" />
</Row>

## Function

The machine converts water from water buckets into EU.
Water at the front face of the machine also makes EU without buckets.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | LV (32 EU) |
| Output | 32 EU/t |
| Energy storage | 64,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the generator with its front face in water.
2. Put a water bucket into the bucket slot.
3. Remove the empty bucket from the bucket slot.
4. Connect an LV cable to the generator.

## Recipe

<RecipesFor id="mio_icif:generator/block_water_generator" fallbackText="-" />
