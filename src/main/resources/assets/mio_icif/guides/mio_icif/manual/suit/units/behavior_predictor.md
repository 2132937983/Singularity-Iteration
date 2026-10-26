---
navigation:
  title: "Behavior Predictor"
  icon: mio_icif:module/item_module_behavior_predictor
  parent: suit/index.md
  position: 15
item_ids:
  - mio_icif:module/item_module_behavior_predictor_mk2
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

## Paths, countdowns and ranks

- The path follows the movement rules of the game: a creature slides farther on ice, slows down and sinks slowly in water, steps up slabs, jumps onto a one-block ledge (before, the path stopped there) and falls off an edge.
- A creature that was just hit slides back and then walks on. A creature that hunts you stops in front of you.
- The attack mark shows the time to the attack: AIM with the bow draw or the crossbow load, FIRE with the ghast charge, DET with the creeper swell, ATK with the swing interval that the unit learns from the creature.
- #1 to #3 beside the tags rank the most dangerous hostile creatures: how hostile it is, the damage of one hit or its explosion, and how soon it can attack.
- Creatures farther than 12 blocks that do not threaten you get a new path every 4 ticks instead of every 2.

## Mk.II

The Mk.II predictor adds the **Time Dilation** action (see Quantum actions) and the data link below.

## Mk.II data link

The server sends the Mk.II predictor the next steps of the route of the 8 most dangerous creatures within 24 blocks. Their paths follow that route round corners. They are drawn in violet with a LINK mark.

## Recipe

<RecipesFor id="mio_icif:module/item_module_behavior_predictor" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_behavior_predictor_mk2" fallbackText="-" />
