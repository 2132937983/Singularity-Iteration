package com.miophas.singularity_iteration.common.armory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Holds the showcase's own outfit, or mirrors a suit stored in a linked Armory.
 * Items are kept in {@link ArmoryPiece#COLUMNS} order (helmet ... off hand).
 */
@SuppressWarnings("null")
public class ArmorShowcaseBlockEntity extends BlockEntity {
    public static final int SLOTS = ArmoryPiece.COLUMNS.length;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    /** What the client draws: the own outfit, or a copy of the linked suit. */
    private final NonNullList<ItemStack> display = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    @Nullable private GlobalPos link;
    private int linkedSuit;
    private String label = "";
    private int rotation;

    public ArmorShowcaseBlockEntity(BlockPos pos, BlockState state) {
        super(ArmoryRegistry.SHOWCASE_ENTITY.get(), pos, state);
    }

    // ------------------------------------------------------------------ client view
    public List<ItemStack> display() { return display; }
    public String label() { return label; }
    public boolean linked() { return link != null; }
    /** Turntable angle in degrees (45 degree steps). */
    public int rotation() { return rotation * 45; }
    public ItemStack item(ArmoryPiece piece) { return items.get(piece.column()); }

    // ------------------------------------------------------------------ Armory network
    /** A showcase with its own outfit (not mirroring an Armory suit) is part of the owner's armour network. */
    public boolean hasOwnOutfit() {
        if (link != null) return false;
        for (ItemStack stack : items) if (!stack.isEmpty()) return true;
        return false;
    }

    /** Top of the mannequin, where summoned pieces launch from and purged pieces land. */
    public net.minecraft.world.phys.Vec3 launchTop() {
        return net.minecraft.world.phys.Vec3.atCenterOf(worldPosition).add(0, 0.6, 0);
    }

    /** Server: takes the piece off the mannequin (to fly it to a player). */
    public ItemStack takePiece(ArmoryPiece piece) {
        if (link != null) return ItemStack.EMPTY;
        ItemStack stack = items.get(piece.column());
        if (stack.isEmpty()) return ItemStack.EMPTY;
        items.set(piece.column(), ItemStack.EMPTY);
        changed(null);
        return stack;
    }

    /** Server: puts a piece onto the mannequin (a suit flown in exchange). Returns what did not fit. */
    public ItemStack storePiece(ArmoryPiece preferred, ItemStack stack) {
        if (stack.isEmpty() || link != null) return stack;
        ArmoryPiece piece = items.get(preferred.column()).isEmpty() ? preferred : pieceFor(stack);
        if (!items.get(piece.column()).isEmpty()) return stack;
        items.set(piece.column(), stack.copyWithCount(1));
        ItemStack rest = stack.copy();
        rest.shrink(1);
        changed(SoundEvents.ARMOR_EQUIP_NETHERITE.value());
        return rest;
    }

    // ------------------------------------------------------------------ interaction
    boolean useItem(Player player, InteractionHand hand, ItemStack held) {
        if (held.getItem() instanceof ArmoryRemoteItem) {
            useRemote(player, held);
            return true;
        }
        if (link != null) {
            player.displayClientMessage(Component.translatable("message.mio_icif.showcase.linked_busy"), true);
            return true;
        }
        ArmoryPiece piece = pieceFor(held);
        ItemStack previous = items.get(piece.column());
        items.set(piece.column(), held.copyWithCount(1));
        held.shrink(1);
        if (!previous.isEmpty() && !player.getInventory().add(previous)) player.drop(previous, false);
        changed(SoundEvents.ARMOR_EQUIP_IRON.value());
        return true;
    }

    void useEmptyHand(Player player) {
        if (player.isShiftKeyDown() && link == null) {
            // swap the whole outfit with the player's
            for (ArmoryPiece piece : ArmoryPiece.COLUMNS) {
                ItemStack worn = player.getItemBySlot(piece.slot);
                if (!worn.isEmpty() && !player.isCreative()
                        && EnchantmentHelper.has(worn, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) continue;
                ItemStack shown = items.get(piece.column());
                player.setItemSlot(piece.slot, shown.copy());
                items.set(piece.column(), worn.copy());
            }
            changed(SoundEvents.ARMOR_EQUIP_NETHERITE.value());
            return;
        }
        rotation = (rotation + 1) & 7;
        changed(SoundEvents.ITEM_FRAME_ROTATE_ITEM);
    }

    private void useRemote(Player player, ItemStack remote) {
        GlobalPos target = remote.get(ArmoryComponents.TARGET.get());
        if (player.isShiftKeyDown() || target == null) {
            if (link != null) {
                link = null;
                player.displayClientMessage(Component.translatable("message.mio_icif.showcase.unlinked"), true);
                changed(SoundEvents.ITEM_FRAME_REMOVE_ITEM);
            }
            return;
        }
        if (target.equals(link)) linkedSuit = (linkedSuit + 1) % mio_icif_armory.SETS;
        else { link = target; linkedSuit = 0; }
        refreshLink();
        player.displayClientMessage(Component.translatable("message.mio_icif.showcase.linked", label), true);
        changed(SoundEvents.ITEM_FRAME_ADD_ITEM);
    }

    /** Body position an item goes to on the mannequin. */
    static ArmoryPiece pieceFor(ItemStack stack) {
        Equipable equipable = Equipable.get(stack);
        EquipmentSlot slot = ArmoryRules.naturalSlot(stack);
        if (equipable != null) slot = equipable.getEquipmentSlot();
        if (slot != null && slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) return ArmoryPiece.of(slot);
        return slot == EquipmentSlot.OFFHAND ? ArmoryPiece.OFFHAND : ArmoryPiece.MAINHAND;
    }

    private void changed(@Nullable net.minecraft.sounds.SoundEvent sound) {
        if (level != null && sound != null) level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 0.8F, 1.0F);
        if (link == null) for (int i = 0; i < SLOTS; i++) display.set(i, items.get(i).copy());
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    // ------------------------------------------------------------------ linked mirror
    public static void serverTick(Level level, BlockPos pos, BlockState state, ArmorShowcaseBlockEntity showcase) {
        if (showcase.link != null && level.getGameTime() % 20 == Math.floorMod(pos.asLong(), 20)) showcase.refreshLink();
    }

    /** Copies the linked suit (if its Armory is loaded) and syncs when it changed. */
    void refreshLink() {
        if (link == null || !(level instanceof ServerLevel here)) return;
        ServerLevel home = here.getServer().getLevel(link.dimension());
        boolean changed = false;
        if (home != null && home.isLoaded(link.pos()) && home.getBlockEntity(link.pos()) instanceof mio_icif_armory armory) {
            for (ArmoryPiece piece : ArmoryPiece.COLUMNS) {
                ItemStack now = armory.piece(linkedSuit, piece);
                if (!ItemStack.matches(now, display.get(piece.column()))) {
                    display.set(piece.column(), now.copy());
                    changed = true;
                }
            }
            String name = armory.setName(linkedSuit);
            if (!name.equals(label)) { label = name; changed = true; }
        }
        if (changed) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    void dropContents() {
        if (level == null) return;
        for (ItemStack stack : items) Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, stack.copy());
        items.replaceAll(s -> ItemStack.EMPTY);
    }

    // ------------------------------------------------------------------ persistence & sync
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", writeList(items, registries));
        writeShared(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Items")) readList(tag.getList("Items", 10), items, registries);
        readShared(tag, registries);
    }

    private void writeShared(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("Display", writeList(link == null ? items : display, registries));
        tag.putInt("Rotation", rotation);
        tag.putInt("Suit", linkedSuit);
        tag.putString("Label", label);
        if (link != null) GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, link).result().ifPresent(t -> tag.put("Link", t));
    }

    private void readShared(CompoundTag tag, HolderLookup.Provider registries) {
        readList(tag.getList("Display", 10), display, registries);
        rotation = tag.getInt("Rotation") & 7;
        linkedSuit = Math.floorMod(tag.getInt("Suit"), mio_icif_armory.SETS);
        label = tag.getString("Label");
        link = tag.contains("Link") ? GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("Link")).result().orElse(null) : null;
        if (link == null) label = "";
    }

    private static net.minecraft.nbt.ListTag writeList(List<ItemStack> list, HolderLookup.Provider registries) {
        net.minecraft.nbt.ListTag out = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = new CompoundTag();
            entry.putByte("Slot", (byte) i);
            if (!list.get(i).isEmpty()) entry.put("Item", list.get(i).save(registries));
            out.add(entry);
        }
        return out;
    }

    private static void readList(net.minecraft.nbt.ListTag in, NonNullList<ItemStack> list, HolderLookup.Provider registries) {
        list.replaceAll(s -> ItemStack.EMPTY);
        for (int i = 0; i < in.size(); i++) {
            CompoundTag entry = in.getCompound(i);
            int slot = entry.getByte("Slot");
            if (slot >= 0 && slot < list.size() && entry.contains("Item"))
                list.set(slot, ItemStack.parse(registries, entry.getCompound("Item")).orElse(ItemStack.EMPTY));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        writeShared(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readShared(tag, registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        readShared(packet.getTag(), registries);
    }
}
