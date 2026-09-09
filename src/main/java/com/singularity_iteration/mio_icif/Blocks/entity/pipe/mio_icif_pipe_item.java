package com.singularity_iteration.mio_icif.Blocks.entity.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@SuppressWarnings("null")
public class mio_icif_pipe_item extends mio_icif_pipe_default {

    public enum PipeMode {
        INPUT,     
        TRANSPORT   
    }

    public static final String PIPE_TYPE = "item";

    public static final int TRANSFER_RATE = 1;
    public static final int MAX_TRANSFER_RATE = Integer.MAX_VALUE;

    public static final int TRANSFER_COOLDOWN = 8;


    protected ItemStack bufferItem = ItemStack.EMPTY;

    @Nullable
    protected Direction bufferFromDirection = null;

    protected int transferCooldown = 0;

    protected int roundRobinIndex = 0;
    
    protected int inputSourceIndex = 0;

    protected PipeMode mode;

    protected boolean isProcessing = false;

    public mio_icif_pipe_item(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this(type, pos, state, PipeMode.TRANSPORT);
    }

    public mio_icif_pipe_item(BlockEntityType<?> type, BlockPos pos, BlockState state, PipeMode mode) {
        super(type != null ? type : mio_icif_block_entities.PIPE_ITEM_TRANSPORT_ENTITY_TYPE.get(), pos, state);
        this.mode = mode != null ? mode : PipeMode.TRANSPORT;
    }

    public boolean isProcessing() {
        return isProcessing;
    }

    public void setProcessing(boolean processing) {
        this.isProcessing = processing;
    }

    public PipeMode getMode() {
        return mode;
    }

    public void setMode(PipeMode mode) {
        this.mode = mode != null ? mode : PipeMode.TRANSPORT;
        setChanged();
    }

    public boolean canExtract() {
        return mode == PipeMode.INPUT || mode == PipeMode.TRANSPORT;
    }

    public boolean canInsert() {
        return mode == PipeMode.TRANSPORT;
    }

    public boolean canExtractFromContainer() {
        return mode == PipeMode.INPUT;
    }

    public boolean canInsertToContainer() {
        return mode == PipeMode.TRANSPORT;
    }

    public boolean hasOutputTarget() {
        return hasOutputTarget(new HashSet<>());
    }

