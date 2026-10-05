---
navigation:
  title: "Kinetic Generator"
  icon: mio_icif:generator/block_kinetic_generator
  parent: generators.md
  position: 18
item_ids:
  - mio_icif:generator/block_kinetic_generator
---

# Kinetic Generator

<Row>
  <BlockImage id="mio_icif:generator/block_kinetic_generator" scale="3" />
</Row>

## Function

The machine converts kinetic energy (KU) into EU.
The machine takes KU from the block at its front face.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | MV (128 EU) |
| Output | 4,096 EU/t |
| Energy storage | 100,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the generator with its front face against the back face of a Wind Kinetic Generator.
2. Connect an MV cable to the generator.

## Recipe

<RecipesFor id="mio_icif:generator/block_kinetic_generator" fallbackText="-" />
