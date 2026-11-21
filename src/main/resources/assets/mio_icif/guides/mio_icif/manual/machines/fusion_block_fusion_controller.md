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
| Output | no cap; heat x 35% steam cycle efficiency; 41,440,000 EU/t with the example blanket (118,400,000 EU/t of heat) |
| Fusion power | grows with the square of the feed step: 9,800,000 EU/t at 16 steps, a quarter of that at 8 |
| Input voltage tier | LuV (32,768 EU), any face |
| Input storage | 20,000,000 EU |
| Magnet upkeep | 392,000 EU/t |
| Start-up (central solenoid) | 300,000,000 EU, 30 s at the fastest |
| Heating | 245,000 EU/t per heating port, on until ignition |

## Structure

<GameScene zoom="2" interactive={true}>
  <SiStructure id="mio_icif:fusion_reactor" />
</GameScene>

<SiStructureInfo id="mio_icif:fusion_reactor" />

- Every block must be in place, and the plasma channel inside the ring must be air.
- A port slot (a duct end; on the east side a column of three: disruption mitigation on top, cryogenic in the middle, exhaust below) takes any fusion port. Close the slots you do not use with a vacuum vessel wall.
- To run, the reactor needs a cryogenic port, at least one heating port (neutral beam injector or electron cyclotron heater), an exhaust port and a power output port. Fuel injection and disruption mitigation ports are optional.
- A **diagnostic viewport** may stand in for any blanket module or vacuum vessel wall without breaking the structure. Outside the plasma channel lies a layer of blanket, then a layer of vessel, so it takes two viewports in line (say a blanket module at the outer midplane and the vessel wall outside it) to see the plasma from outside. The demonstration has a band of four sight lines on the east and south sides.
- Through a viewport:
  - the channel glows dull red, orange or yellow-white with the temperature, its edge a little pink in H-mode;
  - bright points run round the torus along the helical field lines, faster the hotter it is;
  - at start-up a violet breakdown ring appears first, and the plasma fills the channel in about 2 seconds;
  - at each ELM the edge flashes white and the exhaust port puffs flame; while it burns the exhaust port smokes now and then;
  - a disruption flares, throws sparks and is gone within 1.5 seconds; a gentle end fades out in 1 second.
- The whole structure turns with the console. The screen names the first wrong block and its position.

## Procedure

1. Build the structure and connect LuV power to the console.
2. Put deuterium fuel rods and tritium fuel rods into the fuel slots (or into a fuel injection port, see Automation below). A fresh rod gives 10,000 fuel units, and the empty fuel rod comes back.
   - Deuterium fuel rods: a thermal centrifuge (heat 4,000 or more, 1,200 ticks) turns a hot water cell into a heavy water cell: the hot water goes in as steam, and the centrifuge separates the heavier HDO and D₂O molecules by their mass (a gas centrifuge sorts by the mass difference, which for water is as large as for uranium hexafluoride). In a canner in canning mode, put the heavy water cell in the left slot and an empty fuel rod in the middle slot: the canner electrolyses the heavy water and fills the deuterium gas into the rod.
   - A deuterium fuel rod also works in a nuclear reactor as a neutron source: each adjacent fuel rod takes one more pulse per cycle, for 5,000 cycles (half a tritium rod), with no decay heat; then the empty fuel rod is left.
3. Fill the blanket (9 columns x 6 rows; column 1 faces the plasma) with components.
4. Apply a redstone signal. The reactor goes through: magnet cooldown (30 s), solenoid charge, heating, ignition (over 10 keV, once the alpha heating covers the losses), burn (15 keV at 16 steps, about 170 million K).
5. Set the feed (1-20; 16 is the reference, 17 and up approach the operating limits, see below) with the screen's buttons. After ignition the heating switches off by itself. The output leaves through the power output ports.

## Automation

