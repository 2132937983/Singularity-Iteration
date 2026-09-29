package com.miophas.singularity_iteration.common.blockentity.pipe;

import com.miophas.singularity_iteration.common.registry.mio_icif_block_entities;
import com.miophas.singularity_iteration.core.runtime.processing.ItemPipeRoute;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import java.util.WeakHashMap;

/** R53: bounded, server-owned custody and endpoint routing. Old buffer keys remain readable. */
public class mio_icif_pipe_item extends mio_icif_pipe_default {
    public enum PipeMode { INPUT, TRANSPORT }
    public static final String PIPE_TYPE = "item";
    public static final int TRANSFER_RATE = 1, MAX_TRANSFER_RATE = 64, TRANSFER_COOLDOWN = 8;
    private static final Direction[] SIDES = Direction.values();
    private static final int WORLD_WORK = 4096, PIPE_WORK = 64;
    private static final int MAX_WORLD_SEARCHES = 16;
    private static final WeakHashMap<Level, WorkWindow> WINDOWS = new WeakHashMap<>();
    private static final class WorkWindow {
        long tick = Long.MIN_VALUE;
        final java.util.ArrayDeque<java.lang.ref.WeakReference<mio_icif_pipe_item>> waiting = new java.util.ArrayDeque<>();
        final WeakHashMap<mio_icif_pipe_item, Boolean> searching = new WeakHashMap<>();
    }
    private boolean queued;
    private long grantedTick = Long.MIN_VALUE;
    protected ItemStack bufferItem = ItemStack.EMPTY;
    @Nullable protected Direction bufferFromDirection;
    protected int transferCooldown, roundRobinIndex, inputSourceIndex;
    protected PipeMode mode;
    protected boolean isProcessing;
    private ItemStack uncertain = ItemStack.EMPTY;
    private String uncertainPhase = "";
    private String uncertainId = "";
    private Tag unparsedBuffer, unparsedUncertain;
    private BlockPos sourceContainer;
    private int sourceSlot;
    private ItemPipeRoute route;
    private ItemPipeRoute.Watch routeWatch;
    private final IItemHandler[] handlers = new IItemHandler[7];
    @SuppressWarnings({"unchecked", "rawtypes"})
    private final BlockCapabilityCache<IItemHandler, Direction>[] neighbors = new BlockCapabilityCache[6];

