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
The output is ordinary Steam. Superheated Steam gives twice as much, as it took twice the heat to raise.
The machine takes heat from all faces except the front face.

## Power data

| Item | Value |
|---|---|
| Steam | 10 mB + 1 HU → 10 mB |
| Superheated Steam | 10 mB + 1 HU → 20 mB |

This block uses no EU.

## Procedure

1. Place a heat generator against a side face of the machine.
2. Pump Steam or Superheated Steam into the machine.
3. Place a tank at the front face or the back face to collect the output steam.

## Recipe

<RecipesFor id="mio_icif:producer/block_steam_repressurizer" fallbackText="-" />
