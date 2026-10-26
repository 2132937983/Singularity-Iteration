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
| `ItemStackMaxStackMixin`, `DataComponentMaxStackMixin` | Save and load of stacks above 99 (the item count and the `max_stack_size` component). NeoForge has no hook for the codec limit. Since 0.1.10.16 they only widen the codec range; they make no stack larger. |
| `LargeStackContainerMixin`, `LargeStackSlotMixin` (0.1.10.16) | `Container#getMaxStackSize(ItemStack)` and `Slot#getMaxStackSize(ItemStack)` give more than 99 only to an SI `LargeStackItem` (the coin), only with the default limit of 99, and only in a container that keeps large stacks (vanilla containers, SI `LargeStackContainer`s). They replace the global `ContainerMaxStackMixin`, which gave 9,999 to every container. Optional (`require = 0`) with a start-up self check. |
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

### 12.3 Fermenter recipes (`"type": "mio_icif:fermenter"`)

The Fermenter has no built-in recipes: it ferments what the recipe type `mio_icif:fermenter` lists. The default recipe is `data/mio_icif/recipe/fermenter/biomass_to_biogas.json` (the IC2 values).

```json
{
  "type": "mio_icif:fermenter",
  "input": { "fluid": "mio_icif:biomass", "amount": 20 },
  "heat": 4000,
  "output": { "id": "mio_icif:biogas", "amount": 400 },
  "byproduct": { "id": "mio_icif:resource/item_fertilizer", "count": 1 },
  "byproduct_interval": 500
}
```

* One run takes `input` (a fluid or a fluid `tag`, and the mB per run) and `heat` (HU, above 0) and gives `output`.
* Every `byproduct_interval` mB of input fermented (default 500) gives `byproduct` (optional).
* A fluid has one fermenter recipe: the first that takes it. The input tank only accepts recipe inputs, the output tank only recipe outputs.
* Java: `IRecipeRegistrationAPI#registerFermenterRecipe(id, FluidStack input, int heat, FluidStack output)` (no by-product) and `IRecipeAPI#findFermenterRecipe(FluidStack, Level)`. The item forms of both are deprecated and do nothing: the fermenter takes fluids.

### 12.4 The recipe API and the machines (0.1.10.14)

Every recipe type of `IRecipeAPI#getAllRecipeTypeIds()` is registered, and recipes of every type sync (before 0.1.10.14 one recipe of an unregistered type stopped the sync of all runtime recipes). The machines read these types:

| Recipe type | Machine | What a recipe does |
|---|---|---|
| `mio_icif:electric_furnace` | Electric Furnace, Induction Furnace | Smelted before vanilla smelting, with its XP. |
| `mio_icif:recycler` | Recycler | The item gives the recipe's output with the recipe's chance (none given: 1/8), else nothing. |
| `mio_icif:electrolyzer` | Electrolyzer | Another input besides water cells: it gives the output and stores 200 EU like a water cell. A recipe for water cells comes first. |
| `mio_icif:mass_fabricator` | Mass Fabricator | An amplifier: one item adds `credit` EU of amplifier credit, before the built-in scrap values. |
| `blender`, `welder`, `laser_engraver`, `precision_assembler`, `vacuum_freezer`, `plasma_furnace`, `fusion_reactor`, `lathe`, `scanner`, `replicator` | none | For add-on machines (`IRecipeAPI#find...`). |

* The machines keep their own speed and EU/t (the IC2 values), as for their JSON recipes: `processTime` and EU/t of a recipe are for add-on machines.
* Mass Fabricator amplifiers: `IRecipeRegistrationAPI#registerMassFabricatorAmplifier(id, Ingredient, int euCredit)`, or a data pack recipe:

```json
{ "type": "mio_icif:mass_fabricator", "ingredient": { "item": "minecraft:nether_star" }, "credit": 700000 }
```

