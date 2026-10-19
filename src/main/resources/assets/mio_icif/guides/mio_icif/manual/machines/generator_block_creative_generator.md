---
navigation:
  title: "Creative Generator"
  icon: mio_icif:generator/block_creative_generator
  parent: generators.md
  position: 18
item_ids:
  - mio_icif:generator/block_creative_generator
---

# Creative Generator

<Row>
  <BlockImage id="mio_icif:generator/block_creative_generator" scale="3" />
</Row>

## Function

A debug generator for creative mode. It sends the output that you set, every tick, on all six faces.
Its energy never melts a cable, never strips insulation, never shocks a player and never makes a machine explode,
whatever the packet size.

- Only a player in creative mode can place it. It has no recipe and drops nothing.
- It cannot be mined in survival mode and resists explosions.
- Only a player in creative mode or an operator (permission level 2) can open its screen.

## Settings

| Control | Effect |
|---|---|
| Text field + Set (or Enter) | Any output from 0 to 1,000,000,000,000 EU/t. Suffixes k, M, G and T work, for example `2.5M`. |
| Preset keys | 32, 128, 512, 2,048, 8,192, 32,768, 131,072 and 1M EU/t |
| x10 and /10 | Multiplies or divides the output by ten |
| ON / OFF | Switches the output |

The screen also shows the EU that left the generator in the last tick.

## Notes

- A machine still takes only what it has room for and what its own input rate allows.
- Cable loss still applies.
- The Infinite Generator stays as it was (fixed 8,192 EU/t, normal overload rules).
