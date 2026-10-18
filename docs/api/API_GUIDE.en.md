# Singularity Iteration API Guide (0.1.10.2)

This guide is for addon authors. The text follows ASD-STE100 (Simplified Technical English):
short sentences, active voice, one instruction in each step.

## 1. Artifacts

| Artifact | Contents | Use |
|---|---|---|
| `mio_icif-api-<version>.jar` | `com.miophas.singularity_iteration.core.api.**` only | Compile an addon that only uses the API. |
| `singularity-iteration-core-<version>.jar` | API, runtime, prefabs, platform helpers | Compile an addon that extends the prefab block entities. |
| `singularity-iteration-<version>.jar` | The full mod | Run the game. |

The API packages refer to no runtime, prefab or `common` class. The build checks this
(`checkCoreArchitecture`). The example addon (`src/addonExample`) compiles against the core JAR
only.

### 1.1 Procedure: add the dependency

1. Put the API JAR in your build as a compile-only dependency.
2. Put the full mod JAR in your run classpath.
3. In `neoforge.mods.toml`, add a dependency on `mio_icif`. Set `type="required"` if your addon
   cannot run without it. Set `type="optional"` if your addon only adds integration.
4. If the dependency is optional, put each call to the API in a class that loads only when
   `ModList.get().isLoaded("mio_icif")` is true.

## 2. General rules

* Call the API on the server thread, unless a section gives a different rule.
* Register during mod construction or during a setup event. Do not register after the server
  starts.
* Registries in the API are open on both sides. Register on both sides, because the client uses
  the data for names, colours and screens.
* The comments of the main `core.api` types (`docs/dev/trilingual_manifest.txt`) have English
  (ASD-STE100), Chinese and Japanese parts (`docs/dev/COMMENT_STYLE.md`). English is the reference text.
* The API does not change the save format of a world. Data that an addon adds stays in the
  addon's own data components or attachments.

**CAUTION:** Do not keep a reference to an `ItemStack` that the API gives to a callback. The stack
can change after the callback.

## 3. Quantum suit units (`core.api.suit`)

An addon can add an upgrade unit for the quantum suit. The unit behaves like a built-in unit: the
quantum modification station installs and removes it, the Singularity Management Panel shows its
switch, and its energy cost uses the full-set energy pool.

| Type | Function |
|---|---|
| `SuitModuleSpec` | The data of the unit: id, slots, drain (EU/t), colour, visor need, unit item. |
| `ISuitModuleItem` | Interface of the unit item. It returns the spec. |
| `SuitModuleBehavior` | The work of the unit. It runs once each server tick while the unit is active. |
| `SuitModuleRegistry` | Registration and the query `isActive(player, id)`. |

### 3.1 Rules

* The id must have your mod namespace, for example `myaddon:sonar`. Built-in units have no
  namespace.
* An addon unit counts against the unit capacity of the piece (helmet 4, chestplate 3,
  leggings 2, boots 2).
* "Active" means: installed in a worn quantum piece, switched on in the panel, enough energy
  (at least one tick of drain), and a quantum helmet worn when `needsVisor` is true.
* The drain is collected once each second. Energy Saving of the piece applies.
* A behavior that throws an exception is logged. The unit stays off for that tick.
* A piece keeps the id of a unit whose mod was removed. That unit does nothing and does not count
  against the capacity.

### 3.2 Procedure: add a unit

1. Make an item class that extends `Item` and implements `ISuitModuleItem`.
2. Make a `SuitModuleSpec`. Example:

   ```java
   public static final SuitModuleSpec SPEC = new SuitModuleSpec(
       ResourceLocation.fromNamespaceAndPath("myaddon", "sonar"),
       Set.of(EquipmentSlot.HEAD), 3, 0x33CCAA, true,
       ResourceLocation.fromNamespaceAndPath("myaddon", "sonar_unit"));
   ```

3. In the mod constructor, register the spec and the behavior:

   ```java
   SuitModuleRegistry.register(SPEC, (player, piece) -> {
       if (player.tickCount % 40 == 0) { /* work of the unit */ }
   });
   ```

