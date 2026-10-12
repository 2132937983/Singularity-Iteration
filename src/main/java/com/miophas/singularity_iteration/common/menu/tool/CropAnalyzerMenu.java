package com.miophas.singularity_iteration.common.menu.tool;

import com.miophas.singularity_iteration.common.item.crop.CropSeedItem;
import com.miophas.singularity_iteration.common.item.normal.mio_icif_normal;
import com.miophas.singularity_iteration.common.item.tools.CropAnalyzerItem;
import com.miophas.singularity_iteration.common.item.upgrade.mio_icif_upgrade;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import com.miophas.singularity_iteration.core.api.item.ICropSeedItem;
import com.miophas.singularity_iteration.core.runtime.crop.BreedingResearch;
import com.miophas.singularity_iteration.core.runtime.crop.PlantRegistry;
import com.miophas.singularity_iteration.core.runtime.crop.ScannerMath;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * <p><b>EN:</b> Menu of the Advanced Crop Scanner (0.1.8.2). Three pages:
 * <ul>
 *   <li>Analysis: 27 slots (a chest grid). Each slot with an unscanned seed bag scans on its own;
 *       at the end the stack gets scan level 4 (all traits visible).</li>
 *   <li>Breeding research (locked until 10 Reinforced Iridium Plates and 1 Living Circuit are
 *       installed): two analyzed parents, a selected target species, a certain result.</li>
 *   <li>Gene editing (locked until 1 Quantum Core, 4 Neutron Plates and 2 Super Circuits are
 *       installed): set Growth / Gain / Resistance of one analyzed bag; UU-Matter is the catalyst.</li>
 * </ul>
 * A battery slot and four overclocker slots serve all pages. The jobs run on the server while the
 * menu is open, once per game tick. One energy budget per tick serves the jobs in this order:
 * analysis, breeding, gene editing. The held scanner's inventory slot is locked while the menu is
 * open, so the scanner cannot be moved or dropped (no duplication).
 * <p><b>ZH:</b> 次世代作物扫描仪的菜单（0.1.8.2），共三页：
 * <ul>
 *   <li>分析：27 个槽位（箱子式网格）。每个放有未分析种子袋的槽位独立扫描，完成后整堆获得扫描等级 4（所有属性可见）。</li>
 *   <li>交配研究（安装 10 块强化铱板与 1 个生体电路后解锁）：两个已分析的亲本、选定的目标物种，结果确定。</li>
 *   <li>基因编辑（安装 1 个量子核心、4 块中子板与 2 个超级电路后解锁）：设定一个已分析种子袋的生长／产量／抗性，以 UU 物质为催化剂。</li>
 * </ul>
 * 电池槽与 4 个超频槽服务所有页面。菜单打开期间，任务每个游戏 tick 在服务器上运行一次；每 tick 的能量预算按分析、交配、基因编辑的顺序分配。
 * 菜单打开时，所持扫描仪所在的物品栏槽位被锁定，扫描仪无法移动或丢弃（防止复制）。
 * <p><b>JA:</b> 次世代型作物スキャナーのメニュー（0.1.8.2）。3 ページ：
 * <ul>
 *   <li>分析：27 スロット（チェスト型グリッド）。未分析の種子袋のある各スロットが独立して走査し、終わるとスタック全体がスキャン段階 4（全特性値が見える）になる。</li>
 *   <li>交配研究（強化イリジウム板 10 枚と生体回路 1 個の取り付けでアンロック）：分析済みの親 2 つと選んだ目標種で、結果は確定。</li>
 *   <li>遺伝子改変（量子コア 1 個、中性子板 4 枚、スーパー回路 2 個の取り付けでアンロック）：分析済みの種子袋 1 つの成長・収量・耐性を設定する。触媒は UU 物質。</li>
 * </ul>
 * 電池スロットとオーバークロッカー 4 スロットは全ページ共通。作業はメニューを開いている間、サーバーで 1 ゲーム tick に 1 回動く。
 * 1 tick のエネルギー予算は分析、交配、遺伝子改変の順に配る。メニューを開いている間、手に持ったスキャナーのインベントリスロットは固定され、
 * スキャナーを動かしたり捨てたりできない（複製防止）。
 */
@SuppressWarnings("null")
public class CropAnalyzerMenu extends AbstractContainerMenu {
    // ---- slot layout (see CropScannerState) ----
    public static final int GRID = CropScannerState.GRID, SLOT_BATTERY = CropScannerState.SLOT_BATTERY,
        SLOT_UPGRADE = CropScannerState.SLOT_UPGRADE, UPGRADES = CropScannerState.UPGRADES,
        SLOT_PARENT_A = CropScannerState.SLOT_PARENT_A, SLOT_PARENT_B = CropScannerState.SLOT_PARENT_B,
        SLOT_BREED_OUT = CropScannerState.SLOT_BREED_OUT, SLOT_GENE = CropScannerState.SLOT_GENE,
        SLOT_CATALYST = CropScannerState.SLOT_CATALYST, SIZE = CropScannerState.SIZE;
    /** EN: Kept for 0.1.8.1 callers: first grid slot. ZH: 为 0.1.8.1 调用者保留：第一个网格槽位。JA: 0.1.8.1 の呼び出し側向けに残す：先頭のグリッドスロット。 */
    public static final int SLOT_INPUT = 0;
    public static final int PAGE_ANALYSIS = 0, PAGE_BREED = 1, PAGE_GENE = 2;

