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

D-T fusion gives 20% of its energy to the plasma, which keeps it hot, and 80% to neutrons, which go into the blanket. The blanket takes the nuclear reactor's components: fuel rods fission from the neutrons (subcritical: no fusion, no fission), lithium cells breed tritium, and vents, heat exchangers and coolant cells keep it cool. The output has no cap: the example blanket below gives 41,440,000 EU/t, more than twice the Mekanism fusion reactor, and stronger blankets give more.

## Principles

This section explains the reactor's parts as a real tokamak works. The game's numbers are scaled; the sections below give the values that apply.

- **The reaction**: a deuterium (D) and a tritium (T) nucleus fuse into a helium-4 nucleus and a neutron, releasing 17.6 MeV. The helium nucleus (the alpha particle) carries 3.5 MeV, about 20%. It is charged, so the magnetic field keeps it in the plasma, where it heats the plasma further. The neutron carries 14.1 MeV, about 80%. It has no charge, so it leaves the plasma at once and goes into the blanket.
- **Why a hundred million degrees**: both nuclei are positively charged and push each other away; only fast enough nuclei get close enough to fuse. D-T fusion pays best at 10–20 keV (1 keV is about 11.6 million K), so the plasma has to be over 100 million K. No material can touch it, so a magnetic field holds it away from the walls.
- **Magnetic confinement**: a charged particle circles around a field line and moves freely along it, but hardly across it. The eight D-shaped **toroidal field coils** make a strong field running round the torus. The plasma's own current adds a field round its cross-section, and together they twist the field lines into helices, which keep the particles from drifting onto the walls. The **poloidal field coils** hold the plasma's position and shape.
- **The central solenoid**: the plasma current is driven like a transformer. The central solenoid is the primary winding and the ring of plasma is a secondary winding of a single turn. While the solenoid's flux changes, it drives a current in the plasma, so a burn lasts only until the flux runs out (pulsed operation) and the solenoid then has to be charged again. Electron cyclotron heaters can push the electrons along with microwaves and keep the current without the solenoid, so the burn can go on.
- **Heating and ignition**: the resistance of the plasma current alone heats the plasma only to some tens of millions of degrees. Beyond that it needs outside heating: the **neutral beam injector** fires fast neutral atoms into the plasma and the **electron cyclotron heater** heats the electrons with microwaves. As the plasma gets hotter the alpha particles leave more and more heat in it. Once they alone make up for the plasma's heat loss, the plasma is **ignited**: the outside heating can be switched off and the burn keeps itself going. Fusion power over outside heating is **Q**: ITER aims at Q = 10, and an ignited plasma has an infinite Q.
- **The Lawson criterion**: to ignite, the product of the plasma's density n, its temperature T and its energy confinement time τE (how long the plasma takes to lose its heat without heating) must be above about 3 × 10²¹ keV·s/m³. A stronger field and a larger machine confine better.
- **The divertor and the exhaust**: the helium nuclei, once they have given up their energy, are "helium ash", which dilutes the fuel and would put the plasma out. The magnetic field guides the plasma's outer layer down onto the divertor at the bottom of the torus, and the exhaust port's cryopumps draw off the ash together with the fuel that has not burned.
- **The blanket**: the neutrons pass through the vacuum vessel into the blanket and turn their energy into heat. Lithium in the blanket takes up neutrons and becomes tritium (lithium-6 + n → tritium + helium-4 + 4.8 MeV), so the fuel can sustain itself. This reactor's blanket also takes fission fuel rods: the neutrons split their nuclei, and each fission gives about 200 MeV, more than ten times a fusion, so the reactor is really a **fusion-fission hybrid**. The rods cannot go critical on their own: when fusion stops, fission stops.
- **Superconducting magnets and the cold**: the coils are superconductors, so their current flows without resistive loss, but only at about −269 °C. The "magnet upkeep" is really the power of the cryogenic plant. When the cooling falls behind, part of a coil stops superconducting and turns back into a resistive conductor, and the energy stored in the magnet turns into heat there in moments: a **quench**.
- **Disruptions**: when the plasma loses all its heat and current within milliseconds, that is a **disruption**. The plasma's heat strikes the first wall, and the falling current induces huge electromagnetic forces in the structure. A mitigation system injects a large amount of matter into the plasma just before, so that the energy leaves as radiation spread over the whole wall instead of striking one spot.

