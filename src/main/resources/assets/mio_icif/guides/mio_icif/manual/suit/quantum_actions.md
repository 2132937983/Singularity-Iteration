---
navigation:
  title: "Quantum actions"
  icon: mio_icif:module/item_module_phase_jump_drive
  parent: suit/index.md
  position: 3
item_ids:
  - mio_icif:module/item_module_phase_jump_drive
  - mio_icif:module/item_module_phase_jump_drive_mk2
  - mio_icif:module/item_module_phase_modulator
  - mio_icif:module/item_module_phase_modulator_mk2
  - mio_icif:module/item_module_coherence_capacitor
  - mio_icif:module/item_module_entanglement_relay
  - mio_icif:module/item_module_deflector_mk2
  - mio_icif:module/item_module_behavior_predictor_mk2
  - mio_icif:module/item_quantum_bay_expander
---

# Quantum actions

Since 0.1.10.0 the quantum suit has active abilities. They use the action keys **V** and **B**, the same keys as the nano bursts. Choose what each key does in the Singularity Management Panel: select a quantum piece and use the two key rows under "Quantum core". By default the keys take the first two actions that you wear.

## Coherence

The actions are paid with **coherence**, shown as the ring around the crosshair (0 to 100). It comes back by itself 1.5 s after the last action, 8 points per second, paid with EU from the suit.
When it reaches 0 the core **decoheres**: for 4 s no action works, a running force field falls, the visor glitches and the sensors refresh at half speed.

## Actions

| Action | Unit | Coherence | Cooldown |
|---|---|---|---|
| Quantum Blink | <ItemLink id="mio_icif:module/item_module_phase_jump_drive" /> | 25 | 2.5 s per charge |
| Phase Shift | <ItemLink id="mio_icif:module/item_module_phase_modulator" /> | 35 | 8 s |
| Force Field | <ItemLink id="mio_icif:module/item_module_deflector_mk2" /> | 15, then 10 per second | 6 s after it falls |
| Time Dilation | <ItemLink id="mio_icif:module/item_module_behavior_predictor_mk2" /> | 60 | 25 s |
| Entangled Volley | <ItemLink id="mio_icif:module/item_module_entanglement_relay" /> | 20 | 10 s |

- **Quantum Blink:** you jump to the point you look at, up to 12 blocks (Mk.II: 20 blocks and two charges). It never goes through a wall: your eyes, waist and feet need a clear line to the landing, and your body must fit there. No fall damage right after the jump.
- **Phase Shift:** for 1 s (Mk.II: 1.5 s) creature attacks, projectiles and blasts pass through you. You cannot attack or mine meanwhile. Lava, fire, drowning and the void still hurt. The Mk.II ends with a shock wave.
- **Force Field:** press once to raise a dome of 4 blocks, press again to drop it. Hostile projectiles stop in the air and fall when the dome falls; fireballs burst on it. Hostile creatures are pushed out.
- **Time Dilation:** for 4 s hostile creatures within 10 blocks move 70 % slower (bosses half as much) and their projectiles fly at a quarter of their speed, while you move 20 % faster. The game has one clock for everybody, so this is a local slow. A server option (single player only, off by default) slows the whole game instead.
- **Entangled Volley:** every Wingman in your dimension fires at your Smart Guidance locks, or at the creature you look at. The bolts are timed so that they all strike at the same moment.

A Mk.I deflector and a Mk.I behavior predictor keep their old effect; the Mk.II items add the action. Actions affect other players only with PvP on (server config section QuantumSuit has one switch per action).

## Combos and synergies

Since 0.1.10.1 the actions chain into combos (Quantum Tunnel, Frozen Volley, Static Barrage) and pairs of quantum units can be entangled for synergies. The suit computer also has a compute bandwidth. See Entanglement and synergies.

## Expansion bay

Put a <ItemLink id="mio_icif:module/item_quantum_bay_expander" /> into the unit slot of the Universal Suit Modification Station together with a quantum piece. The piece gets one more unit slot: helmet 5, chestplate 4, leggings 3, boots 3. The expansion is permanent; pieces without it keep their old slot count.

## Coherence capacitor

The <ItemLink id="mio_icif:module/item_module_coherence_capacitor" /> (leggings or boots) adds 50 to the coherence maximum and makes it regenerate 25 % faster.

## Recipes

<RecipesFor id="mio_icif:module/item_module_phase_jump_drive" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_phase_jump_drive_mk2" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_phase_modulator" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_phase_modulator_mk2" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_coherence_capacitor" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_entanglement_relay" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_deflector_mk2" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_module_behavior_predictor_mk2" fallbackText="-" />

<RecipesFor id="mio_icif:module/item_quantum_bay_expander" fallbackText="-" />
