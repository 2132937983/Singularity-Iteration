---
navigation:
  title: "Electric Kinetic Generator"
  icon: mio_icif:kugenerator/block_kinetic_generator_elc
  parent: generators.md
  position: 8
item_ids:
  - mio_icif:kugenerator/block_kinetic_generator_elc
---

# Electric Kinetic Generator

<Row>
  <BlockImage id="mio_icif:kugenerator/block_kinetic_generator_elc" scale="3" />
</Row>

## Function

The machine converts EU into kinetic energy (KU).
Each Electric Motor in the motor slots increases the KU output.
The machine sends KU only through its front face.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | LV (32 EU) |
| Maximum input | 1,000 EU/t |
| Energy storage | 40,000 EU |
| Use while working | 100 EU/t |

The game measured these values from the block entity of this version.

## Procedure

1. Place the machine with its front face against the KU consumer.
2. Put Electric Motors into the motor slots.
3. Connect a cable to the machine to supply EU.
4. Put a charged battery into the battery slot as a second power source.

## Notes

- Without Electric Motors, the machine makes no KU.
- Turbocharged generators convert KU from this machine at a low efficiency.

## Recipe

<RecipesFor id="mio_icif:kugenerator/block_kinetic_generator_elc" fallbackText="-" />
