---
navigation:
  title: "Stirling Kinetic Generator"
  icon: mio_icif:kugenerator/block_stirling_kinetic_generator
  parent: generators.md
  position: 31
item_ids:
  - mio_icif:kugenerator/block_stirling_kinetic_generator
---

# Stirling Kinetic Generator

<Row>
  <BlockImage id="mio_icif:kugenerator/block_stirling_kinetic_generator" scale="3" />
</Row>

## Function

The machine converts heat (HU) and water into kinetic energy (KU) and hot water.
The machine takes heat through all faces except the front face.
The machine sends KU only through its front face.

## Power data

| Item | Value |
|---|---|
| Conversion | 4 HU + 1 mB Water → 12 KU + 1 mB Hot Water |
| KU buffer | 2,000 KU |
| Heat buffer | 1,000 HU |

This block uses no EU.

## Procedure

1. Place the machine with its front face against the KU consumer.
2. Place a heat generator against another face of the machine.
3. Put water buckets or water cells into the water input slot.
4. Put empty containers into the hot water slot to collect the hot water.

## Notes

- The machine stops when the water tank is empty or the hot water tank is full.

## Recipe

<RecipesFor id="mio_icif:kugenerator/block_stirling_kinetic_generator" fallbackText="-" />
