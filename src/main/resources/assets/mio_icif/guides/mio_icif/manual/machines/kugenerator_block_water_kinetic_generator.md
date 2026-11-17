---
navigation:
  title: "Water Kinetic Generator"
  icon: mio_icif:kugenerator/block_water_kinetic_generator
  parent: generators.md
  position: 38
item_ids:
  - mio_icif:kugenerator/block_water_kinetic_generator
---

# Water Kinetic Generator

<Row>
  <BlockImage id="mio_icif:kugenerator/block_water_kinetic_generator" scale="3" />
</Row>

## Function

The machine converts water flow into kinetic energy (KU) with a rotor.
The block at its front face must be water. The machine sends KU only through its back face.
The rotor turns in a disc of its diameter centred on the block in front, square to the facing. The output is the power of the water flowing through that disc: it grows with the cube of the speed, and only the cells of the disc that are water count, so sink the rotor fully.
- River biomes: a steady current, about 30 % more power in rain and 60 % more in a thunderstorm.
- Ocean and beach biomes: a tidal current that turns twice a day (slack water at 0, 6,000, 12,000 and 18,000 ticks). Full and new moon bring spring tides (1.3 times the power), the quarters neap tides (0.7 times).
- Flowing water anywhere (a canal) moves along its flow; the part along the rotor's axis counts. Falling water counts too.
- A disc full of water in an average current gives (diameter − 3) × 25 + 100 KU/t (+150 at sea instead of +100), the old mean.
- A water rotor in line in front of this one, facing the same way within 16 blocks, slows the water (a wake).
The screen shows the disc seen from upstream (moving water, still water, air, solid blocks), the speed, the share of the disc that is water and a day of the current with the time now.

## Power data

This block uses no EU.

## Procedure

1. Put a rotor into the rotor slot.
2. Place the machine in a river or an ocean with its front face in the water.
3. Place a Kinetic Generator with its front face against the back face of the machine.

## Notes

- Still water outside rivers and oceans does not move: the rotor stands still there.
- The rotor wears during operation. Replace the rotor before it breaks.

## Recipe

<RecipesFor id="mio_icif:kugenerator/block_water_kinetic_generator" fallbackText="-" />
