---
navigation:
  title: "Stirling Generator"
  icon: mio_icif:generator/block_stirling_generator
  parent: generators.md
  position: 30
item_ids:
  - mio_icif:generator/block_stirling_generator
---

# Stirling Generator

<Row>
  <BlockImage id="mio_icif:generator/block_stirling_generator" scale="3" />
</Row>

## Function

The machine converts heat (HU) into EU.
The machine takes heat through its back face and through its front face (the copper plate, the face that looked at you when you placed it). With heat on both faces it takes from the back first.

## Power data

This block uses no EU.

## Procedure

1. Place a heat generator with its front face against the back face or the front face of the Stirling Generator. For example, a Heat Exchanger and a Stirling Generator can face each other with their copper plates.
2. Connect an MV cable to any other face of the Stirling Generator (the front face also gives EU; the back face does not).
3. Supply the heat generator with fuel or EU.

## Recipe

<RecipesFor id="mio_icif:generator/block_stirling_generator" fallbackText="-" />