* Lathe parts: `IRecipeRegistrationAPI#registerLatheShape(id, blank, int[] profile, ItemStack part)` names a profile (5 positions, 1 to 5 for the iron blank, 1 to 3 for the wooden one) as the part for an item. Blanks cut to it show its name (translation key `lathe_shape.<namespace>.<path>`, else the profile) and JEI lists it. A crafting recipe asks for the part with `{"type": "mio_icif:lathe_state", "item": "mio_icif:normal/item_iron_turning_blanks", "state": [5, 4, 3, 2, 1]}`. Shapes are not recipes: register them on both sides (for example in `FMLCommonSetupEvent`).
* Recycler recipes in JSON take `"chance"` (0 to 1, optional).
* Deprecated, always `false`: `registerLatheRecipe` (use `registerLatheShape`), `registerMassFabricatorRecipe` (use `registerMassFabricatorAmplifier`), `registerScannerRecipe` and `registerReplicatorRecipe` (the scanner and the replicator work from UU-matter prices: use a replication list, 12.1).
* `syncToRecipeManager()` changes the server's recipes only; clients (and JEI) see data pack recipes.

## 13. Machine prefab: routing, overclock throttle, fluid containers (0.1.10.6)

### 13.1 Item routing of the Ejector and Pulling upgrades (`AbstractProcessingMachineBlockEntity`)

* Faces without a direction are tried in this order: storage first, then other blocks, then machines. `protected int neighbourKind(Direction)` returns 0 (storage: block tag `mio_icif:automation/storage`, chests, barrels, shulker boxes), 1 (other) or 2 (machine: block tag `mio_icif:automation/processor`, every `AbstractProcessingMachineBlockEntity`, furnaces, brewing stands). `protected Direction[] byTargetPriority(Iterable<Direction>)` sorts stably by this kind. Override `neighbourKind` for special neighbours.
* Ping-pong guard, `protected boolean pingPong(Direction face, boolean eject)`: a machine does not eject into a machine that it pulled from in the last `PING_PONG_TICKS` (200 ticks), does not pull from a machine that it ejected into, and does not eject into a neighbouring machine whose ejector points back (`public boolean ejectsToward(Direction)`). Storage neighbours are never limited.
* Tags: `mio_icif:automation/storage` (default: `#c:chests`, `#c:barrels`, `#minecraft:shulker_boxes`, the storage crates of SI) and `mio_icif:automation/processor` (empty). Add the blocks of your mod to them.

### 13.2 Overclock throttle

* `getInstalledOverclockers()`, `getOverclockThrottle()` (0..100 %), `getActiveOverclockers()`, `setOverclockThrottle(int percent)` (server; clamps, recalculates the stats, marks the block entity changed). `static activeOverclockers(int installed, int percent)` rounds to the nearest whole overclocker.
* The stats come from `MachineUpgradeStats.withActiveOverclockers(int)`: speed and energy use follow the working overclockers, the energy storage bonus keeps all installed ones. A subclass that builds its own stats calls `throttled(MachineUpgradeStats)`.
* Saved as the NBT key `scex_oc_throttle` (absent = 100 %). The machine screen shows a slider when at least one overclocker is installed; it sends the C2S packet `mio_icif:machine_overclock` (container id, percent). The server applies it only while the menu of that machine is open and valid.

### 13.3 Displayed progress

* A fast overclocked operation can finish and reset its progress in one tick, so the GUI always saw 0. `getDisplayProgress()` returns the full progress for the tick after an operation finishes; menus sync it instead of `getProgress()`. `getDisplayProgressPerTick()` and `getTicksPerOperation()` give the speed of the current setting.

### 13.4 Fluid containers and tags

* Fluid machines accept every fluid container through the item fluid capability (`Capabilities.FluidHandler.ITEM`): buckets, partly filled cells, containers of other mods. A container that still holds fluid stays in its slot; an emptied one goes to the empty-container slot. Make your container work by giving it a correct `IFluidHandlerItem` (`getContainer()` returns what is left).
* The Crop Manager takes water and Weed-EX from every face; its Fluid Pulling Upgrade asks the neighbour for each fluid by name (`drain(FluidStack)`).
* The fluid type of Weed-EX has the description id `fluid_type.mio_icif.weed_ex`.
* Milk Wart (`mio_icif:resource/item_milk_wart`) is in the item tags `c:buckets/milk`, `c:drinks/milk`, `c:milks` and `forge:milk`. Recipes that want milk should use one of these tags.
* The Next-Gen Crop Analyzer stores UU-Matter in the scanner custom data (`Scanner` / `UuMb`, at most 4,000 mB); 1 mB is one gene-editing catalyst.

