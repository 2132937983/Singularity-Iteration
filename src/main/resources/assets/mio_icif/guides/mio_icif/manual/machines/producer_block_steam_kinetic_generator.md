---
navigation:
  title: "Steam Kinetic Generator"
  icon: mio_icif:producer/block_steam_kinetic_generator
  parent: machines.md
  position: 62
item_ids:
  - mio_icif:producer/block_steam_kinetic_generator
---

# Steam Kinetic Generator

<Row>
  <BlockImage id="mio_icif:producer/block_steam_kinetic_generator" scale="3" />
</Row>

## Function

The Steam Kinetic Generator turns Steam or Superheated Steam into kinetic energy (KU). The machine outputs KU on its front face.
The machine needs a Steam Turbine in the turbine slot. The turbine loses durability during operation.
The machine sends the exhaust steam to an adjacent Condenser and keeps the condensed Distilled Water in a tank.
When an adjacent Condenser takes all of the exhaust, the turbine expands to 0.1 bar instead of 1 bar: about 1.6 × the KU for Steam and 1.3 × for Superheated Steam.
The screen is a plant mimic: inlet, turbine with its blade life, the shaft, condensate and the exhaust path.

## Power data

| Item | Value |
|---|---|
| Steam | 2 KU per mB, per 1,000 mB: 900 mB of exhaust steam and 1 mB of Distilled Water |
| Superheated Steam | 4 KU per mB, all of it leaves as exhaust steam |
| Throttle | KU × (1 − Distilled Water ÷ 1,000 mB) |

This block uses no EU.

## Procedure

1. Put a Steam Turbine into the turbine slot.
2. Place a Condenser next to the machine.
3. Place the KU consumer against the front face.
4. Pump steam into the machine.
5. Remove the Distilled Water with a pipe.

## Notes


> **WARNING:** Steam that the machine cannot vent causes explosions.

- Distilled Water in the tank reduces the output. A full water tank blocks the turbine.

## Recipe

<RecipesFor id="mio_icif:producer/block_steam_kinetic_generator" fallbackText="-" />