4. Add the translation keys `module.myaddon.sonar` (name) and `module.myaddon.sonar.desc`
   (description).
5. Add a recipe for the unit item.

The panel switch key is `fcs_<namespace>_<path>` (`SuitModuleSpec.featureKey()`).

### 3.3 Nano suit units and bursts (0.1.8.5)

Since 0.1.8.5 the nano suit takes units too. A nano piece counts lattice points (LP: helmet 4,
chestplate 6, leggings 4, boots 4, +2 for each densifier, at most 2). The two-argument
`register` is unchanged: such a unit is quantum only.

| Type | Function |
|---|---|
| `SuitClass` | `QUANTUM` or `NANO`. |
| `SuitModuleTraits` | LP on a nano piece (1..6), the suit classes, and an optional burst. `nano(lp, burst)`, `shared(lp)`, `QUANTUM_ONLY`. |
| `SuitBurstSpec` | Burst costs: reserve points, heat, cooldown ticks, sustained flag. |
| `SuitBurstBehavior` | `fire(player, piece, input)` on the server. Return true when the burst fired; only then the costs are paid. |
| `SuitNanoCore` | Read-only reserve, heat, heat cap and overheat of a player. |
| `ArmorCapacityBonus` (`core.api.item`) | Extra EU capacity of one electric armor piece from its own data. |

Procedure: register with traits.

```java
SuitModuleRegistry.register(SPEC, SuitModuleBehavior.NONE,
    SuitModuleTraits.nano(2, new SuitBurstSpec(15, 10, 60, false)),
    (player, piece, input) -> { /* the burst */ return true; });
```

Rules:

* A burst unit needs `traits.burst()` and the burst behavior together.
* The wearer assigns the burst to the V or B key in the Singularity Management Panel.
* Singularity Iteration checks the unit (installed in a worn nano piece, on, powered), the
  overheat lock, the cooldown and the reserve before `fire`.
* Addon bursts are single bursts. A sustained flag is ignored for addon units in 0.1.8.5.

### 3.4 Frame cores (0.1.9.0)

A suit helmet or chestplate may hold one frame core in its core socket. The socket is the data
component `mio_icif:suit_core` (a string id: `solar_array_1`, `solar_array_2`, `solar_array_3`,
`graviton`, `propulsion`). Read it to show or react to a core; do not write it, use the
Universal Suit Modification Station.

| Hook (`core.prefab.flight.JetpackFlightController`) | Function |
|---|---|
| `THRUST_SCALE` | Thrust multiplier of a chest jetpack stack (Advanced Propulsion: 1.15). |
| `NO_CEILING` | True when the chest jetpack stack has no flight height limit. |

Singularity Iteration sets both hooks at start-up. An addon that changes them must keep the
previous function and combine it.

The solar helmets, the advanced quantum chestplate and the heavy quantum chestplate were retired.
Their ids stay as hidden legacy shells that convert into the standard piece with a core. Their
classes (`mio_icif_*_solar_helmet`, `mio_icif_chestplate_advanced_quantum`,
`mio_icif_chestplate_heavy_quantum`) were deleted; addons that referenced them must check the core
instead.

### 3.5 Quantum actions (0.1.10.0)

Since 0.1.10.0 a quantum unit may have an active ability on the action keys (V and B, shared with
the nano bursts). Actions are paid with quantum coherence (0..100, +50 per coherence capacitor,
regained from EU). At 0 the core decoheres for a few seconds and refuses every action.
Nothing changed for units without an action: all 0.1.9.2 types and signatures are unchanged.

| Type | Function |
|---|---|
| `SuitActionSpec` | Coherence cost, cooldown ticks, sustained flag, coherence per second of a sustained action. `single(cost, cooldown)`, `sustained(start, perSecond, cooldown)`. |
| `SuitActionBehavior` | `fire(player, piece, input)` on the server; true when it fired (only then the cost is paid). Sustained actions also get `sustain` each tick (false ends it) and `end`. |
| `SuitQuantumActions` | `register(unitId, spec, behavior)`, `get(key)`, `all()`. |
| `SuitQuantumCore` | Read-only coherence, coherence maximum and decoherence lock of a player. |

