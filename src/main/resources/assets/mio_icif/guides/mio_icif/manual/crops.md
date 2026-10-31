---
navigation:
  title: "Crops and Crossbreeding"
  icon: mio_icif:crop/stick
  parent: index.md
  position: 14
item_ids:
  - mio_icif:crop/stick
  - mio_icif:crop/stick_upgraded
  - mio_icif:normal/item_crop_analyzer
---

# Crops and Crossbreeding

Crops grow on <ItemLink id="mio_icif:crop/stick" /> placed on farmland.
This page gives the rules that the code uses. All values come from the crop block entity of this version.

## The crop cycle

- A crop stick runs one cycle every 256 ticks (12.8 seconds).
- In each cycle the crop reads its environment, gets growth points and loses 1 water and 1 nutrient.
- When the growth points reach the growth time of the stage, the crop goes to the next stage.

## Quality and requirement

The crop compares its **quality** with its **requirement** in each cycle.

| Value | Formula |
|---|---|
| Requirement | (tier - 1) x 4 + growth + gain - resistance |
| Quality | 5 x (humidity + nutrients + air quality) |
| Growth points | 3 + random 0 to 6 + growth |

- Quality above the requirement: each extra point gives +1 % growth points.
- Quality below the requirement: each missing point takes 2.5 % off the growth points, compounding (10 missing points leave 78 %, 30 leave 47 %). The penalty is 2 for each missing point.
- A penalty above 100 (more than 50 missing points) can kill the crop. The crop dies when a random value 0 to 31 is higher than its resistance.
- So more growth, gain or resistance never lowers the yield per hour in the same field.
- Server option `strictIc2Rules` (section `Crops` of the SI config): the IC2 Experimental rules. Resistance raises the requirement, each missing point costs 4 % (lethal above 25 missing points), a crop with growth 24 or more is a weed, and Weed-EX does not protect empty crop sticks.
- Some crops weigh the three values differently: coffee 0.4 / 1.4 / 1.2, cocoa 0.8 / 1.3 / 0.9, sugar cane 1.2 / 1.0 / 0.8, pumpkin and melon 1.1 / 0.9 / 1.0 (humidity / nutrients / air quality).
- A crop that a <ItemLink id="mio_icif:producer/block_matron_elc" /> tends gets at least the requirement + 50.

## Humidity

Humidity = biome value + 2 for wet farmland + 2 when the stored water is 5 or more + 1 for each started 25 water.

- The farmland is wet at moisture 7: keep water within 4 blocks of the farmland.
- A crop stick holds up to 200 water. Use a water cell on it: it takes up to 200 mB (a bucket does not work, because a bucket gives only 1000 mB at a time). Full water gives +8.
- The biome value comes from the rainfall and the temperature of the biome. The temperature falls above y = 80, so the value changes on mountains.

| Biome | Biome humidity | Nutrient bonus |
|---|---|---|
| Jungle, Bamboo Jungle | +19 | +10 |
| Swamp, Mangrove Swamp | +19 | +10 |
| Mushroom Fields | +19 | +5 |
| Forest, Dark Forest | +12 | +5 |
| Birch Forest | +3 | +5 |
| Meadow, Cherry Grove | +10 | 0 |
| Taiga | +7 | 0 |
| Other forests in the tag minecraft:is_forest | depends on the biome | +5 |
| River | 0 | +2 |
| Plains | -1 | 0 |
| Desert, Savanna, Badlands | -20 | 0 |

## Soil nutrients

Nutrients = biome bonus + 1 for each dirt block under the farmland + 1 for each started 20 stored nutrients.

- The dirt blocks are the 3 blocks directly under the farmland. They count without a gap: dirt, dirt, dirt gives +3. Stone in the middle stops the count.
- A crop stick holds up to 199 nutrients. Fertilizer adds 100 when the crop has less than 100. Full nutrients give +10.
- The biome bonus is +10 in a jungle or swamp, +5 in a forest or on mushroom fields, +2 at a river.

