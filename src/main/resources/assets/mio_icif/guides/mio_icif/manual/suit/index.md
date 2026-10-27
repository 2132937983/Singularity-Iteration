---
navigation:
  title: "Quantum suit upgrades"
  icon: mio_icif:armor/item_armor_quantum_helmet
  parent: index.md
  position: 5
---

# Quantum suit upgrades

The quantum suit accepts upgrade units. Each unit adds one sensor or one defense function.
The Universal Suit Modification Station installs and removes the units. Since 0.1.8.5 the nano suit takes units too (see Nano suit units).
Each piece has a fixed number of unit slots: helmet 4, chestplate 3, leggings 2, boots 2.
A Quantum Bay Expander adds one more slot to a piece (see Quantum actions).
Two quantum units can be entangled for a synergy; the suit computer has a compute bandwidth (see Entanglement and synergies).
With an Entanglement Relay the suit commands its Wingmen: command wheel, orders, holo table (see Quantum command).
A HUD unit shows its data only when you wear a quantum helmet. The helmet is the visor.
Each installed unit adds a switch to the equipment console. A unit uses power only while its switch is on.
The nano and quantum suits glow: their lights and circuit lines shine in the light colour, amber below half charge and red below a quarter, with a slow breath, an energy band that runs down the body and a heartbeat when the charge is low. An empty piece is dark. When a charged piece comes on (put on, the Armory suit-up, charged from empty) the light spreads from the chest core. Shader packs treat the light as emissive. Client options suit_glow and suit_glow_strength, or the Look page of the equipment console.
Since 0.1.10.30 the quantum suit takes a palette like the nano suit (equipment console, Look page, Palette): the plate colour and the light colour of each piece. The light colour also colours the glowing parts of a frame core exterior.
Since 0.1.10.31 the quantum suit shows its thrust in normal mode too: in jetpack flight two jets burn under the back unit (and at the soles with the quantum boots); a boost jump of the quantum boots leaves a ring and a dust ring at the take-off point and the soles flare; a hard landing (3.5 blocks and more) raises a shock ring; the quantum leggings draw thin air streaks from the calves while you sprint. Other players see it too. The flames are animated and turn from white through blue to violet with the thrust, and throw sparks at full thrust. Client option motion_fx.
Visor glass (client option visor_glass or the Look page, off by default): in first person with a nano or quantum helmet, a faint rim and colour fringes, a reflection that slides as you turn, raindrops in the rain and cracks when the helmet is almost empty. In Special Maneuver hover your own palm thrusters show in first person; a gauntlet flashes when its hand fires an energy weapon.

| Unit | Fits | Power |
|---|---|---|
| <ItemLink id="mio_icif:module/item_module_ore_scanner" /> | Boots / Leggings | 24 EU/t while on |
| <ItemLink id="mio_icif:module/item_module_grid_telemetry" /> | Leggings / Helmet | 4 EU/t while on |
| <ItemLink id="mio_icif:module/item_module_entity_esp" /> | Helmet | 12 EU/t while on |
| <ItemLink id="mio_icif:module/item_module_ballistic" /> | Helmet | 4 EU/t while on |
| <ItemLink id="mio_icif:module/item_module_blast_warning" /> | Chestplate / Helmet | 2 EU/t while on |
| <ItemLink id="mio_icif:module/item_module_behavior_predictor" /> | Helmet | 8 EU/t while on |
| <ItemLink id="mio_icif:module/item_module_holomap" /> | Helmet | 10 EU/t while on |
| <ItemLink id="mio_icif:module/item_module_threat_sensor" /> | Chestplate | 6 EU/t while on |
| <ItemLink id="mio_icif:module/item_module_deflector" /> | Chestplate | 4000 EU per damage point, max 12 points per hit |
| <ItemLink id="mio_icif:module/item_module_phase_jump_drive" /> | Leggings / Boots | Action: Quantum Blink (coherence) |
| <ItemLink id="mio_icif:module/item_module_phase_modulator" /> | Chestplate | Action: Phase Shift (coherence) |
| <ItemLink id="mio_icif:module/item_module_entanglement_relay" /> | Chestplate / Leggings | Action: Entangled Volley (coherence) |
| <ItemLink id="mio_icif:module/item_module_coherence_capacitor" /> | Leggings / Boots | Passive: coherence maximum +50 |
| <ItemLink id="mio_icif:module/item_module_compute_core" /> | Chestplate / Leggings | 6 EU/t while on; bandwidth +8 QB, links +1 |

<SubPages id="suit/index.md" icons={true} />
