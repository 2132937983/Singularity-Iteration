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

<SubPages id="power.md" icons={true} alphabetical={true} />