Procedure: register the unit as before, then its action.

```java
SuitModuleRegistry.register(SPEC, SuitModuleBehavior.NONE);                // a quantum unit
SuitQuantumActions.register(SPEC.id(), SuitActionSpec.single(15, 200),
    (player, piece, input) -> { /* the action */ return true; });
```

Rules:

* The unit must allow the quantum class and sit on a worn quantum piece; the wearer assigns the
  action to V or B in the Singularity Management Panel (quantum core rows).
* Singularity Iteration checks the unit (installed, on, powered), the decoherence lock, the
  cooldown and the coherence before `fire`. A sustained action needs more coherence than its start
  cost; it ends when the key is pressed again, `sustain` returns false, the coherence runs out
  (decoherence) or the unit stops.
* The id needs the addon namespace; register on both sides before the server starts.

### 3.6 Entanglement synergies and compute bandwidth (0.1.10.1)

Since 0.1.10.1 the wearer can link two quantum units on the quantum chestplate (2 links, 3 with an
active compute core). A linked pair is a synergy. Each active unit on a quantum piece also loads
the suit computer (compute bandwidth, QB). Over the budget (20 QB, 28 with a compute core) the
sensors refresh at half rate and coherence regenerates at half speed. Nothing changed for existing
addons: all 0.1.10.0 types and signatures are unchanged.

| Type | Function |
|---|---|
| `SuitSynergies` | `register(id, unitA, unitB)`, `get(key)`, `all()`, `active(player, id)`. The pair appears in the Entanglement Matrix; the addon implements the effect where `active` is true. Names: `Entry.nameKey()` and `nameKey + ".desc"`. |
| `SuitBandwidth` | `register(unitId, cost)` (0..16 QB), `cost(key)`, `load(player)`, `budget(player)`. |

Procedure: register the synergy, then test it where the effect applies.

```java
SuitSynergies.register(ResourceLocation.fromNamespaceAndPath(MODID, "deep_scan"), MODID + ":sonar_unit", "ore_scanner");
SuitBandwidth.register(ResourceLocation.fromNamespaceAndPath(MODID, "sonar_unit"), 2);

if (SuitSynergies.active(player, DEEP_SCAN)) { /* the synergy effect */ }
```

Rules:

* Both units must be installed on worn quantum pieces and be active (installed, on, powered).
* A unit key is a built-in id (`"entity_esp"`) or a registered addon id (`"addon:unit"`). The two keys must differ.
* An addon unit without a registered cost loads 0 QB (the 0.1.10.0 behaviour).
* The ids need the addon namespace; register on both sides before the server starts. A link to a
  synergy of a removed addon stays stored on the chestplate and does nothing.

## 4. Ballistics (`core.api.item`)

The fire-control HUD, the Smart Trajectory Guidance unit and the drones use one ballistic model.
One step is one tick: move, then drag, then gravity.

| Type | Function |
|---|---|
| `BallisticProfile` | Launch speed, gravity, drag, water drag, "ready" flag. Factories: `arrow`, `thrown`, `straight`. |
| `IBallisticWeapon` | Interface of a weapon item. It returns the profile for a shot now. |
| `BallisticProfiles` | Lookup and providers for items of other mods. |

The lookup order is:

1. The item implements `IBallisticWeapon`.
2. A registered provider (`BallisticProfiles.register`), in registration order.
3. The profile that the client learned from shots of the item (see 4.2).
4. The built-in vanilla profiles (bow, crossbow, trident, thrown items).

### 4.1 Procedure: describe a weapon

1. Implement `IBallisticWeapon` on the weapon item.
2. Return a profile for the shot. Use `useTicks` for a weapon with a draw time.
   `BallisticProfile.drawPower(useTicks, fullDrawTicks)` gives the vanilla draw curve.
3. Return `null` when the weapon cannot fire.

For an item of a different mod, register a provider. The provider must return `null` for every
item that it does not know.

### 4.2 Learned profiles

