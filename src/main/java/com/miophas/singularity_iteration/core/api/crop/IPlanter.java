package com.miophas.singularity_iteration.core.api.crop;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * <p><b>EN:</b> Block entity of a crop planter (crop sticks). Plant types, machines (Crop Harvester,
 * Cropmatron) and tools use only this interface. Setters clamp their value to the valid range.
 * The crop cycle runs on the server; call setters on the server thread.
 * <p><b>ZH:</b> 作物架的方块实体接口。植物类型、机器（作物收割机、作物监护机）与工具只使用本接口。
 * setter 会把数值限制在有效范围内。作物周期在服务器运行，请在服务器线程调用 setter。
 * <p><b>JA:</b> 作物棒のブロックエンティティのインターフェース。植物タイプ、機械（作物収穫機・作物管理機）、道具はこのインターフェースだけを使う。
 * セッターは値を有効範囲に制限する。作物サイクルはサーバーで動くので、セッターはサーバースレッドで呼ぶ。
 */
public interface IPlanter {

    // ==================== Plant type / 植物类型 / 植物タイプ ====================
    /** EN: The plant, or null for an empty planter. ZH: 植物，空作物架为 null。JA: 植物。空の作物棒なら null。 */
    PlantType getPlant();
    /** EN: Sets the plant (null = empty). ZH: 设置植物（null = 空）。JA: 植物を設定する（null = 空）。 */
    void setPlant(PlantType plantType);

    // ==================== Growth stage / 生长阶段 / 成長段階 ====================
    /** EN: Growth stage, 0..max stage of the plant. ZH: 生长阶段，0..植物最大阶段。JA: 成長段階、0..植物の最大段階。 */
    int getGrowthStage();
    /** EN: Sets the growth stage (clamped). ZH: 设置生长阶段（会被限制）。JA: 成長段階を設定する（制限あり）。 */
    void setGrowthStage(int stage);

    // ==================== Traits 0-31 / 属性 0-31 / 特性値 0-31 ====================
    /** EN: Growth trait: faster growth, higher need. ZH: 生长属性：生长更快，需求更高。JA: 成長値：成長が速く、要求も高い。 */
    int getGrowthSpeed();
    /** EN: Sets the growth trait (0..31). ZH: 设置生长属性（0..31）。JA: 成長値を設定する（0..31）。 */
    void setGrowthSpeed(int speed);

    /** EN: Gain trait: more drops. ZH: 产量属性：更多掉落。JA: 収量値：ドロップが増える。 */
    int getYield();
    /** EN: Sets the gain trait (0..31). ZH: 设置产量属性（0..31）。JA: 収量値を設定する（0..31）。 */
    void setYield(int yield);

    /** EN: Resistance trait: survives poor environments, resists weeds. ZH: 抗性属性：耐受恶劣环境、抵抗杂草。JA: 耐性値：悪い環境に耐え、雑草に抵抗する。 */
    int getResilience();
    /** EN: Sets the resistance trait (0..31). ZH: 设置抗性属性（0..31）。JA: 耐性値を設定する（0..31）。 */
    void setResilience(int resilience);

    // ==================== Stored resources / 储存资源 / 貯蔵資源 ====================
    /** EN: Stored nutrients (0..199); -1 each cycle. ZH: 储存营养（0..199），每周期 -1。JA: 貯蔵栄養（0..199）。毎サイクル -1。 */
    int getNutrients();
    /** EN: Sets the stored nutrients. ZH: 设置储存营养。JA: 貯蔵栄養を設定する。 */
    void setNutrients(int nutrients);

    /** EN: Stored water (0..200); -1 each cycle. ZH: 储水（0..200），每周期 -1。JA: 貯水（0..200）。毎サイクル -1。 */
    int getWater();
    /** EN: Sets the stored water. ZH: 设置储水。JA: 貯水を設定する。 */
    void setWater(int water);

    /** EN: Weed-EX reserve (0..150); above 0 no weed grows here. ZH: 除草剂储量（0..150），大于 0 时此处不长杂草。JA: 除草剤の残量（0..150）。0 より大きい間は雑草が生えない。 */
    int getWeedControl();
    /** EN: Sets the Weed-EX reserve. ZH: 设置除草剂储量。JA: 除草剤の残量を設定する。 */
    void setWeedControl(int weedControl);

    // ==================== Growth points / 生长点 / 成長ポイント ====================
    /** EN: Growth points of the current stage. ZH: 当前阶段的生长点。JA: 現在の段階の成長ポイント。 */
    int getProgress();
    /** EN: Sets the growth points (0 or more). ZH: 设置生长点（不小于 0）。JA: 成長ポイントを設定する（0 以上）。 */
    void setProgress(int progress);

