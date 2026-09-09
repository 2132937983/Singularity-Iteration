package com.singularity_iteration.mio_icif.uu;

import java.util.List;
import java.util.Set;

/**
 * 延迟配方解析器接口
 * 在初始节点创建后执行，用于处理需要已知节点的配方
 * 与 IC2 1.12.2 原版 ILateRecipeResolver 对应
 */
public interface ILateRecipeResolver {
    List<RecipeTransformation> getTransformations(Set<LeanItemStack> knownNodes);
}