// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.suit;

import net.minecraft.world.item.ItemStack;

/**
 * <p><b>EN:</b> An addon item that is a quantum suit upgrade unit (public API since 0.1.8.0). The quantum
 * modification station accepts it when its {@link SuitModuleSpec} is registered in {@link SuitModuleRegistry}.
 * <p><b>ZH:</b> 作为量子套装升级单元的附属物品（0.1.8.0 起的公共 API）。其 {@link SuitModuleSpec} 已在 {@link SuitModuleRegistry} 中注册时，量子改装台接受它。
 * <p><b>JA:</b> 量子スーツ強化ユニットとなるアドオンのアイテム（0.1.8.0 以降の公開 API）。その {@link SuitModuleSpec} が {@link SuitModuleRegistry} に
 * 登録されていれば、量子改造台が受け付ける。
 */
public interface ISuitModuleItem {
    /** EN: The unit that this item installs. ZH: 本物品安装的单元。JA: このアイテムが取り付けるユニット。 */
    SuitModuleSpec suitModule(ItemStack stack);
}
