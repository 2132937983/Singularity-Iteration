package com.singularity_iteration.mio_icif.uu;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * UU 配方图计算引擎
 * 基于 IC2 1.12.2 原版 UuGraph 算法实现
 * 通过逆向推导计算每个物品的 UU 价值
 */
public class UuGraph {
    private static final double EPSILON = 1.0E-9D;
    private static final List<Node> EMPTY_LIST = new ArrayList<>();

    private final Map<LeanItemStack, Node> nodes = new HashMap<>();
    private final Map<Item, Set<Node>> itemNodes = new IdentityHashMap<>();
    private final List<InitialValue> initialValues = new ArrayList<>();
    private volatile boolean calculationDone = false;

    /**
     * 构建 UU 图
     * @param reset 是否重置已有数据
     * @param resolvers 配方解析器列表
     * @param lateResolvers 延迟配方解析器列表
     */
    public void build(boolean reset, List<IRecipeResolver> resolvers, List<ILateRecipeResolver> lateResolvers) {
        if (reset) {
            nodes.clear();
            itemNodes.clear();
        }

        long startTime = System.nanoTime();
        List<RecipeTransformation> transformations = new ArrayList<>();

        // 收集所有配方转换
        for (IRecipeResolver resolver : resolvers) {
            transformations.addAll(resolver.getTransformations());
        }

        // 为所有输出创建节点
        for (RecipeTransformation transform : transformations) {
            for (LeanItemStack output : transform.outputs) {
                getInternal(output);
            }
        }

        // 为初始值创建节点
        for (InitialValue initialValue : initialValues) {
            getInternal(initialValue.stack);
        }

        // 延迟解析器
        if (lateResolvers != null) {
            Set<LeanItemStack> knownNodes = new HashSet<>(nodes.keySet());
            for (ILateRecipeResolver resolver : lateResolvers) {
                transformations.addAll(resolver.getTransformations(knownNodes));
            }
        }

        Singularity_Iteration.LOGGER.debug("[UU] {} recipe transformations fetched after {} ms.",
                transformations.size(), (System.nanoTime() - startTime) / 1000000L);

        // 异步处理配方
        processRecipes(transformations);
    }

    /**
     * 设置物品的初始 UU 价值（基础值）
     */
    public void set(ItemStack stack, double value) {
        if (calculationDone) throw new IllegalStateException("setting values isn't allowed after calculation.");
        initialValues.add(new InitialValue(new LeanItemStack(stack), value));
    }

    /**
     * 获取物品的 UU 价值
     * @return UU 价值（原始值），如果无法计算返回 Double.POSITIVE_INFINITY
     */
    public double get(ItemStack stack) {
        finishCalculation();
        LeanItemStack key = new LeanItemStack(stack, 1);
        Node ret = nodes.get(key);
        if (ret == null) return Double.POSITIVE_INFINITY;
        return ret.value;
    }

    /**
     * 获取物品的 UU 价值（以 bucket 为单位）
     * IC2 原版 UuIndex.getInBuckets 使用 1.0E-5 转换因子
     * uu_scan_values.ini 中的值 × 1.0E-5 = bucket 值
     */
    public double getInBuckets(ItemStack stack) {
        double ret = get(stack);
        if (ret == Double.POSITIVE_INFINITY) return Double.POSITIVE_INFINITY;
        return ret * 1.0E-5D;
    }

    /**
     * 查找物品对应的已知节点
     * @return 找到的 ItemStack，如果未找到返回 EMPTY
     */
    public ItemStack find(ItemStack stack) {
        finishCalculation();
        LeanItemStack key = new LeanItemStack(stack, 1);
        Node exactNode = nodes.get(key);
        if (exactNode != null) {
            return exactNode.stack.toMcStack();
        }
        return ItemStack.EMPTY;
    }

    /**
     * 获取所有已计算的物品及其 UU 价值
     */
    public Iterator<Map.Entry<ItemStack, Double>> iterator() {
        finishCalculation();
        return new ValueIterator();
    }

    public boolean isCalculationDone() {
        return calculationDone;
    }

