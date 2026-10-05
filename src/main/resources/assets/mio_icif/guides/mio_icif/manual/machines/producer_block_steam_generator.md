---
navigation:
  title: "Steam Generator"
  icon: mio_icif:producer/block_steam_generator
  parent: machines.md
  position: 61
item_ids:
  - mio_icif:producer/block_steam_generator
---

# Steam Generator

<Row>
  <BlockImage id="mio_icif:producer/block_steam_generator" scale="3" />
</Row>

## Function

The Steam Generator uses heat (HU) and water to make Steam or Superheated Steam.
The GUI sets the water flow and the pressure valve. The machine pushes the steam into adjacent tanks.
Normal water calcifies the boiler and stops the machine. Distilled Water causes no calcification.

## Power data

This block uses no EU.

## Procedure

1. Place heat generators next to the Steam Generator.
2. Pump Distilled Water into the machine.
3. Open the GUI and set the water flow and the pressure valve.
4. Place a tank or a Steam Kinetic Generator next to the machine to take the steam.

## Notes


> **WARNING:** When the system heat passes the limit, the Steam Generator explodes and destroys itself.


> **WARNING:** Steam that no neighbor accepts causes small explosions.


## Recipe

<RecipesFor id="mio_icif:producer/block_steam_generator" fallbackText="-" />
