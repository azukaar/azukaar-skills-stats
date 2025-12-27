package com.azukaar.ass.trees.farmer;

import java.util.List;

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
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
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
    private static final String HERD_COMMUNION_SKILL = "azukaarskillsstats:herd_communion";
    private static final String HERD_COMMUNION_EFFECT = "azukaarskillsstats:herd_communion";
    private static final String BREEDING_AREA_SKILL = "azukaarskillsstats:breeding_area";
    private static final String BREEDING_AREA_EFFECT = "azukaarskillsstats:breeding_area";
    private static final String SAPLING_NURTURING_SKILL = "azukaarskillsstats:sapling_nurturing";
    private static final String FOREST_COMMUNION_SKILL = "azukaarskillsstats:forest_communion";
    private static final String NATURE_RENEWAL_SKILL = "azukaarskillsstats:nature_renewal";
    private static final String NATURE_RENEWAL_EFFECT = "azukaarskillsstats:nature_renewal";

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

        ActiveSkillEffectRegistry.register(
            ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "mass_breeding"),
            new MassBreedingEffect()
        );

        ActiveSkillEffectRegistry.register(
            ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "timber"),
            new TimberEffect()
        );

        ActiveSkillEffectRegistry.register(
            ResourceLocation.fromNamespaceAndPath(AzukaarSkillsStats.MODID, "tree_calling"),
            new TreeCallingEffect()
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

            // Check if player has Sapling Nurturing skill
            boolean canGrowSaplings = PlayerData.getSkillLevel(player, SAPLING_NURTURING_SKILL) > 0;

            // Check if player has Nature's Domain skill
            int domainLevel = PlayerData.getSkillLevel(player, NATURES_DOMAIN_SKILL);

            if (domainLevel > 0) {
                BlockPos playerPos = player.getOnPos();
                playerPos = playerPos.offset(0, 1, 0);

                // Get area from Nature's Domain effect (base 3, +2 per level)
                int area = (int) SkillEffect.getSkillParameter(player, NATURES_DOMAIN_SKILL, NATURES_DOMAIN_EFFECT, "area");
                int radius = area / 2;

                // Mature all crops (and saplings if skill unlocked) in the area
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        for (int y = -1; y <= 1; y++) {
                            BlockPos pos = playerPos.offset(x, y, z);
                            BlockState state = level.getBlockState(pos);
                            Block block = state.getBlock();

                            if (block instanceof CropBlock crop && !crop.isMaxAge(state)) {
                                level.setBlock(pos, crop.getStateForAge(crop.getMaxAge()), 2);
                            } else if (canGrowSaplings && block instanceof net.minecraft.world.level.block.SaplingBlock sapling) {
                                // Force grow the sapling into a tree
                                sapling.advanceTree(level, pos, state, level.random);
                            }
                        }
                    }
                }

                AzukaarSkillsStats.LOGGER.info("Player {} used Green Touch with Nature's Domain (area {}x{})",
                    player.getName().getString(), area, area);
            } else {
                // Get the block the player is looking at
                HitResult hitResult = player.pick(5.0D, 0.0F, false);
                if (hitResult.getType() != HitResult.Type.BLOCK) {
                    return false;
                }

                BlockPos targetPos = ((BlockHitResult) hitResult).getBlockPos();
                BlockState targetState = level.getBlockState(targetPos);
                Block targetBlock = targetState.getBlock();

                // Check if target is a growable crop or sapling
                if (targetBlock instanceof CropBlock crop) {
                    if (!crop.isMaxAge(targetState)) {
                        level.setBlock(targetPos, crop.getStateForAge(crop.getMaxAge()), 2);
                    }
                } else if (canGrowSaplings && targetBlock instanceof net.minecraft.world.level.block.SaplingBlock sapling) {
                    sapling.advanceTree(level, targetPos, targetState, level.random);
                } else {
                    return false;
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

            BlockPos playerPos = player.getOnPos();

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

            // Check for Nature Renewal replanting
            int renewalLevel = PlayerData.getSkillLevel(player, NATURE_RENEWAL_SKILL);
            double replantChance = 0;
            if (renewalLevel > 0) {
                replantChance = SkillEffect.getSkillParameter(player, NATURE_RENEWAL_SKILL, NATURE_RENEWAL_EFFECT, "replant_chance") / 100.0;
            }

            // Harvest all mature crops in the area centered on player
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    // Check a few Y levels around player
                    for (int y = -1; y <= 1; y++) {
                        BlockPos pos = playerPos.offset(x, y + 1, z);
                        BlockState state = level.getBlockState(pos);
                        Block block = state.getBlock();

                        if (block instanceof CropBlock crop && crop.isMaxAge(state)) {
                            // Drop the crop items
                            Block.dropResources(state, level, pos);

                            // Replant if Nature Renewal and roll succeeds
                            if (replantChance > 0 && level.random.nextDouble() < replantChance) {
                                level.setBlock(pos, crop.getStateForAge(0), 2);
                            } else {
                                level.removeBlock(pos, false);
                            }

                            harvestedCount++;
                        }
                    }
                }
            }

            // Herd Communion: Also loot passive animals in the area
            int herdCommunionLevel = PlayerData.getSkillLevel(player, HERD_COMMUNION_SKILL);
            int animalsLooted = 0;
            if (herdCommunionLevel > 0) {
                double lootChance = SkillEffect.getSkillParameter(player, HERD_COMMUNION_SKILL, HERD_COMMUNION_EFFECT, "loot_chance") / 100.0;

                // Find all animals in the sweep area
                AABB searchBox = new AABB(
                    playerPos.getX() - radius, playerPos.getY() - 1, playerPos.getZ() - radius,
                    playerPos.getX() + radius + 1, playerPos.getY() + 3, playerPos.getZ() + radius + 1
                );

                List<Animal> animals = level.getEntitiesOfClass(Animal.class, searchBox, animal -> !animal.isBaby());

                for (Animal animal : animals) {
                    // Decide if animal survives (Nature Renewal can save it)
                    boolean animalSurvives = replantChance > 0 && level.random.nextDouble() < replantChance;

                    if (animalSurvives) {
                        // Animal survives - roll for loot chance
                        if (level.random.nextDouble() >= lootChance) continue;

                        // Generate loot from the animal's loot table
                        net.minecraft.resources.ResourceKey<LootTable> lootTableKey = animal.getLootTable();
                        if (lootTableKey == null) continue;

                        LootTable lootTable = level.getServer().reloadableRegistries()
                            .getLootTable(lootTableKey);

                        LootParams.Builder paramsBuilder = new LootParams.Builder(level)
                            .withParameter(LootContextParams.THIS_ENTITY, animal)
                            .withParameter(LootContextParams.ORIGIN, animal.position())
                            .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(player))
                            .withParameter(LootContextParams.ATTACKING_ENTITY, player)
                            .withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, player)
                            .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player);

                        LootParams params = paramsBuilder.create(LootContextParamSets.ENTITY);
                        List<ItemStack> loot = lootTable.getRandomItems(params);

                        // Drop the loot at animal's position
                        for (ItemStack stack : loot) {
                            ItemEntity itemEntity = new ItemEntity(
                                level,
                                animal.getX(), animal.getY(), animal.getZ(),
                                stack
                            );
                            level.addFreshEntity(itemEntity);
                        }
                    } else {
                        // Animal dies - kill() handles loot drop naturally
                        animal.kill();
                    }

                    animalsLooted++;
                }
            }

            // Forest Communion: Also harvest trees in the area
            int forestCommunionLevel = PlayerData.getSkillLevel(player, FOREST_COMMUNION_SKILL);
            int treesHarvested = 0;
            if (forestCommunionLevel > 0) {
                // Find logs in the sweep area and start cutting trees
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        for (int y = -1; y <= 3; y++) {
                            BlockPos pos = playerPos.offset(x, y + 1, z);
                            BlockState state = level.getBlockState(pos);

                            if (state.is(net.minecraft.tags.BlockTags.LOGS)) {
                                // Get tree base for replanting before cutting
                                BlockPos basePos = TreeCutter.getTreeBasePos(level, pos);
                                BlockState logState = basePos != null ? level.getBlockState(basePos) : null;

                                // Try to start cutting this tree
                                if (TreeCutter.startCutting(level, pos)) {
                                    treesHarvested++;

                                    // Replant sapling if Nature Renewal
                                    if (replantChance > 0 && basePos != null && logState != null) {
                                        if (level.random.nextDouble() < replantChance) {
                                            Block saplingBlock = getSaplingForLog(logState.getBlock());
                                            if (saplingBlock != null) {
                                                level.setBlock(basePos, saplingBlock.defaultBlockState(), 2);
                                            }
                                        }
                                    }
                                }
                            }
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

            AzukaarSkillsStats.LOGGER.info("Player {} used Harvest Sweep ({}x{} area), harvested {} crops, looted {} animals, harvested {} trees",
                player.getName().getString(), area, area, harvestedCount, animalsLooted, treesHarvested);

            return harvestedCount > 0 || animalsLooted > 0 || treesHarvested > 0;
        }

        private static Block getSaplingForLog(Block logBlock) {
            String logName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(logBlock).getPath();
            String saplingName = logName.replace("_log", "_sapling").replace("_wood", "_sapling");
            return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                net.minecraft.resources.ResourceLocation.withDefaultNamespace(saplingName)
            );
        }
    }

    public static class FastPlantingEffect implements ActiveSkillEffect {
        private int seedCursor = 0;

        @Override
        public boolean execute(Player player, String skillId, int skillLevel, EffectData effectData) {
            if (!(player.level() instanceof ServerLevel level)) {
                return false;
            }

            BlockPos playerPos = player.getOnPos();

            // Check if player has Sapling Nurturing skill
            boolean canPlantSaplings = PlayerData.getSkillLevel(player, SAPLING_NURTURING_SKILL) > 0;

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

            // Build list of plantable stacks in inventory (seeds + saplings if unlocked)
            java.util.List<net.minecraft.world.item.ItemStack> seedStacks = new java.util.ArrayList<>();
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty()) {
                    if (stack.is(net.minecraft.tags.ItemTags.VILLAGER_PLANTABLE_SEEDS)) {
                        seedStacks.add(stack);
                    } else if (canPlantSaplings && stack.is(net.minecraft.tags.ItemTags.SAPLINGS)) {
                        seedStacks.add(stack);
                    }
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
                    for (int y = -1; y <= 1; y++) {
                        BlockPos groundPos = playerPos.offset(x, y, z);
                        BlockPos cropPos = playerPos.offset(x, y + 1, z);

                        // Skip if there's already something at crop position
                        if (!level.getBlockState(cropPos).isAir()) continue;

                        BlockState groundState = level.getBlockState(groundPos);
                        Block groundBlock = groundState.getBlock();

                        // Find next available seed/sapling using cursor
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

                        boolean isSapling = seedStack.is(net.minecraft.tags.ItemTags.SAPLINGS);

                        // Handle ground preparation based on item type
                        if (isSapling) {
                            // Saplings need dirt or grass
                            if (groundBlock != net.minecraft.world.level.block.Blocks.DIRT &&
                                groundBlock != net.minecraft.world.level.block.Blocks.GRASS_BLOCK &&
                                groundBlock != net.minecraft.world.level.block.Blocks.PODZOL &&
                                groundBlock != net.minecraft.world.level.block.Blocks.MYCELIUM) {
                                continue;
                            }
                        } else {
                            // Seeds need farmland - convert dirt/grass if needed
                            if (groundBlock == net.minecraft.world.level.block.Blocks.DIRT ||
                                groundBlock == net.minecraft.world.level.block.Blocks.GRASS_BLOCK ||
                                groundBlock == net.minecraft.world.level.block.Blocks.DIRT_PATH ||
                                groundBlock == net.minecraft.world.level.block.Blocks.COARSE_DIRT ||
                                groundBlock == net.minecraft.world.level.block.Blocks.ROOTED_DIRT) {
                                level.setBlock(groundPos, net.minecraft.world.level.block.Blocks.FARMLAND.defaultBlockState(), 2);
                            } else if (groundBlock != net.minecraft.world.level.block.Blocks.FARMLAND) {
                                continue;
                            }
                        }

                        // Simulate right-click on the ground
                        BlockHitResult hitResult = new BlockHitResult(
                            groundPos.getCenter(),
                            net.minecraft.core.Direction.UP,
                            groundPos,
                            false
                        );

                        net.minecraft.world.item.context.UseOnContext context = new net.minecraft.world.item.context.UseOnContext(
                            level, player, net.minecraft.world.InteractionHand.MAIN_HAND, seedStack, hitResult
                        );

                        int countBefore = seedStack.getCount();
                        net.minecraft.world.InteractionResult result = seedStack.useOn(context);
                        int countAfter = seedStack.getCount();

                        if (result.consumesAction()) {
                            plantedCount++;

                            // Only restore if item was actually consumed
                            if (countAfter < countBefore && retentionChance > 0 && level.random.nextDouble() < retentionChance) {
                                seedStack.grow(1);
                            }

                            // Remove empty stacks from list
                            if (seedStack.isEmpty()) {
                                seedStacks.remove(seedStack);
                                if (seedStacks.isEmpty()) break;
                            }
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

    public static class MassBreedingEffect implements ActiveSkillEffect {
        @Override
        public boolean execute(Player player, String skillId, int skillLevel, EffectData effectData) {
            if (!(player.level() instanceof ServerLevel level)) {
                return false;
            }

            // Get base radius from effect data
            int radius = (int) effectData.getDouble("radius", 10);

            // Check for Breeding Area bonus
            int breedingAreaLevel = PlayerData.getSkillLevel(player, BREEDING_AREA_SKILL);
            if (breedingAreaLevel > 0) {
                int radiusBonus = (int) SkillEffect.getSkillParameter(player, BREEDING_AREA_SKILL, BREEDING_AREA_EFFECT, "radius_bonus");
                radius += radiusBonus;
            }

            // Find all adult animals in radius (exclude babies and already in love)
            AABB searchBox = player.getBoundingBox().inflate(radius);
            List<Animal> animals = level.getEntitiesOfClass(Animal.class, searchBox, animal ->
                !animal.isBaby() && !animal.isInLove()
            );

            int affectedCount = 0;
            for (Animal animal : animals) {
                // Reset breeding cooldown so animals can breed again immediately
                animal.setAge(0);
                // Set animal in love mode (the "horny" effect)
                animal.setInLove(player);
                
                affectedCount++;

                // Spawn heart particles
                level.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.HEART,
                    animal.getX(), animal.getY() + animal.getBbHeight() / 2, animal.getZ(),
                    7, 0.5, 0.5, 0.5, 0.1
                );
            }

            AzukaarSkillsStats.LOGGER.info("Player {} used Mass Breeding (radius {}), affected {} animals",
                player.getName().getString(), radius, affectedCount);

            return affectedCount > 0;
        }
    }

    public static class TimberEffect implements ActiveSkillEffect {
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

            // Get tree base position and log type before cutting (for replanting)
            BlockPos basePos = TreeCutter.getTreeBasePos(level, targetPos);
            BlockState logState = basePos != null ? level.getBlockState(basePos) : null;

            // Start cutting the tree
            boolean started = TreeCutter.startCutting(level, targetPos);

            if (started) {
                // Check for Nature Renewal replanting
                int renewalLevel = PlayerData.getSkillLevel(player, NATURE_RENEWAL_SKILL);
                if (renewalLevel > 0 && basePos != null && logState != null) {
                    double replantChance = SkillEffect.getSkillParameter(player, NATURE_RENEWAL_SKILL, NATURE_RENEWAL_EFFECT, "replant_chance") / 100.0;
                    if (level.random.nextDouble() < replantChance) {
                        // Get matching sapling for this log type
                        Block saplingBlock = getSaplingForLog(logState.getBlock());
                        if (saplingBlock != null) {
                            // Plant sapling at base position (will be placed after tree is cut)
                            level.scheduleTick(basePos, saplingBlock, 70); // Delay to let tree cut first
                            level.setBlock(basePos, saplingBlock.defaultBlockState(), 2);
                        }
                    }
                }

                AzukaarSkillsStats.LOGGER.info("Player {} used Timber on tree at {}",
                    player.getName().getString(), targetPos);
            }

            return started;
        }

        private static Block getSaplingForLog(Block logBlock) {
            String logName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(logBlock).getPath();
            String saplingName = logName.replace("_log", "_sapling").replace("_wood", "_sapling");
            return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                net.minecraft.resources.ResourceLocation.withDefaultNamespace(saplingName)
            );
        }
    }

    public static class TreeCallingEffect implements ActiveSkillEffect {
        @Override
        public boolean execute(Player player, String skillId, int skillLevel, EffectData effectData) {
            if (!(player.level() instanceof ServerLevel level)) {
                return false;
            }

            // Check if player is holding a sapling
            ItemStack heldItem = player.getMainHandItem();
            if (!heldItem.is(net.minecraft.tags.ItemTags.SAPLINGS)) {
                heldItem = player.getOffhandItem();
                if (!heldItem.is(net.minecraft.tags.ItemTags.SAPLINGS)) {
                    return false;
                }
            }

            // Get the block the player is looking at
            HitResult hitResult = player.pick(5.0D, 0.0F, false);
            if (hitResult.getType() != HitResult.Type.BLOCK) {
                return false;
            }

            BlockPos targetPos = ((BlockHitResult) hitResult).getBlockPos();
            BlockPos placePos = targetPos.above();

            // Check if we can place here
            if (!level.getBlockState(placePos).isAir()) {
                return false;
            }

            // Get the sapling block from the item
            if (!(heldItem.getItem() instanceof net.minecraft.world.item.BlockItem blockItem)) {
                return false;
            }

            Block saplingBlock = blockItem.getBlock();
            if (!(saplingBlock instanceof net.minecraft.world.level.block.SaplingBlock sapling)) {
                return false;
            }

            // Place the sapling and grow it into a tree
            BlockState saplingState = saplingBlock.defaultBlockState();
            level.setBlock(placePos, saplingState, 2);

            // Try to grow the tree over the next ticks (up to 20 attempts)
            scheduleTreeGrowth(level, placePos, 20);

            AzukaarSkillsStats.LOGGER.info("Player {} used Tree Calling at {}",
                player.getName().getString(), placePos);

            return true;
        }

        private void scheduleTreeGrowth(ServerLevel level, BlockPos pos, int attemptsRemaining) {
            if (attemptsRemaining <= 0) return;

            level.getServer().execute(() -> {
                BlockState state = level.getBlockState(pos);

                // Stop if block is no longer air or sapling (tree grew or was removed)
                if (!state.isAir() && !(state.getBlock() instanceof net.minecraft.world.level.block.SaplingBlock)) {
                    return;
                }

                // Try to grow if it's a sapling
                if (state.getBlock() instanceof net.minecraft.world.level.block.SaplingBlock s) {
                    s.advanceTree(level, pos, state, level.random);
                }

                // Schedule next attempt
                scheduleTreeGrowth(level, pos, attemptsRemaining - 1);
            });
        }
    }
}