    // ==================== Scan level / 扫描等级 / スキャン段階 ====================
    /** EN: Crop analyzer scan level of the plant (0..4). ZH: 作物分析仪扫描等级（0..4）。JA: 作物分析機のスキャン段階（0..4）。 */
    int getScanLevel();
    /** EN: Sets the scan level (0..4). ZH: 设置扫描等级（0..4）。JA: スキャン段階を設定する（0..4）。 */
    void setScanLevel(int level);

    // ==================== Crossing base / 杂交基座 / 交配台 ====================
    /** EN: True for crossing sticks (two sticks, no plant). ZH: 杂交作物架（双层、无植物）为真。JA: 交配用作物棒（2 本重ね、植物なし）なら真。 */
    boolean isHybridBase();
    /** EN: Sets the crossing flag. ZH: 设置杂交标志。JA: 交配フラグを設定する。 */
    void setHybridBase(boolean hybridBase);

    // ==================== Custom data / 自定义数据 / カスタムデータ ====================
    /**
     * <p><b>EN:</b> Saved data that a plant type can use (for example the "eaten" flag of the eating
     * plant). Mark the block entity changed after a change.
     * <p><b>ZH:</b> 植物类型可使用的已保存数据（例如食人花的 “eaten” 标志）。修改后请标记方块实体已更改。
     * <p><b>JA:</b> 植物タイプが使える保存データ（例：食虫植物の「eaten」フラグ）。変更したらブロックエンティティに変更済みの印を付ける。
     */
    CompoundTag getCustomData();

    // ==================== Environment / 环境 / 環境 ====================
    /** EN: Humidity (biome, farmland moisture, stored water). ZH: 湿度（生物群系、耕地湿度、储水）。JA: 湿度（バイオーム、耕地の水分、貯水）。 */
    int getHumidity();
    /** EN: Soil nutrients (biome, dirt under the farmland, stored nutrients). ZH: 土壤营养（生物群系、耕地下方泥土、储存营养）。JA: 土壌栄養（バイオーム、耕地下の土、貯蔵栄養）。 */
    int getSoilNutrients();
    /** EN: Air quality (height, free neighbour cells, sky view). ZH: 空气质量（高度、相邻空格、可见天空）。JA: 空気品質（高さ、隣接の空き、空の見え方）。 */
    int getAirQuality();

    /** EN: The level of the planter. ZH: 作物架所在的世界。JA: 作物棒のあるレベル。 */
    Level getPlanterWorld();
    /** EN: The position of the planter. ZH: 作物架的位置。JA: 作物棒の位置。 */
    BlockPos getPlanterPos();

    /** EN: Light level at the planter (cached for one tick). ZH: 作物架处的光照等级（缓存一个 tick）。JA: 作物棒の位置の明るさ（1 tick キャッシュ）。 */
    int getLightLevel();

