---
navigation:
  title: "Steam Repressurizer"
  icon: mio_icif:producer/block_steam_repressurizer
  parent: machines.md
  position: 63
item_ids:
  - mio_icif:producer/block_steam_repressurizer
---

# Steam Repressurizer

<Row>
  <BlockImage id="mio_icif:producer/block_steam_repressurizer" scale="3" />
</Row>

## Function

The Steam Repressurizer uses heat (HU) to increase the volume of Steam or Superheated Steam.
The output fluid is the same type as the input fluid. Superheated Steam gives a larger gain.
The machine takes heat from all faces except the front face.

## Power data

This block uses no EU.

## Procedure

1. Place a heat generator against a side face of the machine.
2. Pump Steam or Superheated Steam into the machine.
3. Place a tank at the front face or the back face to collect the output steam.

## Recipe

<RecipesFor id="mio_icif:producer/block_steam_repressurizer" fallbackText="-" />