### 13.5 Quantum Compute Cores

* Cores on the chestplate and on the leggings stack: each active core adds one link and 8 QB.

## 14. Server performance (0.1.10.7)

The changes below keep every result (energy split, losses, fuses, shocks, meter values) and make the
same work cheaper. Addons need to change nothing unless a point says so.

### 14.1 Energy network

* `IndependentEnergyMode`: the overload switches (`voltageOverload`, `enetExplosions`, `enetCableMeltdown`, `enetShockDamage`) are cached until `CoreConfig.revision()` changes (every `CoreConfig.overrideBool` counts) or a setter of the class runs. `enabled()` and `feature(String)` read their launch property once. The distribution mode, line loss model and loss rounding still read their system property at each call.
* Path profiles: per conductor snapshot and delivery path, the engine keeps the lowest fuse limit, insulation breakdown and insulation absorption of the path. A delivery at or below all of them is settled without visiting the path cable by cable; any other delivery, and any path with an unloaded chunk or a missing block entity, is visited as before. A fuse or stripped insulation clears the profiles.
* Meter samples (`IEnergyNetAPI#getNodeStats`): the samples of a tick are queued and added when a meter reads, in the order they were taken (same values as before).
* A native pass in which no source has an allowance replays the result of the same pass of an earlier tick (same snapshot, routing generation, endpoint edits and start cursor).
* The commit of a window reuses the validation done for the same window when nothing was written in between; the commit still checks every written storage.
* Diagnostics: `IndependentSiEnergy#optimizationCounters()` (profile hits, path visits, idle replays, reused validations) and the operator command `/mio_icif perf`.

### 14.2 Cables

* A cable of the independent network has no block entity ticker. `AbstractWireBlockEntity#isPowered()` compares the game time with the time of the last `onEnergyPass()` (20 ticks); `poweredTicksRemaining` is still set and is counted down only by wires that tick (legacy network).
* An addon cable block whose block entity extends `AbstractWireBlockEntity` should return no ticker when `IndependentSiEnergy.controls(state)` is true.

### 14.3 Machines and automation

* Item ejector / pulling upgrades (`AbstractProcessingMachineBlockEntity`): after a run that moved nothing they wait 4, 8, 16, then 20 ticks (`core.runtime.processing.AutomationPacing`; config `Performance.automationPacing`). `onInventoryChanged` wakes them. A subclass with its own item transfers calls the new `noteAutomationMove()` after a move. Fluid upgrades run every tick.
* `neighbourKind(Direction)` is cached per face until the block on that face changes (capability invalidation), at most 200 ticks.
* `WorldFuelCollection` (item and experience generators): after a search that found nothing it searches again after 10 ticks.
* The geomagnetic generator checks its structure when an antenna or pedestal next to it is placed or removed, after loading, and every 40 ticks; the biome every 600 ticks.

## 15. Server performance, phase C (0.1.10.8)

As in section 14, the energy results do not change. The machine sleep and the fluid upgrade pacing
change when a machine looks again (at most 1 second later); both have a config switch.

### 15.1 Energy network

* Route plan cache: a network with more than one native window (more than 32 sources, more than 128
  reachable sinks, or the route vertex bound) keeps the plan of each window after its settlement. The
  next pass takes it again when the conductor snapshot, the routing view and generation, the endpoint
  edits, the first source and the first receiver are the same; such a plan is the plan that would be
  built again. The cache holds at most 64 plans, 1,000,000 route array cells and the routes of two full
  plans per grid, and is emptied when any of these keys changes. The per-tick route budgets are still
  reserved for every window.
* The balance change of a window is summed as credits and debits in `EnergyAmount` (was one signed
  `BigInteger` per port); the result and the exceptions are the same.
* `IndependentSiEnergy#optimizationCounters()` has a fifth value: windows taken from the plan cache.
  `/mio_icif perf` shows it with the machine ticks skipped by the sleep.

### 15.2 Machines