    private void processRecipes(List<RecipeTransformation> transformations) {
        long startTime = System.nanoTime();

        for (RecipeTransformation transform : transformations) {
            transform.merge();
            registerTransform(transform);
        }

        for (InitialValue initialValue : initialValues) {
            getInternal(initialValue.stack).setValue(initialValue.value);
        }

        initialValues.clear();

        for (Node node : nodes.values()) {
            node.provides = null;
        }

        calculationDone = true;

        Singularity_Iteration.LOGGER.info("[UU] UU graph built with {} nodes after {} ms.",
                nodes.size(), (System.nanoTime() - startTime) / 1000000L);
    }

    private Node getInternal(LeanItemStack stack) {
        LeanItemStack key = stack.copyWithSize(1);
        Node ret = nodes.get(key);
        if (ret == null) {
            ret = new Node(key);
            nodes.put(key, ret);

            Item item = key.getItem();
            Set<Node> itemNodeSet = itemNodes.get(item);
            if (itemNodeSet == null) {
                itemNodeSet = new HashSet<>(1);
                itemNodes.put(item, itemNodeSet);
            }
            itemNodeSet.add(ret);
        }
        return ret;
    }

    private Collection<Node> getAll(LeanItemStack stack) {
        Collection<Node> ret = itemNodes.get(stack.getItem());
        if (ret != null) return ret;
        return EMPTY_LIST;
    }

    private void registerTransform(RecipeTransformation transform) {
        NodeTransform nt = new NodeTransform(transform);

        for (List<LeanItemStack> inputs : transform.inputs) {
            for (LeanItemStack input : inputs) {
                for (Node node : getAll(input)) {
                    node.provides.add(nt);
                }
            }
        }

        for (LeanItemStack output : transform.outputs) {
            Node node = getInternal(output);
            nt.out.add(node);
        }
    }

    private void finishCalculation() {
        // 同步计算，不需要等待
    }

    private class Node {
        final LeanItemStack stack;
        double value = Double.POSITIVE_INFINITY;
        Set<NodeTransform> provides = new HashSet<>();

        Node(LeanItemStack stack) {
            this.stack = stack;
        }

        void setValue(double value) {
            if (value >= this.value - EPSILON) return;
            this.value = value;

            for (NodeTransform nt : this.provides) {
                for (Node node : nt.out) {
                    int outputSize = nt.getOutputSize(node.stack);
                    if (outputSize <= 0) continue;
                    if (node.value > value / outputSize) {
                        node.updateValue(nt, outputSize);
                    }
                }
            }
        }

        void updateValue(NodeTransform nt, int outputSize) {
            double newValue = nt.transform.transformCost;

            for (List<LeanItemStack> inputs : nt.transform.inputs) {
                double minValue = Double.POSITIVE_INFINITY;
                for (LeanItemStack input : inputs) {
                    double minValue2 = Double.POSITIVE_INFINITY;
                    for (Node node : UuGraph.this.getAll(input)) {
                        if (node.value < minValue2) minValue2 = node.value;
                    }
                    minValue2 *= input.getSize();
                    if (minValue2 < minValue) minValue = minValue2;
                }
                newValue += minValue;
            }

            setValue(newValue / outputSize);
        }
    }

    private static class NodeTransform {
        final RecipeTransformation transform;
        final Set<Node> out = new HashSet<>();

        NodeTransform(RecipeTransformation transform) {
            this.transform = transform;
        }

        int getOutputSize(LeanItemStack output) {
            for (LeanItemStack stack : this.transform.outputs) {
                if (stack.hasSameItem(output)) {
                    return stack.getSize();
                }
            }
            return 0;
        }
    }

    private static class InitialValue {
        final LeanItemStack stack;
        final double value;

        InitialValue(LeanItemStack stack, double value) {
            this.stack = stack;
            this.value = value;
        }
    }

    private class ValueIterator implements Iterator<Map.Entry<ItemStack, Double>> {
        private final Iterator<Node> parentIterator = nodes.values().iterator();

        @Override
        public boolean hasNext() {
            return parentIterator.hasNext();
        }

        @Override
        public Map.Entry<ItemStack, Double> next() {
            Node node = parentIterator.next();
            return new AbstractMap.SimpleImmutableEntry<>(node.stack.toMcStack(), node.value);
        }
    }
}