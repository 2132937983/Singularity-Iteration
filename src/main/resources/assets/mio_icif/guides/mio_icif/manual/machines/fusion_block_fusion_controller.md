---
navigation:
  title: "Fusion Reactor Console"
  icon: mio_icif:fusion/block_fusion_controller
  parent: heavy.md
  position: 40
item_ids:
  - mio_icif:fusion/block_fusion_controller
---

# Fusion Reactor Console

<Row><BlockImage id="mio_icif:fusion/block_fusion_controller" scale="3" /></Row>

## Function

The console of the tokamak fusion reactor. The structure is a torus (a doughnut) 25 blocks across and 11 high: a central solenoid, eight D-shaped toroidal field coils, two outer poloidal field coils, the vacuum vessel, and inside it the blanket, the divertor and the plasma channel.

D-T fusion gives 20% of its energy to the plasma, which keeps it hot, and 80% to neutrons, which go into the blanket. The blanket takes the nuclear reactor's components: fuel rods fission from the neutrons (subcritical: no fusion, no fission), lithium cells breed tritium, and vents, heat exchangers and coolant cells keep it cool. The best blanket gives twice the output of the Mekanism fusion reactor.

## Power data

| Item | Value |
|---|---|
| Output | up to 39,200,000 EU/t (best blanket) |
| Fusion power | 612,500 EU/t per feed step, 9,800,000 EU/t at 16 steps |
| Input voltage tier | LuV (32,768 EU), any face |
| Input storage | 20,000,000 EU |
| Magnet upkeep | 392,000 EU/t |
| Start-up (central solenoid) | 300,000,000 EU, 30 s at the fastest |
| Heating | 245,000 EU/t per heating port, at most a tenth of the fusion power |

## Structure

<GameScene zoom="2" interactive={true}>
  <SiStructure id="mio_icif:fusion_reactor" />
</GameScene>

<SiStructureInfo id="mio_icif:fusion_reactor" />

- Every block must be in place, and the plasma channel inside the ring must be air.
- A port slot (a duct end; on the east side a column of three: disruption mitigation on top, cryogenic in the middle, exhaust below) takes any fusion port. Close the slots you do not use with a vacuum vessel wall.
- To run, the reactor needs a cryogenic port, at least one heating port (neutral beam injector or electron cyclotron heater), an exhaust port and a power output port. Fuel injection and disruption mitigation ports are optional.
- The whole structure turns with the console. The screen names the first wrong block and its position.

## Procedure

1. Build the structure and connect LuV power to the console.
2. Put deuterium fuel rods and tritium fuel rods into the fuel slots (or through a fuel injection port). A fresh rod gives 10,000 fuel units, and the empty fuel rod comes back.
   - Deuterium fuel rods: a thermal centrifuge (heat 4,000 or more, 1,200 ticks) turns a hot water cell into an unstable hot water cell; in a canner in canning mode, put the unstable hot water cell in the left slot and an empty fuel rod in the middle slot.
   - A deuterium fuel rod also works in a nuclear reactor as a neutron source: each adjacent fuel rod takes one more pulse per cycle, for 5,000 cycles (half a tritium rod), with no decay heat; then the empty fuel rod is left.
3. Fill the blanket (9 columns x 6 rows; column 1 faces the plasma) with components.
4. Apply a redstone signal. The reactor goes through: magnet cooldown (30 s), solenoid charge, heating, ignition (14 keV, about 160 million K), burn (about 15 keV).
5. Set the feed (1-16) with the screen's buttons. After ignition no heating is needed. The output leaves through the power output ports.

## Plasma

- Before ignition the heating ports heat the plasma (Q = 10). From 14 keV the alpha particles keep it burning, and Q is infinite.
- The solenoid's flux carries a burn of about 400 s, then it must charge again. With two electron cyclotron heaters the plasma current is driven without it (1.25% of the fusion power each), and the burn is steady.
- The plasma uses 1 unit of deuterium and 1 unit of tritium per feed step and tick. When the fuel runs out, the plasma ends gently.
- Once it makes power, the console keeps 40 ticks of magnet upkeep from its own output.

## Blanket

- The fusion neutrons weaken column by column: at full power each slot of columns 1, 2 and 3 gets 2, 1 and 1 neutrons per cycle, and the columns after that get none.
- Fuel rods count them as extra pulses, so they make more heat and more power (the nuclear reactor's own rules). Lithium cells breed 40 per neutron and become tritium fuel rods at 10,000; about 5 lithium cells in column 1 keep the tritium self-sufficient.
- Output = neutron power x (1 + 0.2 x the share of neutrons the lithium cells take) + 14,000 x the rods' fission output (in nuclear reactor EU/t) x the power fraction. It is capped at 39,200,000 EU/t.
- For example, proton quad rods boxed in by iridium overclocked vents reach the cap. A blanket at its maximum heat (10,000 plus platings) melts down (see below).

## Hazards

- **Disruption:** it happens when:
  - the magnets quench;
  - the structure is broken while there is plasma;
  - helium ash builds up without an exhaust port (60 s).

  It destroys the components in the blanket's first column and heats the blanket by half its maximum. With a coolant cell in the disruption mitigation slot, one is injected (used up) instead: the components survive and the blanket heats less: 30% with a 10k coolant cell, 20% with a 30k one, 5% with a 60k one (a used cell counts the cold it has left). A disruption never damages the structure.
- **Meltdown:** a blanket at its maximum heat (overheated by itself, or by a disruption's heat on top) melts down: the console and its contents are gone, and a nuclear blast goes off at the centre of the torus. Its power is 50 times the nuclear reactor's explosion power limit (config fusionExplosionMultiplier; by default 50 x 45 = 2,250), its largest radius twice that but at most 2,000 blocks. With the multiplier at 0 an overheated blanket only disrupts the plasma; with enableNuclearExplosion off it is a local explosion that breaks nothing around.
- **Quench:** without the magnet upkeep, the magnets lose superconductivity. After 60 s they must be cooled again.
- **Radiation:** while the plasma burns, living things within 16 blocks of the torus take 0-3 radiation damage a second. Shielding blocks reduce it, and a full hazmat suit protects completely.

## Recipe

<RecipesFor id="mio_icif:fusion/block_fusion_controller" fallbackText="-" />