* `AbstractProcessingMachineBlockEntity#idleSleepAllowed()` (new, protected, default `false`): a
  machine that returns `true` sleeps after 20 idle ticks (no work, no progress, redstone allows work,
  no `setChanged()`, stored energy unchanged, empty battery slot). While it sleeps, only its automation
  upgrades run. Any `setChanged()` (also every inventory change), a change of the stored energy,
  `forceStartWork()`, or 20 ticks wake it. Return `true` only when `canWork()` depends on nothing else.
  SI enables it for the macerator, electric furnace, extractor, compressor, recycler and metal former.
  `isIdleAsleep()` and `wakeFromIdleSleep()` are public. Config `Performance.machineSleep`;
  `core.runtime.processing.MachineSleep`.
* Fluid ejector / pulling upgrades use the waits of the item upgrades (4, 8, 16, 20 ticks after a
  run that moved nothing) when the machine uses the built-in transfer; a change of its own tanks or
  inventory runs them at the next tick. A machine that overrides `handleFluidUpgrades`, `ejectFluids`
  or `pullFluids` runs them every tick as before. Config `Performance.automationPacing` covers both.
* The recycler no longer calls `setChanged()` every tick while idle.

### 15.3 Suits and Wingmen

* The suit sync hashes are computed without boxing (the same values as `Objects.hash`).
* Wingman target choice and firing station: the squad and the nearby hostiles are fetched once per
  choice, and the line-of-sight rays are cast only for the best candidates. The choice is the same.
  The separation push and the projectile scan of the escort run every 2nd tick.
* The suit HUD eases the reserve, heat and coherence bars toward the synced values.

## 16. Server performance, phase D (0.1.10.10)

The results do not change; the changes remove work that had no effect.

* `core.runtime.TickMarks.saved(BlockEntity)`: marks the chunk for saving without the neighbour and
  comparator update of `setChanged()`. SI generators and heat/kinetic machines use it for the counters
  of a tick. Use `setChanged()` when a neighbour must react or a comparator value can change.
* `core.runtime.NeighbourCaps.get(owner, capability, face)`: the capability of the neighbour on `face`
  (context `face.getOpposite()`), kept while the neighbour has the same block state object and block
  entity, at most 100 ticks; `null` for an unloaded neighbour (it never loads a chunk). A provider
  whose answer depends on data inside its block entity is asked again at least every 100 ticks; call
  `NeighbourCaps.forget(owner)` to drop the kept answers of an owner.
* `core.runtime.sync.ClientSyncThrottle.request(be)` is now used by more SI blocks; addons that send a
  block entity update on every change should use it too (at most one update per 5 ticks, the last
  state always arrives).
* `MachineStatusTracker.sentStatus(machine)` returns the current status for a machine in a chunk that
  no player watches (the tracker reads only watched chunks, each chunk at its own tick of the 10-tick
  interval).
* Energy network: a fair-mode window whose sinks all have no room ends before the distribution
  (`optimizationCounters()[5]`). The block type, conductor faces and emission positions of a port are
  kept per tile until its block state or the endpoint edit count changes; the faces a machine opens
  (`canConnect`), storage output sides, the output switch and the remote switch are read every tick.
* Diagnostics: `core.runtime.PerfCounters` (per-tick averages of the last 100 ticks) and the new load
  line of `/mio_icif perf`. `AbstractEnergyBlockEntity`, `AbstractHeatBlockEntity` and
  `AbstractKineticBlockEntity` count their server-side `setChanged()` calls.

## 17. Debug sources without overload effects (0.1.10.11)

* `core.api.energy.IOverloadFreeEnergySource`: a source block entity that implements it (and returns
  `true` from `ignoresOverload()`) never causes overload effects in the independent energy network:
  no melted cable, no stripped insulation, no shock and no exploding receiver, whatever its packet
  size. Its deliveries are settled, metered (EU detector cables see the real voltage) and limited by
  the room and the rated input of the receivers as usual; cable loss applies. Meant for creative and
  test blocks only; the SI Creative Generator uses it together with `IEnergyPacketSource` (one packet
  of the whole output per tick).
* The SI chunk loader is owned by the player who places it and follows the server rules of the config
  section `ChunkLoaders`; an addon that places a loader for a player should place it through the block
  (so `setPlacedBy` sets the owner).

## 18. Max-min fair share and the reactor accident audit (0.1.10.12)

