package com.azukaar.ass;

import com.azukaar.ass.capabilities.IPlayerSkills;

import java.io.FileWriter;
import java.io.PrintWriter;

import net.minecraft.world.Difficulty;

public class BalanceDebug {
    private static final double XP_PER_HOUR_PER_ASPECT = 5000.0;

    public static void generateCsv(String path) {
        try (PrintWriter out = new PrintWriter(new FileWriter(path))) {
            writeAspectSheet(out);
            out.println();
            writeMainSheet(out);
            out.println();
            writeMainLevelByHour(out);
            AzukaarSkillsStats.LOGGER.info("Balance CSV written to {}", path);
        } catch (Exception e) {
            AzukaarSkillsStats.LOGGER.error("Failed to write balance CSV: {}", e.getMessage());
        }
    }

    private static void writeAspectSheet(PrintWriter out) {
        out.println("=== ASPECT LEVEL SCALING ===");
        out.println("Level,Base XP,Easy XP (cap " + IPlayerSkills.EASY_CAP + "),Normal XP (cap " + IPlayerSkills.NORMAL_CAP + "),Hard XP (cap " + IPlayerSkills.HARD_CAP + "),Cumul Base,Cumul Easy,Cumul Normal,Cumul Hard,Hours (Easy),Hours (Normal),Hours (Hard)");

        int cumulBase = 0;
        int cumulEasy = 0;
        int cumulNormal = 0;
        int cumulHard = 0;

        for (int level = 1; level <= IPlayerSkills.BASE_CAP; level++) {
            int baseXp = IPlayerSkills.getXpForSpecificLevel(level);
            int easyXp = level <= IPlayerSkills.EASY_CAP ? IPlayerSkills.getScaledXpForLevel(level, IPlayerSkills.EASY_CAP) : 0;
            int normalXp = level <= IPlayerSkills.NORMAL_CAP ? IPlayerSkills.getScaledXpForLevel(level, IPlayerSkills.NORMAL_CAP) : 0;
            int hardXp = IPlayerSkills.getScaledXpForLevel(level, IPlayerSkills.HARD_CAP);

            cumulBase += baseXp;
            if (level <= IPlayerSkills.EASY_CAP) cumulEasy += easyXp;
            if (level <= IPlayerSkills.NORMAL_CAP) cumulNormal += normalXp;
            cumulHard += hardXp;

            String hoursEasy = level <= IPlayerSkills.EASY_CAP ? String.format("%.1f", cumulEasy / XP_PER_HOUR_PER_ASPECT) : "";
            String hoursNormal = level <= IPlayerSkills.NORMAL_CAP ? String.format("%.1f", cumulNormal / XP_PER_HOUR_PER_ASPECT) : "";
            String hoursHard = String.format("%.1f", cumulHard / XP_PER_HOUR_PER_ASPECT);

            out.printf("%d,%d,%s,%s,%d,%d,%s,%s,%d,%s,%s,%s%n",
                level, baseXp,
                level <= IPlayerSkills.EASY_CAP ? String.valueOf(easyXp) : "",
                level <= IPlayerSkills.NORMAL_CAP ? String.valueOf(normalXp) : "",
                hardXp,
                cumulBase,
                level <= IPlayerSkills.EASY_CAP ? String.valueOf(cumulEasy) : "",
                level <= IPlayerSkills.NORMAL_CAP ? String.valueOf(cumulNormal) : "",
                cumulHard,
                hoursEasy, hoursNormal, hoursHard);
        }
    }

