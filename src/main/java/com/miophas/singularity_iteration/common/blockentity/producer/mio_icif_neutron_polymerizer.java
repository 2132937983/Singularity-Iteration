package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.common.menu.producer.NeutronPolymerizerMenu;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.energy.ICableTier;
import com.miophas.singularity_iteration.core.prefab.blockentity.GenericMachineBlockEntity;
import com.miophas.singularity_iteration.core.api.machine.IProducerBlock;
import com.miophas.singularity_iteration.core.api.machine.ISlotLayout;
import com.miophas.singularity_iteration.core.api.machine.ISlotType;
import com.miophas.singularity_iteration.core.api.machine.builder.IElectricMachineBuilder;
import com.miophas.singularity_iteration.core.api.machine.builder.IMachineBuilderAPI;
import com.miophas.singularity_iteration.core.api.recipe.IRecipeAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

@SuppressWarnings("null")
public class mio_icif_neutron_polymerizer extends GenericMachineBlockEntity {

    public static final long DEFAULT_CAPACITY = 2304000L;
    public static final long DEFAULT_MAX_RECEIVE = 2048L;
    public static final long DEFAULT_MAX_EXTRACT = 0L;
    public static final int DEFAULT_WORK_TIME = 1500;
    public static final long DEFAULT_ENERGY_PER_TICK = 1536L;

    private static IMachineBuilderAPI.MachineConfiguration CACHED_CONFIGURATION;
    private static ISlotLayout CACHED_LAYOUT;
    private static ICableTier CACHED_TIER;

    private final int inputSlot;
    private final int batterySlot;
    private final int[] outputSlots;

    public mio_icif_neutron_polymerizer(BlockPos pos, BlockState state) {
        this(pos, state, mio_icif_block_entities.NEUTRON_POLYMERIZER_ENTITY_TYPE.get());
    }

