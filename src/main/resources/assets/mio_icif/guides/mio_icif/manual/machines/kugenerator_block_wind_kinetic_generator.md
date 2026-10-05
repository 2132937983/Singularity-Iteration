---
navigation:
  title: "Wind Kinetic Generator"
  icon: mio_icif:kugenerator/block_wind_kinetic_generator
  parent: generators.md
  position: 40
item_ids:
  - mio_icif:kugenerator/block_wind_kinetic_generator
---

# Wind Kinetic Generator

<Row>
  <BlockImage id="mio_icif:kugenerator/block_wind_kinetic_generator" scale="3" />
</Row>

## Function

The machine converts wind into kinetic energy (KU) with a rotor.
The wind strength increases with height and in bad weather. Blocks in front of the rotor decrease it.
The machine sends KU only through its back face.

## Power data

This block uses no EU.

## Procedure

1. Put a rotor into the rotor slot.
2. Remove all blocks from the area in front of the rotor.
3. Place a Kinetic Generator with its front face against the back face of the machine.
4. Use an Anemometer on the machine to read the effective wind.

## Notes


> **WARNING:** Wind above the rotor limit damages the rotor four times faster.

- The rotor wears during operation. Replace the rotor before it breaks.

## Recipe

<RecipesFor id="mio_icif:kugenerator/block_wind_kinetic_generator" fallbackText="-" />
