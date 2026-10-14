---
navigation:
  title: "Seismic Ore Scanner"
  icon: mio_icif:module/item_module_ore_scanner
  parent: suit/index.md
  position: 10
item_ids:
  - mio_icif:module/item_module_ore_scanner_mk2
  - mio_icif:module/item_module_ore_scanner
---

# Seismic Ore Scanner

<ItemImage id="mio_icif:module/item_module_ore_scanner" scale="3" />

## Function

When you switch on the unit, it sends a sonar pulse through the ground in a 16-block radius.
The pulse spreads out from the wearer as a ring of light. A short glitch effect runs over the visor.
Each ore block lights up when the echo reaches it. The ore shows as a glowing cell in the color of the ore, also through rock.
After the pulse, a small tag shows the nearest ore of each type and its distance.

## Power data

| Item | Value |
|---|---|
| Fits | Boots / Leggings |
| Power | 24 EU/t while on |
| Display | Needs a quantum helmet visor |

## Procedure

1. Install the unit into the quantum boots or leggings.
2. Wear a quantum helmet.
3. Walk to the mining area.
4. Switch on the unit and wait for the echo.
5. Dig towards the lit cells.

## Notes

- After the first pulse, the unit scans again every 50 ticks without a pulse effect.
- Switch the unit off and on again to send a new pulse.

## Mk.II

The Mk.II scanner reaches 24 blocks. The suit piece that carries it gets an **Ore filter** row in the Singularity Management Panel: click it to step through all ores and each common ore tag (c:ores/...). The scan starts again with a pulse, and the echo read-out shows the filter.

## Recipe

<RecipesFor id="mio_icif:module/item_module_ore_scanner" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_ore_scanner_mk2" fallbackText="-" />
