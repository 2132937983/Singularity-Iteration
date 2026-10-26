// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.exchange;

/**
 * <p><b>EN:</b> How a bounty pays its deliverers (public API since 0.1.10.26). FIRST: every delivery is paid at once, each
 * reward in proportion to the value delivered. CONTRIBUTION: paid when the bounty is complete or expires, each reward split
 * by the value each player delivered (largest remainder), so players can gather together.
 * <p><b>ZH:</b> 悬赏向交付者支付的方式（0.1.10.26 起的公共 API）。FIRST：每次交付立即支付，各项奖励按交付价值的比例。
 * CONTRIBUTION：在完成或到期时支付，各项奖励按每名玩家交付的价值分配（最大余数法），便于多人合作收集。
 * <p><b>JA:</b> 懸賞が納品者に払う方法（0.1.10.26 以降の公開 API）。FIRST：納品のたびにすぐ払い、各報酬は納品した価値に比例する。
 * CONTRIBUTION：完了か期限切れのときに払い、各報酬を各プレイヤーが納品した価値で分ける（最大剰余法）。複数人で協力して集められる。
 */
public enum BountySplit {
    FIRST, CONTRIBUTION
}