* Fair mode shares each stage max-min fair (water-filling): every receiver gets `min(demand, level)`,
  where the level is the highest one the budget pays. Small demands are met in full before a large one
  gets more. Before 0.1.10.12 every receiver got the same fraction of its demand, so a receiver with an
  unlimited rated input (the Molecular Transformer, 120M EU of room) took almost the whole output and
  the machines behind storage blocks starved. An addon machine with a huge `maxReceive` still gets at
  least the level, but it no longer takes the share of the others. The stage order is unchanged: loads
  and transformers first, then storage.
* `core.runtime.energy.engine.FairShareDistributor.Receiver` has a fourth component, `relay` (EU in this
  tick). A storage block (class 1) with a relay asks for `min(rated, relay)` in stage 0, together with
  the loads. The engine sets the relay to the part of one tick of output (packet size times packets)
  that the storage block cannot offer from its own buffer, and only when its output reaches another
  receiver. A storage block that feeds nothing charges after the loads, as before. The constructor
  with three arguments stays (relay 0).
* Receiver windows (at most 128 receivers each) rotate: in each tick the first window starts after the
  receiver where the previous tick stopped, so the receivers of a long bus share in turn.
* `core.runtime.energy.OverloadBlastLog`: the last 64 overvoltage explosions of receivers (dimension,
  position, game time), `near(dimension, x, y, z, radius, since)`. It is cleared when the server stops.
  The reactor audit uses it to name an external overvoltage as a cause.
* Reactor accident audit (in common, not API): a Nuclear Reactor Generator keeps its last 40 cycles in
  memory. An accident, a meltdown with the explosions off, or the removal of a running or hot reactor
  becomes an incident (`/mio_icif reactor audit`). An addon that destroys a reactor should remove the
  block normally, so `onRemove` records the removal.

## 19. Update tags without inventories (0.1.10.17)

The update tag of a block entity (`getUpdateTag`) goes to every player that tracks the chunk, with
every chunk load, and with every block entity packet (`getUpdatePacket`). Since 0.1.10.17 the SI tags
carry only what client code reads.

* `syncInventoryToClient()` (protected, default `false`) in `AbstractProcessingMachineBlockEntity`,
  `AbstractEnergyStorageBlockEntity`, `AbstractGeneratorBlockEntity`, `AbstractHeatGeneratorBlockEntity`
  and `AbstractKineticGeneratorBlockEntity`: the update tag contains the inventory (`inventory` or
  `Items`) only when it returns `true`. Before 0.1.10.17 the inventory was always in the tag.
* Return `true` in an addon block entity when client code reads its items: a block entity renderer
  that draws a stored item, for example. A screen does not need it: the menu gets the items from the
  vanilla slot sync when it opens.
* The prefab loaders apply an inventory only when the tag has one, so a tag without the inventory never
  clears the items of the client copy (both on a chunk load and on a block entity packet).
* Rule for an addon `getUpdateTag`: write the keys the client reads, not the save
  (`saveWithoutMetadata` or `saveAdditional`). A save can hold server data (owners, credits, timers)
  and is sent to every player near the block. When the block entity also sends packets, make its
  `loadAdditional` tolerate missing keys, or apply the update tag in `handleUpdateTag` and
  `onDataPacket` yourself: the vanilla default of both runs the full `loadAdditional` on the tag.

## 20. Reactor layouts and multiblock blueprints (0.1.10.19)

Two data-only registries feed the JEI categories "Reactor Layouts" and "Multiblock Structures" and the
guide. Register in the mod constructor or in a setup event. A second registration of the same id
replaces the first; the namespace `minecraft` is refused. JEI reads both registries when it loads its
recipes (when a world is joined).

* `core.api.reactor.ReactorLayout` / `ReactorLayoutRegistry`: a reactor design in the preset form of the
  guide simulator, rows from the top separated by `/`, cells by `,`. A cell is an SI component key
  (`uranium_quad`, `iridium_vent`), the item id of an SI reactor component, or `_` for an empty slot. The
  first row sets the columns (3 to 9 = 3 + chambers). SI runs each layout through the reactor model for a
  full uranium load (20,000 cycles, or until the steady state) and shows EU/t, the heat balance, the risk
  (stable, components melt, meltdown, with the time), the explosion with the local config, and the fuel
  life. A cell that is not an SI component is shown when its item exists, and the page says that the
  design cannot be simulated. Title key: `reactor_layout.<namespace>.<path>` unless given; the note key
  is optional.

