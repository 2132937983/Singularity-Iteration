---
navigation:
  title: "Experience Generator"
  icon: mio_icif:generator/block_experience_generator
  parent: generators.md
  position: 9
item_ids:
  - mio_icif:generator/block_experience_generator
---

# Experience Generator

<Row>
  <BlockImage id="mio_icif:generator/block_experience_generator" scale="3" />
</Row>

## Function

The machine pulls experience orbs within its range and converts them into EU.
Orbs with more experience give more EU.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | ULV (8 EU) |
| Output | 160 EU/t |
| Energy storage | 1,000,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the generator near a mob farm.
2. Connect an HV cable to the generator.
3. Kill mobs near the generator to release experience orbs.

## Notes

- The machine also takes the experience orbs that players need.

## Recipe

<RecipesFor id="mio_icif:generator/block_experience_generator" fallbackText="-" />
