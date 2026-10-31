---
navigation:
  title: "Advanced Stirling Generator"
  icon: mio_icif:generator/block_advanced_stirling_generator
  parent: generators.md
  position: 4
item_ids:
  - mio_icif:generator/block_advanced_stirling_generator
---

# Advanced Stirling Generator

<Row>
  <BlockImage id="mio_icif:generator/block_advanced_stirling_generator" scale="3" />
</Row>

## Function

The machine converts heat (HU) into EU at a higher ratio than the Stirling Generator.
The machine draws heat from each adjacent heat generator whose front face points at it.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | MV (128 EU) |
| Energy storage | 1,000,000 EU |
| Conversion | 100 HU → 85 EU |
| Heat input | up to 2,000 HU/t |

With a generator output multiplier other than 1.0 (config GeneratorOutput), the EU values change.

## Procedure

1. Place one or more heat generators next to the machine.
2. Turn the front face of each heat generator to the machine.
3. Connect an MV cable to the machine.

## Recipe

<RecipesFor id="mio_icif:generator/block_advanced_stirling_generator" fallbackText="-" />
