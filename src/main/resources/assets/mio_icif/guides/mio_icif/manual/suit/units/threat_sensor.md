---
navigation:
  title: "Threat Sensor"
  icon: mio_icif:module/item_module_threat_sensor
  parent: suit/index.md
  position: 17
item_ids:
  - mio_icif:module/item_module_threat_sensor
---

# Threat Sensor

<ItemImage id="mio_icif:module/item_module_threat_sensor" scale="3" />

## Function

The server checks all creatures within 32 blocks four times per second.
A yellow warning means that a player or a neutral creature looks at the wearer.
A red warning means that a creature targets the wearer or that a hostile creature looks at the wearer.
Markers on the outer ring of the HUD point to each threat. The holomap shows the same colors.

## Power data

| Item | Value |
|---|---|
| Fits | Chestplate |
| Power | 6 EU/t while on |
| Display | Needs a quantum helmet visor |

## Procedure

1. Install the unit into the quantum chestplate.
2. Wear a quantum helmet.
3. Turn towards a red marker.
4. Engage the target or leave the area.

## Notes

- The banner at the top counts LOCK (red) and WATCH (yellow) contacts.

## Recipe

<RecipesFor id="mio_icif:module/item_module_threat_sensor" fallbackText="-" />
