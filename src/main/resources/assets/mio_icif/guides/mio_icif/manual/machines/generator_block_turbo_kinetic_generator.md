---
navigation:
  title: "Turbocharged Kinetic Generator"
  icon: mio_icif:generator/block_turbo_kinetic_generator
  parent: generators.md
  position: 33
item_ids:
  - mio_icif:generator/block_turbo_kinetic_generator
---

# Turbocharged Kinetic Generator

<Row>
  <BlockImage id="mio_icif:generator/block_turbo_kinetic_generator" scale="3" />
</Row>

## Function

The machine converts kinetic energy (KU) from the block at its front face into EU.
KU from wind or water kinetic generators converts at full efficiency.
KU from other sources converts at a lower efficiency, still above the Kinetic Generator.
The screen is a plant mimic: the KU source on the front, the shaft turning at its speed, the generator and the EU out, with the input load and the EU per KU.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | HV (512 EU); EV (2,048 EU) above 512 EU/t |
| Production | 0.85 EU per KU from wind or water kinetic generators, 0.35 EU per KU from other sources |
| KU input | up to 2,000 KU/t (up to 1,700 EU/t) |
| Energy storage | 200,000 EU |

The game measured these values from the block entity of this version.

With a generator output multiplier other than 1.0 (config GeneratorOutput), the EU values change.

## Procedure

1. Place the generator with its front face against the back face of a Wind Kinetic Generator.
2. Connect an EV cable to the generator (an HV cable is enough while it makes at most 512 EU/t).

## Notes

- The packet size follows the KU input: above 602 KU/t from wind or water, or above 1,462 KU/t from other sources (above 512 EU/t), the generator sends EV packets, which melt HV cables and blow up HV machines.

## Recipe

<RecipesFor id="mio_icif:generator/block_turbo_kinetic_generator" fallbackText="-" />
