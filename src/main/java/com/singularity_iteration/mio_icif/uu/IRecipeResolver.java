package com.singularity_iteration.mio_icif.uu;

import java.util.List;

/**
 * 配方解析器接口
 * 与 IC2 1.12.2 原版 IRecipeResolver 对应
 */
public interface IRecipeResolver {
    List<RecipeTransformation> getTransformations();
}