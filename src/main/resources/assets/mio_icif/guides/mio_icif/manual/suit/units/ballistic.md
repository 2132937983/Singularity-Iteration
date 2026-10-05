---
navigation:
  title: "Ballistic Computer"
  icon: mio_icif:module/item_module_ballistic
  parent: suit/index.md
  position: 13
item_ids:
  - mio_icif:module/item_module_ballistic
---

# Ballistic Computer

<ItemImage id="mio_icif:module/item_module_ballistic" scale="3" />

## Function

The unit calculates the path of the projectile of the held weapon.
It uses the same gravity and drag steps as the game, so the path matches the real flight.
The visor shows the path as a line of moving dots. The dots become brighter near the impact point.
A turning ring on the surface marks the impact point. A tag next to it shows the range and the time of flight.
When the path hits a creature, red brackets lock on to it and a lock tone sounds.
For a moving creature near the aim line, a yellow LEAD diamond shows the point to aim at.

## Power data

| Item | Value |
|---|---|
| Fits | Helmet |
| Power | 4 EU/t while on |
| Display | Needs a quantum helmet visor |

## Procedure

1. Install the unit into the quantum helmet.
2. Hold a bow, a crossbow, a trident or a throwable item.
3. Draw the bow until the panel shows READY.
4. Put the impact ring on the LEAD diamond.
5. Release the shot.

## Notes

- The path is grey while the weapon charges, amber when it is ready and red when it hits a creature.

## Recipe

<RecipesFor id="mio_icif:module/item_module_ballistic" fallbackText="-" />
