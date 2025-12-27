package com.azukaar.ass.trees.farmer;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import com.azukaar.ass.AzukaarSkillsStats;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Utility for cutting trees over time to avoid lag
 */
public class TreeCutter {

    private static final int MAX_BLOCKS_PER_TREE = 64;

    // Active tree cutting operations
    private static final Map<UUID, TreeCutOperation> activeOperations = new HashMap<>();

    /**
     * Get the base position of a tree
     */
    public static BlockPos getTreeBasePos(ServerLevel level, BlockPos startPos) {
        BlockState state = level.getBlockState(startPos);
        if (!state.is(BlockTags.LOGS)) {
            return null;
        }
        BlockPos basePos = findTreeBase(level, startPos);
        if (basePos == null || !isValidTree(level, basePos)) {
            return null;
        }
        return basePos;
    }

    /**
     * Start cutting a tree at the given position
     * @param level The server level
     * @param startPos The starting position (should be a log block)
     * @return true if a valid tree was found and cutting started
     */
    public static boolean startCutting(ServerLevel level, BlockPos startPos) {
        // Verify it's a log block
        BlockState state = level.getBlockState(startPos);
        if (!state.is(BlockTags.LOGS)) {
            return false;
        }

        // Find the base of the tree (lowest connected log)
        BlockPos basePos = findTreeBase(level, startPos);
        if (basePos == null) {
            return false;
        }

        // Verify it's a valid tree (at least 3 stacked logs)
        if (!isValidTree(level, basePos)) {
            AzukaarSkillsStats.LOGGER.info("Not a valid tree at {}", basePos);
            return false;
        }

        // Find all blocks to cut
        Set<BlockPos> blocksToCut = findTreeBlocks(level, basePos);
        if (blocksToCut.isEmpty()) {
            return false;
        }

        // Create operation
        UUID operationId = UUID.randomUUID();
        TreeCutOperation operation = new TreeCutOperation(level, blocksToCut);
        activeOperations.put(operationId, operation);

        AzukaarSkillsStats.LOGGER.info("Started tree cutting operation {} with {} blocks", operationId, blocksToCut.size());
        return true;
    }

    /**
     * Find the base (lowest log) of a tree
     */
    private static BlockPos findTreeBase(ServerLevel level, BlockPos startPos) {
        BlockPos currentPos = startPos;

        // Go down until we find a non-log block
        while (level.getBlockState(currentPos.below()).is(BlockTags.LOGS)) {
            currentPos = currentPos.below();
        }

        return currentPos;
    }

    /**
     * Check if this is a valid tree (at least 3 stacked logs)
     */
    private static boolean isValidTree(ServerLevel level, BlockPos basePos) {
        int logCount = 0;
        BlockPos checkPos = basePos;

        // Count stacked logs
        while (level.getBlockState(checkPos).is(BlockTags.LOGS) && logCount < 3) {
            logCount++;
            checkPos = checkPos.above();
        }

        return logCount >= 3;
    }

    /**
     * Find all tree blocks (logs and leaves) connected to the base
     */
    private static Set<BlockPos> findTreeBlocks(ServerLevel level, BlockPos basePos) {
        Set<BlockPos> result = new HashSet<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> toCheck = new ArrayDeque<>();

        // Start with the base position
        toCheck.add(basePos);

        // First pass: find all logs
        while (!toCheck.isEmpty() && result.size() < MAX_BLOCKS_PER_TREE) {
            BlockPos pos = toCheck.poll();

            if (visited.contains(pos)) continue;
            visited.add(pos);

            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockTags.LOGS)) continue;

            result.add(pos);

            // Check 3x3 area around this log (and above)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = 0; dy <= 1; dy++) {
                        BlockPos neighbor = pos.offset(dx, dy, dz);
                        if (!visited.contains(neighbor)) {
                            toCheck.add(neighbor);
                        }
                    }
                }
            }
        }

        // Second pass: find leaves connected to logs
        Set<BlockPos> logPositions = new HashSet<>(result);
        toCheck.clear();
        visited.clear();

        // Start from all log positions
        for (BlockPos logPos : logPositions) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        toCheck.add(logPos.offset(dx, dy, dz));
                    }
                }
            }
        }

        // Find connected leaves
        while (!toCheck.isEmpty() && result.size() < MAX_BLOCKS_PER_TREE) {
            BlockPos pos = toCheck.poll();

            if (visited.contains(pos)) continue;
            if (logPositions.contains(pos)) continue; // Skip logs
            visited.add(pos);

            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockTags.LEAVES)) continue;

            result.add(pos);

            // Check 3x3 area around this leaf
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        BlockPos neighbor = pos.offset(dx, dy, dz);
                        if (!visited.contains(neighbor) && !logPositions.contains(neighbor)) {
                            toCheck.add(neighbor);
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Tick handler - process one block per operation per tick
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (activeOperations.isEmpty()) return;

        // Process each operation
        activeOperations.entrySet().removeIf(entry -> {
            TreeCutOperation op = entry.getValue();
            boolean finished = op.tick();
            if (finished) {
                AzukaarSkillsStats.LOGGER.info("Tree cutting operation {} completed", entry.getKey());
            }
            return finished;
        });
    }

    /**
     * Represents an active tree cutting operation
     */
    private static class TreeCutOperation {
        private final ServerLevel level;
        private final Queue<BlockPos> blocksToRemove;

        public TreeCutOperation(ServerLevel level, Set<BlockPos> blocks) {
            this.level = level;
            this.blocksToRemove = new ArrayDeque<>(blocks);
        }

        /**
         * Process one block
         * @return true if operation is complete
         */
        public boolean tick() {
            if (blocksToRemove.isEmpty()) {
                return true;
            }

            BlockPos pos = blocksToRemove.poll();
            BlockState state = level.getBlockState(pos);

            // Only break if still a log or leaf
            if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
                Block.dropResources(state, level, pos);
                level.destroyBlock(pos, false);
            }

            return blocksToRemove.isEmpty();
        }
    }
}
