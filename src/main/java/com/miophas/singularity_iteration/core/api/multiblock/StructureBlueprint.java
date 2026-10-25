// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.api.multiblock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

/**
 * <p><b>EN:</b> The build plan of a multiblock structure or of a placement rule (public API since 0.1.10.19).
 * Singularity Iteration shows each registered blueprint in the JEI category "Multiblock Structures": a 3D view
 * that turns with the mouse, layer by layer, with the material list. The guide shows it through
 * {@code <GameScene><SiStructure id="..."/></GameScene>}.
 * The plan is a stack of layers from the bottom up. A layer is a list of rows from north to south; a row is a
 * string with one symbol per block from west to east. A symbol has a {@link Key}: the blocks that are
 * permitted at the position (the first block is the one in the view; the view changes between the blocks every
 * second). A space or {@code '.'} is a position that the structure does not use. All layers must have the
 * same rows, and all rows the same length. The blueprint only describes the structure: the controller of the
 * structure still checks it in the world ({@code IMultiblockValidator}).
 * <p><b>ZH:</b> 多方块结构或摆放规则的建造图（0.1.10.19 起的公共 API）。奇点迭代在 JEI 分类“多方块结构”中显示每个已注册的建造图
 * （可用鼠标转动、可逐层查看并带材料清单的 3D 视图），并通过 {@code <GameScene><SiStructure id="..."/></GameScene>} 在手册中显示。
 * 建造图是自下而上的一叠层。每层是由北向南的行列表；每行是一个字符串，每个方块一个符号，由西向东。每个符号有一个 {@link Key}：
 * 该位置允许的方块（视图显示第一个方块，并每秒在这些方块之间切换）。空格或 {@code '.'} 表示结构不使用的位置。所有层的行数必须相同，
 * 所有行的长度也必须相同。建造图只描述结构：结构的控制器仍会在世界中检查它（{@code IMultiblockValidator}）。
 * <p><b>JA:</b> マルチブロック構造または設置ルールの建造図（0.1.10.19 以降の公開 API）。奇点迭代は登録された建造図を JEI カテゴリ
 * 「マルチブロック構造」（マウスで回せて層ごとに見られる 3D ビューと材料リスト）と、ガイドの
 * {@code <GameScene><SiStructure id="..."/></GameScene>} で表示する。建造図は下から上への層の積み重ね。層は北から南への行のリストで、
 * 行は西から東へ 1 ブロック 1 記号の文字列。記号には {@link Key} がある：その位置に置けるブロック（ビューは最初のブロックを表示し、
 * 1 秒ごとに候補を切り替える）。空白または {@code '.'} は構造が使わない位置。すべての層は同じ行数、すべての行は同じ長さでなければならない。
 * 建造図は構造を説明するだけで、構造のコントローラーは引き続きワールドで構造を検査する（{@code IMultiblockValidator}）。
 *
 * <pre>{@code
 * StructureBlueprint plan = StructureBlueprint.builder(ResourceLocation.fromNamespaceAndPath("mymod", "boiler"))
 *     .layer("CCC", "CCC", "CCC")
 *     .layer("CCC", "CMC", "CCC")
 *     .key('C', "structure.mymod.boiler.casing", BlockRef.of("mymod:casing"), BlockRef.of("mymod:port"))
 *     .key('M', BlockRef.of("mymod:boiler_controller").with("facing", "north"))
 *     .controller('M')
 *     .note("structure.mymod.boiler.note")
 *     .build();
 * StructureBlueprintRegistry.register(plan);
 * }</pre>
 */
public final class StructureBlueprint {
    /** EN: Largest size on each axis. ZH: 每个轴的最大尺寸。JA: 各軸の最大サイズ。 */
    public static final int MAX_SIZE = 32;