    private boolean hasOutputTarget(Set<BlockPos> visited) {
        if (level == null) return false;
        if (visited.contains(worldPosition)) return false;
        visited.add(worldPosition);

        for (Direction dir : Direction.values()) {
            if (!isConnected(dir)) continue;

            BlockPos adjacentPos = worldPosition.relative(dir);

            if (level.getBlockEntity(adjacentPos) instanceof mio_icif_pipe_item otherPipe) {
                if (mode == PipeMode.INPUT && otherPipe.canInsert()) {
  
                    if (otherPipe.hasOutputTargetRecursive(visited)) {
                        return true;
                    }
                }
                if (mode == PipeMode.TRANSPORT && otherPipe.canInsert()) {
                    return true;
                }
                continue;
            }

            IItemHandler handler = level.getCapability(
                Capabilities.ItemHandler.BLOCK, adjacentPos, dir.getOpposite()
            );

            if (handler != null) {
                if (mode == PipeMode.TRANSPORT) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean hasOutputTargetRecursive(Set<BlockPos> visited) {
        if (level == null) return false;
        if (visited.contains(worldPosition)) return false;
        visited.add(worldPosition);

        for (Direction dir : Direction.values()) {
            if (!isConnected(dir)) continue;

            BlockPos adjacentPos = worldPosition.relative(dir);

            if (level.getBlockEntity(adjacentPos) instanceof mio_icif_pipe_item otherPipe) {
                if (otherPipe.mode == PipeMode.TRANSPORT) {
                    if (otherPipe.hasOutputTargetRecursive(visited)) {
                        return true;
                    }
                }
                continue;
            }

            IItemHandler handler = level.getCapability(
                Capabilities.ItemHandler.BLOCK, adjacentPos, dir.getOpposite()
            );

            if (handler != null) {
                if (mode == PipeMode.TRANSPORT) {
                    return true;
                }
            }
        }

        return false;
    }

    public int getMaxBufferSize() {
        return mode == PipeMode.TRANSPORT ? MAX_TRANSFER_RATE : 1;
    }

    @Override
    protected void doTransfer() {
        if (level == null || level.isClientSide()) return;

        if (transferCooldown > 0) {
            transferCooldown--;
            return;
        }

        boolean canTransfer = false;

        if (!bufferItem.isEmpty()) {
            if (mode == PipeMode.TRANSPORT) {
                int transferred = transferItemToNeighbors(bufferItem, bufferFromDirection);
                if (transferred > 0) {
                    bufferItem.shrink(transferred);
                    if (bufferItem.isEmpty()) {
                        bufferItem = ItemStack.EMPTY;
                        bufferFromDirection = null;
                    }
                    transferCooldown = TRANSFER_COOLDOWN;
                    return;
                }

                boolean outputToContainer = outputBufferToContainer();
                if (outputToContainer) {
                    bufferFromDirection = null;
                    transferCooldown = TRANSFER_COOLDOWN;
                    return;
                }
            }
        }

        if (canTransfer || (mode == PipeMode.INPUT && hasOutputTarget())) {
            boolean transferred = extractAndTransferItem();
            if (transferred) {
                transferCooldown = TRANSFER_COOLDOWN;
            }
        }
    }

    protected boolean extractAndTransferItem() {
        List<ItemSource> containerSources = new ArrayList<>(); 
        List<ItemDestination> containerDests = new ArrayList<>(); 
        List<mio_icif_pipe_item> pipeNeighbors = new ArrayList<>(); 

        for (Direction dir : Direction.values()) {
            if (!isConnected(dir)) continue;

            BlockPos adjacentPos = worldPosition.relative(dir);

            if (level.getBlockEntity(adjacentPos) instanceof mio_icif_pipe_item otherPipe) {
                pipeNeighbors.add(otherPipe);
                continue;
            }

            IItemHandler handler = level.getCapability(
                Capabilities.ItemHandler.BLOCK, adjacentPos, dir.getOpposite()
            );

            if (handler == null) continue;

            if (mode == PipeMode.INPUT) {
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    ItemStack available = handler.extractItem(slot, TRANSFER_RATE, true);
                    if (!available.isEmpty() && available.getCount() > 0) {
                        containerSources.add(new ItemSource(adjacentPos, dir, handler, slot, available, false));
                        break;
                    }
                }
            }

            if (mode == PipeMode.TRANSPORT && !bufferItem.isEmpty()) {
                ItemStack testItem = bufferItem.copy();
                ItemStack remainingTest = ItemHandlerHelper.insertItem(handler, testItem, true);
                int canInsert = testItem.getCount() - remainingTest.getCount();
                if (canInsert > 0) {
                    containerDests.add(new ItemDestination(adjacentPos, dir, handler, canInsert, false));
                }
            }
        }

        if (mode == PipeMode.INPUT && !containerSources.isEmpty()) {
            Set<BlockPos> visited = new HashSet<>();
            visited.add(worldPosition);

            List<PipeTarget> acceptingPipes = new ArrayList<>();
            for (mio_icif_pipe_item pipe : pipeNeighbors) {
                if (!pipe.canInsert() || visited.contains(pipe.getBlockPos())) continue;
                if (pipe.isProcessing()) continue;

                Direction toPipeDir = Direction.fromDelta(
                    pipe.getBlockPos().getX() - worldPosition.getX(),
                    pipe.getBlockPos().getY() - worldPosition.getY(),
                    pipe.getBlockPos().getZ() - worldPosition.getZ()
                );
                IItemHandler pipeHandler = pipe.getItemHandlerCapability(toPipeDir != null ? toPipeDir.getOpposite() : null);
                if (pipeHandler == null) continue;

                ItemStack testStack = new ItemStack(net.minecraft.world.item.Items.STONE, 1);
                ItemStack remainingTest = pipeHandler.insertItem(0, testStack, true);
                if (!remainingTest.isEmpty()) continue; 
                acceptingPipes.add(new PipeTarget(pipe, pipeHandler, toPipeDir));
            }

            if (acceptingPipes.isEmpty()) {
                return false;
            }

            if (containerSources.isEmpty()) {
                return false;
            }

            int sourceIndex = inputSourceIndex % containerSources.size();
            ItemSource source = containerSources.get(sourceIndex);

            inputSourceIndex = (inputSourceIndex + 1) % containerSources.size();

            ItemStack extracted = source.handler.extractItem(source.slot, 1, false);
            if (extracted.isEmpty()) {
                return false;
            }

            int targetIndex = roundRobinIndex % acceptingPipes.size();
            PipeTarget target = acceptingPipes.get(targetIndex);

            roundRobinIndex = (roundRobinIndex + 1) % acceptingPipes.size();

            ItemStack toInsert = extracted.copy();
            toInsert.setCount(1);

            target.pipe.setProcessing(true);
            try {
                ItemStack notInserted = target.handler.insertItem(0, toInsert, false);
                if (notInserted.isEmpty()) {
                    return true;
                } else {
                    return false;
                }
            } finally {
                target.pipe.setProcessing(false);
            }
        }

        if (mode == PipeMode.TRANSPORT && !bufferItem.isEmpty() && !containerDests.isEmpty()) {
            int destIndex = roundRobinIndex % containerDests.size();
            ItemDestination dest = containerDests.get(destIndex);

            roundRobinIndex = (roundRobinIndex + 1) % containerDests.size();

            int transferAmount = Math.min(bufferItem.getCount(), MAX_TRANSFER_RATE);
            ItemStack toInsert = bufferItem.copy();
            toInsert.setCount(transferAmount);

            ItemStack notInserted = ItemHandlerHelper.insertItem(dest.handler, toInsert, false);
            int actuallyTransferred = transferAmount - notInserted.getCount();
            if (actuallyTransferred > 0) {
                bufferItem.shrink(actuallyTransferred);
                if (bufferItem.isEmpty()) {
                    bufferItem = ItemStack.EMPTY;
                }
                return true;
            }
        }

        if (mode == PipeMode.TRANSPORT && !bufferItem.isEmpty()) {
            Set<BlockPos> visited = new HashSet<>();
            visited.add(worldPosition);
            
            List<PipeTarget> validTargets = new ArrayList<>();
            
            for (mio_icif_pipe_item pipe : pipeNeighbors) {
                if (visited.contains(pipe.getBlockPos())) continue;
                
                if (!pipe.canInsert()) continue;

                if (pipe.isProcessing()) continue;

                Direction toPipeDir = Direction.fromDelta(
                    pipe.getBlockPos().getX() - worldPosition.getX(),
                    pipe.getBlockPos().getY() - worldPosition.getY(),
                    pipe.getBlockPos().getZ() - worldPosition.getZ()
                );
                IItemHandler pipeHandler = pipe.getItemHandlerCapability(toPipeDir != null ? toPipeDir.getOpposite() : null);
                if (pipeHandler == null) continue;

                int testAmount = Math.min(bufferItem.getCount(), MAX_TRANSFER_RATE);
                ItemStack testStack = bufferItem.copy();
                testStack.setCount(testAmount);
                ItemStack remainingTest = pipeHandler.insertItem(0, testStack, true);
                int canInsert = testAmount - remainingTest.getCount();
                if (canInsert <= 0) continue; 

                validTargets.add(new PipeTarget(pipe, pipeHandler, toPipeDir, canInsert));
            }
            
            if (!validTargets.isEmpty() && !bufferItem.isEmpty()) {
                int targetIndex = roundRobinIndex % validTargets.size();
                PipeTarget target = validTargets.get(targetIndex);
                
                roundRobinIndex = (roundRobinIndex + 1) % validTargets.size();
                
                int transferAmount = Math.min(bufferItem.getCount(), target.maxCanInsert);
                transferAmount = Math.min(transferAmount, MAX_TRANSFER_RATE);
                
                ItemStack toInsert = bufferItem.copy();
                toInsert.setCount(transferAmount);
                
                target.pipe.setProcessing(true);
                try {
                    ItemStack notInserted = target.handler.insertItem(0, toInsert, false);
                    int actuallyTransferred = transferAmount - notInserted.getCount();
                    if (actuallyTransferred > 0) {
                        bufferItem.shrink(actuallyTransferred);
                        if (bufferItem.isEmpty()) {
                            bufferItem = ItemStack.EMPTY;
                        }
                        return true;
                    }
                } finally {
                    target.pipe.setProcessing(false);
                }
            }
        }
        
        return false;
    }

    private boolean outputBufferToContainer() {
        if (bufferItem.isEmpty() || level == null) return false;

        List<ItemDestination> containerDests = new ArrayList<>();

        for (Direction dir : Direction.values()) {
            if (!isConnected(dir)) continue;

            BlockPos adjacentPos = worldPosition.relative(dir);

            if (level.getBlockEntity(adjacentPos) instanceof mio_icif_pipe_item) {
                continue;
            }

            IItemHandler handler = level.getCapability(
                Capabilities.ItemHandler.BLOCK, adjacentPos, dir.getOpposite()
            );

            if (handler == null) continue;

            int testAmount = Math.min(bufferItem.getCount(), MAX_TRANSFER_RATE);
            ItemStack testItem = bufferItem.copy();
            testItem.setCount(testAmount);
            ItemStack remainingTest = ItemHandlerHelper.insertItem(handler, testItem, true);
            int canInsert = testAmount - remainingTest.getCount();
            if (canInsert > 0) {
                containerDests.add(new ItemDestination(adjacentPos, dir, handler, canInsert, false));
            }
        }

        if (containerDests.isEmpty()) return false;

        int destIndex = roundRobinIndex % containerDests.size();
        ItemDestination dest = containerDests.get(destIndex);

        roundRobinIndex = (roundRobinIndex + 1) % containerDests.size();

        int transferAmount = Math.min(bufferItem.getCount(), dest.maxInsert);
        transferAmount = Math.min(transferAmount, MAX_TRANSFER_RATE);

        ItemStack toInsert = bufferItem.copy();
        toInsert.setCount(transferAmount);

        ItemStack notInserted = ItemHandlerHelper.insertItem(dest.handler, toInsert, false);
        int actuallyTransferred = transferAmount - notInserted.getCount();
        if (actuallyTransferred > 0) {
            bufferItem.shrink(actuallyTransferred);
            if (bufferItem.isEmpty()) {
                bufferItem = ItemStack.EMPTY;
            }
            return true;
        }

        return false;
    }

    private int transferItemToNeighbors(ItemStack item, @Nullable Direction fromDirection) {
        return transferItemToNeighbors(item, fromDirection, new HashSet<>());
    }

    private int transferItemToNeighbors(ItemStack item, @Nullable Direction fromDirection, Set<BlockPos> visited) {
        if (item.isEmpty() || item.getCount() <= 0 || level == null) {
            return 0;
        }

        if (visited.contains(worldPosition)) {
            return 0;
        }
        visited.add(worldPosition);

        List<PipeTarget> validTargets = new ArrayList<>();

        for (Direction direction : Direction.values()) {
            if (fromDirection != null && direction == fromDirection) {
                continue;
            }
            if (!isConnected(direction)) continue;

            BlockPos adjacentPos = worldPosition.relative(direction);

            if (!(level.getBlockEntity(adjacentPos) instanceof mio_icif_pipe_item otherPipe)) {
                continue;
            }

            if (visited.contains(adjacentPos)) {
                continue;
            }

            if (!otherPipe.canInsert()) {
                continue;
            }

            if (otherPipe.isProcessing()) {
                continue;
            }

            IItemHandler pipeHandler = otherPipe.getItemHandlerCapability(direction.getOpposite());
            if (pipeHandler == null) continue;

            int testAmount = Math.min(item.getCount(), MAX_TRANSFER_RATE);
            ItemStack testStack = item.copy();
            testStack.setCount(testAmount);
            ItemStack notInsertedTest = pipeHandler.insertItem(0, testStack, true);
            int canInsert = testAmount - notInsertedTest.getCount();
            if (canInsert <= 0) continue;

            validTargets.add(new PipeTarget(otherPipe, pipeHandler, direction, canInsert));
        }

        if (validTargets.isEmpty()) {
            return 0;
        }

        int targetIndex = roundRobinIndex % validTargets.size();
        PipeTarget target = validTargets.get(targetIndex);

        roundRobinIndex = (roundRobinIndex + 1) % validTargets.size();

        int transferAmount = Math.min(item.getCount(), target.maxCanInsert);
        transferAmount = Math.min(transferAmount, MAX_TRANSFER_RATE);

        ItemStack toInsert = item.copy();
        toInsert.setCount(transferAmount);

        target.pipe.setProcessing(true);
        try {
            ItemStack notInserted = target.handler.insertItem(0, toInsert, false);
            int actuallyTransferred = transferAmount - notInserted.getCount();
            if (actuallyTransferred > 0) {
                return actuallyTransferred;
            }
        } finally {
            target.pipe.setProcessing(false);
        }

        return 0;
    }

    @SuppressWarnings("unused")
    private static class PipeTarget {
        final mio_icif_pipe_item pipe;
        final IItemHandler handler;
        final Direction direction;
        final int maxCanInsert;

        PipeTarget(mio_icif_pipe_item pipe, IItemHandler handler, Direction direction) {
            this(pipe, handler, direction, 1);
        }

        PipeTarget(mio_icif_pipe_item pipe, IItemHandler handler, Direction direction, int maxCanInsert) {
            this.pipe = pipe;
            this.handler = handler;
            this.direction = direction;
            this.maxCanInsert = maxCanInsert;
        }
    }

    private void addToBuffer(ItemStack item) {
        if (item.isEmpty()) return;

        if (bufferItem.isEmpty()) {
            bufferItem = item.copy();
        } else if (ItemStack.isSameItem(bufferItem, item)) {
            int canAdd = Math.min(item.getCount(), bufferItem.getMaxStackSize() - bufferItem.getCount());
            if (canAdd > 0) {
                bufferItem.grow(canAdd);
            }
        }
    }

    @Override
    protected boolean canConnectTo(BlockPos pos, Direction direction) {
        if (level == null) return false;

        if (level.getBlockEntity(pos) instanceof mio_icif_pipe_item otherPipe) {
            Direction opposite = direction.getOpposite();
            if (otherPipe.isDirectionBlocked(opposite)) {
                return false;
            }
            return this.canConnectToPipe(otherPipe);
        }

        IItemHandler handler = level.getCapability(
            Capabilities.ItemHandler.BLOCK, pos, direction.getOpposite()
        );

        if (handler != null) {
            return true;
        }

        return false;
    }

    @Override
    public String getPipeTypeString() {
        return PIPE_TYPE;
    }

    @Nullable
    public IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        return new PipeItemHandler(side);
    }

    public ItemStack getBufferItem() {
        return bufferItem.copy();
    }

    public boolean isEmpty() {
        return bufferItem.isEmpty();
    }

    public boolean isFull() {
        return !bufferItem.isEmpty() && bufferItem.getCount() >= bufferItem.getMaxStackSize();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!bufferItem.isEmpty()) {
            tag.put("buffer", bufferItem.save(registries));
        }
        if (bufferFromDirection != null) {
            tag.putString("buffer_from_dir", bufferFromDirection.name());
        }
        tag.putString("mode", mode.name());
        tag.putInt("transferCooldown", transferCooldown);
        tag.putInt("roundRobinIndex", roundRobinIndex);
        tag.putInt("inputSourceIndex", inputSourceIndex);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("buffer", CompoundTag.TAG_COMPOUND)) {
            bufferItem = ItemStack.parse(registries, tag.getCompound("buffer")).orElse(ItemStack.EMPTY);
        }
        if (tag.contains("buffer_from_dir")) {
            try {
                bufferFromDirection = Direction.valueOf(tag.getString("buffer_from_dir"));
            } catch (IllegalArgumentException e) {
                bufferFromDirection = null;
            }
        }
        if (tag.contains("mode")) {
            try {
                mode = PipeMode.valueOf(tag.getString("mode"));
            } catch (IllegalArgumentException e) {
                mode = PipeMode.TRANSPORT;
            }
        }
        if (tag.contains("transferCooldown")) {
            transferCooldown = tag.getInt("transferCooldown");
        }
        if (tag.contains("roundRobinIndex")) {
            roundRobinIndex = tag.getInt("roundRobinIndex");
        }
        if (tag.contains("inputSourceIndex")) {
            inputSourceIndex = tag.getInt("inputSourceIndex");
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_pipe_item blockEntity) {
        mio_icif_pipe_default.tick(level, pos, state, blockEntity);
    }

