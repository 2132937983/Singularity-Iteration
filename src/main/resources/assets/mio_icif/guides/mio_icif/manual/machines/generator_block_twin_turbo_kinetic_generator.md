---
navigation:
  title: "Twin-turbocharged Kinetic Generator"
  icon: mio_icif:generator/block_twin_turbo_kinetic_generator
  parent: generators.md
  position: 34
item_ids:
  - mio_icif:generator/block_twin_turbo_kinetic_generator
---

# Twin-turbocharged Kinetic Generator

<Row>
  <BlockImage id="mio_icif:generator/block_twin_turbo_kinetic_generator" scale="3" />
</Row>

## Function

The machine converts kinetic energy (KU) from the block at its front face into EU.
KU from wind or water kinetic generators converts at full efficiency.
KU from other sources converts at a lower efficiency, still above the Kinetic Generator.
The screen is a plant mimic: the KU source on the front, the shaft turning at its speed, the generator and the EU out, with the input load and the EU per KU.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | EV (2,048 EU); IV (8,192 EU) above 2,048 EU/t |
| Production | 1.2 EU per KU from wind or water kinetic generators, 0.45 EU per KU from other sources |
| KU input | up to 4,000 KU/t (up to 4,800 EU/t) |
| Energy storage | 400,000 EU |

The game measured these values from the block entity of this version.

With a generator output multiplier other than 1.0 (config GeneratorOutput), the EU values change.

## Procedure

1. Place the generator with its front face against the back face of a Wind Kinetic Generator.
2. Connect an IV cable to the generator (an EV cable is enough while it makes at most 2,048 EU/t).

## Notes

- The machine has a higher KU to EU ratio than the Turbocharged Kinetic Generator.
- The packet size follows the KU input: above 1,706 KU/t from wind or water (above 2,048 EU/t) the generator sends IV packets, which melt EV cables and blow up EV machines. Other sources give at most 4,000 × 0.45 = 1,800 EU/t and always stay at EV.

## Recipe

<RecipesFor id="mio_icif:generator/block_twin_turbo_kinetic_generator" fallbackText="-" />
