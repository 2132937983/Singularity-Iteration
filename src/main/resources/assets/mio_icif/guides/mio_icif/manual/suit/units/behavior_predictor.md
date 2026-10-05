---
navigation:
  title: "Behavior Predictor"
  icon: mio_icif:module/item_module_behavior_predictor
  parent: suit/index.md
  position: 15
item_ids:
  - mio_icif:module/item_module_behavior_predictor
---

# Behavior Predictor

<ItemImage id="mio_icif:module/item_module_behavior_predictor" scale="3" />

## Function

The unit follows the movement of each creature within 24 blocks.
It draws the path of the creature for the next second.
It marks attack signs before the attack: aim, charge, cast, swell, beam and dive.
A red line shows the aim axis of a creature that aims a weapon.

## Power data

| Item | Value |
|---|---|
| Fits | Helmet |
| Power | 8 EU/t while on |
| Display | Needs a quantum helmet visor |

## Procedure

1. Install the unit into the quantum helmet.
2. Watch the intent codes next to the tags.
3. Step out of a red aim line before the shot.

## Notes

- Codes for close and ranged attacks: ATK melee, AIM ranged weapon, DET explosion.
- Codes for special attacks: CAST spell, BEAM guardian, FIRE ghast, DIVE vex, AGGRO enderman.

## Recipe

<RecipesFor id="mio_icif:module/item_module_behavior_predictor" fallbackText="-" />
