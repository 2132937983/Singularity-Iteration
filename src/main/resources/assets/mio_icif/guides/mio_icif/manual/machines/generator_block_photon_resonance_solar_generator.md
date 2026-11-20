---
navigation:
  title: "Photon Resonance Solar Generator"
  icon: mio_icif:generator/block_photon_resonance_solar_generator
  parent: generators.md
  position: 22
item_ids:
  - mio_icif:generator/block_photon_resonance_solar_generator
---

# Photon Resonance Solar Generator

<Row>
  <BlockImage id="mio_icif:generator/block_photon_resonance_solar_generator" scale="3" />
</Row>

## Function

The machine is a photon resonance panel: resonant nano-antennas on its surface absorb a wide spectrum, diffuse light too. It reaches its rated output in weak light (450 W/m²).
Under cover it still gets 45 % of the sky light. Rain keeps about 85 %, a thunderstorm about 61 %.
Heat hardly matters: the output falls only 0.2 % per °C above 25 °C, by 15 % at most.
At night, with open sky straight above it, the machine radiates its heat to the cold clear sky and makes power from that flow: 25 % of its rating (96 EU/t). Rain halves it, a thunderstorm leaves a quarter.
The screen shows today's record: the sunlight on the panel, a clear day for comparison and the output, with the night sky factor.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | HV (512 EU) |
| Output | 384 EU/t |
| Output at night | 96 EU/t |
| Full output from | 450 W/m² |
| Energy storage | 40,000,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the generator under open sky; it also works where it rains often or under cover.
2. Connect a HV cable to the generator.
3. Put a battery into the charge slot.

## Recipe

<RecipesFor id="mio_icif:generator/block_photon_resonance_solar_generator" fallbackText="-" />