## Air quality

Air quality = height value + half of the free cells + 4 when the crop sees the sky.

- Height value: 0 below y = 55, 1 from y = 55, 2 from y = 70.
- Free cells: the count starts at 9. The crop cell itself, and each of the cells to the west, north and north-west that is a solid block or another crop, takes 1 away. The best value is 8, which gives +4. A dense field of crops gives only +2.
- Sky: no block above the crop that stops the sky light. Glass is not a problem.
- The best air quality is 10: y = 70 or higher, open cells to the west, north and north-west, and open sky.

## Light

The light is the higher value of the sky light and the block light at the crop.

| Crop | Light needed to grow |
|---|---|
| Wheat, carrots, potatoes, beetroot, coffee, hops, flax, pumpkin, melon | 9 or more |
| Flowers, Venomilia | 12 or more |
| Red wheat | 5 to 10 |
| Eating plant | more than 10, and lava under the farmland after stage 3 |
| Mushrooms | no light value; the crop needs water |
| Cocoa | no light value; the crop needs 3 or more nutrients |
| Metal crops | the ore or the storage block of the metal under the farmland for the last stage |

At night the sky light falls to 4. A torch gives light 9 at a distance of 5 blocks, a glowstone or a lamp at 6 blocks.

## Crossbreeding

1. Put a crop stick on farmland. Use a second crop stick on it: this makes a crossing stick.
2. Plant mature crops on 2 to 4 of the horizontal neighbours (north, south, east, west).
3. Keep the crossing stick in light and in a good environment for the new crop.

- One try in three cycles. Each neighbour joins when it is at its harvest stage, when the new crop can grow at the crossing stick, and when it passes a random check.
- Join chance: (4 + 1 at growth 16 or more + 1 at growth 30 or more + 1) in 16. A resistance of 28 or more lowers it: at resistance 31 it is 1 in 16.
- At least 2 neighbours must join. The new crop is a weighted choice: the same species as a parent is very likely, similar stats and shared attributes help.
- Each trait of the new crop is the average of the parents plus a random change of up to the number of parents. Four parents give the largest change (plus or minus 4).
- With only one crop neighbour, the crossing stick copies that crop with the same traits.
- The environment does not change the crossing chance. It changes how fast the parents get mature and if the new crop can grow at the stick.

## Milk Wart

- <ItemLink id="mio_icif:resource/item_milk_wart" /> is the harvest of the milk wart crop. Eat it like milk: it removes the status effects that milk removes and gives a little food. You can eat it when you are full.
- Recipes that ask for milk by tag take it (c:buckets/milk, c:drinks/milk, c:milks, forge:milk). A cake can use milk warts in place of milk buckets, also mixed.

## UU-Matter for gene editing

- The gene page of the <ItemLink id="mio_icif:normal/item_crop_analyzer" /> has a UU tank (4,000 mB) under the catalyst slot.
- Put UU-Matter cells, UU-Matter buckets or other UU containers into the catalyst slot: they empty into the tank, and the empty containers go to your inventory. A bucket waits until 1,000 mB fit.
- 1 mB of UU-Matter is one catalyst, the same as one UU-Matter item. The tank pays first, items in the slot pay the rest.

## Tips

- Plant in a jungle or a swamp: humidity +19 and nutrients +10.
- Put 3 dirt blocks under the farmland.
- Keep water and nutrients full: use a <ItemLink id="mio_icif:producer/block_matron_elc" />.
- Leave the cell to the west, north and north-west of each crop free (for example rows with paths), and do not cover the field.
- Build the field at y = 70 or higher.
- Put light sources so that each crop has light 9 or more at night.
- Growth 24 or more spreads weeds: 4 % per cycle at growth 24, 4 % more for each further point, minus the resistance (31/31/31: 1 %).
- A crop stick without a plant and without Weed-EX can become a weed (1 in 100 per cycle). Weed-EX on an empty crop stick also stops weeds that spread to it (5 Weed-EX each time).
