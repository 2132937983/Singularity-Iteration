---
navigation:
  title: "Power transmission and storage"
  icon: mio_icif:wiring/block_mfe
  parent: index.md
  position: 12
---

# Power transmission and storage

Cables move EU between blocks. Transformers change the voltage tier.
Storage blocks keep EU and charge items.

## Cables

| Cable | Tier | Largest packet | Insulation for no shock | Line loss* |
|---|---|---|---|---|
| Tin (Low-Voltage) | LV | 32 EU | 1 layer | 0.2 EU |
| Iron (Medium-Voltage) | MV | 128 EU | 1 layer | 0.2 EU |
| Copper (High-Voltage) | HV | 512 EU | 2 layers | 0.4 EU |
| Gold (Extra-High-Voltage) | EV | 2,048 EU | 3 layers | 0.8 EU |
| Glass fibre (IV) | IV | 8,192 EU | never shocks | 0.025 EU |
| Superconducting | any | no limit | never shocks | none |
| Detector, splitter, breaker, fuse | IV | 8,192 EU | never shock | 0.5 EU |

- **The rating is the largest packet, not a flow limit.** A tin cable carries any number of packets of up to 32 EU each tick. A larger packet melts the cable, and a machine explodes on a packet above its input tier. Use a transformer between tiers.
- **No line loss by default.** *With the server option `lineLossModel = proportional` each packet loses the amount in the table per block; a packet that would lose all of it on the way is not sent. Without it the loss values only choose between two routes.
- **Shock.** A bare or partly insulated cable hurts living things within 1.5 blocks while it carries a packet above what its insulation absorbs: bare tin 8 EU, bare iron, copper and gold 32 EU, each layer four times more (1 layer 32 / 128, 2 layers 512, 3 layers 2,048). The damage is (packet − absorption) / 64, rounded up, per tick. The item tooltip of each cable says when it shocks.
- **Insulation.** Rubber on a cable adds a layer; the Cable Cutter takes one off and gives the rubber back. Neither works through construction foam or a wall.
- **Colours.** Dye an insulated cable to keep two lines apart: two dyed cables connect only when they have the same colour; an undyed (black) cable connects to every colour.
- **Long lines.** A line is cut where it crosses a chunk that does not tick (outside the loaded area); both sides keep working. Keep the whole route chunk-loaded. The meter's HUD warns about cables in such chunks.
- **Measuring.** The EU Meter on a cable shows the EU that went through that cable this tick and its largest packet. `/mio_icif grid` (operators) shows the health of the grid: rebuilds, quarantined chunks, cables in chunks that do not tick.

## Server load of the energy network

- An operator can type /mio_icif perf to see the server tick, and for each dimension the time that the energy network takes per tick (median, 95th percentile, maximum of the last 256 ticks).
- Deliveries through cables use a cached profile of their path: the lowest fuse, insulation and shock limits. A delivery below all of them needs no visit of each cable. Fuses, stripped insulation and shocks happen exactly as before.
- Cables do not tick. A cable that carried energy in the last second counts as powered.
- /mio_icif perf also shows the load of the last 100 ticks, per tick: block entity changes that update their neighbours, client updates, lamp checks, neighbour lookups and how many came from the cache, and machine ticks saved by the idle sleep.
- A large network is settled in several windows of at most 32 sources. The route plan of each window is kept while the cables, machines and their sides stay the same, so it is not searched again every tick.

## Blocks of other mods (FE)

The SI grid exchanges Forge Energy (FE) with the blocks of other mods next to its cables, at 4 FE per EU both ways (no loss, like Mekanism). Since 0.1.11.1:

- The grid settles these blocks in its fair round, with SI machines and storage. A machine of another mod gets its share of the surplus after the SI loads, like SI storage. A generator of another mod that gives FE when asked supplies the grid like an SI generator. FE enters the grid at voltage 0: it never melts a cable and needs no transformer.
- A battery of another mod (it takes and gives FE) charges until it is 90 % full. Then it supplies SI loads until it is 10 % full, and so on. It never charges SI storage. Thus energy does not go back and forth between it and SI storage.
- SI cables do not take FE. A block of another mod that only pushes FE (it gives no FE when asked) must touch an SI machine or a grid-tie interface.
- SI cables do not connect to the cables of other mods (block tag `mio_icif:foreign_energy_cables`, or a block id with cable, duct, conduit, transmitter, wire or energy_pipe). To connect a network of another mod, use a grid-tie interface (export, import or bidirectional). GregTech and Modern Industrialization cables stay connected (EU, 1 EU = 1 EU).
- A machine with a Joule view (Mekanism) keeps the part of a transfer below 1 EU for the next transfer. Small flows arrive in full.
- `/mio_icif perf` shows the FE per tick moved by grid-tie interfaces, given to and taken from the blocks of other mods, and the neighbour probes.

<SubPages id="power.md" icons={true} alphabetical={true} />