## Power data

| Item | Value |
|---|---|
| Output | no cap; 41,440,000 EU/t with the example blanket |
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
2. Put deuterium fuel rods and tritium fuel rods into the fuel slots (or into a fuel injection port, see Automation below). A fresh rod gives 10,000 fuel units, and the empty fuel rod comes back.
   - Deuterium fuel rods: a thermal centrifuge (heat 4,000 or more, 1,200 ticks) turns a hot water cell into an unstable hot water cell; in a canner in canning mode, put the unstable hot water cell in the left slot and an empty fuel rod in the middle slot.
   - A deuterium fuel rod also works in a nuclear reactor as a neutron source: each adjacent fuel rod takes one more pulse per cycle, for 5,000 cycles (half a tritium rod), with no decay heat; then the empty fuel rod is left.
3. Fill the blanket (9 columns x 6 rows; column 1 faces the plasma) with components.
4. Apply a redstone signal. The reactor goes through: magnet cooldown (30 s), solenoid charge, heating, ignition (14 keV, about 160 million K), burn (about 15 keV).
5. Set the feed (1-16) with the screen's buttons. After ignition no heating is needed. The output leaves through the power output ports.

## Automation

The fuel injection port and the disruption mitigation port have slots of their own; right-click one to open its screen (like the nuclear reactor's fluid port):

- **Fuel injection port**: 2 slots of deuterium fuel rods, 2 of tritium fuel rods and 2 outputs for the empty fuel rods. Pipes and hoppers can only put fuel rods into their own slots, and can only take the empty fuel rods out.
- **Disruption mitigation port**: 4 slots of coolant cells (10k, 30k, 60k).
- Once the structure is formed, the console takes one rod or cell from a port whenever its deuterium, tritium or mitigation slot is empty, and sends its empty fuel rods to the fuel injection port's outputs. While the structure is not formed, the items stay in the port.
- The screen shows whether the port is linked to the console, and the fuel or coolant cells the console holds. Breaking a port drops its contents.
- Hoppers can also feed the console itself, but it only exposes the fuel slots and the empty-rod outputs, never the blanket. Coolant cells go in only through a disruption mitigation port or by hand.

## Plasma

- Before ignition the heating ports heat the plasma (Q = 10). From 14 keV the alpha particles keep it burning, and Q is infinite.
- The solenoid's flux carries a burn of about 400 s, then it must charge again. With two electron cyclotron heaters the plasma current is driven without it (1.25% of the fusion power each), and the burn is steady.
- The plasma uses 1 unit of deuterium and 1 unit of tritium per feed step and tick. When the fuel runs out, the plasma ends gently.
- Once it makes power, the console keeps 40 ticks of magnet upkeep from its own output.

## Blanket

- The fusion neutrons weaken column by column: at full power each slot of columns 1, 2 and 3 gets 2, 1 and 1 neutrons per cycle, and the columns after that get none.
- Fuel rods count them as extra pulses, so they make more heat and more power (the nuclear reactor's own rules). Lithium cells breed 40 per neutron and become tritium fuel rods at 10,000; about 5 lithium cells in column 1 keep the tritium self-sufficient.
- Output = neutron power x (1 + 0.2 x the share of neutrons the lithium cells take) + 14,000 x the rods' fission output (in nuclear reactor EU/t) x the power fraction. There is no cap (it stops only at the grid's top voltage, 2,147,483,647 EU/t, against overflow).
- For example, proton quad rods boxed in by iridium overclocked vents give 41,440,000 EU/t (past the full power advancement at 39,200,000); blankets with more fission give more. A blanket at its maximum heat (10,000 plus platings) melts down (see below).

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
