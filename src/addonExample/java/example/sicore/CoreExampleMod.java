package example.sicore;

import com.miophas.singularity_iteration.core.api.energy.EnergyNodeProfile;
import com.miophas.singularity_iteration.core.api.energy.EnergyNodeRegistry;
import com.miophas.singularity_iteration.core.platform.neoforge.CoreCapabilityRegistration;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** An actual separate mod, compiled using only the published core JAR and NeoForge. */
@Mod(CoreExampleMod.ID)
public final class CoreExampleMod {
    public static final String ID = "si_core_example";
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    public static final net.neoforged.neoforge.registries.DeferredItem<ForeignCropSeed> FOREIGN_CROP_SEED = ITEMS.register("foreign_crop_seed", ForeignCropSeed::new);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
    public static final DeferredBlock<Block> SOURCE = BLOCKS.register("source", () -> new ExampleBlock(InheritedSource::new));
    public static final DeferredBlock<Block> STORAGE = BLOCKS.register("storage", () -> new ExampleBlock(ComposedStorage::new));
    public static final DeferredBlock<Block> SINK = BLOCKS.register("sink", () -> new ExampleBlock(InheritedSink::new));
    public static final DeferredBlock<Block> MULTI_SOURCE = BLOCKS.register("multi_source", () -> new ExampleBlock(InheritedMultiSource::new));
    public static final DeferredBlock<Block> PACKET_SOURCE = BLOCKS.register("packet_source", () -> new ExampleBlock(ComposedPacketSource::new));
    public static final DeferredBlock<Block> FOREIGN_BATTERY = BLOCKS.register("foreign_battery", () -> new ExampleBlock(ForeignBattery::new));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ForeignBattery>> FOREIGN_BATTERY_ENTITY =
        ENTITIES.register("foreign_battery", () -> BlockEntityType.Builder.of(ForeignBattery::new, FOREIGN_BATTERY.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InheritedMultiSource>> MULTI_SOURCE_ENTITY =
        ENTITIES.register("multi_source", () -> BlockEntityType.Builder.of(InheritedMultiSource::new, MULTI_SOURCE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ComposedPacketSource>> PACKET_SOURCE_ENTITY =
        ENTITIES.register("packet_source", () -> BlockEntityType.Builder.of(ComposedPacketSource::new, PACKET_SOURCE.get()).build(null));
    public static final DeferredBlock<Block> FLUID_MACHINE = BLOCKS.register("fluid_machine",
        () -> new ExampleBlock(FluidMachine::create));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InheritedSource>> SOURCE_ENTITY =
        ENTITIES.register("source", () -> BlockEntityType.Builder.of(InheritedSource::new, SOURCE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ComposedStorage>> STORAGE_ENTITY =
        ENTITIES.register("storage", () -> BlockEntityType.Builder.of(ComposedStorage::new, STORAGE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidMachine>> FLUID_MACHINE_ENTITY =
        ENTITIES.register("fluid_machine", () -> BlockEntityType.Builder.of(
            FluidMachine::create, FLUID_MACHINE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InheritedSink>> SINK_ENTITY =
        ENTITIES.register("sink", () -> BlockEntityType.Builder.of(InheritedSink::new, SINK.get()).build(null));

    public CoreExampleMod(IEventBus bus) {
        EnergyNodeRegistry.register(id("source"), EnergyNodeProfile.generator(32));
        EnergyNodeRegistry.register(id("storage"), EnergyNodeProfile.storage(32));
        EnergyNodeRegistry.register(id("sink"), EnergyNodeProfile.consumer());
        EnergyNodeRegistry.register(id("multi_source"), EnergyNodeProfile.generator(32));
        EnergyNodeRegistry.register(id("packet_source"), EnergyNodeProfile.generator(32));
        ITEMS.register(bus);
        BLOCKS.register(bus);
        ENTITIES.register(bus);
        bus.addListener(CoreExampleMod::capabilities);
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(ID, path); }
    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,
            FOREIGN_BATTERY_ENTITY.get(), (entity, side) -> entity.storage);
        CoreCapabilityRegistration.registerEntity(event, SOURCE_ENTITY.get());
        CoreCapabilityRegistration.registerEntity(event, SINK_ENTITY.get());
        CoreCapabilityRegistration.registerEntity(event, MULTI_SOURCE_ENTITY.get());
        CoreCapabilityRegistration.registerComponent(event, PACKET_SOURCE_ENTITY.get());
        CoreCapabilityRegistration.registerComponent(event, STORAGE_ENTITY.get());
        CoreCapabilityRegistration.registerEntity(event, FLUID_MACHINE_ENTITY.get());
        CoreCapabilityRegistration.registerFluidHandler(event, FLUID_MACHINE_ENTITY.get());
    }

    private static final class ExampleBlock extends Block implements EntityBlock {
        private final BiFunction<BlockPos, BlockState, BlockEntity> factory;
        private ExampleBlock(BiFunction<BlockPos, BlockState, BlockEntity> factory) {
            super(BlockBehaviour.Properties.of().strength(2));
            this.factory = factory;
        }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return factory.apply(pos, state); }
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (level.isClientSide || type != PACKET_SOURCE_ENTITY.get()) return null;
            return (world, pos, currentState, entity) -> ((ComposedPacketSource) entity).tickFeProbe();
        }
    }
}