    @SuppressWarnings("unused")
    private static class ItemSource {
        final BlockPos pos;
        final Direction direction;
        final IItemHandler handler;
        final int slot;
        final ItemStack item;
        final boolean isPipe;

        ItemSource(BlockPos pos, Direction direction, IItemHandler handler, int slot, ItemStack item, boolean isPipe) {
            this.pos = pos;
            this.direction = direction;
            this.handler = handler;
            this.slot = slot;
            this.item = item;
            this.isPipe = isPipe;
        }
    }

    @SuppressWarnings("unused")
    private static class ItemDestination {
        final BlockPos pos;
        final Direction direction;
        final IItemHandler handler;
        final int maxInsert;
        final boolean isPipe;

        ItemDestination(BlockPos pos, Direction direction, IItemHandler handler, int maxInsert, boolean isPipe) {
            this.pos = pos;
            this.direction = direction;
            this.handler = handler;
            this.maxInsert = maxInsert;
            this.isPipe = isPipe;
        }
    }

    private class PipeItemHandler implements IItemHandler {
        private final Direction inputDirection;

        public PipeItemHandler(@Nullable Direction inputDirection) {
            this.inputDirection = inputDirection;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return bufferItem.copy();
        }

        @Override
        public int getSlotLimit(int slot) {
            return getMaxBufferSize();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return ItemStack.EMPTY;

            if (!canInsert()) {
                return stack;
            }

            if (mode == PipeMode.TRANSPORT && !hasOutputTarget()) {
                return stack;
            }

            int maxBuffer = getMaxBufferSize();

            if (simulate) {
                if (bufferItem.isEmpty()) {
                    int canAccept = Math.min(stack.getCount(), maxBuffer);
                    if (canAccept >= stack.getCount()) {
                        return ItemStack.EMPTY; 
                    } else {
                        ItemStack remaining = stack.copy();
                        remaining.setCount(stack.getCount() - canAccept);
                        return remaining;
                    }
                } else if (ItemStack.isSameItem(bufferItem, stack)) {
                    int canAdd = Math.min(stack.getCount(), maxBuffer - bufferItem.getCount());
                    if (canAdd >= stack.getCount()) {
                        return ItemStack.EMPTY; 
                    } else if (canAdd > 0) {
                        ItemStack remaining = stack.copy();
                        remaining.setCount(stack.getCount() - canAdd);
                        return remaining;
                    } else {
                        return stack; 
                    }
                } else {
                    return stack;
                }
            }

            if (mode == PipeMode.TRANSPORT) {
                int transferred = transferItemToNeighbors(stack, inputDirection);
                if (transferred < stack.getCount()) {
                    ItemStack remaining = stack.copy();
                    remaining.setCount(stack.getCount() - transferred);
                    if (!remaining.isEmpty()) {
                        if (bufferItem.isEmpty()) {
                            ItemStack toBuffer = remaining.copy();
                            toBuffer.setCount(Math.min(toBuffer.getCount(), maxBuffer));
                            addToBuffer(toBuffer);
                            remaining.shrink(toBuffer.getCount());
                            bufferFromDirection = inputDirection;
                        } else if (ItemStack.isSameItem(bufferItem, remaining)) {
                            int canAdd = Math.min(remaining.getCount(), maxBuffer - bufferItem.getCount());
                            if (canAdd > 0) {
                                bufferItem.grow(canAdd);
                                remaining.shrink(canAdd);
                                setChanged();
                            }
                        }
                    }
                    if (remaining.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                    return remaining;
                }
                return ItemStack.EMPTY;
            }

            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!canExtract()) {
                return ItemStack.EMPTY;
            }

            if (bufferItem.isEmpty() || amount <= 0) return ItemStack.EMPTY;

            int toExtract = Math.min(amount, bufferItem.getCount());
            ItemStack extracted = bufferItem.copy();
            extracted.setCount(toExtract);

            if (!simulate) {
                bufferItem.shrink(toExtract);
                if (bufferItem.isEmpty()) {
                    bufferItem = ItemStack.EMPTY;
                }
            }
            return extracted;
        }
    }
}