    // ---- menu buttons (clickMenuButton ids; the vanilla packet carries one byte, so all are below 128) ----
    public static final int BTN_UNLOCK_BREED = 3, BTN_UNLOCK_GENE = 4, BTN_BREED_TOGGLE = 5, BTN_GENE_TOGGLE = 6,
        BTN_GENE_ADJUST = 8, BTN_SELECT = 32;

    // ---- synced data (ContainerData values are 16-bit on the wire) ----
    public static final int D_PAGE = 0, D_UNLOCK = 1, D_PROGRESS = 2, D_ENERGY = D_PROGRESS + GRID, D_CAPACITY = D_ENERGY + 1,
        D_OC = D_CAPACITY + 1, D_BREED_PROGRESS = D_OC + 1, D_BREED_SELECT = D_BREED_PROGRESS + 1, D_BREED_RUN = D_BREED_SELECT + 1,
        D_GENE_PROGRESS = D_BREED_RUN + 1, D_GENE_TARGET = D_GENE_PROGRESS + 1, D_GENE_RUN = D_GENE_TARGET + 3,
        D_SCAN_STATUS = D_GENE_RUN + 1, D_BREED_STATUS = D_SCAN_STATUS + 1, D_GENE_STATUS = D_BREED_STATUS + 1, D_COUNT = D_GENE_STATUS + 1;

    /** EN: Status codes shown by the screen. ZH: 界面显示的状态码。JA: 画面に出す状態コード。 */
    public static final int ST_IDLE = 0, ST_RUNNING = 1, ST_NO_ENERGY = 2, ST_NEED_CATALYST = 3, ST_OUTPUT_FULL = 4,
        ST_NOT_ANALYZED = 5, ST_LOCKED = 6, ST_NO_CHANGE = 7, ST_SELECT_TARGET = 8, ST_DONE = 9;
    /** EN: Ticks that the "done" status stays visible. ZH: “完成” 状态保持显示的 tick 数。JA: 「完了」状態を表示し続ける tick 数。 */
    private static final int DONE_FLASH = 60;

    /** EN: Install cost of breeding research. ZH: 交配研究的安装成本。JA: 交配研究の取り付けコスト。 */
    public static final Requirement[] BREED_UNLOCK = {
        new Requirement("mio_icif:resource/item_iridium_plate", 10), new Requirement("mio_icif:normal/item_living_circuit", 1)};
    /** EN: Install cost of gene editing. ZH: 基因编辑的安装成本。JA: 遺伝子改変の取り付けコスト。 */
    public static final Requirement[] GENE_UNLOCK = {
        new Requirement("mio_icif:resource/item_quantum_core", 1), new Requirement("mio_icif:resource/item_neutron_plate", 4),
        new Requirement("mio_icif:normal/item_super_circuit", 2)};
    /** EN: Gene-editing catalyst (UU-Matter). ZH: 基因编辑催化剂（UU 物质）。JA: 遺伝子改変の触媒（UU 物質）。 */
    public static final ResourceLocation CATALYST = ResourceLocation.fromNamespaceAndPath("mio_icif", "normal/item_matter");

