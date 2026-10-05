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

This block uses no EU.

## Procedure

1. Place one or more heat generators next to the machine.
2. Turn the front face of each heat generator to the machine.
3. Connect an MV cable to the machine.

## Recipe

<RecipesFor id="mio_icif:generator/block_advanced_stirling_generator" fallbackText="-" />