    public mio_icif_neutron_polymerizer(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type,
            DEFAULT_CAPACITY,
            DEFAULT_MAX_RECEIVE,
            DEFAULT_MAX_EXTRACT,
            DEFAULT_WORK_TIME,
            getOrCreateLayout(),
            DEFAULT_ENERGY_PER_TICK,
            getOrCreateTier());
        ISlotLayout layout = getOrCreateLayout();
        this.inputSlot = layout.getInputSlots()[0];
        this.batterySlot = layout.getBatterySlots()[0];
        this.outputSlots = layout.getOutputSlots();
        setConfiguration(getOrCreateConfiguration());
        setWorkHandler(this::handleWork);
    }

    public static ISlotLayout getOrCreateLayout() {
        if (CACHED_LAYOUT == null) {
            CACHED_LAYOUT = MioIcifAPI.instance().getMachineBuilderAPI()
                .createStandardLayout(1, 1, true, 4);
        }
        return CACHED_LAYOUT;
    }

    public static ICableTier getOrCreateTier() {
        if (CACHED_TIER == null) {
            CACHED_TIER = MioIcifAPI.instance().getEnergyNetAPI().getCableTier("ev");
        }
        return CACHED_TIER;
    }

    public static IMachineBuilderAPI.MachineConfiguration getOrCreateConfiguration() {
        if (CACHED_CONFIGURATION == null) {
            CACHED_CONFIGURATION = buildMachineDefinition();
        }
        return CACHED_CONFIGURATION;
    }

    private static IMachineBuilderAPI.MachineConfiguration buildMachineDefinition() {
        IElectricMachineBuilder builder = MioIcifAPI.instance().getMachineBuilderAPI()
            .createElectricMachineBuilder()
            .setName("neutron_polymerizer")
            .setTranslationKey("container.mio_icif.neutron_polymerizer")
            .setEnergyCapacity(DEFAULT_CAPACITY)
            .setMaxReceive(DEFAULT_MAX_RECEIVE)
            .setMaxExtract(DEFAULT_MAX_EXTRACT)
            .setEnergyPerTick(DEFAULT_ENERGY_PER_TICK)
            .setProcessTime(DEFAULT_WORK_TIME)
            .setCableTier(getOrCreateTier())
            .useStandardLayout(1, 1, true, 4)
            .setSupportsUpgrades(true)
            .setMachineType(com.miophas.singularity_iteration.core.api.machine.IMachineAPI.MachineType.NEUTRON_POLYMERIZER)
            .withEntityType(mio_icif_block_entities.NEUTRON_POLYMERIZER_ENTITY_TYPE.get());

        builder.buildAndRegister("mio_icif");
        return builder.getConfiguration();
    }

    /** 0.1.11.8: the work happens in {@link #doWork}; the generic handler is not used. */
    private void handleWork(GenericMachineBlockEntity machine, ItemStack[] inputs) { }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        ISlotType type = getOrCreateLayout().getType(slot);
        if (type.isInput()) return isPolymerizable(stack);
        if (type.isBattery()) return isBattery(stack);
        if (type.isOutput()) return false;
        if (type.isUpgrade()) return getItemAPI().isUpgrade(stack);
        return false;
    }

    @Override
    protected int[] getSlotsForDirection(Direction side) {
        int[] allSlots = new int[1 + outputSlots.length + 1];
        allSlots[0] = inputSlot;
        System.arraycopy(outputSlots, 0, allSlots, 1, outputSlots.length);
        allSlots[allSlots.length - 1] = batterySlot;
        return allSlots;
    }

    private boolean isPolymerizable(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        return MioIcifAPI.instance().getRecipeAPI().findNeutronPolymerizerRecipe(stack, level).isPresent();
    }

    @Override
    protected boolean hasValidRecipe() {
        ItemStack input = itemHandler.getStackInSlot(inputSlot);
        return isPolymerizable(input);
    }

    // ------------------------------------------------------------------ 0.1.11.8: the job (recipe values, chain, warm-up, radiation)

    /** Warm-up: the accelerator reaches full power after this many ticks of work and cools in twice as many. */
    public static final int WARMUP_TICKS = 200;
    /** Speed (and draw) at a cold start; full speed when warm. */
    public static final float COLD_SPEED = 0.15F;
    /** Radiation reach in blocks around an unshielded machine; each adjacent lead block takes one away. */
    public static final int RADIATION_RANGE = 6;
    private static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> LEAD_BLOCKS =
        net.minecraft.tags.BlockTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/lead"));

    private double jobProgress;
    private String jobKey = "";
    private float ramp;
    @org.jetbrains.annotations.Nullable private net.minecraft.world.item.Item target;
    private int radiationRadius;
    private boolean radiating;

    private java.util.List<com.miophas.singularity_iteration.common.recipe.generic.PolymerizerChain.Stage> job() {
        if (level == null) return java.util.List.of();
        return com.miophas.singularity_iteration.common.recipe.generic.PolymerizerChain.job(level, itemHandler.getStackInSlot(inputSlot), target);
    }

    private static String keyOf(java.util.List<com.miophas.singularity_iteration.common.recipe.generic.PolymerizerChain.Stage> job) {
        StringBuilder b = new StringBuilder();
        for (var st : job) b.append(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(st.result().getItem())).append('>');
        return b.toString();
    }

    /** Speed of the job relative to a warm machine (the draw follows it, so an item always costs the same energy). */
    public float speedFactor() {
        return COLD_SPEED + (1 - COLD_SPEED) * ramp;
    }

    /** The stage the job is in at {@code ticks}, and the ticks before it. */
    private int stageAt(java.util.List<com.miophas.singularity_iteration.common.recipe.generic.PolymerizerChain.Stage> job, double ticks, int[] start) {
        int before = 0;
        for (int i = 0; i < job.size(); i++) {
            int t = job.get(i).ticks();
            if (ticks < before + t || i == job.size() - 1) { if (start != null) start[0] = before; return i; }
            before += t;
        }
        return 0;
    }

    /** The current stage's draw, scaled by the warm-up (the upgrades and work cycles scale it further). */
    @Override
    public long getEnergyPerTick() {
        if (itemHandler == null || level == null) return DEFAULT_ENERGY_PER_TICK;
        var job = job();
        if (job.isEmpty()) return DEFAULT_ENERGY_PER_TICK;
        int eut = job.get(stageAt(job, jobProgress, null)).eut();
        return Math.max(1, Math.round(eut * (double) speedFactor()));
    }

    private java.util.Optional<com.miophas.singularity_iteration.core.runtime.processing.RecipeSlots.Prepared> plan(
            java.util.List<com.miophas.singularity_iteration.common.recipe.generic.PolymerizerChain.Stage> job) {
        return com.miophas.singularity_iteration.core.runtime.processing.RecipeSlots.prepare(itemHandler, inputSlot,
            job.get(0).ingredientCount(), outputSlots, java.util.List.of(job.get(job.size() - 1).result()));
    }

    @Override
    protected boolean canWork() {
        var job = job();
        if (job.isEmpty() || !hasEnoughEnergy()) return false;
        return com.miophas.singularity_iteration.core.runtime.processing.RecipeSlots.fits(itemHandler, inputSlot,
            job.get(0).ingredientCount(), outputSlots, java.util.List.of(job.get(job.size() - 1).result()));
    }

    @Override
    protected void doWork() {
        var job = job();
        if (job.isEmpty()) { stopWork(); return; }
        String key = keyOf(job);
        if (!key.equals(jobKey)) { jobKey = key; jobProgress = 0; }
        float speed = speedFactor();
        if (!consumeEnergy()) { stopWork(); return; }
        isWorking = true;
        jobProgress += getProgressPerTick() * (double) speed;
        ramp = Math.min(1F, ramp + 1F / WARMUP_TICKS);
        int total = com.miophas.singularity_iteration.common.recipe.generic.PolymerizerChain.totalTicks(job);
        maxProgress = total;
        progress = (int) Math.min(total, Math.floor(jobProgress));
        irradiate(job.get(stageAt(job, jobProgress, null)));
        if (jobProgress >= total) finishJob(job);
    }

    private void finishJob(java.util.List<com.miophas.singularity_iteration.common.recipe.generic.PolymerizerChain.Stage> job) {
        var prepared = plan(job);
        if (prepared.isEmpty()) { jobProgress = maxProgress; return; }      // output full: wait at the end
        if (!prepared.get().commit()) { jobProgress = maxProgress; return; }
        jobProgress = 0;
        finishWork();
        setChanged();
        if (canWork()) isWorking = true;
    }

    /** Progress is kept in {@link #jobProgress} (fractional, for the warm-up). */
    @Override
    protected void updateProgress() { }

    /** Neutron radiation: once a second while a radiating step runs, living things in reach get the radiation effect. */
    private void irradiate(com.miophas.singularity_iteration.common.recipe.generic.PolymerizerChain.Stage stage) {
        radiating = stage.process().radiation();
        if (!radiating || !(level instanceof net.minecraft.server.level.ServerLevel server)) { radiationRadius = 0; return; }
        int lead = 0;
        for (Direction d : Direction.values()) if (server.getBlockState(worldPosition.relative(d)).is(LEAD_BLOCKS)) lead++;
        radiationRadius = Math.max(0, RADIATION_RANGE - lead);
        if (radiationRadius == 0 || server.getGameTime() % 20 != 0) return;
        var centre = net.minecraft.world.phys.Vec3.atCenterOf(worldPosition);
        for (var e : server.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                new net.minecraft.world.phys.AABB(worldPosition).inflate(radiationRadius))) {
            if (e.position().distanceToSqr(centre) > (double) radiationRadius * radiationRadius) continue;
            if (com.miophas.singularity_iteration.common.util.RadiationProtectionUtil.isRadiationProtected(e)) continue;
            e.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.miophas.singularity_iteration.common.effect.mio_icif_effects.RADIATION,
                100, 0, false, true));
        }
    }

    /** Menu button 0: the next chain target (none → each product the chain can reach → none). */
    public boolean cycleTarget() {
        if (level == null) return false;
        var targets = com.miophas.singularity_iteration.common.recipe.generic.PolymerizerChain.targets(level, itemHandler.getStackInSlot(inputSlot));
        if (targets.isEmpty()) target = null;
        else {
            int i = target == null ? -1 : targets.indexOf(target);
            target = i + 1 < targets.size() ? targets.get(i + 1) : null;
        }
        setChanged();
        return true;
    }

    public float getRamp() { return ramp; }
    public double getJobProgress() { return jobProgress; }
    @org.jetbrains.annotations.Nullable public net.minecraft.world.item.Item getTarget() { return target; }
    public int getRadiationRadius() { return radiating ? radiationRadius : -1; }

    /** Menu data (see the screen): progress, total, working, energy, ramp, stage, radiation, target, draw, speed. */
    public net.minecraft.world.inventory.ContainerData getContainerData() {
        return new net.minecraft.world.inventory.ContainerData() {
            @Override public int getCount() { return DATA_COUNT; }
            @Override public void set(int i, int v) { }
            @Override public int get(int i) {
                var job = job();
                int[] start = new int[1];
                int stage = job.isEmpty() ? 0 : stageAt(job, jobProgress, start);
                return switch (i) {
                    case 0 -> (int) Math.floor(jobProgress);
                    case 1 -> job.isEmpty() ? 0 : com.miophas.singularity_iteration.common.recipe.generic.PolymerizerChain.totalTicks(job);
                    case 2 -> isWorking ? 1 : 0;
                    case 3 -> (int) Math.min(Integer.MAX_VALUE, energyStorage.getAmount());
                    case 4 -> (int) Math.min(Integer.MAX_VALUE, energyStorage.getCapacity());
                    case 5 -> Math.round(ramp * 1000);
                    case 6 -> stage;
                    case 7 -> job.size();
                    case 8 -> isWorking ? (int) Math.min(Integer.MAX_VALUE, getEffectiveEnergyPerTick()) : 0;
                    case 9 -> getRadiationRadius();
                    case 10 -> target == null ? -1 : net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(target);
                    case 11 -> start[0];
                    case 12 -> Math.round(getProgressPerTick() * speedFactor() * 1000);
                    default -> 0;
                };
            }
        };
    }

    public static final int DATA_COUNT = 13;

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("PolyJob", jobProgress);
        tag.putString("PolyKey", jobKey);
        tag.putFloat("PolyRamp", ramp);
        if (target != null) tag.putString("PolyTarget", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(target).toString());
    }

    @Override
    public void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        jobProgress = Math.max(0, tag.getDouble("PolyJob"));
        jobKey = tag.getString("PolyKey");
        ramp = Math.max(0, Math.min(1, tag.getFloat("PolyRamp")));
        target = null;
        if (tag.contains("PolyTarget")) {
            var rl = net.minecraft.resources.ResourceLocation.tryParse(tag.getString("PolyTarget"));
            if (rl != null && net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(rl)) target = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(rl);
        }
    }

    public ItemStack getResultItem() {
        var job = job();
        return job.isEmpty() ? ItemStack.EMPTY : job.get(job.size() - 1).result().copy();
    }

    public ItemStack getInputItem() {
        return itemHandler.getStackInSlot(inputSlot);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_neutron_polymerizer blockEntity) {
        if (level.isClientSide()) return;
        if (blockEntity instanceof IProducerBlock producer) {
            producer.serverTick();
        }
        blockEntity.setLit(blockEntity.isWorking());
        // 0.1.11.8: the accelerator cools when it does not work
        if (!blockEntity.isWorking() && blockEntity.ramp > 0) blockEntity.ramp = Math.max(0F, blockEntity.ramp - 1F / (2 * WARMUP_TICKS));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.neutron_polymerizer");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new NeutronPolymerizerMenu(containerId, playerInventory, this);
    }
}