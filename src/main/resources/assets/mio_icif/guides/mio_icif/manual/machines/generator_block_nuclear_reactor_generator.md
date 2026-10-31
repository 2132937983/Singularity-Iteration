---
navigation:
  title: "Nuclear Reactor Generator"
  icon: mio_icif:generator/block_nuclear_reactor_generator
  parent: generators.md
  position: 21
item_ids:
  - mio_icif:generator/block_nuclear_reactor_generator
---

# Nuclear Reactor Generator

<Row>
  <BlockImage id="mio_icif:generator/block_nuclear_reactor_generator" scale="3" />
</Row>

## Function

The machine burns nuclear fuel rods and makes EU and heat.
Each adjacent Reactor Chamber adds one column of component slots.
With a Reactor Pressure Vessel structure, the machine changes to fluid mode and heats coolant. In fluid mode it makes no EU: the heat leaves as hot coolant (Jade shows how many HU/s), for a <ItemLink id="mio_icif:hugenerator/block_heat_source_fluid" />. MOX rods make twice the heat there above half of the maximum hull heat.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | IV (8,192 EU) |
| Output | 524,288 EU/t |
| Energy storage | 1,000,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place Reactor Chambers against the faces of the reactor.
2. Put fuel rods and cooling components into the slots.
3. Connect a cable to the reactor.
4. Apply a redstone signal to the reactor to start it. A chamber or a redstone port of the pressure vessel also takes the signal. Without one the reactor does nothing, and its screen and Jade say "Off: no redstone signal".

## Heat stages

From 4,000 HU of hull heat, the reactor checks these stages once per cycle (1 second), by hull heat
divided by the maximum hull heat. Chances and damage are multiplied by the containment factor: 1
without reactor plating, lower with each plate (a Reactor Plating multiplies it by 0.95). When the
server turns reactor explosions off (explosion multiplier or power limit 0), these effects are off too.

| Hull heat / maximum | Effect (centred on the reactor) | Area |
|---|---|---|
| ≥ 40 % | With the containment factor as chance, one random block: a flammable block (planks, logs, leaves, saplings, flowers, crops, grass) turns into fire. | 5×5×5 |
| ≥ 50 % | With the containment factor as chance, one random block: water boils away. Water sources and flowing water vanish, a waterlogged block only loses its water, kelp, seagrass and bubble columns dry up. | 5×5×5 |
| ≥ 70 % | Every cycle, each living creature in the area takes 0 to 3 radiation damage (times the containment factor). A full hazmat suit or a full quantum suit protects completely; the nano toxin filter reduces the damage. | 7×7×7 |
| ≥ 85 % | With 20 % times the containment factor as chance, one random block: air turns into fire; stone, metal and other pickaxe blocks turn into lava; other breakable blocks turn into fire. | 5×5×5 |
| 100 % | The reactor explodes. | - |

Blocks that the heat never changes: unbreakable blocks such as bedrock, blocks with a block entity
(chambers, ports, machines), and the blocks of the data tag `mio_icif:reactor_heat_immune`:
construction foam and its walls, the reactor, the reactor chambers, the Reactor Pressure Vessel and
its interface blocks (access hatch, fluid port, redstone port), the coolant injectors.

> **NOTE:** Around a reactor under water, the lava of the 85 % stage reacts with the water by the
> vanilla rules: a lava source becomes obsidian, flowing lava becomes cobblestone or stone. The
> protected blocks themselves never change, but ordinary blocks next to the reactor (stone, for
> example) can turn into lava first and then into obsidian.

## Radiation shielding

Lead stops radiation. Radiation goes in a straight line from the reactor to a creature, and each block on this line lets only a part of it through:

| Block | Radiation that gets through |
|---|---|
| Lead Block and every block of the tag c:storage_blocks/lead (data tag mio_icif:radiation_shielding) | 10 % |
| Reinforced Stone, obsidian, crying obsidian, reinforced deepslate, iron block, netherite block, steel block (data tag mio_icif:radiation_shielding_weak) | 50 % |

- Below 5 % nothing gets through: two lead blocks, or one lead block and two of the weaker blocks, stop the radiation completely.
- A creature is exposed through the more open of two lines: the line to its eyes and the line to its middle. Cover the whole body.
- The same rule applies to the radioactive blocks of a nuclear explosion (the line from the block) and to a nuclear blast zone. In a blast zone two lines count: the line toward the blast centre and the line 16 blocks up (the roof). The less open of the two counts, so a shielding wall toward the blast centre or a shielding roof protects you, also in the open.
- A full hazmat suit or a full quantum suit still protects completely.
- Radioactive items in a <ItemLink id="mio_icif:normal/item_contain_ment_box" /> do not irradiate you. The box has 18 slots: use it to open it.

## Accident audit

The reactor records its last 40 cycles. After an accident a <ItemLink id="mio_icif:reactor/item_reactor_blackbox" /> drops at the site. Use it, or run `/mio_icif reactor audit`, to read the report: the main cause, the core layout with the melted parts and the hull heat graph. See [Reactor Accident Audit](reactor_audit.md).

## Notes


> **WARNING:** The reactor explodes when the hull heat reaches its limit.


> **WARNING:** From 40 % hull heat the reactor sets blocks on fire, boils water, irradiates creatures and makes lava (see Heat stages).


## Structure

Drag the view to turn it. The list gives the blocks and their rules. JEI shows the same plan in the category "Multiblock Structures".

<GameScene zoom="4" interactive={true}>
  <SiStructure id="mio_icif:nuclear_reactor" />
</GameScene>

<SiStructureInfo id="mio_icif:nuclear_reactor" />

## Recipe

<RecipesFor id="mio_icif:generator/block_nuclear_reactor_generator" fallbackText="-" />
