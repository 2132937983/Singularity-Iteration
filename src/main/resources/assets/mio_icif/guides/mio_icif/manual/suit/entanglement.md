---
navigation:
  title: "Entanglement and synergies"
  icon: mio_icif:module/item_module_compute_core
  parent: suit/index.md
  position: 4
item_ids:
  - mio_icif:module/item_module_compute_core
  - mio_icif:module/item_module_smart_guidance_mk2
  - mio_icif:module/item_module_resonance_field_mk2
---

# Entanglement and synergies

Since 0.1.10.1 two quantum units can be **entangled**. An entangled pair works together as a **synergy**: an effect that neither unit has alone.

## Links

- The quantum chestplate is the **link hub**: it stores the links. Without a quantum chestplate no synergy works.
- The hub holds **2 links**, 3 with an active <ItemLink id="mio_icif:module/item_module_compute_core" />.
- Both units of a pair must be installed on worn quantum pieces. A synergy works while both units are switched on and powered.
- Links stay stored on the chestplate when you change units; they work again when the units come back. A link to a unit of a removed mod stays stored but does nothing.

## Entanglement Matrix

Open the Singularity Management Panel, select the quantum chestplate and click the **Links** row under "Quantum core".

- Left: your quantum units as nodes on a ring. A faint dashed thread: the pair can be linked. A solid thread: linked, but a unit is off. A pulsing double helix: the synergy works.
- Right: every synergy and its state (LINKED, READY, OFFLINE, OVER CAP, MISSING). Click a row to link or unlink it.
- You can also **drag** one unit onto another. While you drag, the synergy of the pair is shown; release to link or unlink.
- The footer shows the links in use and the compute bandwidth.

On the HUD the working synergies show as small chips beside the crosshair.

## Synergies

| Synergy | Units | Effect |
|---|---|---|
| Predictive Guidance | <ItemLink id="mio_icif:module/item_module_smart_guidance" /> + <ItemLink id="mio_icif:module/item_module_behavior_predictor" /> | Guided shots fly to where the target will be, along the path a creature walks. Turn +4 degrees per tick, 2 more locks. |
| Threat Sight | <ItemLink id="mio_icif:module/item_module_entity_esp" /> + <ItemLink id="mio_icif:module/item_module_threat_sensor" /> | Creatures that hunt you glow red through walls, with a ring that counts down to their attack. |
| Ore Hologram | <ItemLink id="mio_icif:module/item_module_holomap" /> + <ItemLink id="mio_icif:module/item_module_ore_scanner" /> | The scanned ores appear as 3D voxels in the holomap and the tactical map. |
| Ballistic Sync | <ItemLink id="mio_icif:module/item_module_ballistic" /> + <ItemLink id="mio_icif:module/item_module_smart_guidance" /> | Locks take 30 % less time; the planned paths of the guided shots are drawn. |
| Blast Forecast | <ItemLink id="mio_icif:module/item_module_blast_warning" /> + <ItemLink id="mio_icif:module/item_module_behavior_predictor" /> | A coming blast shows its radius as a 3D sphere, and an arrow points the way out. |
| Resonant Deflection | <ItemLink id="mio_icif:module/item_module_deflector" /> + <ItemLink id="mio_icif:module/item_module_resonance_field" /> | 25 % of the damage the deflector stops charges a resonance. At 8 it bursts and hits the hostile creatures within 4 blocks. |
| Afterimage Decoy | <ItemLink id="mio_icif:module/item_module_phase_jump_drive" /> + <ItemLink id="mio_icif:module/item_module_behavior_predictor" /> | A blink leaves a decoy at the start point. For 2 s the creatures that hunted you walk to it and cannot take you back. |
| Phase Counter | <ItemLink id="mio_icif:module/item_module_phase_modulator" /> + <ItemLink id="mio_icif:module/item_module_deflector" /> | Damage that passed through you during a phase shift (up to 20) returns as a shock wave when it ends. |
| Fleet Datalink | <ItemLink id="mio_icif:module/item_module_entanglement_relay" /> + <ItemLink id="mio_icif:module/item_module_entity_esp" /> | The hostile creatures your Wingmen see appear on your HUD; the Wingmen attack the creature in your sights. |

Each synergy has an on/off switch in the server config (section QuantumSuit, synergies). Synergy bursts hit players only with PvP on.

## Chain combos

Combos need no link, only timing. A banner and a sound show each combo.

- **Quantum Tunnel:** a Phase Shift within 1 s after a Quantum Blink costs half the coherence.
- **Frozen Volley:** an Entangled Volley inside your own Time Dilation. Up to 8 slowed creatures become targets, the bolts strike at once without the charge time, and they deal 1.5 times the damage.
- **Static Barrage:** switch your Force Field off with the key, and every projectile it holds flies back at its shooter as your shot.

## Compute bandwidth

Every active unit on a quantum piece loads the suit computer, measured in QB. A unit installed on two pieces counts once; a Mk.II unit costs 1 QB more.

| QB | Units |
|---|---|
| 3 | biometric scanner (ESP), behavior predictor, holomap, ore scanner, phase modulator, entanglement relay |
| 2 | smart guidance, threat sensor, phase jump drive |
| 1 | ballistic computer, blast warning, grid telemetry, deflector, resonance field |
| 0 | all other units |

The budget is **20 QB**, 28 QB with an active compute core. Every suit that 0.1.9.2 allowed fits in 20 QB. Over the budget the suit **saturates**: the sensors refresh at half rate and the coherence regenerates at half speed. Nothing switches off. The QB meter near the crosshair turns red.

## Quantum Compute Core

The <ItemLink id="mio_icif:module/item_module_compute_core" /> (chestplate or leggings, 6 EU/t) adds 8 QB to the budget and one more link to the hub.

## Mk.II: guidance and resonance field

- <ItemLink id="mio_icif:module/item_module_smart_guidance_mk2" />: up to 10 locks, guided shots turn up to 20 degrees per tick (Mk.I: 6 locks, 14 degrees).
- <ItemLink id="mio_icif:module/item_module_resonance_field_mk2" />: radius 3.75 and up to 9 blocks per tick (Mk.I: radius 2.5, 6 blocks).

The ore scanner, grid telemetry, biometric scanner and ballistic computer also have a Mk.II: see their pages.

## Recipes

<RecipesFor id="mio_icif:module/item_module_compute_core" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_smart_guidance_mk2" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_resonance_field_mk2" fallbackText="-" />
