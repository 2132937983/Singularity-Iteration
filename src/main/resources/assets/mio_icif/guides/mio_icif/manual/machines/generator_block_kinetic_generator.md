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
| Production | 1 EU per 4 KU, up to 250 EU/t |
| KU input | up to 1,000 KU/t |
| Energy storage | 100,000 EU |

The game measured these values from the block entity of this version.

With a generator output multiplier other than 1.0 (config GeneratorOutput), the EU values change.

## Procedure

1. Place the generator with its front face against the back face of a Wind Kinetic Generator.
2. Connect an MV cable to the generator.

## Recipe

<RecipesFor id="mio_icif:generator/block_kinetic_generator" fallbackText="-" />