    public mio_icif_pipe_item(BlockEntityType<?> type, BlockPos pos, BlockState state) { this(type,pos,state,PipeMode.TRANSPORT); }
    public mio_icif_pipe_item(BlockEntityType<?> type, BlockPos pos, BlockState state, PipeMode mode) {
        super(type != null ? type : mio_icif_block_entities.PIPE_ITEM_TRANSPORT_ENTITY_TYPE.get(),pos,state);
        this.mode = mode == null ? PipeMode.TRANSPORT : mode;
        for (int i=0;i<handlers.length;i++) handlers[i] = new PipeItemHandler(i==6 ? null : SIDES[i]);
    }
    public boolean isProcessing() { return isProcessing; }
    public void setProcessing(boolean value) { isProcessing = value; }
    public PipeMode getMode() { return mode; }
    public void setMode(PipeMode value) { mode = value == null ? PipeMode.TRANSPORT : value; markForUpdate(); dirty(); }
    public boolean canExtract() { return true; }
    public boolean canInsert() { return mode == PipeMode.TRANSPORT; }
    public boolean canExtractFromContainer() { return mode == PipeMode.INPUT; }
    public boolean canInsertToContainer() { return mode == PipeMode.TRANSPORT; }
    public int getMaxBufferSize() { return MAX_TRANSFER_RATE; }
    public boolean hasOutputTarget() {
        for (Direction side:SIDES) if (linkedPipe(side) != null || (canInsertToContainer() && neighbor(side) != null)) return true;
        return false;
    }
    protected boolean canWork() {
        if (isRemoved() || !(level instanceof ServerLevel server) || !server.getServer().isSameThread()) return false;
        return server.shouldTickBlocksAt(net.minecraft.world.level.ChunkPos.asLong(worldPosition)) && loaded(worldPosition) == this;
    }
    private WorkWindow window() { return WINDOWS.computeIfAbsent(level, ignored -> new WorkWindow()); }
    private int allowance() {
        WorkWindow w=window(); long now=level.getGameTime();
        if(w.tick!=now){
            w.tick=now;
            int remaining=WORLD_WORK/PIPE_WORK;
            // Active bounded searches make progress even with thousands of waiting inputs.
            for(var pipe:w.searching.keySet()) { pipe.grantedTick=now; remaining--; }
            int examined=Math.min(WORLD_WORK/PIPE_WORK,w.waiting.size());
            for(int i=0;i<examined && remaining>0 && !w.waiting.isEmpty();i++){
                var pipe=w.waiting.remove().get();
                if(pipe==null)continue;
                if(pipe.isRemoved()){pipe.queued=false;continue;}
                if(pipe.grantedTick==now){pipe.queued=false;continue;}
                if(!pipe.bufferItem.isEmpty()) {
                    if(w.searching.size()>=MAX_WORLD_SEARCHES){w.waiting.add(new java.lang.ref.WeakReference<>(pipe));continue;}
                    w.searching.put(pipe,Boolean.TRUE);
                }
                pipe.queued=false;pipe.grantedTick=now;remaining--;
            }
        }
        if(grantedTick==now){grantedTick=Long.MIN_VALUE;return PIPE_WORK;}
        if(!queued){queued=true;w.waiting.add(new java.lang.ref.WeakReference<>(this));}
        return 0;
    }
    private void releaseSearch() { if(level!=null)window().searching.remove(this); }
    protected void dirty() {
        if(level instanceof ServerLevel server) {
            var chunk=server.getChunkSource().getChunkNow(worldPosition.getX()>>4,worldPosition.getZ()>>4);
            if(chunk!=null) chunk.setUnsaved(true);
        }
    }
    protected BlockEntity loaded(BlockPos at) {
        if(!(level instanceof ServerLevel server)) return null;
        var chunk=server.getChunkSource().getChunkNow(at.getX()>>4,at.getZ()>>4);
        return chunk==null ? null : chunk.getBlockEntity(at, net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK);
    }
    protected mio_icif_pipe_item linkedPipe(Direction side) {
        if(isDirectionBlocked(side) || !isConnected(side)) return null;
        if(loaded(worldPosition.relative(side)) instanceof mio_icif_pipe_item other
            && !other.isRemoved() && other.canInsert() && !other.isDirectionBlocked(side.getOpposite())
            && other.isConnected(side.getOpposite())) return other;
        return null;
    }
    protected IItemHandler neighbor(Direction side) {
        if(isDirectionBlocked(side) || !isConnected(side) || !(level instanceof ServerLevel server)) return null;
        BlockPos at=worldPosition.relative(side);
        if(loaded(at) instanceof mio_icif_pipe_item) return null;
        int index=side.ordinal();
        if(neighbors[index]==null) neighbors[index]=BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK,server,at,side.getOpposite(),
            () -> !isRemoved() && level==server, this::markForUpdate);
        return neighbors[index].getCapability();
    }
    protected void observeRoute(ItemPipeRoute observed) {
        if(observed==null)return;
        if(routeWatch==null)routeWatch=new ItemPipeRoute.Watch();
        routeWatch.observe(observed);
    }
    private void invalidateObservedRoutes() { if(routeWatch!=null)routeWatch.changed(); }
    private final ItemPipeRoute.Access access = new ItemPipeRoute.Access() {
        private mio_icif_pipe_item at(BlockPos pos) {
            var pipe=pos.equals(worldPosition) ? mio_icif_pipe_item.this : loaded(pos) instanceof mio_icif_pipe_item other ? other : null;
            if(pipe!=null)pipe.observeRoute(route);
            return pipe;
        }
        @Override public BlockPos pipe(BlockPos from, Direction side) {
            var pipe=at(from);
            // Both endpoints affect a connection, including currently blocked edges.
            at(from.relative(side));
            var next=pipe==null?null:pipe.linkedPipe(side);return next==null?null:next.getBlockPos();
        }
        @Override public IItemHandler inventory(BlockPos from, Direction side) { var pipe=at(from); return pipe==null || !pipe.canInsertToContainer()?null:pipe.neighbor(side); }
        @Override public long revision() { return 0; }
    };
    @Override protected void doTransfer() {
        if(!canWork() || isProcessing || !uncertainPhase.isEmpty()) return;
        if(transferCooldown>0){transferCooldown--;return;}
        if(bufferItem.isEmpty() && !canExtractFromContainer()) { releaseSearch(); return; }
        int budget=allowance(); if(budget==0) return;
        isProcessing=true;
        try {
            if(!bufferItem.isEmpty()) {
                if(route==null) route=new ItemPipeRoute(worldPosition,sourceContainer,bufferItem.copyWithCount(Math.min(MAX_TRANSFER_RATE,bufferItem.getCount())));
                ItemPipeRoute currentRoute=route;
                var target=currentRoute.advance(access,budget);
                if(route!=currentRoute) return;
                if(target!=null) {
                    IItemHandler handler=access.inventory(target.pipe(),target.side());
                    int accepted=handler==null ? 0 : deliver(handler,target.slot(),target.accepted());
                    if(accepted>0 || hasUncertainTransfer()) {
                        if(accepted>0)transferCooldown=TRANSFER_COOLDOWN;
                        route=null;releaseSearch();
                    } else if(route==currentRoute) {
                        // A confirmed rejection must not send the next search back to
                        // the same first outlet forever. Continue the bounded search.
                        currentRoute.rejectTarget();
                    }
                } else if(currentRoute.exhausted()) { route=null; releaseSearch(); transferCooldown=20+Math.floorMod(worldPosition.hashCode(),8); }
                return;
            }
            if(canExtractFromContainer() && extractOwned(Math.min(16,budget))) transferCooldown=extractionCooldown();
            else transferCooldown=8;
        } catch(RuntimeException failure) {
            transferCooldown=20; route=null; releaseSearch();
        } finally { isProcessing=false; }
    }
    protected int extractionLimit() { return TRANSFER_RATE; }
    protected int extractionCooldown() { return TRANSFER_COOLDOWN; }
    protected void extracted(int count) { }
    protected boolean extractAndTransferItem() { return extractOwned(16); }
    private boolean extractOwned(int budget) {
        if(!bufferItem.isEmpty() || !uncertainPhase.isEmpty() || !hasOutputTarget()) return false;
        while(budget-->0) {
            Direction side=SIDES[Math.floorMod(inputSourceIndex,6)];
            IItemHandler handler=neighbor(side);
            if(handler==null || sourceSlot>=handler.getSlots()){ inputSourceIndex=(inputSourceIndex+1)%6;sourceSlot=0;continue; }
            int slot=sourceSlot++;
            ItemStack quote=handler.extractItem(slot,Math.min(MAX_TRANSFER_RATE,extractionLimit()),true);
            if(quote.isEmpty()) continue;
            uncertain=quote.copy();uncertainPhase="extract";uncertainId=java.util.UUID.randomUUID().toString();
            sourceContainer=worldPosition.relative(side);bufferFromDirection=side;dirty();
            ItemStack actual=handler.extractItem(slot,Math.min(quote.getCount(),MAX_TRANSFER_RATE),false);
            bufferItem=actual.copy();sourceContainer=worldPosition.relative(side);bufferFromDirection=side;
            uncertain=ItemStack.EMPTY;uncertainPhase="";uncertainId="";route=null;dirty();
            if(!actual.isEmpty()){ extracted(actual.getCount());return true; }
        }
        return false;
    }
    protected int deliver(IItemHandler target,int slot,int requested) {
        if(bufferItem.isEmpty() || requested<=0 || !uncertainPhase.isEmpty()) return 0;
        int amount=Math.min(MAX_TRANSFER_RATE,Math.min(bufferItem.getCount(),requested));
        uncertain=bufferItem.copyWithCount(amount);uncertainPhase="insert";uncertainId=java.util.UUID.randomUUID().toString();
        bufferItem=bufferItem.copyWithCount(bufferItem.getCount()-amount);dirty();
        ItemStack remaining=target.insertItem(slot,uncertain.copy(),false);
        if(!ItemPipeRoute.validRemainder(uncertain,remaining)) {
            // The external call may already have committed. Refunding or replaying
            // this batch could duplicate it. Keep the intent and untouched buffer.
            route=null;releaseSearch();transferCooldown=20;dirty();
            return 0;
        }
        int accepted=amount-remaining.getCount();
        if(bufferItem.isEmpty()) bufferItem=remaining.copy(); else bufferItem.grow(remaining.getCount());
        uncertain=ItemStack.EMPTY;uncertainPhase="";uncertainId="";
        if(bufferItem.isEmpty()){ bufferFromDirection=null;sourceContainer=null; }
        dirty();return accepted;
    }
    @Override protected boolean canConnectTo(BlockPos pos,Direction direction) {
        if(!(level instanceof ServerLevel server) || server.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null) return false;
        if(loaded(pos) instanceof mio_icif_pipe_item pipe) return !pipe.isDirectionBlocked(direction.getOpposite());
        return level.getCapability(Capabilities.ItemHandler.BLOCK,pos,direction.getOpposite())!=null;
    }
    @Override public void markForUpdate() {
        invalidateObservedRoutes();
        super.markForUpdate();route=null;releaseSearch();transferCooldown=0;
    }
    private int connectionMask() { int mask=0;for(Direction side:SIDES)if(isConnected(side))mask|=1<<side.ordinal();return mask; }
    @Override protected void updateConnections() {
        int before=connectionMask();super.updateConnections();
        if(before!=connectionMask())invalidateObservedRoutes();
    }
    @Override public void setConnection(Direction side,boolean connected) {
        boolean changed=isConnected(side)!=connected;super.setConnection(side,connected);
        if(changed)invalidateObservedRoutes();
    }
    @Override public void setRemoved() { markForUpdate();java.util.Arrays.fill(neighbors,null);super.setRemoved(); }
    @Override public void onLoad() { super.onLoad();markForUpdate();if(hasUncertainTransfer())dirty(); }
    @Override public String getPipeTypeString() { return PIPE_TYPE; }
    public IItemHandler getItemHandlerCapability(@Nullable Direction side) { return handlers[side==null?6:side.ordinal()]; }
    public ItemStack getBufferItem() { return bufferItem.copy(); }
    public boolean isEmpty() { return bufferItem.isEmpty(); }
    public boolean isFull() { return !bufferItem.isEmpty() && bufferItem.getCount()>=Math.min(MAX_TRANSFER_RATE,bufferItem.getMaxStackSize()); }
    public boolean hasUncertainTransfer() { return !uncertainPhase.isEmpty(); }
    public String getUncertainTransferId() { return uncertainId; }
    public String getUncertainTransferPhase() { return uncertainPhase; }
    public ItemStack getUncertainItem() { return uncertain.copy(); }
    /** Only for an explicit operator reconciliation after inspecting the external inventory.
     * confirmedCount is what the external insert accepted, or what the external extract removed.
     * The saved intent identity prevents stale/repeated commands from touching a later transfer.
     */
    public boolean resolveUncertainTransfer(String expectedId,int confirmedCount) {
        if(!canWork() || isProcessing || uncertainId.isEmpty() || !uncertainId.equals(expectedId)
            || uncertain.isEmpty() || unparsedBuffer!=null || unparsedUncertain!=null
            || (!uncertainPhase.equals("insert") && !uncertainPhase.equals("extract"))
            || confirmedCount<0 || confirmedCount>uncertain.getCount())return false;
        int restore=uncertainPhase.equals("insert") ? uncertain.getCount()-confirmedCount : confirmedCount;
        if((!bufferItem.isEmpty() && !ItemStack.isSameItemSameComponents(bufferItem,uncertain))
            || (long)bufferItem.getCount()+restore>Integer.MAX_VALUE)return false;
        boolean extraction=uncertainPhase.equals("extract");
        isProcessing=true;
        try {
            if(restore>0) {
                if(bufferItem.isEmpty())bufferItem=uncertain.copyWithCount(restore);
                else bufferItem.grow(restore);
            }
            uncertain=ItemStack.EMPTY;uncertainPhase="";uncertainId="";
            route=null;releaseSearch();transferCooldown=20;
            if(bufferItem.isEmpty()){bufferFromDirection=null;sourceContainer=null;}
            dirty();
            if(extraction && confirmedCount>0)extracted(confirmedCount);
            return true;
        } finally { isProcessing=false; }
    }
    private static CompoundTag saveStack(ItemStack stack,HolderLookup.Provider registries) {
        // ItemStack's codec permits at most 99, while old pipe buffers can be larger.
        CompoundTag tag=(CompoundTag)stack.copyWithCount(Math.min(99,stack.getCount())).save(registries);
        if(stack.getCount()>99)tag.putInt("scex_pipe_count",stack.getCount());
        return tag;
    }
    private static ItemStack loadStack(Tag raw,HolderLookup.Provider registries) {
        if(!(raw instanceof CompoundTag tag))return ItemStack.EMPTY;
        CompoundTag normalized=tag.copy();
        int count=tag.contains("count") ? tag.getInt("count") : 1;
        if(tag.contains("scex_pipe_count")) {
            if(!tag.contains("scex_pipe_count",Tag.TAG_INT))return ItemStack.EMPTY;
            count=tag.getInt("scex_pipe_count");
            if(count<=99 || tag.getInt("count")!=99)return ItemStack.EMPTY;
        }
        if(count<=0)return ItemStack.EMPTY;
        normalized.putInt("count",Math.min(99,count));
        ItemStack stack=ItemStack.parse(registries,normalized).orElse(ItemStack.EMPTY);
        if(!stack.isEmpty())stack.setCount(count);
        return stack;
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries);
        if(unparsedBuffer!=null)tag.put("buffer",unparsedBuffer.copy());
        else if(!bufferItem.isEmpty())tag.put("buffer",saveStack(bufferItem,registries));
        if(bufferFromDirection!=null)tag.putString("buffer_from_dir",bufferFromDirection.name());
        if(sourceContainer!=null)tag.putLong("scex_pipe_source",sourceContainer.asLong());
        tag.putString("mode",mode.name());tag.putInt("transferCooldown",transferCooldown);
        tag.putInt("roundRobinIndex",roundRobinIndex);tag.putInt("inputSourceIndex",inputSourceIndex);
        if(!uncertainPhase.isEmpty()) {
            tag.putString("scex_pipe_phase",uncertainPhase);
            tag.putString("scex_pipe_transfer_id",uncertainId);
            if(unparsedUncertain!=null)tag.put("scex_pipe_uncertain",unparsedUncertain.copy());
            else if(!uncertain.isEmpty())tag.put("scex_pipe_uncertain",saveStack(uncertain,registries));
        }
    }
    @Override public void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        invalidateObservedRoutes();releaseSearch();
        super.loadAdditional(tag,registries);route=null;sourceSlot=0;
        bufferItem=loadStack(tag.get("buffer"),registries);
        unparsedBuffer=bufferItem.isEmpty() && tag.contains("buffer") ? tag.get("buffer").copy() : null;
        bufferFromDirection=null;
        try{ if(tag.contains("buffer_from_dir"))bufferFromDirection=Direction.valueOf(tag.getString("buffer_from_dir")); }catch(IllegalArgumentException ignored){}
        try{ if(tag.contains("mode"))mode=PipeMode.valueOf(tag.getString("mode")); }catch(IllegalArgumentException ignored){}
        sourceContainer=tag.contains("scex_pipe_source",4)?BlockPos.of(tag.getLong("scex_pipe_source")):null;
        transferCooldown=Math.clamp(tag.getInt("transferCooldown"),0,40);
        roundRobinIndex=Math.max(0,tag.getInt("roundRobinIndex"));inputSourceIndex=Math.floorMod(tag.getInt("inputSourceIndex"),6);
        uncertainPhase=tag.getString("scex_pipe_phase");
        uncertain=loadStack(tag.get("scex_pipe_uncertain"),registries);
        unparsedUncertain=uncertain.isEmpty() && tag.contains("scex_pipe_uncertain") ? tag.get("scex_pipe_uncertain").copy() : null;
        if(uncertainPhase.isEmpty() && (unparsedBuffer!=null || tag.contains("scex_pipe_uncertain") || tag.contains("scex_pipe_phase")))uncertainPhase="unmapped";
        uncertainId=uncertainPhase.isEmpty()?"":tag.getString("scex_pipe_transfer_id");
        if(!uncertainPhase.isEmpty() && uncertainId.isEmpty())uncertainId=java.util.UUID.randomUUID().toString();
    }
    public static void tick(Level level,BlockPos pos,BlockState state,mio_icif_pipe_item pipe) {
        if(!level.isClientSide() && Math.floorMod(level.getGameTime()+pos.asLong(),40)==0)pipe.needsUpdate=true;
        mio_icif_pipe_default.tick(level,pos,state,pipe);
    }
    private final class PipeItemHandler implements IItemHandler {
        private final Direction side;
        PipeItemHandler(Direction side){this.side=side;}
        private boolean allowed(){return canWork() && !isProcessing && uncertainPhase.isEmpty() && (side==null || !isDirectionBlocked(side));}
        @Override public int getSlots(){return 1;}
        @Override public ItemStack getStackInSlot(int slot){return slot==0?bufferItem.copy():ItemStack.EMPTY;}
        @Override public int getSlotLimit(int slot){return slot==0?MAX_TRANSFER_RATE:0;}
        @Override public boolean isItemValid(int slot,ItemStack stack){return slot==0 && canInsert() && (side==null || !isDirectionBlocked(side));}
        @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate) {
            if(stack.isEmpty())return ItemStack.EMPTY;
            if(!allowed() || !isItemValid(slot,stack) || (!bufferItem.isEmpty() && !ItemStack.isSameItemSameComponents(bufferItem,stack)))return stack;
            int accepted=Math.min(stack.getCount(),Math.max(0,Math.min(MAX_TRANSFER_RATE,stack.getMaxStackSize())-bufferItem.getCount()));
            if(!simulate && accepted>0){
                if(bufferItem.isEmpty()){bufferItem=stack.copyWithCount(accepted);bufferFromDirection=side;sourceContainer=side==null?null:worldPosition.relative(side);}
                else bufferItem.grow(accepted);
                route=null;dirty();
            }
            return stack.copyWithCount(stack.getCount()-accepted);
        }
        @Override public ItemStack extractItem(int slot,int amount,boolean simulate) {
            if(slot!=0 || amount<=0 || !allowed() || bufferItem.isEmpty())return ItemStack.EMPTY;
            int count=Math.min(bufferItem.getMaxStackSize(),Math.min(bufferItem.getCount(),amount));ItemStack result=bufferItem.copyWithCount(count);
            if(!simulate){bufferItem.shrink(count);if(bufferItem.isEmpty()){bufferFromDirection=null;sourceContainer=null;}route=null;dirty();}
            return result;
        }
    }
}
