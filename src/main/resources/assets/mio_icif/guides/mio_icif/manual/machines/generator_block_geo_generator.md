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
Lava within 2 blocks of the machine adds EU output: a lava block touching a face 1 EU/t, any other within 2 blocks 0.5 EU/t, flowing lava half. The deeper the machine the more: times 1 at Y 64 and above up to 2 at Y −64; times 1.5 in the Nether. At most 12 EU/t from the lava around.
The screen is a recorder: a borehole log with the depth factor, the lava around as a sum and the tank.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | LV (32 EU) |
| Output | 20 EU/t |
| Energy storage | 2,400 EU |
| Fuel: Lava | 2 mB/t → 20 EU/t (10 EU/mB) |
| Lava around (2 blocks) | 1 EU/t touching, 0.5 EU/t further, × depth, at most 12 EU/t |

The game measured these values from the block entity of this version.

With a generator output multiplier other than 1.0 (config GeneratorOutput), the EU values change. The fuel use stays the same.

## Procedure

1. Connect an LV cable to the generator.
2. Put a lava bucket into the input slot.
3. Remove the empty buckets from the output slot.
4. Put a battery into the battery slot to charge it.

## Notes

- The machine does not empty a bucket when the output slot is full.

## Recipe

<RecipesFor id="mio_icif:generator/block_geo_generator" fallbackText="-" />