    /** EN: One unlock ingredient. ZH: 一种解锁材料。JA: アンロック素材 1 種。 */
    public record Requirement(String id, int count) {
        public Item item() { return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)); }
    }

    private final ItemStack containerStack;
    private final InteractionHand hand;
    @Nullable private final Player player;
    private final CropScannerState state;
    private final ContainerData data;
    private final Container container;
    private final int lockedSlot;
    private int page;
    private long lastTick = Long.MIN_VALUE;
    private boolean dirty;
    private int saveTimer;
    private final ItemStack[] lastSeen = new ItemStack[GRID];
    private int scanStatus, breedStatus, geneStatus, breedDone, geneDone;
    private String breedSignature = "", geneSignature = "";
    private String candidateKey;
    private List<BreedingResearch.Candidate> candidateCache = List.of();

    /** EN: Client constructor (the buffer carries the hand). ZH: 客户端构造函数（缓冲区携带手）。JA: クライアント用コンストラクタ（バッファーに手が入る）。 */
    public CropAnalyzerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, readHand(extraData), null, true);
    }

    /** EN: Server constructor; the hand is found from the stack (kept for 0.1.8.1 callers). ZH: 服务器构造函数；由物品堆判断手（为 0.1.8.1 调用者保留）。JA: サーバー用コンストラクタ。手はスタックから判断する（0.1.8.1 の呼び出し側向け）。 */
    public CropAnalyzerMenu(int containerId, Inventory playerInventory, ItemStack containerStack) {
        this(containerId, playerInventory, containerStack, playerInventory.player.getItemInHand(InteractionHand.OFF_HAND) == containerStack
            ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
    }

    /** EN: Server constructor. ZH: 服务器构造函数。JA: サーバー用コンストラクタ。 */
    public CropAnalyzerMenu(int containerId, Inventory playerInventory, ItemStack containerStack, InteractionHand hand) {
        this(containerId, playerInventory, hand, containerStack, false);
    }

    private CropAnalyzerMenu(int containerId, Inventory playerInventory, InteractionHand hand, @Nullable ItemStack stack, boolean client) {
        super(mio_icif_tool_menus.CROP_ANALYZER_MENU.get(), containerId);
        this.hand = hand;
        this.containerStack = stack != null ? stack : playerInventory.player.getItemInHand(hand);
        stack = this.containerStack;
        this.player = playerInventory.player;
        this.state = client ? new CropScannerState() : CropScannerState.load(stack, playerInventory.player.level().registryAccess());
        this.lockedSlot = hand == InteractionHand.MAIN_HAND ? playerInventory.selected : -1;
        this.container = new StateContainer();
        this.data = client ? new SimpleContainerData(D_COUNT) : new ServerData();
        for (int i = 0; i < GRID; i++) lastSeen[i] = state.items[i];
        addScannerSlots();
        addPlayerInventorySlots(playerInventory);
        addDataSlots(data);
    }

    private static InteractionHand readHand(FriendlyByteBuf buf) {
        return buf != null && buf.readableBytes() > 0 ? buf.readEnum(InteractionHand.class) : InteractionHand.MAIN_HAND;
    }

    // ------------------------------------------------------------------ slots
    private void addScannerSlots() {
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++)
            addSlot(new PageSlot(container, r * 9 + c, 8 + c * 18, 31 + r * 18, PAGE_ANALYSIS, 64, s -> s.getItem() instanceof ICropSeedItem));
        addSlot(new PageSlot(container, SLOT_BATTERY, 8, 89, -1, 1, s -> MioIcifAPI.instance().getItemAPI().isBattery(s)));
        for (int i = 0; i < UPGRADES; i++) addSlot(new PageSlot(container, SLOT_UPGRADE + i, 44 + i * 18, 89, -1, 64, CropAnalyzerMenu::isOverclocker));
        addSlot(new PageSlot(container, SLOT_PARENT_A, 8, 33, PAGE_BREED, 64, s -> s.getItem() instanceof ICropSeedItem));
        addSlot(new PageSlot(container, SLOT_PARENT_B, 8, 63, PAGE_BREED, 64, s -> s.getItem() instanceof ICropSeedItem));
        addSlot(new PageSlot(container, SLOT_BREED_OUT, 152, 33, PAGE_BREED, 64, s -> false));
        addSlot(new PageSlot(container, SLOT_GENE, 8, 33, PAGE_GENE, 1, s -> s.getItem() instanceof ICropSeedItem));
        addSlot(new PageSlot(container, SLOT_CATALYST, 8, 63, PAGE_GENE, 64, CropAnalyzerMenu::isCatalyst));
    }

    private void addPlayerInventorySlots(Inventory inventory) {
        for (int row = 0; row < 3; ++row) for (int col = 0; col < 9; ++col)
            addSlot(new LockedSlot(inventory, col + row * 9 + 9, 8 + col * 18, 134 + row * 18));
        for (int col = 0; col < 9; ++col) addSlot(new LockedSlot(inventory, col, 8 + col * 18, 192));
    }

    public static boolean isOverclocker(ItemStack stack) {
        return stack.getItem() instanceof mio_icif_upgrade upgrade && upgrade.getLocalUpgradeType() == mio_icif_upgrade.UpgradeType.OVERCLOCKER;
    }

    public static boolean isCatalyst(ItemStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(CATALYST);
    }

    private boolean client() { return player != null && player.level().isClientSide; }

    /** EN: Page of the menu (client: synced value). ZH: 菜单当前页（客户端为同步值）。JA: メニューの現在ページ（クライアントは同期値）。 */
    public int page() { return client() ? data.get(D_PAGE) : page; }
    public int data(int index) { return data.get(index); }
    public ItemStack scannerStack() { return containerStack; }

    /** EN: True when the page is usable (analysis always; the others after their unlock). ZH: 该页可用时为真（分析页始终可用，其余解锁后可用）。JA: そのページが使えるなら真（分析は常に、他はアンロック後）。 */
    public boolean pageUnlocked(int p) {
        int flags = client() ? data.get(D_UNLOCK) : state.unlocks;
        if (p == PAGE_BREED) return (flags & CropScannerState.UNLOCK_BREED) != 0;
        if (p == PAGE_GENE) return (flags & CropScannerState.UNLOCK_GENE) != 0;
        return true;
    }

    /** EN: Stored energy (client: from the synced fraction). ZH: 储存能量（客户端由同步的比例计算）。JA: 蓄電量（クライアントは同期した割合から計算）。 */
    public long energy() {
        if (client()) return (long) data.get(D_ENERGY) * capacity() / 10_000L;
        return containerStack.getItem() instanceof CropAnalyzerItem s ? s.getEnergy(containerStack) : 0;
    }

    /** EN: Energy capacity (client: synced in kEU). ZH: 能量容量（客户端以 kEU 同步）。JA: 蓄電容量（クライアントは kEU で同期）。 */
    public long capacity() {
        if (client()) return data.get(D_CAPACITY) * 1000L;
        return containerStack.getItem() instanceof CropAnalyzerItem s ? s.getMaxEnergy() : 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand) == containerStack && containerStack.getItem() instanceof CropAnalyzerItem;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // EN: Block the swap keys that would move the held scanner.
        // ZH: 阻止会移动所持扫描仪的交换键。
        // JA: 手持ちスキャナーを動かす入れ替えキーを止める。
        if (clickType == ClickType.SWAP && ((hand == InteractionHand.OFF_HAND && button == 40) || button == lockedSlot)) return;
        super.clicked(slotId, button, clickType, player);
    }

    // ------------------------------------------------------------------ buttons
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id <= 2) { page = id; return true; }
        if (id == BTN_UNLOCK_BREED) return unlock(player, CropScannerState.UNLOCK_BREED, BREED_UNLOCK);
        if (id == BTN_UNLOCK_GENE) return unlock(player, CropScannerState.UNLOCK_GENE, GENE_UNLOCK);
        if (id == BTN_BREED_TOGGLE && state.unlocked(CropScannerState.UNLOCK_BREED)) { state.breedRun = !state.breedRun; dirty = true; return true; }
        if (id == BTN_GENE_TOGGLE && state.unlocked(CropScannerState.UNLOCK_GENE)) {
            if (!state.geneRun) {
                int[] cur = geneCurrent();
                if (cur == null || ScannerMath.geneCost(cur, state.geneTarget) == 0) return false;
                if (catalystCount() < ScannerMath.geneCatalyst(cur, state.geneTarget)) { geneStatus = ST_NEED_CATALYST; return false; }
            }
            state.geneRun = !state.geneRun;
            dirty = true;
            return true;
        }
        if (id >= BTN_GENE_ADJUST && id < BTN_GENE_ADJUST + 12) {
            int stat = (id - BTN_GENE_ADJUST) / 4, op = (id - BTN_GENE_ADJUST) % 4;
            int delta = switch (op) { case 0 -> -1; case 1 -> 1; case 2 -> -5; default -> 5; };
            setGeneTarget(stat, state.geneTarget[stat] + delta);
            return true;
        }
        if (id >= BTN_SELECT && id < BTN_SELECT + BreedingResearch.MAX_CANDIDATES && state.unlocked(CropScannerState.UNLOCK_BREED)) {
            List<BreedingResearch.Candidate> list = candidates();
            int index = id - BTN_SELECT;
            if (index >= list.size()) return false;
            if (!list.get(index).key().equals(state.breedTarget)) {
                state.breedTarget = list.get(index).key();
                state.breedProgress = 0;
                dirty = true;
            }
            return true;
        }
        return false;
    }

    /**
     * <p><b>EN:</b> Sets one gene target (slider, from {@code CropScannerPacket}); ignored while the job runs.
     * <p><b>ZH:</b> 设置一个基因目标（滑块，来自 {@code CropScannerPacket}）；任务运行中忽略。
     * <p><b>JA:</b> 遺伝子の目標値を 1 つ設定する（スライダー、{@code CropScannerPacket} から）。作業中は無視する。
     */
    public void setGeneTarget(int stat, int value) {
        if (state.geneRun || stat < 0 || stat > 2 || !state.unlocked(CropScannerState.UNLOCK_GENE)) return;
        int next = Math.clamp(value, 0, 31);
        if (state.geneTarget[stat] == next) return;
        state.geneTarget[stat] = next;
        state.geneProgress = 0;
        dirty = true;
    }

    private boolean unlock(Player player, int flag, Requirement[] requirements) {
        if (state.unlocked(flag)) return false;
        if (!player.getAbilities().instabuild) {
            for (Requirement r : requirements) if (player.getInventory().countItem(r.item()) < r.count()) return false;
            for (Requirement r : requirements) {
                int left = r.count();
                var inv = player.getInventory();
                for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
                    ItemStack s = inv.getItem(i);
                    if (s.is(r.item())) { int take = Math.min(left, s.getCount()); s.shrink(take); left -= take; }
                }
            }
        }
        state.unlocks |= flag;
        save();
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6F, 1.6F);
        return true;
    }

    // ------------------------------------------------------------------ processing (server)
    @Override
    public void broadcastChanges() {
        if (player != null && !player.level().isClientSide) {
            long now = player.level().getGameTime();
            if (now != lastTick) {
                lastTick = now;
                processTick();
            }
        }
        super.broadcastChanges();
    }

    /**
     * <p><b>EN:</b> One processing step (charge, analysis, breeding, gene editing). {@link #broadcastChanges()}
     * calls it once per game tick; tests call it directly. It does nothing on the client.
     * <p><b>ZH:</b> 一个处理步骤（充电、分析、交配、基因编辑）。{@link #broadcastChanges()} 每个游戏 tick 调用一次；测试直接调用。客户端上不做任何事。
     * <p><b>JA:</b> 処理 1 ステップ（充電、分析、交配、遺伝子改変）。{@link #broadcastChanges()} が 1 ゲーム tick に 1 回呼ぶ。テストは直接呼ぶ。クライアントでは何もしない。
     */
    public void processTick() {
        if (client() || !(containerStack.getItem() instanceof CropAnalyzerItem scanner)) return;
        chargeFromBattery(scanner);
        int cards = ScannerMath.overclockers(overclockerCount());
        long budget = scanner.getEnergy(containerStack), spent = 0;

        // EN: 1. Analysis: each unscanned stack scans on its own.
        // ZH: 1. 分析：每个未分析的堆独立扫描。
        // JA: 1. 分析：未分析のスタックごとに独立して走査する。
        int ticksNeeded = ScannerMath.analysisTicks(cards);
        boolean anyScanning = false, starved = false;
        for (int i = 0; i < GRID; i++) {
            ItemStack stack = state.items[i];
            if (stack != lastSeen[i]) { lastSeen[i] = stack; state.progress[i] = 0; }
            if (stack.isEmpty() || !(stack.getItem() instanceof ICropSeedItem seed) || seed.cropScanLevel(stack) >= 4) {
                state.progress[i] = 0;
                continue;
            }
            // EN: Full power each tick; the last tick takes only the rest of the job energy.
            // ZH: 每 tick 满功率；最后一个 tick 只扣任务能量的剩余部分。
            // JA: 毎 tick 満額。最後の tick は作業エネルギーの残りだけを取る。
            long cost = ScannerMath.tickCharge(ScannerMath.analysisTotalEu(stack.getCount(), cards),
                ScannerMath.analysisEuPerTick(stack.getCount(), cards), state.progress[i] + 1, ticksNeeded);
            if (budget - spent < cost) { starved = true; continue; }
            spent += cost;
            anyScanning = true;
            if (++state.progress[i] >= ticksNeeded) {
                seed.setCropScanLevel(stack, 4);
                state.progress[i] = 0;
                dirty = true;
            }
        }
        scanStatus = starved ? ST_NO_ENERGY : anyScanning ? ST_RUNNING : ST_IDLE;

        // EN: 2. Breeding research, 3. gene editing.
        // ZH: 2. 交配研究，3. 基因编辑。
        // JA: 2. 交配研究、3. 遺伝子改変。
        spent += tickBreeding(cards, budget - spent);
        spent += tickGene(cards, budget - spent);

        if (spent > 0) scanner.consumeEnergy(containerStack, spent);
        if (breedDone > 0) breedDone--;
        if (geneDone > 0) geneDone--;
        if (dirty && ++saveTimer >= 20) save();
    }

    private long tickBreeding(int cards, long budget) {
        if (!state.unlocked(CropScannerState.UNLOCK_BREED)) { breedStatus = ST_LOCKED; return 0; }
        PlantType a = analyzedPlant(state.items[SLOT_PARENT_A]), b = analyzedPlant(state.items[SLOT_PARENT_B]);
        if (a == null || b == null) { breedStatus = breedDone > 0 ? ST_DONE : ST_NOT_ANALYZED; return 0; }
        BreedingResearch.Candidate target = selectedCandidate();
        if (target == null) { breedStatus = ST_SELECT_TARGET; return 0; }
        String signature = traitKey(state.items[SLOT_PARENT_A]) + "|" + traitKey(state.items[SLOT_PARENT_B]) + "|" + target.key();
        if (!signature.equals(breedSignature)) {
            // EN: The first check after opening keeps the saved progress.
            // ZH: 打开后的首次检查保留已保存的进度。
            // JA: 開いた後の最初の確認では保存済みの進捗を残す。
            if (!breedSignature.isEmpty()) state.breedProgress = 0;
            breedSignature = signature;
        }
        if (!state.breedRun) { breedStatus = breedDone > 0 ? ST_DONE : ST_IDLE; return 0; }
        ItemStack result = breedResult(target.plant());
        if (!fits(state.items[SLOT_BREED_OUT], result)) { breedStatus = ST_OUTPUT_FULL; return 0; }
        int ticks = ScannerMath.researchTicks(target.cost(), ScannerMath.BREED_POWER, cards);
        long cost = ScannerMath.tickCharge(ScannerMath.researchTotalEu(target.cost(), ScannerMath.BREED_POWER, cards),
            ScannerMath.researchEuPerTick(ScannerMath.BREED_POWER, cards), state.breedProgress + 1, ticks);
        if (budget < cost) { breedStatus = ST_NO_ENERGY; return 0; }
        breedStatus = ST_RUNNING;
        if (++state.breedProgress >= ticks) {
            state.breedProgress = 0;
            if (state.items[SLOT_BREED_OUT].isEmpty()) state.items[SLOT_BREED_OUT] = result;
            else state.items[SLOT_BREED_OUT].grow(result.getCount());
            state.items[SLOT_PARENT_A].shrink(1);
            state.items[SLOT_PARENT_B].shrink(1);
            if (state.items[SLOT_PARENT_A].isEmpty()) state.items[SLOT_PARENT_A] = ItemStack.EMPTY;
            if (state.items[SLOT_PARENT_B].isEmpty()) state.items[SLOT_PARENT_B] = ItemStack.EMPTY;
            if (state.items[SLOT_PARENT_A].isEmpty() || state.items[SLOT_PARENT_B].isEmpty()) state.breedRun = false;
            breedSignature = traitKey(state.items[SLOT_PARENT_A]) + "|" + traitKey(state.items[SLOT_PARENT_B]) + "|" + target.key();
            breedStatus = ST_DONE;
            breedDone = DONE_FLASH;
            dirty = true;
            if (player != null) player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.4F);
        }
        return cost;
    }

    private long tickGene(int cards, long budget) {
        if (!state.unlocked(CropScannerState.UNLOCK_GENE)) { geneStatus = ST_LOCKED; return 0; }
        int[] cur = geneCurrent();
        if (cur == null) { geneStatus = geneDone > 0 ? ST_DONE : ST_NOT_ANALYZED; state.geneRun = false; return 0; }
        String signature = traitKey(state.items[SLOT_GENE]);
        if (!signature.equals(geneSignature)) {
            boolean first = geneSignature.isEmpty();
            geneSignature = signature;
            // EN: A new seed starts with its own traits as the target; the first check after opening keeps the saved job.
            // ZH: 新种子以自身属性作为初始目标；打开后的首次检查保留已保存的任务。
            // JA: 新しい種子は自分の特性値を初期目標にする。開いた後の最初の確認では保存済みの作業を残す。
            if (!first || (state.geneTarget[0] | state.geneTarget[1] | state.geneTarget[2]) == 0) {
                System.arraycopy(cur, 0, state.geneTarget, 0, 3);
                state.geneProgress = 0;
                state.geneRun = false;
            }
        }
        long total = ScannerMath.geneCost(cur, state.geneTarget);
        if (total == 0) { geneStatus = geneDone > 0 ? ST_DONE : ST_NO_CHANGE; state.geneRun = false; return 0; }
        if (!state.geneRun) { geneStatus = ST_IDLE; return 0; }
        int need = ScannerMath.geneCatalyst(cur, state.geneTarget);
        if (catalystCount() < need) { geneStatus = ST_NEED_CATALYST; return 0; }
        int ticks = ScannerMath.researchTicks(total, ScannerMath.GENE_POWER, cards);
        long cost = ScannerMath.tickCharge(ScannerMath.researchTotalEu(total, ScannerMath.GENE_POWER, cards),
            ScannerMath.researchEuPerTick(ScannerMath.GENE_POWER, cards), state.geneProgress + 1, ticks);
        if (budget < cost) { geneStatus = ST_NO_ENERGY; return 0; }
        geneStatus = ST_RUNNING;
        if (++state.geneProgress >= ticks) {
            ItemStack seed = state.items[SLOT_GENE];
            ((ICropSeedItem) seed.getItem()).setCropTraits(seed, state.geneTarget[0], state.geneTarget[1], state.geneTarget[2]);
            state.items[SLOT_CATALYST].shrink(need);
            if (state.items[SLOT_CATALYST].isEmpty()) state.items[SLOT_CATALYST] = ItemStack.EMPTY;
            state.geneProgress = 0;
            state.geneRun = false;
            geneSignature = traitKey(seed);
            geneStatus = ST_DONE;
            geneDone = DONE_FLASH;
            dirty = true;
            if (player != null) player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.7F, 1.5F);
        }
        return cost;
    }

    private void chargeFromBattery(CropAnalyzerItem scanner) {
        ItemStack battery = state.items[SLOT_BATTERY];
        if (battery.isEmpty()) return;
        var api = MioIcifAPI.instance().getItemAPI();
        if (!api.isBattery(battery)) return;
        long room = scanner.getMaxEnergy() - scanner.getEnergy(containerStack);
        if (room <= 0) return;
        long take = Math.min(Math.min(scanner.getChargeRate(), room), api.getBatteryStored(battery));
        if (take > 0) scanner.addEnergy(containerStack, api.dischargeBattery(battery, take, false));
    }

    // ------------------------------------------------------------------ helpers (both sides)
    public int overclockerCount() {
        int n = 0;
        for (int i = 0; i < UPGRADES; i++) {
            ItemStack s = slots.get(SLOT_UPGRADE + i).getItem();
            if (isOverclocker(s)) n += s.getCount();
        }
        return n;
    }

    /** EN: Plant of a fully analyzed seed bag, or null. ZH: 已完全分析的种子袋对应的植物，否则为 null。JA: 完全に分析済みの種子袋の植物。そうでなければ null。 */
    @Nullable
    public static PlantType analyzedPlant(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ICropSeedItem seed) || seed.cropScanLevel(stack) < 4) return null;
        return PlantRegistry.instance.getPlant(seed.cropModId(stack), seed.cropId(stack));
    }

    /** EN: Candidate list of the current parents (cached while the parents stay the same). ZH: 当前亲本的候选列表（亲本不变时缓存）。JA: 現在の親の候補一覧（親が同じ間はキャッシュ）。 */
    public List<BreedingResearch.Candidate> candidates() {
        PlantType a = analyzedPlant(slots.get(SLOT_PARENT_A).getItem()), b = analyzedPlant(slots.get(SLOT_PARENT_B).getItem());
        String key = (a == null ? "-" : a.getModId() + ":" + a.getTypeId()) + "|" + (b == null ? "-" : b.getModId() + ":" + b.getTypeId());
        if (!key.equals(candidateKey)) {
            candidateKey = key;
            candidateCache = BreedingResearch.candidates(a, b);
        }
        return candidateCache;
    }

    /** EN: Selected target (server: by key; client: synced index). ZH: 选中的目标（服务器按键，客户端按同步索引）。JA: 選択中の目標（サーバーはキー、クライアントは同期番号）。 */
    @Nullable
    public BreedingResearch.Candidate selectedCandidate() {
        List<BreedingResearch.Candidate> list = candidates();
        if (client()) {
            int i = data.get(D_BREED_SELECT);
            return i >= 0 && i < list.size() ? list.get(i) : null;
        }
        for (BreedingResearch.Candidate c : list) if (c.key().equals(state.breedTarget)) return c;
        return null;
    }

    private int selectedIndex() {
        List<BreedingResearch.Candidate> list = candidates();
        for (int i = 0; i < list.size(); i++) if (list.get(i).key().equals(state.breedTarget)) return i;
        return -1;
    }

    /** EN: The bag that breeding produces for {@code plant} from the current parents. ZH: 由当前亲本为 {@code plant} 产出的种子袋。JA: 現在の親から {@code plant} について交配が作る種子袋。 */
    public ItemStack breedResult(PlantType plant) {
        ItemStack a = slots.get(SLOT_PARENT_A).getItem(), b = slots.get(SLOT_PARENT_B).getItem();
        ICropSeedItem sa = (ICropSeedItem) a.getItem(), sb = (ICropSeedItem) b.getItem();
        ItemStack out = CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), plant.getModId(), plant.getTypeId(),
            ScannerMath.breedTrait(sa.cropGrowth(a), sb.cropGrowth(b)), ScannerMath.breedTrait(sa.cropGain(a), sb.cropGain(b)),
            ScannerMath.breedTrait(sa.cropResistance(a), sb.cropResistance(b)));
        CropSeedItem.setScanLevel(out, 4);
        return out;
    }

    /** EN: Current traits of the gene slot seed, or null when it is not analyzed. ZH: 基因槽种子的当前属性；未分析时为 null。JA: 遺伝子スロットの種子の現在の特性値。未分析なら null。 */
    @Nullable
    public int[] geneCurrent() {
        ItemStack s = slots.get(SLOT_GENE).getItem();
        if (analyzedPlant(s) == null) return null;
        ICropSeedItem seed = (ICropSeedItem) s.getItem();
        return new int[]{seed.cropGrowth(s), seed.cropGain(s), seed.cropResistance(s)};
    }

    /** EN: Gene targets (client: synced). ZH: 基因目标（客户端为同步值）。JA: 遺伝子の目標値（クライアントは同期値）。 */
    public int[] geneTarget() {
        if (client()) return new int[]{data.get(D_GENE_TARGET), data.get(D_GENE_TARGET + 1), data.get(D_GENE_TARGET + 2)};
        return state.geneTarget.clone();
    }

    public int catalystCount() {
        ItemStack s = slots.get(SLOT_CATALYST).getItem();
        return isCatalyst(s) ? s.getCount() : 0;
    }

    private static String traitKey(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ICropSeedItem s)) return "";
        return s.cropModId(stack) + ":" + s.cropId(stack) + "/" + s.cropGrowth(stack) + "/" + s.cropGain(stack) + "/" + s.cropResistance(stack)
            + "/" + s.cropScanLevel(stack);
    }

    private static boolean fits(ItemStack slot, ItemStack add) {
        return slot.isEmpty() || (ItemStack.isSameItemSameComponents(slot, add) && slot.getCount() + add.getCount() <= slot.getMaxStackSize());
    }

    private void save() {
        saveTimer = 0;
        dirty = false;
        if (player != null && !player.level().isClientSide) state.save(containerStack, player.level().registryAccess());
    }

    @Override
    public void removed(Player player) {
        if (!player.level().isClientSide && !containerStack.isEmpty()) state.save(containerStack, player.level().registryAccess());
        super.removed(player);
    }

    // ------------------------------------------------------------------ shift-click
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (index < SIZE) {
            if (!moveItemStackTo(stack, SIZE, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved;
            if (MioIcifAPI.instance().getItemAPI().isBattery(stack)) moved = moveItemStackTo(stack, SLOT_BATTERY, SLOT_BATTERY + 1, false);
            else if (isOverclocker(stack)) moved = moveItemStackTo(stack, SLOT_UPGRADE, SLOT_UPGRADE + UPGRADES, false);
            else if (stack.getItem() instanceof ICropSeedItem) moved = switch (page()) {
                case PAGE_BREED -> pageUnlocked(PAGE_BREED) && moveItemStackTo(stack, SLOT_PARENT_A, SLOT_PARENT_B + 1, false);
                case PAGE_GENE -> pageUnlocked(PAGE_GENE) && moveItemStackTo(stack, SLOT_GENE, SLOT_GENE + 1, false);
                default -> moveItemStackTo(stack, 0, GRID, false);
            };
            else if (isCatalyst(stack)) moved = pageUnlocked(PAGE_GENE) && moveItemStackTo(stack, SLOT_CATALYST, SLOT_CATALYST + 1, false);
            else moved = false;
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    // ------------------------------------------------------------------ inner classes
    private final class PageSlot extends Slot {
        private final int pageOf, max;
        private final java.util.function.Predicate<ItemStack> filter;
        PageSlot(Container container, int index, int x, int y, int pageOf, int max, java.util.function.Predicate<ItemStack> filter) {
            super(container, index, x, y);
            this.pageOf = pageOf;
            this.max = max;
            this.filter = filter;
        }
        @Override public boolean isActive() { return pageOf < 0 || (page() == pageOf && pageUnlocked(pageOf)); }
        @Override public boolean mayPlace(ItemStack stack) { return isActive() && filter.test(stack); }
        @Override public boolean mayPickup(Player player) { return isActive(); }
        @Override public int getMaxStackSize() { return max; }
        @Override public int getMaxStackSize(ItemStack stack) { return Math.min(max, stack.getMaxStackSize()); }
    }

    private final class LockedSlot extends Slot {
        LockedSlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        private boolean locked() { return getContainerSlot() == lockedSlot; }
        @Override public boolean mayPickup(Player player) { return !locked() && super.mayPickup(player); }
        @Override public boolean mayPlace(ItemStack stack) { return !locked() && super.mayPlace(stack); }
    }

    private final class StateContainer implements Container {
        @Override public int getContainerSize() { return SIZE; }
        @Override public boolean isEmpty() { for (ItemStack s : state.items) if (!s.isEmpty()) return false; return true; }
        @Override public ItemStack getItem(int slot) { return slot >= 0 && slot < SIZE ? state.items[slot] : ItemStack.EMPTY; }
        @Override public ItemStack removeItem(int slot, int amount) {
            if (slot < 0 || slot >= SIZE || state.items[slot].isEmpty()) return ItemStack.EMPTY;
            ItemStack out = state.items[slot].split(amount);
            if (state.items[slot].isEmpty()) state.items[slot] = ItemStack.EMPTY;
            dirty = true;
            return out;
        }
        @Override public ItemStack removeItemNoUpdate(int slot) {
            if (slot < 0 || slot >= SIZE) return ItemStack.EMPTY;
            ItemStack out = state.items[slot];
            state.items[slot] = ItemStack.EMPTY;
            dirty = true;
            return out;
        }
        @Override public void setItem(int slot, ItemStack stack) { if (slot >= 0 && slot < SIZE) { state.items[slot] = stack; dirty = true; } }
        @Override public void setChanged() { dirty = true; }
        @Override public void clearContent() { java.util.Arrays.fill(state.items, ItemStack.EMPTY); dirty = true; }
        @Override public boolean stillValid(Player player) { return true; }
    }

    private final class ServerData implements ContainerData {
        @Override public int get(int index) {
            int cards = ScannerMath.overclockers(overclockerCount());
            if (index >= D_PROGRESS && index < D_PROGRESS + GRID)
                return Math.min(1000, state.progress[index - D_PROGRESS] * 1000 / ScannerMath.analysisTicks(cards));
            return switch (index) {
                case D_PAGE -> page;
                case D_UNLOCK -> state.unlocks;
                case D_ENERGY -> {
                    long cap = capacity();
                    yield cap <= 0 ? 0 : (int) Math.min(10_000L, energy() * 10_000L / cap);
                }
                case D_CAPACITY -> (int) Math.min(30_000L, capacity() / 1000L);
                case D_OC -> overclockerCount();
                case D_BREED_PROGRESS -> {
                    BreedingResearch.Candidate c = selectedCandidate();
                    yield c == null ? 0 : (int) Math.min(1000, state.breedProgress * 1000L / ScannerMath.researchTicks(c.cost(), ScannerMath.BREED_POWER, cards));
                }
                case D_BREED_SELECT -> selectedIndex();
                case D_BREED_RUN -> state.breedRun ? 1 : 0;
                case D_GENE_PROGRESS -> {
                    int[] cur = geneCurrent();
                    long total = cur == null ? 0 : ScannerMath.geneCost(cur, state.geneTarget);
                    yield total == 0 ? 0 : (int) Math.min(1000, state.geneProgress * 1000L / ScannerMath.researchTicks(total, ScannerMath.GENE_POWER, cards));
                }
                case D_GENE_TARGET -> state.geneTarget[0];
                case D_GENE_TARGET + 1 -> state.geneTarget[1];
                case D_GENE_TARGET + 2 -> state.geneTarget[2];
                case D_GENE_RUN -> state.geneRun ? 1 : 0;
                case D_SCAN_STATUS -> scanStatus;
                case D_BREED_STATUS -> breedStatus;
                case D_GENE_STATUS -> geneStatus;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {}
        @Override public int getCount() { return D_COUNT; }
    }

    /** EN: Menu provider used by the item. ZH: 物品使用的菜单提供器。JA: アイテムが使うメニュープロバイダー。 */
    public static class Provider implements MenuProvider {
        private final ItemStack containerStack;
        private final InteractionHand hand;

        public Provider(ItemStack containerStack, InteractionHand hand) {
            this.containerStack = containerStack;
            this.hand = hand;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("gui.mio_icif.crop_analyzer.title");
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new CropAnalyzerMenu(containerId, playerInventory, containerStack, hand);
        }
    }
}
