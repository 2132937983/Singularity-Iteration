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
KU from other sources converts at a low efficiency.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | HV (512 EU) |
| Output | 2,048 EU/t |
| Energy storage | 200,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the generator with its front face against the back face of a Wind Kinetic Generator.
2. Connect an HV cable to the generator.

## Recipe

<RecipesFor id="mio_icif:generator/block_turbo_kinetic_generator" fallbackText="-" />
