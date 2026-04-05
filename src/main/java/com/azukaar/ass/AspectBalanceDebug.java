package com.azukaar.ass;

import com.azukaar.ass.api.AspectDefinition;
import com.azukaar.ass.api.PlayerData;
import com.azukaar.ass.capabilities.IPlayerSkills;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;

import java.io.FileWriter;
import java.io.PrintWriter;

public class AspectBalanceDebug {

    // --- Gameplay assumptions ---
    private static final int BLOCKS_PER_HOUR = 600;
    private static final double AVG_BLOCK_DURABILITY = 3.0;

    private static final int MOBS_PER_HOUR = 40;
    private static final double AVG_MOB_HP = 25.0;

    private static final int NEW_CHUNKS_PER_HOUR = 50;
    private static final int BIOMES_PER_HOUR = 1;
    private static final int STRUCTURES_PER_HOUR = 1;

    private static final int ENTITY_INTERACTIONS_PER_HOUR = 20;
    private static final int BREEDS_PER_HOUR = 5;
    private static final double AVG_BREED_PARENT_HP = 10.0;
    private static final double TAMES_PER_HOUR = 0.5;
    private static final double AVG_TAME_HP = 10.0;
    private static final int CROP_HARVESTS_PER_HOUR = 40;

    public static void generateCsv(String path, MinecraftServer server) {
        try (PrintWriter out = new PrintWriter(new FileWriter(path))) {
            AspectDefinition mining = SkillDataManager.INSTANCE.getAspect("azukaarskillsstats:mining");
            AspectDefinition combat = SkillDataManager.INSTANCE.getAspect("azukaarskillsstats:combat");
            AspectDefinition exploration = SkillDataManager.INSTANCE.getAspect("azukaarskillsstats:exploration");
            AspectDefinition nature = SkillDataManager.INSTANCE.getAspect("azukaarskillsstats:nature");

            double miningMultiplier = mining.getXpMultiplier();
            double combatMultiplier = combat.getXpMultiplier();
            double natureMultiplier = nature.getXpMultiplier();
            double miningXpPerHour = BLOCKS_PER_HOUR * AVG_BLOCK_DURABILITY * miningMultiplier;
            double combatXpPerHour = MOBS_PER_HOUR * AVG_MOB_HP * combatMultiplier;

            // Nature data from datapack
            double interactXp = nature.getDouble("interact_xp", 5.0);
            double breedMultiplier = nature.getDouble("breed_multiplier", 2.0);
            double tameMultiplier = nature.getDouble("tame_multiplier", 5.0);
            double cropHarvestXp = nature.getDouble("crop_harvest_xp", 15.0);
            double natureXpPerHour = (ENTITY_INTERACTIONS_PER_HOUR * interactXp
                + BREEDS_PER_HOUR * AVG_BREED_PARENT_HP * breedMultiplier
                + TAMES_PER_HOUR * AVG_TAME_HP * tameMultiplier
                + CROP_HARVESTS_PER_HOUR * cropHarvestXp) * natureMultiplier;

            // Exploration data from datapack + registries
            double chunkBonusXp = exploration.getDouble("chunk_bonus_xp", 0);
            double maxDiscoveryXp = exploration.getDouble("max_discovery_xp", 0);
            int biomeRampStart = exploration.getInt("biome_ramp_start", 0);
            double biomeRampRate = exploration.getDouble("biome_ramp_rate", 0);
            double structureRampRate = exploration.getDouble("structure_ramp_rate", 0);
            double biomeRatio = exploration.getDouble("biome_ratio", 0);
            double structureRatio = exploration.getDouble("structure_ratio", 0);
            int targetMaxLevel = exploration.getInt("target_max_level", 0);

            int totalXpBudget = PlayerData.getTotalXpForLevel(targetMaxLevel);
            int biomeCount = server.registryAccess().registryOrThrow(Registries.BIOME).size();
            int structureCount = server.registryAccess().registryOrThrow(Registries.STRUCTURE).size();

            double biomeXpBase = (biomeCount > 0) ? Math.min(totalXpBudget * biomeRatio / biomeCount, maxDiscoveryXp) : 0;
            double structureXpBase = (structureCount > 0) ? Math.min(totalXpBudget * structureRatio / structureCount, maxDiscoveryXp) : 0;

            // --- Assumptions ---
            out.println("=== ASSUMPTIONS ===");
            out.printf("Mining: %d blocks/hr * %.1f avg durability * %.1f multiplier = %.0f XP/hr%n",
                BLOCKS_PER_HOUR, AVG_BLOCK_DURABILITY, miningMultiplier, miningXpPerHour);
            out.printf("Combat: %d mobs/hr * %.0f avg HP * %.1f multiplier = %.0f XP/hr%n",
                MOBS_PER_HOUR, AVG_MOB_HP, combatMultiplier, combatXpPerHour);
            out.printf("Exploration: %d new chunks/hr * %.0f XP + %d biomes/hr (of %d total) + %d structures/hr (of %d total)%n",
                NEW_CHUNKS_PER_HOUR, chunkBonusXp, BIOMES_PER_HOUR, biomeCount, STRUCTURES_PER_HOUR, structureCount);
            out.printf("  Biome XP: %.0f per discovery (ramp: start after %d, rate %.2f)%n", biomeXpBase, biomeRampStart, biomeRampRate);
            out.printf("  Structure XP: %.0f per discovery (ramp rate %.2f)%n", structureXpBase, structureRampRate);
            out.printf("Nature: %d interactions/hr * %.0f XP + %d breeds/hr * %.0f HP * %.1f mult + %.1f tames/hr * %.0f HP * %.1f mult + %d crops/hr * %.0f XP = %.0f XP/hr%n",
                ENTITY_INTERACTIONS_PER_HOUR, interactXp,
                BREEDS_PER_HOUR, AVG_BREED_PARENT_HP, breedMultiplier,
                TAMES_PER_HOUR, AVG_TAME_HP, tameMultiplier,
                CROP_HARVESTS_PER_HOUR, cropHarvestXp, natureXpPerHour);
            out.println();

            // --- Unified hour-by-hour simulation ---
            out.println("=== ALL ASPECTS (hour by hour) ===");
            out.println("Hour," +
                "Mining XP/hr,Mining Cumul,Mining Lvl (Hard),Mining Lvl (Normal),Mining Lvl (Easy)," +
                "Combat XP/hr,Combat Cumul,Combat Lvl (Hard),Combat Lvl (Normal),Combat Lvl (Easy)," +
                "Explor XP/hr,Explor Cumul,Biomes Found,Structs Found,Explor Lvl (Hard),Explor Lvl (Normal),Explor Lvl (Easy)," +
                "Nature XP/hr,Nature Cumul,Nature Lvl (Hard),Nature Lvl (Normal),Nature Lvl (Easy)");

            double miningCumul = 0;
            double combatCumul = 0;
            double explorationCumul = 0;
            double natureCumul = 0;
            int biomesDiscovered = 0;
            int structuresDiscovered = 0;

            for (int hour = 1; hour <= 200; hour++) {
                miningCumul += miningXpPerHour;
                combatCumul += combatXpPerHour;

                // Exploration: finite discoveries with ramp
                double explorationThisHour = NEW_CHUNKS_PER_HOUR * chunkBonusXp;

                int newBiomes = Math.min(BIOMES_PER_HOUR, biomeCount - biomesDiscovered);
                for (int b = 0; b < newBiomes; b++) {
                    double mult = Math.min((biomesDiscovered - biomeRampStart) * biomeRampRate, 1.0);
                    if (mult < 0) mult = 0;
                    explorationThisHour += biomeXpBase * mult;
                    biomesDiscovered++;
                }

                int newStructures = Math.min(STRUCTURES_PER_HOUR, structureCount - structuresDiscovered);
                for (int s = 0; s < newStructures; s++) {
                    double mult = Math.min(structuresDiscovered * structureRampRate, 1.0);
                    explorationThisHour += structureXpBase * mult;
                    structuresDiscovered++;
                }

                explorationCumul += explorationThisHour;
                natureCumul += natureXpPerHour;

                out.printf("%d,%.0f,%.0f,%s,%s,%s,%.0f,%.0f,%s,%s,%s,%.0f,%.0f,%d/%d,%d/%d,%s,%s,%s,%.0f,%.0f,%s,%s,%s%n",
                    hour,
                    miningXpPerHour, miningCumul,
                    levelStr(miningCumul, IPlayerSkills.HARD_CAP),
                    levelStr(miningCumul, IPlayerSkills.NORMAL_CAP),
                    levelStr(miningCumul, IPlayerSkills.EASY_CAP),
                    combatXpPerHour, combatCumul,
                    levelStr(combatCumul, IPlayerSkills.HARD_CAP),
                    levelStr(combatCumul, IPlayerSkills.NORMAL_CAP),
                    levelStr(combatCumul, IPlayerSkills.EASY_CAP),
                    explorationThisHour, explorationCumul,
                    biomesDiscovered, biomeCount,
                    structuresDiscovered, structureCount,
                    levelStr(explorationCumul, IPlayerSkills.HARD_CAP),
                    levelStr(explorationCumul, IPlayerSkills.NORMAL_CAP),
                    levelStr(explorationCumul, IPlayerSkills.EASY_CAP),
                    natureXpPerHour, natureCumul,
                    levelStr(natureCumul, IPlayerSkills.HARD_CAP),
                    levelStr(natureCumul, IPlayerSkills.NORMAL_CAP),
                    levelStr(natureCumul, IPlayerSkills.EASY_CAP));
            }

            AzukaarSkillsStats.LOGGER.info("Aspect balance CSV written to {}", path);
        } catch (Exception e) {
            AzukaarSkillsStats.LOGGER.error("Failed to write aspect balance CSV: {}", e.getMessage());
        }
    }

    private static String levelStr(double cumulXp, int cap) {
        int level = 0;
        double remaining = cumulXp;
        while (level < cap) {
            int needed = IPlayerSkills.getScaledXpForLevel(level + 1, cap);
            if (remaining < needed) break;
            remaining -= needed;
            level++;
        }
        if (level >= cap) return cap + " (MAX)";
        return String.valueOf(level);
    }
}
