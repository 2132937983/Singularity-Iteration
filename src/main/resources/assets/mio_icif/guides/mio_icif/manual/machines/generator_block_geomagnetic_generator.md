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
The machine runs on the field's change, so its output follows the field's disturbance: 30,720 EU/t is the output at a disturbance of 1. On a quiet day the sunlit sky stirs the field by day (about 0.55 at noon, 0.08 all night); every day there is a 12 % chance of a geomagnetic storm (Kp 6 to 9, half a day to a day and a half), which raises the disturbance up to 1.5. All generators in a world see the same storms. On average it makes about 27 % of the old constant output.
The screen is a magnetogram: the disturbance over the last three days, Kp every three hours, the output and the site's ratio. The Geomagnetic Detector shows the site's ratio and the current Kp.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | ZPM (131,072 EU) |
| Output | 30,720 EU/t at a disturbance of 1 (about 8,300 EU/t on average) |
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