    private static void writeMainSheet(PrintWriter out) {
        out.println("=== MAIN LEVEL SCALING ===");
        out.println("Main Level,Aspect Level-ups Needed,Cumul Level-ups,Est. Hours (Easy 3 aspects),Est. Hours (Normal 3 aspects),Est. Hours (Hard 3 aspects)");

        int cumulLevelUps = 0;
        int numAspects = 3;

        int maxEasyLevelUps = numAspects * IPlayerSkills.EASY_CAP;
        int maxNormalLevelUps = numAspects * IPlayerSkills.NORMAL_CAP;
        int maxHardLevelUps = numAspects * IPlayerSkills.HARD_CAP;

        for (int level = 1; level <= 120; level++) {
            int xpNeeded = IPlayerSkills.getXpForMainLevel(level);
            cumulLevelUps += xpNeeded;

            String hoursEasy = cumulLevelUps <= maxEasyLevelUps
                ? String.format("%.1f", estimateHoursForAspectLevelUps(cumulLevelUps, numAspects, IPlayerSkills.EASY_CAP)) : "";
            String hoursNormal = cumulLevelUps <= maxNormalLevelUps
                ? String.format("%.1f", estimateHoursForAspectLevelUps(cumulLevelUps, numAspects, IPlayerSkills.NORMAL_CAP)) : "";
            String hoursHard = cumulLevelUps <= maxHardLevelUps
                ? String.format("%.1f", estimateHoursForAspectLevelUps(cumulLevelUps, numAspects, IPlayerSkills.HARD_CAP)) : "";

            out.printf("%d,%d,%d,%s,%s,%s%n",
                level, xpNeeded, cumulLevelUps,
                hoursEasy, hoursNormal, hoursHard);
        }
    }

    private static void writeMainLevelByHour(PrintWriter out) {
        out.println("=== MAIN LEVEL BY HOUR (for graphing) ===");
        out.println("Hour,Main Level (Easy),Main Level (Normal),Main Level (Hard)");

        int numAspects = 3;
        int[] caps = { IPlayerSkills.EASY_CAP, IPlayerSkills.NORMAL_CAP, IPlayerSkills.HARD_CAP };

        // Track per difficulty: aspect XP, aspect levels, main XP, main level
        double[][] aspectXp = new double[3][numAspects]; // [difficulty][aspect]
        int[][] aspectLevels = new int[3][numAspects];
        double[] mainXp = new double[3];
        int[] mainLevel = new int[3];

        for (int hour = 1; hour <= 200; hour++) {
            for (int d = 0; d < 3; d++) {
                int cap = caps[d];
                int levelUpsBefore = 0;
                for (int a = 0; a < numAspects; a++) levelUpsBefore += aspectLevels[d][a];

                for (int a = 0; a < numAspects; a++) {
                    aspectXp[d][a] += XP_PER_HOUR_PER_ASPECT;
                    int level = aspectLevels[d][a];

                    int needed = IPlayerSkills.getScaledXpForLevel(level + 1, cap);
                    while (aspectXp[d][a] >= needed && level < cap) {
                        aspectXp[d][a] -= needed;
                        level++;
                        needed = IPlayerSkills.getScaledXpForLevel(level + 1, cap);
                    }
                    if (level >= cap) aspectXp[d][a] = 0;
                    aspectLevels[d][a] = level;
                }

                int levelUpsAfter = 0;
                for (int a = 0; a < numAspects; a++) levelUpsAfter += aspectLevels[d][a];
                int newLevelUps = levelUpsAfter - levelUpsBefore;

                if (newLevelUps > 0) {
                    mainXp[d] += newLevelUps;
                    int mLevel = mainLevel[d];
                    int mNeeded = IPlayerSkills.getXpForMainLevel(mLevel + 1);
                    while (mainXp[d] >= mNeeded) {
                        mainXp[d] -= mNeeded;
                        mLevel++;
                        mNeeded = IPlayerSkills.getXpForMainLevel(mLevel + 1);
                    }
                    mainLevel[d] = mLevel;
                }
            }

            out.printf("%d,%d,%d,%d%n", hour, mainLevel[0], mainLevel[1], mainLevel[2]);
        }
    }

    // Estimate hours to accumulate a given number of total aspect level-ups across N aspects
    private static double estimateHoursForAspectLevelUps(int totalLevelUps, int numAspects, int cap) {
        // Distribute level-ups evenly across aspects
        int perAspect = Math.min(totalLevelUps / numAspects, cap);
        int remainder = totalLevelUps - (perAspect * numAspects);

        // Total XP needed = sum of scaled XP for each aspect's levels
        double totalXp = 0;
        for (int a = 0; a < numAspects; a++) {
            int aspectLevels = perAspect + (a < remainder ? 1 : 0);
            aspectLevels = Math.min(aspectLevels, cap);
            for (int l = 1; l <= aspectLevels; l++) {
                totalXp += IPlayerSkills.getScaledXpForLevel(l, cap);
            }
        }

        return totalXp / (XP_PER_HOUR_PER_ASPECT * numAspects);
    }
}