When the local player fires a weapon without a profile, the client watches the new projectile for
one tick. It measures the launch speed, the drag and the gravity. It keeps up to 12 shots for each
item in `config/mio_icif-ballistics.json`. A shot in a fluid, a shot with thrust, and a shot that
hits something at once are not used.

### 4.3 Jetpack thruster sound (0.1.8.1)

A jetpack item that implements `IJetpackItem` and calls `JetpackFlightController.tick` gets the
thruster sound with no other code. The client plays an ignition cue, a loop that follows the
thrust, and a shutdown cue. Other players hear the sound through a small packet that the server
sends only when the thrust state changes.

1. Override `getThrusterSound()` to select the profile: `THRUSTER_ELECTRIC` (light turbine,
   default), `THRUSTER_ADVANCED` (heavy roar) or `THRUSTER_IONIC` (deep ion hum).
2. Do not play your own loop for the same flight. The two sounds overlap.

An unknown value uses the electric profile.
## 5. Armory (`core.api.armory`)

Singularity Iteration posts these events on `NeoForge.EVENT_BUS`, on the server thread.

| Event | When | Cancelable |
|---|---|---|
| `ArmoryEvent.Summon` | Before a suit flies to a wearer (the player or a companion). | Yes. No energy is spent and no item moves. |
| `ArmoryEvent.PieceDocked` | After a flying piece is equipped. | No. |
| `ArmoryEvent.WingmanDeploy` | Before a stored suit deploys as a Wingman drone. | Yes. |
| `ArmoryEvent.WingmanCommand` (0.1.10.2) | Before a quantum command order from the command wheel or the holo table: MOVE, HOLD, SUPPRESS, FOCUS, FORM_UP, MODE, SQUAD, RETURN, QUANTUM_RECALL. `getOrder()`, `getWingmen()`, `getPoint()` (anchored orders), `getTarget()` (FOCUS). | Yes: the order changes nothing. |

### 5.1 Data tags

| Tag | Type | Function |
|---|---|---|
| `mio_icif:armory/remote_equip` | Entity types | Companions that can receive a suit from the Armory. The entity must be `OwnableEntity` and owned by the player. Default: `touhou_little_maid:maid` (optional entry). |
| `mio_icif:armory/drawn_blade` | Items | Items that fly to the hand as a drawn blade. SlashBlade: Resharped blades are found without the tag. |
| `mio_icif:wingman/fulton_immune` | Entity types | Mobs that the Fulton recovery cannot take. Bosses (`c:bosses`) are always immune. |
| `mio_icif:resonance_immune` | Blocks | Blocks that the Resonance Deconstruction Field does not break. |
| `mio_icif:radiation_shielding` (0.1.10.5) | Blocks | Strong radiation shielding: each block on the line lets 10 % through. Default: lead blocks and `#c:storage_blocks/lead`. |
| `mio_icif:radiation_shielding_weak` (0.1.10.5) | Blocks | Weak radiation shielding: each block lets 50 % through. Default: Reinforced Stone, obsidian, crying obsidian, reinforced deepslate, iron block, netherite block, `#c:storage_blocks/steel`. |
| `mio_icif:blast_warning` (0.1.10.5) | Entity types | The blast warning unit treats the entity as an explosive (radius 4). The data map of the same name sets radius, fuse and label (section 12). |
| `c:capturing_not_supported` and others (0.1.10.5) | Entity types | The Wingman is in `c:capturing_not_supported`, `c:teleporting_not_supported`, `industrialforegoing:mob_imprisonment_tool_blacklist`, `ars_nouveau:jar_blacklist`, `enderio:soul_vial_blacklist`. |

### 5.2 Wingman sortie tokens and crash guard (0.1.10.5)