    // ==================== Operations / 操作 / 操作 ====================
    /**
     * <p><b>EN:</b> Uproots the plant. Seeds can drop (chance from the plant and the resistance). The planter becomes empty.
     * <p><b>ZH:</b> 拔除植物。可能掉落种子（概率取决于植物与抗性）。作物架变为空。
     * <p><b>JA:</b> 植物を引き抜く。種子が落ちることがある（確率は植物と耐性による）。作物棒は空になる。
     */
    boolean pick();
    /**
     * <p><b>EN:</b> Harvests by hand and spawns the drops at the planter. Returns false when there was nothing to harvest.
     * <p><b>ZH:</b> 手动收获，并在作物架处生成掉落物。没有可收获的东西时返回 false。
     * <p><b>JA:</b> 手で収穫し、ドロップを作物棒の位置に出す。収穫物がなければ false を返す。
     */
    boolean doManualHarvest();
    /**
     * <p><b>EN:</b> Harvests by hand for {@code player} (since 0.1.8.2). The drops go into the player's
     * inventory; what does not fit drops at the player's feet. A harvest that would give no item
     * does nothing (the plant keeps its stage). The default calls {@link #doManualHarvest()}.
     * <p><b>ZH:</b> 为 {@code player} 手动收获（0.1.8.2 起）。掉落物放入玩家物品栏，放不下的掉在玩家脚下。不会产出任何物品的收获不执行（植物保持阶段）。
     * 默认实现调用 {@link #doManualHarvest()}。
     * <p><b>JA:</b> {@code player} のために手で収穫する（0.1.8.2 以降）。ドロップはプレイヤーのインベントリに入り、入らない分は足元に落ちる。
     * アイテムが出ない収穫は何もしない（植物は段階を保つ）。既定の実装は {@link #doManualHarvest()} を呼ぶ。
     */
    default boolean doManualHarvest(net.minecraft.world.entity.player.Player player) {
        return doManualHarvest();
    }
    /**
     * <p><b>EN:</b> Marks the planter as tended by a Cropmatron until {@code untilGameTime} (since 0.1.8.2).
     * A tended crop gets at least the required environment quality plus a margin, so high-trait seeds keep growing.
     * <p><b>ZH:</b> 标记作物架由作物监护机照料，直到 {@code untilGameTime}（0.1.8.2 起）。被照料的作物至少获得所需环境品质加上余量，高属性种子因此持续生长。
     * <p><b>JA:</b> 作物棒を {@code untilGameTime} まで作物管理機の管理下とする（0.1.8.2 以降）。管理下の作物は少なくとも要求環境品質＋余裕を得るので、
     * 高い特性値の種子も成長し続ける。
     */
    default void markTended(long untilGameTime) {}
    /** EN: True while a Cropmatron tends the planter. ZH: 作物监护机照料中时为真。JA: 作物管理機の管理下なら真。 */
    default boolean isTended() { return false; }
    /**
     * <p><b>EN:</b> Harvests without drops (the plant goes back to its after-harvest stage).
     * <p><b>ZH:</b> 收获但不产生掉落物（植物回到收获后阶段）。
     * <p><b>JA:</b> ドロップなしで収穫する（植物は収穫後の段階に戻る）。
     */
    boolean doHarvestWithoutSeeds();
    /**
     * <p><b>EN:</b> Harvests and returns the drops to the caller (machines use this). The plant
     * goes back to its after-harvest stage, also when the list is empty.
     * <p><b>ZH:</b> 收获并把掉落物返回给调用者（机器使用）。即使列表为空，植物也会回到收获后阶段。
     * <p><b>JA:</b> 収穫し、ドロップを呼び出し元に返す（機械が使う）。リストが空でも植物は収穫後の段階に戻る。
     */
    List<ItemStack> doHarvest();
    /**
     * <p><b>EN:</b> Removes the plant, the traits, the growth points, the scan level and the crossing flag.
     * Water, nutrients and Weed-EX stay.
     * <p><b>ZH:</b> 移除植物、属性、生长点、扫描等级与杂交标志。水、营养与除草剂保留。
     * <p><b>JA:</b> 植物、特性値、成長ポイント、スキャン段階、交配フラグを消す。水・栄養・除草剤は残る。
     */
    void reset();
    /**
     * <p><b>EN:</b> Writes the visible state to the block (age, crossing, redstone, light) and sends it to clients.
     * Call it after a change from outside the crop cycle.
     * <p><b>ZH:</b> 把可见状态写入方块（年龄、杂交、红石、光照）并发送给客户端。在作物周期之外修改后调用。
     * <p><b>JA:</b> 見える状態（年齢、交配、レッドストーン、光）をブロックに書き、クライアントへ送る。作物サイクル外で変更したら呼ぶ。
     */
    void updateState();

    /**
     * <p><b>EN:</b> True when the block is in the root zone under the planter (up to the root depth
     * of the plant; an air block stops the search).
     * <p><b>ZH:</b> 该方块位于作物架下方的根系范围内（深度为植物的根深；遇到空气停止搜索）时为真。
     * <p><b>JA:</b> そのブロックが作物棒の下の根の範囲（植物の根の深さまで。空気で探索を止める）にあれば真。
     */
    boolean isBlockBelow(Block block);
    /**
     * <p><b>EN:</b> Same as {@link #isBlockBelow(Block)} for a tag or id. "oreX" maps to
     * {@code c:ores/x}, "blockX" to {@code c:storage_blocks/x}.
     * <p><b>ZH:</b> 与 {@link #isBlockBelow(Block)} 相同，但参数为标签或 ID。“oreX” 对应 {@code c:ores/x}，“blockX” 对应 {@code c:storage_blocks/x}。
     * <p><b>JA:</b> {@link #isBlockBelow(Block)} と同じだが、タグか ID で指定する。「oreX」は {@code c:ores/x}、「blockX」は {@code c:storage_blocks/x} になる。
     */
    boolean isBlockBelow(String oredictName);

    // ==================== Seed creation / 种子创建 / 種子の作成 ====================
    /**
     * <p><b>EN:</b> Creates seed bags with the scan level of this planter. The second argument is the stack count.
     * <p><b>ZH:</b> 以本作物架的扫描等级创建种子袋。第二个参数为物品数量。
     * <p><b>JA:</b> この作物棒のスキャン段階で種子袋を作る。第 2 引数はスタック数。
     */
    ItemStack makeSeeds(PlantType plantType, int count, int growthSpeed, int yield, int resilience);
}
