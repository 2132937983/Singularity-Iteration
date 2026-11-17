---
navigation:
  title: "Chunk Loader"
  icon: mio_icif:producer/block_chunk_loader
  parent: machines.md
  position: 16
item_ids:
  - mio_icif:producer/block_chunk_loader
---

# Chunk Loader

<Row>
  <BlockImage id="mio_icif:producer/block_chunk_loader" scale="3" />
</Row>

## Function

The Chunk Loader keeps selected chunks loaded. The machine loads up to 25 chunks in a 9 x 9 chunk area around itself.
Each loaded chunk costs EU. When the EU runs out, the loader keeps only its own chunk, free, for a grace period (10 minutes by default) and takes all its chunks back as soon as energy returns. After the grace it stops, and its chunk unloads unless a player is near.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | MV (128 EU) |
| Maximum input | 128 EU/t |
| Energy storage | 2,500 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an MV cable to the machine.
2. Open the GUI and click or drag over the chunks to load.
3. Keep a steady EU supply to the machine.

## Map

- Green: the chunk is loading. Orange: selected but not loading now (a chunk limit, or no energy); the tooltip says why. Faint green: not selected, but running because a loading chunk is next to it. Dark: not selected. The yellow frame marks the loader's own chunk, which is always selected.
- A loaded chunk also runs the chunks around it, as `/forceload` does: selecting the 5 × 5 in the middle runs 7 × 7. Only the selected chunks cost EU.
- Click a chunk to select or remove it. Drag to select an area; right-click or right-drag to remove. **+** selects the whole grid, nearest first up to the limit; **−** clears all but the own chunk.

## Notes

- The chunk that holds the Chunk Loader is part of the selection grid.

> **WARNING:** The machine explodes on overvoltage above MV. Install a Transformer Upgrade for a higher voltage tier.

## Server rules

The player who places the loader owns it. The server can limit loaders in the config section `ChunkLoaders`:

| Setting | Default | Effect |
|---|---|---|
| `enabled` | true | false: every loader releases its chunks |
| `maxChunksPerLoader` | 25 | Chunks per loader |
| `maxChunksPerPlayer` | 0 (no limit) | Chunks of all loaders of one player |
| `maxLoadersPerPlayer` | 0 (no limit) | Working loaders per player |
| `offlineOwnerMinutes` | -1 (always) | Minutes a loader keeps working after its owner logged off |
| `euPerChunkPerTick` | 1.0 | EU per chunk and tick |
| `randomTicks` | true | false: no random ticks and no natural spawning in loaded chunks; machines keep running |
| `ownerOnlyEdit` | false | Only the owner and operators change the selection |
| `noEnergyGraceMinutes` | 10 | Minutes a loader out of energy keeps its own chunk; 0: it stops at once |

- Over a chunk limit the loader keeps its own chunk first, then the nearest selected chunks.
- A red band over the map tells why the loader does not load (all) its chunks. The chunk count shows the details.
- A loader stopped by a rule restarts by itself when the reason is gone, for example when its owner logs in.
- Operators: `/mio_icif chunkloaders` lists all loaders; `disable` and `enable` lock or unlock one loader; `disable_all` stops all.


## Recipe

<RecipesFor id="mio_icif:producer/block_chunk_loader" fallbackText="-" />