* Each deployment gets a sortie token (entity NBT `Sortie`, world data `mio_icif_wingman_sorties`). The token closes when the suit goes home. A Wingman that enters a level with a closed token, or with the token of a loaded Wingman, is a copy: its `EntityJoinLevelEvent` is cancelled and its items are cleared without a drop. An old Wingman without a token is adopted.
* No item works on a Wingman (`mobInteract` returns CONSUME; `PlayerInteractEvent.EntityInteract` and `EntityInteractSpecific` are cancelled). Do not rely on item interactions with a Wingman.
* `LevelEntityTickGuardMixin` wraps the tick of every entity: when a Wingman, or an entity whose owner chain leads to a Wingman (or that carries the entity tag `mio_icif.wingman_owned`), throws, the Wingman sends its suit home and leaves and the other entity is removed. Every other entity goes to the vanilla crash report as before.
* A Wingman has every player attribute (`ENTITY_INTERACTION_RANGE`, `SWEEPING_DAMAGE_RATIO` and the others), so weapon mods that read player attributes from a `Mob` no longer throw.

## 6. Energy (`core.api.energy`)

The energy API did not change its contracts in 0.1.8.0. The EU network uses fair-share
distribution by default (`distributionMode=fair` in `config/core.ini`, or the common config).

1. Register the role of each block with `EnergyNodeRegistry.register(id, profile)`.
   Use `EnergyNodeProfile.generator(packet)`, `storage(packet)` or `consumer()`.
2. Expose the block capability `EUApi.SIDED`. For a prefab block entity, call
   `CoreCapabilityRegistration.registerEntity(event, type)`.
3. The network gives energy in four stages: loads up to their rated input, then the remaining room
   of the loads, then storages up to their rated input, then the remaining room of the storages.
   In each stage, a receiver gets energy in proportion to its remaining demand. Generators give
   before storages.

### 6.1 Battery stacking (`core.prefab.item.AbstractBattery`, 0.1.10.3)

* Only empty batteries stack. A battery built with a stack size above 1 stacks up to that size while
  it holds no energy. As soon as it holds energy, `getMaxStackSize(ItemStack)` stops the stack from
  growing, so two charged batteries never merge, even with the same charge.
* New constructor `AbstractBattery(Properties, maxEnergy, initialEnergy, chargeRate, maxStackSize,
  emptyStackSize)`: with `maxStackSize` 1 the battery keeps the non-stackable storage (the default
  component holds `initialEnergy`, so old saves keep their charge), and empty batteries still stack
  up to `emptyStackSize`. `getEmptyStackSize()` returns that size.
* An empty stackable battery carries no `mio_icif:battery_energy` component. A new, a crafted and a
  discharged battery are the same stack. Stacks that older versions saved with an explicit zero
  are normalized when they load (`verifyComponentsAfterLoad`).
* A stack of more than one battery never takes or gives energy: `addEnergy`, `extractEnergy`, the FE
  capability and the item API transfer refuse it, and `setEnergy` ignores a positive value on such a
  stack of a stacking battery. Machine battery slots hold one battery, so a stack put into a charge
  slot leaves one battery there.
* Addon batteries that relied on charged batteries with the same charge stacking can override
  `getMaxStackSize(ItemStack)`.

### 6.2 Closed faces of a machine (`IEnergyBlock#canConnect`, 0.1.10.4)

* The EU network now asks every machine built on `AbstractEnergyBlockEntity` for
  `canConnect(side)` when it builds its ports. A side that returns false takes and gives no EU,
  also from a generator or a multi-block source (a nuclear reactor and its chambers) that touches
  it directly; the external EU and FE capabilities of that side refuse too.
* The base class opens all six sides, so existing machines keep their faces.
* A machine whose answer changes (for example by its neighbours) calls
  `IndependentSiEnergy.changed(blockEntity)` so the network rebuilds its ports. The reactor coolant
  injectors use this to close the faces that touch the reactor.

### 6.3 Energy Saving and cost checks (`core.api.item.EnergySaving`, 0.1.10.5)

