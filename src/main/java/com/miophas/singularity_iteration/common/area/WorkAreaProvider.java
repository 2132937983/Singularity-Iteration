package com.miophas.singularity_iteration.common.area;

import java.util.List;

/**
 * Implemented by block entities that act on a region of the world (miners, pumps,
 * defence towers, collectors...). The Area Preview asks the <b>server-side</b> entity,
 * so implementations may read server-only state such as installed scanners or upgrades.
 * Addons can implement it on their own block entities to join the preview.
 */
public interface WorkAreaProvider {
    /** Current work regions in world space; empty when the machine has no range right now. */
    List<WorkArea> workAreas();
}
