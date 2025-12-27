package com.azukaar.ass.trees.farmer;

import com.azukaar.ass.AzukaarSkillsStats;
import com.azukaar.ass.api.ActiveSkillEffect;
import com.azukaar.ass.api.ActiveSkillEffectRegistry;
import com.azukaar.ass.api.EffectData;
import com.azukaar.ass.api.PlayerData;
import com.azukaar.ass.types.SkillEffect;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Active effects for the Farmer skill tree
 */
public class ActiveEffects {

    private static final String NATURES_DOMAIN_SKILL = "azukaarskillsstats:natures_domain";
    private static final String NATURES_DOMAIN_EFFECT = "azukaarskillsstats:natures_domain";
    private static final String HARVEST_AREA_SKILL = "azukaarskillsstats:harvest_area";
    private static final String HARVEST_AREA_EFFECT = "azukaarskillsstats:harvest_area";
    private static final String FORAGERS_APPETITE_SKILL = "azukaarskillsstats:foragers_appetite";
    private static final String PLANTING_AREA_SKILL = "azukaarskillsstats:planting_area";
    private static final String PLANTING_AREA_EFFECT = "azukaarskillsstats:planting_area";
    private static final String SEED_MASTER_SKILL = "azukaarskillsstats:seed_master";
    private static final String SEED_MASTER_EFFECT = "azukaarskillsstats:seed_master";

    public static void registerAll() {
        ActiveSkillEffectRegistry.register(
            ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "animal_whisperer"),
            new AnimalWhispererEffect()
        );