* Every debit goes through `EnergySaving.apply` first: the EU taken is lower than the nominal cost, and a small cost can be 0. **Never compare the EU taken with the nominal cost.**
* An energy check before a debit uses the new `EnergySaving.maxCost(stack, amount)` (the reduced cost rounded up; `apply` never takes more). `IWeaponItem#canShoot`, `IJetpackItem#canFlyTick`, `IElectricToolItem#hasEnoughEnergy`, `IElectricArmorItem#hasEnoughEnergy` and the same methods of the prefabs `AbstractElectricTool` and `AbstractElectricArmor` use it now.
* Suit costs: `SuitEnergyPool.canPay` / `payAll` (all or nothing, returns whether the cost was met) / `payPart` (returns the share of the nominal cost that is covered). Up to 0.1.10.4 Smart Guidance compared the EU taken with the nominal cost and never worked with an enchanted helmet.

## 7. Blueprint Builder and Material Interface

* The Armory Material Interface exposes `Capabilities.ItemHandler.BLOCK` (27 slots). Any storage
  next to it that exposes the same capability supplies the drones.
* The drones take only plain stacks: no changed components (contents, name, enchantments).
* The builder reads `.nbt` structure files and `.litematic` files from the `schematics` folder,
  and deployed Create schematics (data components `create:schematic_*`).
* Each placement fires `BlockEvent.EntityPlaceEvent` with a fake player that has the owner's
  profile. A protection mod can cancel it.

## 8. Documentation hooks

* GuideME: the manual is a code-built guide (`mio_icif:manual`). The tag
  `<ReactorSimulator chambers="N" preset="row/row/..."/>` shows the reactor simulator. A preset row
  is a comma-separated list of component keys (`_` = empty).
* Create Ponder: scenes for the Armory, the Nuclear Reactor Generator, the Blueprint Builder and the
  Ejector Pipe. Text keys: `mio_icif.ponder.<scene>.header` and `.text_N`.

## 9. Mixins

0.1.8.0 removes the invoker mixin `CropBiomeAccessor`. An access transformer
(`META-INF/accesstransformer.cfg`) replaces it. These features use NeoForge events or data
attachments, not mixins: electric armor absorption (`LivingDamageEvent.Pre`), the suit energy pool,
the Armory origin (player attachment `mio_icif:armory_origin`), the Wingman owner registry, the
smart guidance and the resonance field (tick events).

The remaining mixins and the reason for each:

| Mixin | Reason |
|---|---|
| `ItemStackMaxStackMixin`, `DataComponentMaxStackMixin`, `ContainerMaxStackMixin` | Stack sizes above 99. NeoForge has no hook for the codec limit. |
| `LivingEquipEnergyMixin` | A stack that differs only in stored energy must not trigger the equip sound and the game event. No cancelable NeoForge event exists. |
| `ArmorEquipSoundMixin` | A null equip sound of a different mod crashes footstep mods. The mixin gives a fallback. |
| `client.ItemInHandReequipMixin` | No re-equip animation for energy-only changes on all items (also addon items). `IItemExtension` covers only own items. |
| `client.AdvancementTabMiophaMixin`, `client.AdvancementWidgetHintMixin` | Advancement screen layout. No event exists. |
| `client.FcsGlowMixin`, `client.FcsTeamColorMixin` | Client-only outline glow of the suit sensors. No event exists. |
| `client.PlayerManeuverPoseMixin` | Limb poses of the flight. `RenderPlayerEvent` changes only the pose stack, not the model parts. |
| `LevelEntityTickGuardMixin` (0.1.10.5) | Wraps the entity tick in `Level#guardEntityTick`: an error of a Wingman or of an entity it made no longer crashes the level. No event exists. |
| `IndependentChunkMixin` (core) | The energy network topology clock and the cable / transformer block entity factory. No NeoForge hook creates a block entity. |

## 10. Version rules (proposed policy)

These rules are a proposal for the API. The project has not adopted them formally yet.

* In the version iteration rules, the version format is 0.1.x.y.
* Under this version format, 0 indicates an unofficial release; 1 represents the first series of Singularity Iteration releases; x represents the x-th major        update in that series; and y represents the y-th addition or bug fix made on top of the x-th update.
* A minor version (0.1.8.x) keeps the API source compatible. A removal is announced one version
  before.
* `@Deprecated` API stays for one minor version.
* Check `MioIcifAPI.instance()` at run time. Do not call internal classes.

## 11. Crops and the Advanced Crop Scanner (0.1.8.2)