The fuel injection port and the disruption mitigation port have slots of their own; right-click one to open its screen (like the nuclear reactor's fluid port):

- **Fuel injection port**: 2 slots of deuterium fuel rods, 2 of tritium fuel rods and 2 outputs for the empty fuel rods. Pipes and hoppers can only put fuel rods into their own slots, and can only take the empty fuel rods out.
- **Disruption mitigation port**: 4 slots of frozen neon pellets (coolant cells too, in this version).
- Once the structure is formed, the console takes one rod or cell from a port whenever its deuterium, tritium or mitigation slot is empty, and sends its empty fuel rods to the fuel injection port's outputs. While the structure is not formed, the items stay in the port.
- The screen shows whether the port is linked to the console, and the fuel or coolant cells the console holds. Breaking a port drops its contents.
- Hoppers can also feed the console itself, but it only exposes the fuel slots and the empty-rod outputs, never the blanket. Coolant cells go in only through a disruption mitigation port or by hand.

## Plasma

- The plasma is a zero-dimensional power balance of a reference machine the size of ITER (830 m³):
  - the feed step sets the electron density: step k aims at k/16 × 10²⁰ m⁻³, reached in about a second;
  - fusion power = (n/2)² × ⟨σv⟩ × 17.6 MeV × volume, with the Bosch-Hale fit of ⟨σv⟩, so it grows with the square of the density, and about with the square of the temperature at 10-20 keV;
  - losses = conduction (the energy confinement time τE follows ITER's IPB98 scaling in the density and the heating power, about 2 s in the reference burn) + bremsstrahlung.
- **Ignition**: once the plasma is over 10 keV and the alpha heating alone covers the losses, the heating ports switch off and Q is infinite. If an ignited plasma cools below 8 keV (a lower feed step, say), the heating comes back on.
- **The Lawson criterion**: at too low a density n·T·τE stays short of about 3 × 10²¹ keV·s/m³ and the plasma cannot ignite. At 8 steps and below it can only burn **driven**: the heating stays on and Q is finite (about 0.5-13 with one heater).
- **L-mode and H-mode**: the confinement time above is that of the high-confinement mode (H-mode). The plasma enters H-mode only when the power through its edge (alpha heating + outside heating - bremsstrahlung) reaches the L-H threshold (the Martin 2008 scaling: about 87 MW at 10²⁰ m⁻³, more at higher density); below 80% of it, it falls back to the low-confinement mode (L-mode), with half the confinement time.
- **How many heaters**: ignition takes 9 steps or more, in H-mode. One heater (about 40 MW) cannot bring a plasma at ignition densities into H-mode: it only burns driven at low steps. Two ignite 9 to 14 steps; for more, ignite low and then raise the feed (as real machines raise the density step by step). Three or more ignite 16 steps directly, four in about 4 seconds.
- **ELMs (edge-localised modes)**: the edge of an H-mode plasma collapses now and then, throwing about 1.5% of the thermal energy onto the divertor (about every 2 s at the reference burn, more often with more edge power, 1-6 s). The conduction between them is lower by as much, so the average power is unchanged, but the fusion power swings by a few percent.
- **Operating limits**: the feed goes up to 20 steps. Past the reference of 16 the power grows with the square of the density (about 1.56 times at 20), at the cost of nearing two limits:
  - the Greenwald density n_G = I_p/(πa²) ≈ 1.19 × 10²⁰ m⁻³ (84% at 16 steps, about 100% at 19);
  - the Troyon beta limit β_N ≤ 3.5 (87% at 16 steps and 15 keV).
  - From 90% of either the plasma may disrupt: about 2% a second at 100%, and 2% a second more for each 1% past it. So 16 steps are safe, 17 almost, 18 are a risk, and 19-20 disrupt within seconds. The screen says what caused a disruption.
- Hover over the readouts on the right of the screen for the density, τE, the triple product (as a share of the Lawson criterion), the alpha heating against the losses, the outside heating, the confinement mode (with the ELM count in H-mode), and the Greenwald density and beta limit fractions (yellow from 90%, red from 100%).
- The solenoid's flux carries the plasma for about 400 s (counted from the heating), then it must charge again. With two electron cyclotron heaters the plasma current is driven without it (1.25% of the fusion power each), and the burn is steady.
- **Fuel cycle**: the pellet injectors put in 1 unit of deuterium and 1 of tritium per tick for each 1/16 of the full fusion rate (16 of each at full power). Only **3%** of it fuses in one pass (the burn fraction: ITER about 0.3-1%, a power plant 1-3%); the exhaust port's cryopumps draw the rest off with the helium ash and the tritium plant returns it to the tanks. So the plasma uses only 0.48 units of each a tick at full power, and a fresh rod (10,000 units) lasts about 17 minutes. Without an exhaust port nothing comes back: it uses all it injects (and the helium ash disrupts it within 60 s).
- When the fuel runs out, the plasma ends gently.
- Once it makes power, the console keeps 40 ticks of magnet upkeep from its own output.

## Blanket

- The fusion neutrons weaken column by column: at full power each slot of columns 1, 2 and 3 gets 2, 1 and 1 neutrons per cycle, and the columns after that get none.
- Fuel rods count them as extra pulses, so they make more heat and more power (the nuclear reactor's own rules). Lithium cells breed 1 unit of tritium per fusion neutron (lithium-6 + n → T + ⁴He) and become tritium fuel rods at 10,000. With 5 lithium cells in column 1 the tritium breeding ratio is about 1.04 (self-sufficient), with 6 about 1.25. Lithium cells next to fuel rods also breed from the fission neutrons. (Near-depleted isotope rods still gain 40 per fusion neutron.)
- This is a **fusion-fission hybrid** blanket: it only makes heat, and the console's steam cycle turns **35%** of it into electricity (like a pressurised-water reactor). The heat has four parts:
  - the alpha particles' heat on the first wall and divertor: a quarter of the neutron power;
  - the neutrons' own energy: the neutron power x 1;
  - the 4.8 MeV lithium-6 releases when it takes a neutron: the neutron power x 0.34 x the share of neutrons the lithium cells take;
  - the fission the neutrons cause (about 200 MeV each): 45,250 x the rods' fission output (in nuclear reactor EU/t) x the power fraction.
- Output = heat x 35%. There is no cap (it stops only at the grid's top voltage, 2,147,483,647 EU/t, against overflow).
- The "Power plant" part of the screen's hover readout shows: the heat and the electricity; the blanket's energy multiplication M (its heat over the neutron power: about 1.3 for lithium only, about 14.9 for the example blanket); the fissions each fusion neutron causes (about 1 in the example blanket); the tritium breeding ratio TBR (tritium bred each cycle over tritium burned: above 1 to be self-sufficient); and the engineering Q (output over the console's own power draw).
- For example, proton quad rods boxed in by iridium overclocked vents give 118,400,000 EU/t of heat and 41,440,000 EU/t of output (past the full power advancement at 39,200,000); a blanket without fuel rods gives only 35% of its heat, an empty one about 3,430,000 EU/t; blankets with more fission give more. A blanket at its maximum heat (10,000 plus platings) melts down (see below).

## Hazards

- **Disruption:** it happens when:
  - the magnets quench;
  - the structure is broken while there is plasma;
  - helium ash builds up without an exhaust port (60 s).
  - the plasma runs past an operating limit (density or beta, see above).

  The plasma's thermal energy strikes the first wall, and the blanket heats in proportion to it: 1 at the reference burn (16 steps, 15 keV), less at low steps or early in the heating, more at 20 steps.
  - Unmitigated: the blanket heats by half its maximum at the reference burn; from 25% of the reference plasma energy, the components in the blanket's first column are destroyed.
  - **Shattered pellet injection**: with a **frozen neon pellet** in the disruption mitigation slot, one is shattered into the plasma just before (used up). The neon turns the heat into radiation spread evenly over the whole wall: the components survive, and the blanket heats by only 5% of its maximum at the reference burn. A compressor freezes frozen neon pellets from compressed air cells (32 EU/t, 400 ticks).
  - Coolant cells still go in the mitigation slot in this version (10k 30%, 30k 20%, 60k 5%, scaled the same way); a later version drops them.
  - A disruption never damages the structure.
- **Meltdown:** a blanket at its maximum heat (overheated by itself, or by a disruption's heat on top) melts down: the console and its contents are gone. A tokamak holds a few grams of fuel and cannot blow up like a bomb, so by default:
  - the superconducting magnets quench and their stored energy (ITER: about 51 GJ) goes at once: an ordinary blast of power 16 at the centre of the torus and one of power 8 by each of the eight toroidal field coils round the plasma ring, tearing open the vacuum vessel and the blanket (breaking nothing with enableNuclearExplosion off);
  - the melted hybrid blanket (with its fission fuel) leaves a radioactive zone of 48 blocks radius for 24 hours (real time).
  - With the config fusionNuclearMeltdown set to true it is the old nuclear blast: fusionExplosionMultiplier times the nuclear reactor's explosion power limit (by default 50 x 45 = 2,250). With fusionExplosionMultiplier at 0 neither happens: an overheated blanket only disrupts the plasma.
- **Quench:** without the magnet upkeep, the magnets lose superconductivity. After 60 s they must be cooled again.
- **Radiation:** while the plasma burns, living things within 16 blocks of the torus take 0-3 radiation damage a second. Shielding blocks reduce it, and a full hazmat suit protects completely.

## Recipe

<RecipesFor id="mio_icif:fusion/block_fusion_controller" fallbackText="-" />
