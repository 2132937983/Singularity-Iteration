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

Hold **X** to open the wheel, move the mouse to a sector or an item and let go; a click works too, and the centre cancels. A tap on **X** (without opening the wheel) is the Smart Ping: a focus attack on the creature in your crosshair, or a move to the ground there. Since 0.1.11.230 the wheel opens without Wingmen or an Entanglement Relay as well: the squad sectors are grey, Gear and Favorites work. **G** only switches the mode of an electric tool.

When the wheel opens, it takes the point and the creature in your crosshair (up to 256 blocks; a look at the sky gives a point 48 blocks ahead in the air). The tags above the wheel choose all Wingmen or one; the mouse wheel cycles them too.

| Sector | What it holds |
|---|---|
| MOVE | Fly to the point, then guard it: engage hostile creatures within the hold radius (10 blocks). No fighting on the way. |
| SUPPRESS | Patrol a ring above the area and clear every hostile creature in it (14 blocks). |
| FOCUS | Attack the creature in your crosshair until it is down (up to 128 blocks from the Wingman), then form up. |
| FORM UP | End the orders: back to formation and the tactical mode. |
| CONTROL ▸ | The three tactical modes, return home, and the stance: Auto stance, In the air, On foot (see Wingman tactics on the Armory page). A mode set for one Wingman is its own. |
| SUIT ▸ | Suit up, send the suit, remote pilot, Clean Slate, call a piece, quantum recall (see below). |
| GEAR ▸ | Tactical map, holo table, jetpack mode, HUD on/off, HUD layout, Singularity panel. |
| FAVORITES ★ | Up to 3 items of your choice. Right-click an item in any fan to pin it here or unpin it. At first: tactical map, jetpack mode, remote pilot. |

## Keys

Since 0.1.11.230 the keys are consolidated; by default only these are taken:

| Key | Does |
|---|---|
| **X** | Hold: the command wheel. Tap: the Smart Ping. |
| **V** / **B** | Action keys (primary / secondary): a nano burst, a quantum action or the core burst, assigned in the Singularity panel. |
| **G** | With an electric tool in hand: switch its mode. |
| **Left Alt** | Tap: open the Singularity panel. Hold: the suit detail view. |
| Sprint | Throttle of the Special Maneuver Mode and the jetpack boost. |

The tactical map, piece call, remote pilot, jetpack mode, HUD on/off and layout, core burst, boost and detail view can still have keys of their own in the controls, unbound by default. On an update from an older version, bindings still on their old default move to the new one; keys you set yourself stay as they are.

A Wingman on an order no longer follows you: it guards its point instead of staying within 28 blocks of you. A Wingman in an unloaded chunk cannot move; the squad list shows it as unloaded, and the quantum recall or "Recall all" in the console reaches it.

## Data link

With the data link on, the hostile creatures your Wingmen see (24 blocks around each) are marked on the holomap, the tactical map and the holo table. With the Fleet Datalink synergy you also get the world markers and your crosshair target becomes the Wingmen's first target.

## Quantum recall

Every Wingman you own folds back into its Armory at once: in any dimension, loaded or not (an unloaded Wingman's chunk loads for a moment). It costs 30 coherence and has a 30 s cooldown.

## Squad HUD

While the command link works, a squad panel on the left lists each Wingman by its number (W1 to W6: the Armory suit it came from, so the number stays when other Wingmen come and go) with its mode (* = an individual mode), its order and distance, its health and its distance from you. Wingmen out of view get a chevron on the screen edge. In the world, hold points, suppression rings and focus targets are marked for you alone.

## 3D holo table

With a <ItemLink id="mio_icif:module/item_module_holomap_mk2" />, choose Holo table in the wheel's Gear fan. A hologram of the terrain around you rises in front of you: the columns grow out from the centre, interference bands and a scan line run over it. It shows you, your Wingmen with their numbers and orders, hostile creatures (the ones in the data link with a ring), other players and, with Ore Hologram, the scanned ores.

- Drag: turn the table. Mouse wheel: 24, 48 or 96 blocks.
- Left click on the terrain: MOVE there. Right click: SUPPRESS there. Left click on a creature: FOCUS.
- H: HOLD. F: FORM UP. 1 to 6: choose the Wingman with that number. 0: all Wingmen. A held key acts once.
