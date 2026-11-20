---
navigation:
  title: "METS Advanced Solar Generator"
  icon: mio_icif:generator/block_mets_advanced_solar_generator
  parent: generators.md
  position: 19
item_ids:
  - mio_icif:generator/block_mets_advanced_solar_generator
---

# METS Advanced Solar Generator

<Row>
  <BlockImage id="mio_icif:generator/block_mets_advanced_solar_generator" scale="3" />
</Row>

## Function

The machine is a photon resonance panel: resonant nano-antennas on its surface absorb a wide spectrum, diffuse light too. It reaches its rated output in weak light (600 W/m²).
Under cover it still gets 30 % of the sky light. Rain keeps about 85 %, a thunderstorm about 61 %.
Heat hardly matters: the output falls only 0.2 % per °C above 25 °C, by 15 % at most.
At night, with open sky straight above it, the machine radiates its heat to the cold clear sky and makes power from that flow: 15 % of its rating (7 EU/t). Rain halves it, a thunderstorm leaves a quarter.
The screen shows today's record: the sunlight on the panel, a clear day for comparison and the output, with the night sky factor.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | MV (128 EU) |
| Output | 48 EU/t |
| Output at night | 7 EU/t |
| Full output from | 600 W/m² |
| Energy storage | 200,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the generator under open sky; it also works where it rains often or under cover.
2. Connect a MV cable to the generator.
3. Put a battery into the charge slot.

## Recipe

<RecipesFor id="mio_icif:generator/block_mets_advanced_solar_generator" fallbackText="-" />
