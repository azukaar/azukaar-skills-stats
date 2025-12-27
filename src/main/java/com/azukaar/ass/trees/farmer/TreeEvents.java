package com.azukaar.ass.trees.farmer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.azukaar.ass.api.PlayerData;
import com.azukaar.ass.types.SkillEffect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Event handlers for the Farmer skill tree
 */
public class TreeEvents {

    private static final String NATURES_CALL_SKILL = "azukaarskillsstats:natures_call";
    private static final String NATURES_CALL_EFFECT = "azukaarskillsstats:natures_call";
    private static final String HARVESTS_BLESSING_SKILL = "azukaarskillsstats:harvests_blessing";
    private static final String HARVESTS_BLESSING_EFFECT = "azukaarskillsstats:harvests_blessing";
    private static final String BREEDING_MASTERY_SKILL = "azukaarskillsstats:breeding_mastery";
    private static final String BREEDING_MASTERY_EFFECT = "azukaarskillsstats:breeding_mastery";
    private static final String ANIMAL_HARMONY_SKILL = "azukaarskillsstats:animal_harmony";
    private static final String LIVESTOCK_GUARDIAN_SKILL = "azukaarskillsstats:livestock_guardian";

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        // Animal Harmony: every 5 ticks
        if (player.tickCount % 5 == 0) {
            tickAnimalHarmony(player);
        }

        // Nature's Call: every 15 ticks
        if (player.tickCount % 15 == 0) {
            tickNaturesCall(player);
        }