### 11.1 New default methods

| Method | Purpose |
|---|---|
| `IPlanter#doManualHarvest(Player)` | Harvest by hand for a player. The drops go into the inventory; what does not fit drops at the feet. A harvest that gives no item does nothing (the plant keeps its stage). The default calls `doManualHarvest()`. |
| `IPlanter#markTended(long)` / `isTended()` | Cropmatron tending. A tended crop gets at least the required environment quality + 50 (no penalty, at least +50 % growth). The defaults do nothing. |
| `ICropSeedItem#setCropTraits(ItemStack, int, int, int)` | Writes Growth / Gain / Resistance (each 0..31). Gene editing uses it. Override it when your seed item has its own storage. |

### 11.2 Data

* Ingredient `mio_icif:crop_seed`: a seed bag of one plant and a Gain range. A machine that tests
  recipes with `Ingredient#test` can then scale the output with Gain.
  `{"type": "mio_icif:crop_seed", "plant": "mio_icif:wheat", "min_gain": 8, "max_gain": 15}`
  (`min_gain` default 0, `max_gain` default 31).
* Item tag `mio_icif:crop_seed_bags`: the seed bag. The bag is not in `c:seeds`, because several
  mods consume `#c:seeds` in recipes (seed oil, millstones) and would destroy analyzed bags.
* Scientific name of an addon plant (scanner HUD and menu): language key
  `<modId>.plant.<typeId>.scientific`. Without the key the name is "<Type> sp.".

### 11.3 Rules

* Breeding research lists the candidates of natural crossing (`PlantHybridization#calculateRatio`).
  A plant that natural crossing cannot make cannot be researched either.
* Do not call `CropAnalyzerMenu` from an addon. It is internal; the menu and its slot layout can change.

## 12. Data pack extensions (0.1.10.5)

### 12.1 Replication lists (`data/<namespace>/uu_replication/*.json`)

The replicator prices come from the IC2 reference table and from the recipes. A replication list adds fixed prices, energy costs or denials on top. Its entries are fixed prices: they win over the reference table and the recipe prices, and items made from them get derived prices.

```json
{
  "entries": [
    { "item": "minecraft:netherite_scrap", "uu": 1500 },
    { "item": "minecraft:nether_star", "uu": 100000, "eu": 60000000 },
    { "tag": "c:ores/tin", "uu": 1.4 },
    { "item": "minecraft:dragon_egg", "deny": true },
    { "item": "othermod:gem", "uu": 40, "required": false }
  ]
}
```

* `item` or `tag` (exactly one); `uu`: mB per copy (above 0); `eu`: optional EU per copy (standard: 5,120 EU per mB; the replicator takes at most 8,192 EU per tick); `deny: true`: never scanned or replicated; `required: false`: an unknown id is skipped without a warning.
* Files load in the order of their ids (namespace, then path); a later entry for the same item wins. A wrong entry is skipped with a warning; the other prices still load. At most 4096 items, 512 per tag.
* The default list is `data/mio_icif/uu_replication/defaults.json` (95 entries); a data pack replaces it with a file at the same path.
* At run time `UuQuoteBook.energy(server, item)` returns the EU per copy of a list (-1 without one).

### 12.2 Blast warning data map (`mio_icif:blast_warning`)

A data map on entity types, synced to the clients: `data/<namespace>/data_maps/entity_type/blast_warning.json`.

```json
{ "values": { "othermod:c4": { "radius": 6, "fuse": 5, "label": "C4" } } }
```

* `radius`: danger radius in blocks (required); `fuse`: full fuse in seconds (optional); `label`: HUD label (optional, at most 9 characters).
* The timer: a public `getFuse()` of the entity (or `getFuseTime`, `getFuseTicks`, `getFuseTimer`, in ticks), else `fuse` minus the age of the entity on the client, else no timer (the HUD shows "--").
* Without a data map entry, the entity type tag `mio_icif:blast_warning`, subclasses of vanilla `PrimedTnt` and of the creeper, explosive projectiles and entities with an explosive word in their id also count (see the guide page "Blast Warning Timer").
