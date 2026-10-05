---
navigation:
  title: "Geomagnetic Generator"
  icon: mio_icif:generator/block_geomagnetic_generator
  parent: generators.md
  position: 12
item_ids:
  - mio_icif:generator/block_geomagnetic_generator
---

# Geomagnetic Generator

<Row>
  <BlockImage id="mio_icif:generator/block_geomagnetic_generator" scale="3" />
</Row>

## Function

The machine makes EU from the geomagnetic field when its structure is complete.
The output decreases below sea level and with air or water below the machine.
Nether biomes and mountain biomes increase the output.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | ZPM (131,072 EU) |
| Output | 30,720 EU/t |
| Energy storage | 400,000,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the Geomagnetic Generator on solid ground.
2. Put two Geomagnetic Antennas on top of the generator, one above the other.
3. Put four Geomagnetic Pedestals around the block below the generator.
4. Connect a cable to the generator.
5. Put a battery into the battery slot to charge it.

## Notes

- The machine checks up to 20 blocks below it. Each air or water block decreases the output.

## Recipe

<RecipesFor id="mio_icif:generator/block_geomagnetic_generator" fallbackText="-" />