        ActiveSkillEffectRegistry.register(
            ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "green_touch"),
            new GreenTouchEffect()
        );

        ActiveSkillEffectRegistry.register(
            ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "harvest_sweep"),
            new HarvestSweepEffect()
        );

        ActiveSkillEffectRegistry.register(
            ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "fast_planting"),
            new FastPlantingEffect()
        );
    }

    public static class AnimalWhispererEffect implements ActiveSkillEffect {
        @Override
        public boolean execute(Player player, String skillId, int skillLevel, EffectData effectData) {
            // Duration in ticks (2 minutes = 2400 ticks)
            int duration = (int) effectData.getDouble("duration", 2400);

            // Amplifier = skill level - 1 (so level 1 = amp 0, level 5 = amp 4)
            int amplifier = skillLevel - 1;

            // Apply the Animal Whisperer mob effect
            player.addEffect(new MobEffectInstance(
                MobEffects.ANIMAL_WHISPERER,
                duration,
                amplifier,
                false,  // ambient
                true,   // visible
                true    // show icon
            ));

            AzukaarSkillsStats.LOGGER.info("Player {} activated Animal Whisperer at level {} for {} ticks",
                player.getName().getString(), skillLevel, duration);

            return true;
        }
    }

    public static class GreenTouchEffect implements ActiveSkillEffect {
        @Override
        public boolean execute(Player player, String skillId, int skillLevel, EffectData effectData) {
            if (!(player.level() instanceof ServerLevel level)) {
                return false;
            }

            // Get the block the player is looking at
            HitResult hitResult = player.pick(5.0D, 0.0F, false);
            if (hitResult.getType() != HitResult.Type.BLOCK) {
                return false;
            }

            BlockPos targetPos = ((BlockHitResult) hitResult).getBlockPos();
            BlockState targetState = level.getBlockState(targetPos);

            // Check if player has Nature's Domain skill
            int domainLevel = PlayerData.getSkillLevel(player, NATURES_DOMAIN_SKILL);

            if (domainLevel > 0) {
                // Get area from Nature's Domain effect (base 3, +2 per level)
                int area = (int) SkillEffect.getSkillParameter(player, NATURES_DOMAIN_SKILL, NATURES_DOMAIN_EFFECT, "area");
                int radius = area / 2;

                // Mature all crops in the area
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos pos = targetPos.offset(x, 0, z);
                        BlockState state = level.getBlockState(pos);
                        Block block = state.getBlock();

                        if (block instanceof CropBlock crop && !crop.isMaxAge(state)) {
                            level.setBlock(pos, crop.getStateForAge(crop.getMaxAge()), 2);
                        }
                    }
                }

                AzukaarSkillsStats.LOGGER.info("Player {} used Green Touch with Nature's Domain (area {}x{})",
                    player.getName().getString(), area, area);
            } else {
                // Check if target is a growable crop
                if (!(targetState.getBlock() instanceof CropBlock)) {
                    return false;
                }
                
                // Single crop - directly mature without loop
                CropBlock crop = (CropBlock) targetState.getBlock();
                if (!crop.isMaxAge(targetState)) {
                    level.setBlock(targetPos, crop.getStateForAge(crop.getMaxAge()), 2);
                }

                AzukaarSkillsStats.LOGGER.info("Player {} used Green Touch on single crop",
                    player.getName().getString());
            }

            return true;
        }
    }

    public static class HarvestSweepEffect implements ActiveSkillEffect {
        @Override
        public boolean execute(Player player, String skillId, int skillLevel, EffectData effectData) {
            if (!(player.level() instanceof ServerLevel level)) {
                return false;
            }

            BlockPos playerPos = player.blockPosition();

            // Check if player has Harvest Area skill for larger radius
            int harvestAreaLevel = PlayerData.getSkillLevel(player, HARVEST_AREA_SKILL);

            int area;
            if (harvestAreaLevel > 0) {
                // Get area from Harvest Area effect (base 5, +2 per level)
                area = (int) SkillEffect.getSkillParameter(player, HARVEST_AREA_SKILL, HARVEST_AREA_EFFECT, "area");
            } else {
                // Base 5x5 area
                area = 5;
            }

            int radius = area / 2;
            int harvestedCount = 0;

            // Harvest all mature crops in the area centered on player
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    // Check a few Y levels around player
                    for (int y = -1; y <= 1; y++) {
                        BlockPos pos = playerPos.offset(x, y, z);
                        BlockState state = level.getBlockState(pos);
                        Block block = state.getBlock();

                        if (block instanceof CropBlock crop && crop.isMaxAge(state)) {
                            // Drop the crop items
                            Block.dropResources(state, level, pos);
                            // Replant (reset to age 0)
                            level.setBlock(pos, crop.getStateForAge(0), 2);
                            harvestedCount++;
                        }
                    }
                }
            }

            // Apply Forager's Appetite buff if player has the skill
            if (PlayerData.getSkillLevel(player, FORAGERS_APPETITE_SKILL) > 0) {
                player.addEffect(new MobEffectInstance(
                    MobEffects.FORAGERS_APPETITE,
                    1200,  // 1 minute
                    0,
                    false,
                    true,
                    true
                ));
            }

            AzukaarSkillsStats.LOGGER.info("Player {} used Harvest Sweep ({}x{} area), harvested {} crops",
                player.getName().getString(), area, area, harvestedCount);

            return harvestedCount > 0;
        }
    }

    public static class FastPlantingEffect implements ActiveSkillEffect {
        private int seedCursor = 0;

        @Override
        public boolean execute(Player player, String skillId, int skillLevel, EffectData effectData) {
            if (!(player.level() instanceof ServerLevel level)) {
                return false;
            }

            BlockPos playerPos = player.blockPosition();

            // Check if player has Planting Area skill for larger radius
            int plantingAreaLevel = PlayerData.getSkillLevel(player, PLANTING_AREA_SKILL);

            int area;
            if (plantingAreaLevel > 0) {
                area = (int) SkillEffect.getSkillParameter(player, PLANTING_AREA_SKILL, PLANTING_AREA_EFFECT, "area");
            } else {
                area = 3;
            }

            // Get seed retention chance from Seed Master
            int seedMasterLevel = PlayerData.getSkillLevel(player, SEED_MASTER_SKILL);
            double retentionChance = 0;
            if (seedMasterLevel > 0) {
                retentionChance = SkillEffect.getSkillParameter(player, SEED_MASTER_SKILL, SEED_MASTER_EFFECT, "retention") / 100.0;
            }

            // Build list of seed stacks in inventory
            java.util.List<net.minecraft.world.item.ItemStack> seedStacks = new java.util.ArrayList<>();
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty() && stack.is(net.minecraft.tags.ItemTags.VILLAGER_PLANTABLE_SEEDS)) {
                    seedStacks.add(stack);
                }
            }

            if (seedStacks.isEmpty()) {
                return false;
            }

            int radius = area / 2;
            int plantedCount = 0;

            // Plant crops in the area centered on player
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos groundPos = playerPos.offset(x, -1, z);
                    BlockPos cropPos = playerPos.offset(x, 0, z);

                    // Skip if there's already something at crop position
                    if (!level.getBlockState(cropPos).isAir()) continue;

                    BlockState groundState = level.getBlockState(groundPos);
                    Block groundBlock = groundState.getBlock();

                    // Convert dirt/grass to farmland, or skip if already farmland
                    if (groundBlock == net.minecraft.world.level.block.Blocks.DIRT ||
                        groundBlock == net.minecraft.world.level.block.Blocks.GRASS_BLOCK ||
                        groundBlock == net.minecraft.world.level.block.Blocks.DIRT_PATH ||
                        groundBlock == net.minecraft.world.level.block.Blocks.COARSE_DIRT ||
                        groundBlock == net.minecraft.world.level.block.Blocks.ROOTED_DIRT) {
                        level.setBlock(groundPos, net.minecraft.world.level.block.Blocks.FARMLAND.defaultBlockState(), 2);
                    } else if (groundBlock != net.minecraft.world.level.block.Blocks.FARMLAND) {
                        continue;
                    }

                    // Find next available seed using cursor
                    net.minecraft.world.item.ItemStack seedStack = null;
                    int attempts = 0;
                    while (attempts < seedStacks.size()) {
                        seedCursor = seedCursor % seedStacks.size();
                        net.minecraft.world.item.ItemStack candidate = seedStacks.get(seedCursor);
                        seedCursor++;
                        attempts++;

                        if (!candidate.isEmpty()) {
                            seedStack = candidate;
                            break;
                        }
                    }

                    if (seedStack == null || seedStack.isEmpty()) continue;

                    // Simulate right-click on the farmland
                    BlockHitResult hitResult = new BlockHitResult(
                        groundPos.getCenter(),
                        net.minecraft.core.Direction.UP,
                        groundPos,
                        false
                    );

                    net.minecraft.world.item.context.UseOnContext context = new net.minecraft.world.item.context.UseOnContext(
                        level, player, net.minecraft.world.InteractionHand.MAIN_HAND, seedStack, hitResult
                    );

                    net.minecraft.world.InteractionResult result = seedStack.useOn(context);

                    if (result.consumesAction()) {
                        plantedCount++;

                        // If seed was consumed but we have retention, restore it
                        if (retentionChance > 0 && level.random.nextDouble() < retentionChance) {
                            seedStack.grow(1);
                        }

                        // Remove empty stacks from list
                        if (seedStack.isEmpty()) {
                            seedStacks.remove(seedStack);
                            if (seedStacks.isEmpty()) break;
                        }
                    }
                }
                if (seedStacks.isEmpty()) break;
            }

            AzukaarSkillsStats.LOGGER.info("Player {} used Fast Planting ({}x{} area), planted {} crops",
                player.getName().getString(), area, area, plantedCount);

            return plantedCount > 0;
        }
    }
}