        // Livestock Guardian: every 30 ticks
        if (player.tickCount % 30 == 0) {
            tickLivestockGuardian(player);
        }
    }

    /**
     * Nature's Call: Accelerate crop growth near the player
     */
    private static void tickNaturesCall(Player player) {
        int skillLevel = PlayerData.getSkillLevel(player, NATURES_CALL_SKILL);
        if (skillLevel <= 0) return;

        // Get values from skill effect data
        int radius = (int) SkillEffect.getSkillParameter(player, NATURES_CALL_SKILL, NATURES_CALL_EFFECT, "radius");
        double power = SkillEffect.getSkillParameter(player, NATURES_CALL_SKILL, NATURES_CALL_EFFECT, "power");

        if (radius <= 0) radius = 3; // fallback
        if (power <= 0) power = 0.05; // fallback

        ServerLevel level = (ServerLevel) player.level();
        BlockPos playerPos = player.blockPosition();

        // Scan area: radius horizontal, -1 to +4 vertical
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -1; y <= 4; y++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    Block block = state.getBlock();

                    // Check if it's a growable crop
                    if (block instanceof BonemealableBlock growable) {
                        if (growable.isValidBonemealTarget(level, pos, state)) {
                            // Apply N random ticks where N = power * 10
                            int ticks = (int) (power * 10);
                            for (int i = 0; i < ticks; i++) {
                                // Re-fetch state each iteration as it may have changed
                                // random 10% change
                                if (level.random.nextDouble() < 0.1) {
                                    BlockState currentState = level.getBlockState(pos);
                                    currentState.randomTick(level, pos, level.random);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Animal Harmony: Neutral mobs won't aggro you
     */
    private static void tickAnimalHarmony(Player player) {
        int skillLevel = PlayerData.getSkillLevel(player, ANIMAL_HARMONY_SKILL);
        if (skillLevel <= 0) return;

        ServerLevel level = (ServerLevel) player.level();
        AABB searchBox = player.getBoundingBox().inflate(16);

        // Find all neutral mobs that have the player as target
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, searchBox, mob -> {
            if (mob instanceof NeutralMob neutral) {
                return neutral.getPersistentAngerTarget() != null &&
                       neutral.getPersistentAngerTarget().equals(player.getUUID());
            }
            return mob.getTarget() == player;
        });

        for (Mob mob : mobs) {
            // Only affect "passive" neutral mobs (bees, wolves, etc.)
            if (mob instanceof Bee || mob instanceof Wolf || mob instanceof NeutralMob) {
                mob.setTarget(null);
                if (mob instanceof NeutralMob neutral) {
                    neutral.stopBeingAngry();
                }
            }
        }
    }

    /**
     * Livestock Guardian: Nearby animals regenerate health from player's hunger
     */
    private static void tickLivestockGuardian(Player player) {
        int skillLevel = PlayerData.getSkillLevel(player, LIVESTOCK_GUARDIAN_SKILL);
        if (skillLevel <= 0) return;

        // Need at least 1 hunger to heal animals
        if (player.getFoodData().getFoodLevel() <= 0) return;

        ServerLevel level = (ServerLevel) player.level();
        AABB searchBox = player.getBoundingBox().inflate(16);

        // Find all hurt animals
        List<Animal> animals = level.getEntitiesOfClass(Animal.class, searchBox, animal -> {
            return animal.getHealth() < animal.getMaxHealth();
        });

        for (Animal animal : animals) {
            // Check if player still has hunger
            if (player.getFoodData().getFoodLevel() <= 0) break;

            // Heal 2 health, consume 1 hunger
            animal.heal(2.0f);
            player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);

            // Play heart particles
            level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.HEART,
                animal.getX(), animal.getY() + animal.getBbHeight() / 2, animal.getZ(),
                5, 0.5, 0.5, 0.5, 0.1
            );
        }
    }

    /**
     * Harvest's Blessing: Bonus crop drops when harvesting
     */
    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        BlockState state = event.getState();
        Block block = state.getBlock();

        // Only apply to mature crops
        if (!(block instanceof CropBlock crop)) return;
        if (!crop.isMaxAge(state)) return;

        int skillLevel = PlayerData.getSkillLevel(player, HARVESTS_BLESSING_SKILL);
        if (skillLevel <= 0) return;

        // Get bonus percentage from effect data
        double bonus = SkillEffect.getSkillParameter(player, HARVESTS_BLESSING_SKILL, HARVESTS_BLESSING_EFFECT, "bonus");
        if (bonus <= 0) return;

        // Add bonus drops
        ServerLevel level = (ServerLevel) player.level();
        List<ItemEntity> bonusDrops = new ArrayList<>();

        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            int bonusCount = (int) (stack.getCount() * bonus);

            // Roll for partial bonus
            double remainder = (stack.getCount() * bonus) - bonusCount;
            if (level.random.nextDouble() < remainder) {
                bonusCount++;
            }

            if (bonusCount > 0) {
                ItemStack bonusStack = stack.copy();
                bonusStack.setCount(bonusCount);
                ItemEntity bonusEntity = new ItemEntity(
                    level,
                    drop.getX(), drop.getY(), drop.getZ(),
                    bonusStack
                );
                bonusDrops.add(bonusEntity);
            }
        }

        // Add all bonus drops to the event
        event.getDrops().addAll(bonusDrops);
    }

    // Seeds that can be eaten with Forager's Appetite
    private static final Set<Item> EDIBLE_SEEDS = Set.of(
        Items.WHEAT_SEEDS,
        Items.BEETROOT_SEEDS,
        Items.MELON_SEEDS,
        Items.PUMPKIN_SEEDS,
        Items.TORCHFLOWER_SEEDS,
        Items.PITCHER_POD
    );

    /**
     * Forager's Appetite: Makes seeds edible when the buff is active
     */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        // Check if player has Forager's Appetite buff
        if (!player.hasEffect(MobEffects.FORAGERS_APPETITE)) return;

        ItemStack stack = event.getItemStack();
        if (!EDIBLE_SEEDS.contains(stack.getItem())) return;

        // Only eat if player can eat (is hungry or in creative)
        if (!player.canEat(false) && !player.isCreative()) return;

        // Feed the player (6 hunger, 0.6 saturation - equivalent to cooked chicken)
        player.getFoodData().eat(6, 0.6f);

        // Consume the seed
        if (!player.isCreative()) {
            stack.shrink(1);
        }

        // Swing arm for visual feedback
        player.swing(event.getHand());

        // Cancel the event to prevent normal seed behavior
        event.setCanceled(true);
    }

    /**
     * Breeding Mastery: 50% chance for twins when animals breed
     */
    @SubscribeEvent
    public static void onBabySpawn(BabyEntitySpawnEvent event) {
        com.azukaar.ass.AzukaarSkillsStats.LOGGER.info("BabyEntitySpawnEvent fired!");

        if (event.getParentA() == null || event.getChild() == null) {
            com.azukaar.ass.AzukaarSkillsStats.LOGGER.info("ParentA or Child is null, skipping");
            return;
        }
        if (!(event.getParentA().level() instanceof ServerLevel level)) {
            com.azukaar.ass.AzukaarSkillsStats.LOGGER.info("Not server level, skipping");
            return;
        }

        com.azukaar.ass.AzukaarSkillsStats.LOGGER.info("Breeding event: ParentA={}, ParentB={}, Child={}",
            event.getParentA().getType(),
            event.getParentB() != null ? event.getParentB().getType() : "null",
            event.getChild().getType());

        // Find the player who caused the breeding (check nearby players)
        Player breeder = event.getCausedByPlayer();
        com.azukaar.ass.AzukaarSkillsStats.LOGGER.info("CausedByPlayer: {}", breeder != null ? breeder.getName().getString() : "null");

        if (breeder == null) {
            // Try to find nearest player with the skill
            AABB searchBox = event.getParentA().getBoundingBox().inflate(10);
            List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class, searchBox);
            com.azukaar.ass.AzukaarSkillsStats.LOGGER.info("Found {} nearby players", nearbyPlayers.size());

            for (Player player : nearbyPlayers) {
                int skillLevel = PlayerData.getSkillLevel(player, BREEDING_MASTERY_SKILL);
                com.azukaar.ass.AzukaarSkillsStats.LOGGER.info("Player {} has Breeding Mastery level {}",
                    player.getName().getString(), skillLevel);
                if (skillLevel > 0) {
                    breeder = player;
                    break;
                }
            }
        }

        if (breeder == null) {
            com.azukaar.ass.AzukaarSkillsStats.LOGGER.info("No breeder found with skill, skipping");
            return;
        }

        int skillLevel = PlayerData.getSkillLevel(breeder, BREEDING_MASTERY_SKILL);
        com.azukaar.ass.AzukaarSkillsStats.LOGGER.info("Breeder {} has Breeding Mastery level {}",
            breeder.getName().getString(), skillLevel);

        if (skillLevel <= 0) {
            com.azukaar.ass.AzukaarSkillsStats.LOGGER.info("Breeder has no Breeding Mastery skill");
            return;
        }

        // Get twin chance from effect data
        double chance = SkillEffect.getSkillParameter(breeder, BREEDING_MASTERY_SKILL, BREEDING_MASTERY_EFFECT, "chance") / 100.0;
        com.azukaar.ass.AzukaarSkillsStats.LOGGER.info("Twin chance: {}%", chance * 100);

        // Roll for twin
        double roll = level.random.nextDouble();
        com.azukaar.ass.AzukaarSkillsStats.LOGGER.info("Roll: {} vs chance: {}", roll, chance);

        if (roll < chance) {
            // Spawn a twin at the parent's position (child position isn't set yet)
            AgeableMob child = event.getChild();
            Mob parent = event.getParentA();
            AgeableMob twin = (AgeableMob) child.getType().create(level);
            if (twin != null) {
                twin.setBaby(true);
                twin.moveTo(parent.getX(), parent.getY(), parent.getZ(), parent.getYRot(), parent.getXRot());
                level.addFreshEntity(twin);
                com.azukaar.ass.AzukaarSkillsStats.LOGGER.info("Spawned twin {} at ({}, {}, {})",
                    twin.getType(), twin.getX(), twin.getY(), twin.getZ());
            } else {
                com.azukaar.ass.AzukaarSkillsStats.LOGGER.warn("Failed to create twin entity!");
            }
        } else {
            com.azukaar.ass.AzukaarSkillsStats.LOGGER.info("Roll failed, no twin spawned");
        }
    }
}
