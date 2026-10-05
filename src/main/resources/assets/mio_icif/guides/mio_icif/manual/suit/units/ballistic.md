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
The visor shows the path, the impact ring, the time of flight, the range and the drop.
For a moving creature near the aim line, a yellow LEAD marker shows the point to aim at.

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
4. Put the impact ring on the LEAD marker.
5. Release the shot.

## Notes

- The ring turns red when the weapon is ready or the path hits a creature.

## Recipe

<RecipesFor id="mio_icif:module/item_module_ballistic" fallbackText="-" />
