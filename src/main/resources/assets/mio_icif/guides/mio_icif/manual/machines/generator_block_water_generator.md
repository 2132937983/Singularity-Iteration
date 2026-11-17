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
Moving water at the front face makes EU without buckets too: up to 4 EU/t by its speed (a river, the tide, flowing water or a waterfall, the same rules as the Water Kinetic Generator). Still water outside rivers and oceans makes nothing.
The screen shows the water in front with its speed, the tank and both outputs.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | LV (32 EU) |
| Output | up to 8 EU/t |
| Energy storage | 64,000 EU |
| Water in the tank | 1 mB/t → 4 EU/t (4 EU/mB) |
| Water at the front face | 2 EU/t at an average current, up to 4 EU/t |

The game measured these values from the block entity of this version.

With a generator output multiplier other than 1.0 (config GeneratorOutput), the EU values change. The fuel use stays the same.

## Procedure

1. Place the generator with its front face in water.
2. Put a water bucket into the bucket slot.
3. Remove the empty bucket from the bucket slot.
4. Connect an LV cable to the generator.

## Recipe

<RecipesFor id="mio_icif:generator/block_water_generator" fallbackText="-" />