```java
ReactorLayoutRegistry.register(ReactorLayout.of(ResourceLocation.fromNamespaceAndPath("mymod", "starter"),
    "_,vent,_/vent,uranium_simple,vent/_,vent,_", null, "reactor_layout.mymod.starter.note"));
```

* `core.api.multiblock.StructureBlueprint` / `StructureBlueprintRegistry`: a build plan. Layers go from
  the bottom up; a layer is a list of rows from north to south; a row has one symbol per block from west
  to east. Each symbol has a key: the permitted blocks (`BlockRef`: a block id plus block state
  properties by name, so a plan can name blocks before the registry is complete) and an optional note
  key. A space or `.` is a position the structure does not use. `controller(char)` marks the controller,
  which must be in the plan exactly once. `build()` refuses layers or rows of different sizes, a size above
  32, and a symbol without a key. Title key: `structure.<namespace>.<path>` unless given.
* JEI: a 3D view (drag: turn, wheel: zoom, click the layer label: one layer), the materials as input
  slots with their counts (looking up the uses of a block finds the plans that contain it), the key note
  in the slot tooltip, and the plan notes under the view. Blocks are drawn from their models, without
  block entity renderers; a block whose mod is missing is left out.
* Guide (GuideME): `<SiStructure id="mymod:plan" />` inside a `<GameScene>` places the first permitted
  block of each position (optional `x`, `y`, `z` offset); `<SiStructureInfo id="mymod:plan" />` lists the
  materials with their rules and the notes. SI registers both tags for its own manual (guide
  `mio_icif:manual`) only; the plans themselves come from the registry, so the JEI category shows an
  addon plan in any case.
* A blueprint only describes the structure. The controller still checks it in the world with its
  validator (`core.api.machine.IMultiblockValidator`); keep the two in agreement.

```java
StructureBlueprintRegistry.register(StructureBlueprint.builder(ResourceLocation.fromNamespaceAndPath("mymod", "boiler"))
    .layer("CCC", "CCC", "CCC")
    .layer("CCC", "CMC", "CCC")
    .key('C', "structure.mymod.boiler.casing", BlockRef.of("mymod:casing"), BlockRef.of("mymod:port"))
    .key('M', BlockRef.of("mymod:boiler_controller").with("facing", "north"))
    .controller('M')
    .note("structure.mymod.boiler.note")
    .build());
```

## 21. Merit order, load priority and grid protection (0.1.10.22)

* `core.api.energy.grid.DispatchClass` / `IGridDispatch`: a generator block entity declares `MUST_RUN` (its output is
  lost when nobody takes it: solar, wind, water, nuclear) or `DISPATCHABLE` (it burns fuel only while its energy is
  taken). The fair distribution reads the class in every round. The order follows the common config
  `EnergyNet.dispatchMode`: `merit` (default: must-run sources and transformers, then dispatchable generators, then
  storage; fuel charges storage), `economic` (must-run, storage, then fuel; fuel does not charge storage) or `rotation`
  (before 0.1.10.22). A generator without the interface is must-run, so an addon keeps its old behaviour.
* `core.api.energy.grid.LoadPriority` / `ILoadPriority`: `CRITICAL`, `NORMAL` (default) or `DEFERRABLE` for a load or
  a storage block. Each stage of the fair round runs once per priority, critical first: a shortage stops the
  deferrable loads first. `AbstractEnergyBlockEntity` implements the interface and saves the value (`load_priority`);
  the player sets it in the monitoring panel of the energy management terminal. A change is an input of the next
  round, not a topology change.
* Breaker and fuse cables are special cables of the engine (`IndependentSpecialCableBlockEntity` with a protection
  relay). They are internal; an addon sees them as cables that conduct or not.

```java
public final class MyBoilerGenerator extends AbstractGeneratorBlockEntity implements IGridDispatch {
    @Override public DispatchClass dispatchClass() { return DispatchClass.DISPATCHABLE; }
}
```
