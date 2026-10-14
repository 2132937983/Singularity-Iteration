---
navigation:
  title: "Quantum command"
  icon: mio_icif:module/item_module_entanglement_relay
  parent: suit/index.md
  position: 5
---

# Quantum command

Since 0.1.10.2 a quantum suit with an active <ItemLink id="mio_icif:module/item_module_entanglement_relay" /> commands its Wingmen directly: orders by point, focus fire, a data link and an emergency recall. Orders reach your Wingmen at any distance in your dimension.

## Command wheel

Hold **G** to open the wheel, move the mouse to a sector and let go. A click works too; the centre cancels.
G also toggles the mode of a toggleable electric tool: with such a tool in a hand, G stays the tool toggle. Both keys can be bound separately.

When the wheel opens, it takes the point and the creature in your crosshair (up to 256 blocks; a look at the sky gives a point in the air 48 blocks ahead). The chips above the wheel choose the whole squad or one Wingman; the mouse wheel steps through them.

| Sector | Order |
|---|---|
| MOVE | Fly to the point, then guard it: engage hostile creatures within the hold radius (10 blocks). No fighting on the way. |
| HOLD | Hold where each Wingman is now and engage what comes near. |
| SUPPRESS | Patrol a ring above the area and clear every hostile creature in it (14 blocks). |
| FOCUS | Attack the creature in your crosshair until it is down (up to 128 blocks from the Wingman), then form up. |
| VOLLEY | The Entangled Volley (as on the action key). |
| FORM UP | End the orders: back to formation and the tactical mode. |
| MODE | A fan: the four tactical modes, "follow squad" and "return home". A mode for one Wingman is its own; the squad mode no longer changes it until "follow squad". |
| DATA LINK | On or off (see below). |
| HOLO TABLE | Project the 3D holo table (holomap Mk.II). |
| Q-RECALL | Quantum recall (see below). |

A Wingman on an order no longer follows you: it guards its point instead of staying within 28 blocks of you. A Wingman in an unloaded chunk cannot move; the squad list shows it as UNLOADED, and only the quantum recall reaches it.

## Data link

With the data link on, the hostile creatures your Wingmen see (24 blocks around each) are marked on the holomap, the tactical map and the holo table. With the Fleet Datalink synergy you also get the world markers and your crosshair target becomes the Wingmen's first target.

## Quantum recall

Every Wingman you own folds back into its Armory at once: in any dimension, loaded or not (an unloaded Wingman's chunk loads for a moment). It costs 30 coherence and has a 30 s cooldown.

## Squad HUD

While the command link works, a squad panel on the left lists each Wingman with its mode (* = an individual mode), its order and distance, its health and its distance from you. Wingmen out of view get a chevron on the screen edge. In the world, hold points, suppression rings and focus targets are marked for you alone.

## 3D holo table

With a <ItemLink id="mio_icif:module/item_module_holomap_mk2" />, hold the tactical map key (**N**) or choose HOLO TABLE on the wheel. A hologram of the terrain around you rises in front of you: the columns grow out from the centre, interference bands and a scan line run over it. It shows you, your Wingmen with their numbers and orders, hostile creatures (the ones in the data link with a ring), other players and, with Ore Hologram, the scanned ores.

- Drag: turn the table. Mouse wheel: 24, 48 or 96 blocks.
- Left click on the terrain: MOVE there. Right click: SUPPRESS there. Left click on a creature: FOCUS.
- H: HOLD. F: FORM UP. 0 to 9: choose all or one Wingman.
