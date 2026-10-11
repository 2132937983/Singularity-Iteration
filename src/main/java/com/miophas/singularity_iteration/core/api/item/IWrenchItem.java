package com.miophas.singularity_iteration.core.api.item;

/**
 * Marker interface for wrench items.
 * <p>
 * An item that implements this interface is recognized as a wrench.
 * A wrench rotates the block facing or removes machines.
 * <p>
 * Note: this is a marker interface. The item implementation class defines the behavior.
 * A block uses the {@link com.miophas.singularity_iteration.core.api.tool.IWrenchable} interface
 * to declare that a wrench can interact with it.
 */
public interface IWrenchItem {
}
