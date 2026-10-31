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

## Server load of the energy network

- An operator can type /mio_icif perf to see the server tick, and for each dimension the time that the energy network takes per tick (median, 95th percentile, maximum of the last 256 ticks).
- Deliveries through cables use a cached profile of their path: the lowest fuse, insulation and shock limits. A delivery below all of them needs no visit of each cable. Fuses, stripped insulation and shocks happen exactly as before.
- Cables do not tick. A cable that carried energy in the last second counts as powered.
- /mio_icif perf also shows the load of the last 100 ticks, per tick: block entity changes that update their neighbours, client updates, lamp checks, neighbour lookups and how many came from the cache, and machine ticks saved by the idle sleep.
- A large network is settled in several windows of at most 32 sources. The route plan of each window is kept while the cables, machines and their sides stay the same, so it is not searched again every tick.

## Universal cables (FE)

Since 0.1.10.39 SI cables carry Forge Energy (FE) too, at 4 FE per EU both ways (no loss, like Mekanism).

- A machine, generator or battery of another mod next to a cable can push FE into it. The FE enters the network at voltage 0: it never melts a cable and needs no transformer. It is lost only through the normal cable loss.
- SI machines and storage give FE to the machines of other mods next to a cable. Those machines are served last, after SI machines and storage.
- SI cables do not connect to the cables of other mods (block tag `mio_icif:foreign_energy_cables`, or a block id with cable, duct, conduit, transmitter, wire or energy_pipe): two networks would pass the same FE back and forth. GregTech and Modern Industrialization cables stay connected (EU, 1 EU = 1 EU).
- `/mio_icif perf` shows the FE per tick taken into cables, exported and imported, and the neighbour probes.
- The Energy Converter is a legacy block: it is no longer crafted; placed ones keep working.

<SubPages id="power.md" icons={true} alphabetical={true} />
