---
navigation:
  title: "Electric Heat Generator"
  icon: mio_icif:hugenerator/block_heat_generator_elc
  parent: generators.md
  position: 7
item_ids:
  - mio_icif:hugenerator/block_heat_generator_elc
---

# Electric Heat Generator

<Row>
  <BlockImage id="mio_icif:hugenerator/block_heat_generator_elc" scale="3" />
</Row>

## Function

The machine converts EU into heat (HU).
Each coil in the coil slots increases the heat output.
The machine gives heat only through its front face.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | EV (2,048 EU) |
| Maximum input | 2,048 EU/t |
| Energy storage | 10,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the machine with its front face against the heat consumer.
2. Put coils into the coil slots.
3. Connect a cable to the machine to supply EU.
4. Put a charged battery into the battery slot as a second power source.

## Notes

- Without coils, the machine makes no heat.

## Recipe

<RecipesFor id="mio_icif:hugenerator/block_heat_generator_elc" fallbackText="-" />
