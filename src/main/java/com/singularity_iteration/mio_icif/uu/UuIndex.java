package com.singularity_iteration.mio_icif.uu;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * UU 索引管理器（单例）
 * 与 IC2 1.12.2 原版 UuIndex 对应
 * 管理所有配方解析器并触发 UU 图构建
 */
public class UuIndex {
    public static final UuIndex INSTANCE = new UuIndex();

    private List<IRecipeResolver> resolvers = new ArrayList<>();
    private List<ILateRecipeResolver> lateResolvers = new ArrayList<>();
    private UuGraph graph = new UuGraph();
    private boolean initialized = false;

    private UuIndex() {}

    public void addResolver(IRecipeResolver resolver) {
        resolvers.add(resolver);
    }

    public void addResolver(ILateRecipeResolver resolver) {
        lateResolvers.add(resolver);
    }

    public void add(ItemStack stack, double value) {
        if (stack == null || stack.getItem() == null) throw new NullPointerException("invalid itemstack to add");
        graph.set(stack, value);
    }

    public double get(ItemStack request) {
        return graph.get(request);
    }

    public double getInBuckets(ItemStack request) {
        return graph.getInBuckets(request);
    }

    public ItemStack find(ItemStack stack) {
        return graph.find(stack);
    }

    /**
     * 初始化 UU 系统
     * @param level 游戏世界
     * @param scanValues 扫描值配置（必须在构建图之前加载）
     */
    public void init(Level level, UuScanValues scanValues) {
        if (initialized) {
            Singularity_Iteration.LOGGER.info("[UU] UuIndex already initialized, skipping.");
            return;
        }

        doInit(level, scanValues);
    }

    public void reinit(Level level, UuScanValues scanValues) {
        Singularity_Iteration.LOGGER.info("[UU] Re-initializing UU Index...");

        resolvers = new ArrayList<>();
        lateResolvers = new ArrayList<>();
        graph = new UuGraph();
        initialized = false;
        UuRecipeWhitelist.reset();

        doInit(level, scanValues);
    }

    private void doInit(Level level, UuScanValues scanValues) {
        Singularity_Iteration.LOGGER.info("[UU] Initializing UU Index...");

        UuRecipeWhitelist.load();

        if (scanValues != null) {
            loadFromConfig(scanValues);
        }

        resolvers.clear();
        addResolver(new VanillaRecipeResolver(level));
        addResolver(new SmeltingRecipeResolver(level));
        addResolver(new MachineRecipeResolver(level));

        graph.build(true, resolvers, lateResolvers);

        initialized = true;
        Singularity_Iteration.LOGGER.info("[UU] UU Index initialized successfully.");
    }

    /**
     * 从配置文件加载预定义值和世界扫描值
     * 注意：必须在 graph.build() 之前调用
     */
    private void loadFromConfig(UuScanValues scanValues) {
        if (scanValues == null) return;

        // 加载世界扫描基础值
        for (var entry : scanValues.getWorldScanValues().entrySet()) {
            try {
                ItemStack stack = entry.getKey();
                double value = entry.getValue();
                if (!stack.isEmpty()) {
                    add(stack, value);
                    Singularity_Iteration.LOGGER.debug("[UU] Loaded world scan value: {} = {}",
                            stack.getItem().getDescriptionId(), value);
                }
            } catch (Exception e) {
                Singularity_Iteration.LOGGER.warn("[UU] Failed to load world scan value: {}", e.getMessage());
            }
        }

        // 加载预定义值（覆盖自动计算）
        for (var entry : scanValues.getPredefinedValues().entrySet()) {
            try {
                ItemStack stack = entry.getKey();
                double value = entry.getValue();
                if (!stack.isEmpty()) {
                    add(stack, value);
                    Singularity_Iteration.LOGGER.debug("[UU] Loaded predefined value: {} = {}",
                            stack.getItem().getDescriptionId(), value);
                }
            } catch (Exception e) {
                Singularity_Iteration.LOGGER.warn("[UU] Failed to load predefined value: {}", e.getMessage());
            }
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public UuGraph getGraph() {
        return graph;
    }
}