    /**
     * <p><b>EN:</b> A block by registry id, with block state properties by name ({@code "facing" -> "east"}).
     * An unknown block (its mod is not installed) is left out of the view; an unknown property or value is ignored.
     * <p><b>ZH:</b> 用注册 ID 表示的方块，并可按名称指定方块状态属性（{@code "facing" -> "east"}）。未知方块（其模组未安装）不会显示；
     * 未知的属性或值会被忽略。
     * <p><b>JA:</b> レジストリ ID で指定するブロックと、名前で指定するブロックステートのプロパティ（{@code "facing" -> "east"}）。
     * 不明なブロック（その MOD が未導入）はビューから外し、不明なプロパティや値は無視する。
     */
    public record BlockRef(ResourceLocation block, Map<String, String> properties) {
        /** EN: Copies the properties. ZH: 复制属性。JA: プロパティをコピーする。 */
        public BlockRef {
            Objects.requireNonNull(block, "block");
            properties = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(properties, "properties")));
        }

        /** EN: A block by id ({@code "mymod:casing"}). ZH: 按 ID 指定方块（{@code "mymod:casing"}）。JA: ID でブロックを指定する（{@code "mymod:casing"}）。 */
        public static BlockRef of(String id) {
            return new BlockRef(ResourceLocation.parse(id), Map.of());
        }

        /** EN: A block by id. ZH: 按 ID 指定方块。JA: ID でブロックを指定する。 */
        public static BlockRef of(ResourceLocation id) {
            return new BlockRef(id, Map.of());
        }

        /** EN: A registered block. ZH: 已注册的方块。JA: 登録済みのブロック。 */
        public static BlockRef of(Block block) {
            return new BlockRef(BuiltInRegistries.BLOCK.getKey(block), Map.of());
        }

        /** EN: The same block with one more property. ZH: 增加一个属性的同一方块。JA: プロパティを 1 つ足した同じブロック。 */
        public BlockRef with(String property, String value) {
            Map<String, String> more = new LinkedHashMap<>(properties);
            more.put(property, value);
            return new BlockRef(block, more);
        }

        /**
         * <p><b>EN:</b> The block state, or null when the block is not registered. Call after the registries are complete.
         * <p><b>ZH:</b> 方块状态；方块未注册时为 null。请在注册表完成后调用。
         * <p><b>JA:</b> ブロックステート。ブロックが未登録なら null。レジストリの確定後に呼ぶ。
         */
        @Nullable
        public BlockState resolve() {
            Block found = BuiltInRegistries.BLOCK.getOptional(block).orElse(null);
            if (found == null) return null;
            BlockState state = found.defaultBlockState();
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                Property<?> property = found.getStateDefinition().getProperty(entry.getKey());
                if (property != null) state = with(state, property, entry.getValue());
            }
            return state;
        }

        private static <T extends Comparable<T>> BlockState with(BlockState state, Property<T> property, String value) {
            return property.getValue(value).map(v -> state.setValue(property, v)).orElse(state);
        }
    }

    /**
     * <p><b>EN:</b> A symbol of the plan: the permitted blocks (at least one) and an optional translation key that
     * tells the rule of the position ("at least 1 input module").
     * <p><b>ZH:</b> 建造图中的符号：允许的方块（至少一个），以及可选的翻译键，用于说明该位置的规则（“至少 1 个输入模块”）。
     * <p><b>JA:</b> 建造図の記号：置けるブロック（1 つ以上）と、その位置のルールを示す任意の翻訳キー（「入力モジュールが 1 つ以上」）。
     */
    public record Key(char symbol, List<BlockRef> blocks, @Nullable String noteKey) {
        /** EN: Checks that there is at least one block. ZH: 检查至少有一个方块。JA: ブロックが 1 つ以上あることを検査する。 */
        public Key {
            blocks = List.copyOf(blocks);
            if (blocks.isEmpty()) throw new IllegalArgumentException("a blueprint key needs at least one block: " + symbol);
        }
    }

    /**
     * <p><b>EN:</b> A used position of the plan: {@code pos} counts from the west, bottom, north corner (0, 0, 0).
     * <p><b>ZH:</b> 建造图中被使用的位置：{@code pos} 从西、下、北角 (0, 0, 0) 开始计算。
     * <p><b>JA:</b> 建造図で使う位置：{@code pos} は西・下・北の角 (0, 0, 0) から数える。
     */
    public record Placement(BlockPos pos, Key key) {}

    private final ResourceLocation id;
    @Nullable private final String titleKey;
    private final List<List<String>> layers;
    private final Map<Character, Key> keys;
    @Nullable private final Character controller;
    private final List<String> noteKeys;
    private final List<Placement> placements;
    private final int sizeX, sizeY, sizeZ;

    private StructureBlueprint(Builder builder) {
        this.id = builder.id;
        this.titleKey = builder.titleKey;
        this.layers = builder.layers.stream().map(List::copyOf).toList();
        this.keys = Collections.unmodifiableMap(new LinkedHashMap<>(builder.keys));
        this.controller = builder.controller;
        this.noteKeys = List.copyOf(builder.notes);
        this.sizeY = layers.size();
        this.sizeZ = layers.get(0).size();
        this.sizeX = layers.get(0).get(0).length();
        List<Placement> out = new ArrayList<>();
        for (int y = 0; y < sizeY; y++)
            for (int z = 0; z < sizeZ; z++)
                for (int x = 0; x < sizeX; x++) {
                    Key key = keys.get(layers.get(y).get(z).charAt(x));
                    if (key != null) out.add(new Placement(new BlockPos(x, y, z), key));
                }
        this.placements = List.copyOf(out);
    }

    /** EN: Starts a blueprint. ZH: 开始一个建造图。JA: 建造図を作り始める。 */
    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    /** EN: The id. ZH: 建造图的 ID。JA: 建造図の ID。 */
    public ResourceLocation id() { return id; }

    /** EN: The title key: the one given, or {@code structure.<namespace>.<path>}. ZH: 标题键：指定的键，或 {@code structure.<命名空间>.<路径>}。JA: タイトルキー：指定したキー、なければ {@code structure.<名前空間>.<パス>}。 */
    public String titleKey() {
        return titleKey != null ? titleKey : "structure." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    /** EN: The layers from the bottom up (rows from north to south). ZH: 自下而上的层（行由北向南）。JA: 下から上への層（行は北から南）。 */
    public List<List<String>> layers() { return layers; }

    /** EN: The keys in the order of definition. ZH: 按定义顺序的符号。JA: 定義順の記号。 */
    public Map<Character, Key> keys() { return keys; }

    /** EN: The controller key, or null. ZH: 控制器的符号，没有则为 null。JA: コントローラーの記号。なければ null。 */
    @Nullable
    public Key controller() { return controller == null ? null : keys.get(controller); }

    /** EN: The position of the controller, or null. ZH: 控制器的位置，没有则为 null。JA: コントローラーの位置。なければ null。 */
    @Nullable
    public BlockPos controllerPos() {
        if (controller == null) return null;
        for (Placement p : placements) if (p.key().symbol() == controller) return p.pos();
        return null;
    }

    /** EN: Translation keys of the notes under the view. ZH: 视图下方说明的翻译键。JA: ビューの下に出す説明の翻訳キー。 */
    public List<String> noteKeys() { return noteKeys; }

    /** EN: The used positions, bottom layer first. ZH: 被使用的位置，从最底层开始。JA: 使う位置。最下層から。 */
    public List<Placement> placements() { return placements; }

    /** EN: Size from west to east. ZH: 由西向东的尺寸。JA: 西から東へのサイズ。 */
    public int sizeX() { return sizeX; }

    /** EN: Height. ZH: 高度。JA: 高さ。 */
    public int sizeY() { return sizeY; }

    /** EN: Size from north to south. ZH: 由北向南的尺寸。JA: 北から南へのサイズ。 */
    public int sizeZ() { return sizeZ; }

    /** EN: The number of positions of each key, in key order. ZH: 每个符号的位置数，按符号顺序。JA: 各記号の位置の数（記号の順）。 */
    public Map<Key, Integer> counts() {
        Map<Key, Integer> out = new LinkedHashMap<>();
        for (Key key : keys.values()) out.put(key, 0);
        for (Placement p : placements) out.merge(p.key(), 1, Integer::sum);
        out.values().removeIf(n -> n == 0);
        return Collections.unmodifiableMap(out);
    }

    /**
     * <p><b>EN:</b> Builds a {@link StructureBlueprint}. {@link #build()} checks the plan. A wrong plan throws
     * {@link IllegalArgumentException}: no layer; layers or rows of different sizes; a size above
     * {@link StructureBlueprint#MAX_SIZE}; a symbol without a key; a controller symbol that is not in the plan exactly once.
     * <p><b>ZH:</b> 构建 {@link StructureBlueprint}。{@link #build()} 会检查建造图，错误时抛出 {@link IllegalArgumentException}：
     * 没有层、层或行的大小不同、尺寸超过 {@link StructureBlueprint#MAX_SIZE}、符号没有对应的键，或控制器符号在图中不是恰好出现一次。
     * <p><b>JA:</b> {@link StructureBlueprint} を組み立てる。{@link #build()} は建造図を検査し、誤りがあれば
     * {@link IllegalArgumentException} を投げる：層がない、層や行の大きさが違う、サイズが {@link StructureBlueprint#MAX_SIZE} を超える、
     * キーのない記号がある、コントローラーの記号が図にちょうど 1 回ない。
     */
    public static final class Builder {
        private final ResourceLocation id;
        @Nullable private String titleKey;
        private final List<List<String>> layers = new ArrayList<>();
        private final Map<Character, Key> keys = new LinkedHashMap<>();
        @Nullable private Character controller;
        private final List<String> notes = new ArrayList<>();

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        /** EN: The title translation key. ZH: 标题的翻译键。JA: タイトルの翻訳キー。 */
        public Builder title(String translationKey) {
            this.titleKey = translationKey;
            return this;
        }

        /** EN: Adds the next layer up (rows from north to south). ZH: 在上方添加下一层（行由北向南）。JA: 1 つ上の層を足す（行は北から南）。 */
        public Builder layer(String... rows) {
            layers.add(Arrays.asList(rows));
            return this;
        }

        /** EN: Adds the same layer {@code times} times. ZH: 添加 {@code times} 次相同的层。JA: 同じ層を {@code times} 回足す。 */
        public Builder layers(int times, String... rows) {
            for (int i = 0; i < times; i++) layer(rows);
            return this;
        }

        /** EN: Defines a symbol without a note. ZH: 定义一个没有说明的符号。JA: 説明なしの記号を定義する。 */
        public Builder key(char symbol, BlockRef... blocks) {
            return key(symbol, null, blocks);
        }

        /** EN: Defines a symbol with a note key (may be null). ZH: 定义一个带说明键（可为 null）的符号。JA: 説明キー付き（null 可）の記号を定義する。 */
        public Builder key(char symbol, @Nullable String noteKey, BlockRef... blocks) {
            if (symbol == ' ' || symbol == '.') throw new IllegalArgumentException("' ' and '.' are unused positions");
            keys.put(symbol, new Key(symbol, Arrays.asList(blocks), noteKey));
            return this;
        }

        /** EN: The symbol of the controller block. ZH: 控制器方块的符号。JA: コントローラーブロックの記号。 */
        public Builder controller(char symbol) {
            this.controller = symbol;
            return this;
        }

        /** EN: Adds a note under the view (translation key). ZH: 在视图下方添加一条说明（翻译键）。JA: ビューの下に説明を足す（翻訳キー）。 */
        public Builder note(String translationKey) {
            notes.add(Objects.requireNonNull(translationKey, "translationKey"));
            return this;
        }

        /** EN: Checks and builds the blueprint. ZH: 检查并构建建造图。JA: 建造図を検査して組み立てる。 */
        public StructureBlueprint build() {
            if (layers.isEmpty()) throw new IllegalArgumentException("a blueprint needs at least one layer: " + id);
            int rows = layers.get(0).size();
            if (rows == 0) throw new IllegalArgumentException("a blueprint layer needs at least one row: " + id);
            int width = layers.get(0).get(0).length();
            if (width == 0 || layers.size() > MAX_SIZE || rows > MAX_SIZE || width > MAX_SIZE)
                throw new IllegalArgumentException("a blueprint is 1 to " + MAX_SIZE + " blocks on each axis: " + id);
            int controllers = 0;
            for (List<String> layer : layers) {
                if (layer.size() != rows) throw new IllegalArgumentException("all blueprint layers need the same rows: " + id);
                for (String row : layer) {
                    if (row.length() != width) throw new IllegalArgumentException("all blueprint rows need the same length: " + id);
                    for (char c : row.toCharArray()) {
                        if (c == ' ' || c == '.') continue;
                        if (!keys.containsKey(c)) throw new IllegalArgumentException("blueprint symbol '" + c + "' has no key: " + id);
                        if (controller != null && c == controller) controllers++;
                    }
                }
            }
            if (controller != null && controllers != 1)
                throw new IllegalArgumentException("the controller symbol must be in the plan exactly once: " + id);
            return new StructureBlueprint(this);
        }
    }
}
