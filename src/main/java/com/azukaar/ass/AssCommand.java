package com.azukaar.ass;

import com.azukaar.ass.AzukaarSkillsStats;
import com.azukaar.ass.api.PlayerData;
import com.azukaar.ass.capabilities.IPlayerSkills;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class AssCommand {
    
    // Suggestion provider for path names
    private static final SuggestionProvider<CommandSourceStack> PATH_SUGGESTIONS = 
        (context, builder) -> {
            for (String pathName : IPlayerSkills.PATH_NAMES) {
                builder.suggest(pathName);
            }
            return builder.buildFuture();
        };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ass")
            .requires(source -> source.hasPermission(2)) // Requires OP level 2
            
            // XP commands
            .then(Commands.literal("xp")
                // /ass xp set <amount> [player]
                .then(Commands.literal("set")
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                        .executes(context -> setTotalXP(context, DoubleArgumentType.getDouble(context, "amount"), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("targets", EntityArgument.players())
                            .executes(context -> {
                                double amount = DoubleArgumentType.getDouble(context, "amount");
                                Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                                return setTotalXPForTargets(context, amount, targets);
                            }))))
                
                // /ass xp set <path> <amount> [player]
                .then(Commands.literal("set")
                    .then(Commands.argument("path", StringArgumentType.string())
                        .suggests(PATH_SUGGESTIONS)
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                            .executes(context -> setPathXP(context, 
                                StringArgumentType.getString(context, "path"),
                                DoubleArgumentType.getDouble(context, "amount"),
                                context.getSource().getPlayerOrException()))
                            .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> {
                                    String path = StringArgumentType.getString(context, "path");
                                    double amount = DoubleArgumentType.getDouble(context, "amount");
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                                    return setPathXPForTargets(context, path, amount, targets);
                                })))))
                
                // /ass xp add <amount> [player]
                .then(Commands.literal("add")
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                        .executes(context -> addTotalXP(context, DoubleArgumentType.getDouble(context, "amount"), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("targets", EntityArgument.players())
                            .executes(context -> {
                                double amount = DoubleArgumentType.getDouble(context, "amount");
                                Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                                return addTotalXPForTargets(context, amount, targets);
                            }))))
                
                // /ass xp add <path> <amount> [player]
                .then(Commands.literal("add")
                    .then(Commands.argument("path", StringArgumentType.string())
                        .suggests(PATH_SUGGESTIONS)
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                            .executes(context -> addPathXP(context,
                                StringArgumentType.getString(context, "path"),
                                DoubleArgumentType.getDouble(context, "amount"),
                                context.getSource().getPlayerOrException()))
                            .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> {
                                    String path = StringArgumentType.getString(context, "path");
                                    double amount = DoubleArgumentType.getDouble(context, "amount");
                                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                                    return addPathXPForTargets(context, path, amount, targets);
                                })))))
                
                // /ass xp get [player]
                .then(Commands.literal("get")
                    .executes(context -> getXP(context, context.getSource().getPlayerOrException()))
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(context -> getXP(context, EntityArgument.getPlayer(context, "target"))))))
            
            // Level commands
            .then(Commands.literal("level")
                // /ass level set <level> [player]
                .then(Commands.literal("set")
                    .then(Commands.argument("level", IntegerArgumentType.integer(0))
                        .executes(context -> setLevel(context, IntegerArgumentType.getInteger(context, "level"), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("targets", EntityArgument.players())
                            .executes(context -> {
                                int level = IntegerArgumentType.getInteger(context, "level");
                                Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                                return setLevelForTargets(context, level, targets);
                            }))))
                
                // /ass level get [player]
                .then(Commands.literal("get")
                    .executes(context -> getLevel(context, context.getSource().getPlayerOrException()))
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(context -> getLevel(context, EntityArgument.getPlayer(context, "target"))))))
            
            // Skill points commands
            .then(Commands.literal("skillpoints")
                // /ass skillpoints set <amount> [player]
                .then(Commands.literal("set")
                    .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                        .executes(context -> setSkillPoints(context, IntegerArgumentType.getInteger(context, "amount"), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("targets", EntityArgument.players())
                            .executes(context -> {
                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                                return setSkillPointsForTargets(context, amount, targets);
                            }))))
                
                // /ass skillpoints add <amount> [player]
                .then(Commands.literal("add")
                    .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                        .executes(context -> addSkillPoints(context, IntegerArgumentType.getInteger(context, "amount"), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("targets", EntityArgument.players())
                            .executes(context -> {
                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                                return addSkillPointsForTargets(context, amount, targets);
                            }))))
                
                // /ass skillpoints get [player]
                .then(Commands.literal("get")
                    .executes(context -> getSkillPoints(context, context.getSource().getPlayerOrException()))
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(context -> getSkillPoints(context, EntityArgument.getPlayer(context, "target"))))))

            // Cooldown commands
            .then(Commands.literal("cooldown")
                // /ass cooldown reset [player]
                .then(Commands.literal("reset")
                    .executes(context -> resetCooldowns(context, context.getSource().getPlayerOrException()))
                    .then(Commands.argument("targets", EntityArgument.players())
                        .executes(context -> {
                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                            return resetCooldownsForTargets(context, targets);
                        }))))

            // Respec command
            .then(Commands.literal("respec")
                // /ass respec [player]
                .executes(context -> respec(context, context.getSource().getPlayerOrException()))
                .then(Commands.argument("targets", EntityArgument.players())
                    .executes(context -> {
                        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                        return respecForTargets(context, targets);
                    })))
        );
    }

    // XP command implementations
    private static int setTotalXP(CommandContext<CommandSourceStack> context, double amount, ServerPlayer player) {
        IPlayerSkills skills = player.getCapability(AzukaarSkillsStats.PLAYER_SKILLS);
        if (skills == null) {
            context.getSource().sendFailure(Component.literal("Failed to get player skills capability"));
            return 0;
        }

        // Distribute XP evenly across all paths
        int pathCount = IPlayerSkills.PATH_NAMES.length;
        double xpPerPath = amount / pathCount;
        
        for (String pathName : IPlayerSkills.PATH_NAMES) {
            skills.setExperience(pathName, xpPerPath);
        }
        
        syncToClient(player);
        context.getSource().sendSuccess(() -> Component.literal("Set total XP to " + amount + " for " + player.getName().getString()), true);
        return 1;
    }

    private static int setTotalXPForTargets(CommandContext<CommandSourceStack> context, double amount, Collection<ServerPlayer> targets) {
        for (ServerPlayer target : targets) {
            setTotalXP(context, amount, target);
        }
        context.getSource().sendSuccess(() -> Component.literal("Set total XP to " + amount + " for " + targets.size() + " players"), true);
        return targets.size();
    }

    private static int setPathXP(CommandContext<CommandSourceStack> context, String pathName, double amount, ServerPlayer player) {
        IPlayerSkills skills = player.getCapability(AzukaarSkillsStats.PLAYER_SKILLS);
        if (skills == null) {
            context.getSource().sendFailure(Component.literal("Failed to get player skills capability"));
            return 0;
        }

        skills.setExperience(pathName, amount);
        syncToClient(player);
        context.getSource().sendSuccess(() -> Component.literal("Set XP for path " + pathName + " to " + amount + " for " + player.getName().getString()), true);
        return 1;
    }

    private static int setPathXPForTargets(CommandContext<CommandSourceStack> context, String pathName, double amount, Collection<ServerPlayer> targets) {
        for (ServerPlayer target : targets) {
            setPathXP(context, pathName, amount, target);
        }
        context.getSource().sendSuccess(() -> Component.literal("Set XP for path " + pathName + " to " + amount + " for " + targets.size() + " players"), true);
        return targets.size();
    }

    private static int addTotalXP(CommandContext<CommandSourceStack> context, double amount, ServerPlayer player) {
        // Distribute XP evenly across all paths
        int pathCount = IPlayerSkills.PATH_NAMES.length;
        double xpPerPath = amount / pathCount;
        
        for (String pathName : IPlayerSkills.PATH_NAMES) {
            PlayerData.addExperienceServerSide(pathName, xpPerPath, player, player.position());
        }
        
        context.getSource().sendSuccess(() -> Component.literal("Added " + amount + " total XP to " + player.getName().getString()), true);
        return 1;
    }

    private static int addTotalXPForTargets(CommandContext<CommandSourceStack> context, double amount, Collection<ServerPlayer> targets) {
        for (ServerPlayer target : targets) {
            addTotalXP(context, amount, target);
        }
        context.getSource().sendSuccess(() -> Component.literal("Added " + amount + " total XP to " + targets.size() + " players"), true);
        return targets.size();
    }

    private static int addPathXP(CommandContext<CommandSourceStack> context, String pathName, double amount, ServerPlayer player) {
        PlayerData.addExperienceServerSide(pathName, amount, player, player.position());
        context.getSource().sendSuccess(() -> Component.literal("Added " + amount + " XP to path " + pathName + " for " + player.getName().getString()), true);
        return 1;
    }

    private static int addPathXPForTargets(CommandContext<CommandSourceStack> context, String pathName, double amount, Collection<ServerPlayer> targets) {
        for (ServerPlayer target : targets) {
            addPathXP(context, pathName, amount, target);
        }
        context.getSource().sendSuccess(() -> Component.literal("Added " + amount + " XP to path " + pathName + " for " + targets.size() + " players"), true);
        return targets.size();
    }

    private static int getXP(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        IPlayerSkills skills = player.getCapability(AzukaarSkillsStats.PLAYER_SKILLS);
        if (skills == null) {
            context.getSource().sendFailure(Component.literal("Failed to get player skills capability"));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.literal("=== XP for " + player.getName().getString() + " ==="), false);
        context.getSource().sendSuccess(() -> Component.literal("Main Level: " + PlayerData.getMainLevel(player)), false);
        
        double totalXP = 0;
        
        for (String pathName : IPlayerSkills.PATH_NAMES) {
            double pathXP = skills.getExperience(pathName);
            int pathLevel = (int) skills.getLevel(pathName);
            totalXP += pathXP;
            context.getSource().sendSuccess(() -> Component.literal(pathName + ": " + pathXP + " XP (Level " + pathLevel + ")"), false);
        }
        
        final double totalXPFinal = totalXP;
        context.getSource().sendSuccess(() -> Component.literal("Total XP: " + totalXPFinal), false);
        return 1;
    }

    // Level command implementations
    private static int setLevel(CommandContext<CommandSourceStack> context, int level, ServerPlayer player) {
        double requiredXP = IPlayerSkills.getTotalXpForLevel(level);
        
        // Distribute XP evenly across all paths to achieve the desired level
        int pathCount = IPlayerSkills.PATH_NAMES.length;
        double xpPerPath = requiredXP / pathCount;
        
        IPlayerSkills skills = player.getCapability(AzukaarSkillsStats.PLAYER_SKILLS);
        if (skills == null) {
            context.getSource().sendFailure(Component.literal("Failed to get player skills capability"));
            return 0;
        }
        
        for (String pathName : IPlayerSkills.PATH_NAMES) {
            skills.setExperience(pathName, xpPerPath);
        }
        
        syncToClient(player);
        context.getSource().sendSuccess(() -> Component.literal("Set level to " + level + " for " + player.getName().getString()), true);
        return 1;
    }

    private static int setLevelForTargets(CommandContext<CommandSourceStack> context, int level, Collection<ServerPlayer> targets) {
        for (ServerPlayer target : targets) {
            setLevel(context, level, target);
        }
        context.getSource().sendSuccess(() -> Component.literal("Set level to " + level + " for " + targets.size() + " players"), true);
        return targets.size();
    }

    private static int getLevel(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        int mainLevel = PlayerData.getMainLevel(player);
        context.getSource().sendSuccess(() -> Component.literal("Main level for " + player.getName().getString() + ": " + mainLevel), false);
        return 1;
    }

    // Skill points command implementations
    private static int setSkillPoints(CommandContext<CommandSourceStack> context, int amount, ServerPlayer player) {
        IPlayerSkills skills = player.getCapability(AzukaarSkillsStats.PLAYER_SKILLS);
        if (skills == null) {
            context.getSource().sendFailure(Component.literal("Failed to get player skills capability"));
            return 0;
        }

        skills.setSkillPoints(amount);
        syncToClient(player);
        context.getSource().sendSuccess(() -> Component.literal("Set skill points to " + amount + " for " + player.getName().getString()), true);
        return 1;
    }

    private static int setSkillPointsForTargets(CommandContext<CommandSourceStack> context, int amount, Collection<ServerPlayer> targets) {
        for (ServerPlayer target : targets) {
            setSkillPoints(context, amount, target);
        }
        context.getSource().sendSuccess(() -> Component.literal("Set skill points to " + amount + " for " + targets.size() + " players"), true);
        return targets.size();
    }

    private static int addSkillPoints(CommandContext<CommandSourceStack> context, int amount, ServerPlayer player) {
        PlayerData.addSkillPointServerSide(amount, player);
        context.getSource().sendSuccess(() -> Component.literal("Added " + amount + " skill points to " + player.getName().getString()), true);
        return 1;
    }

    private static int addSkillPointsForTargets(CommandContext<CommandSourceStack> context, int amount, Collection<ServerPlayer> targets) {
        for (ServerPlayer target : targets) {
            addSkillPoints(context, amount, target);
        }
        context.getSource().sendSuccess(() -> Component.literal("Added " + amount + " skill points to " + targets.size() + " players"), true);
        return targets.size();
    }

    private static int getSkillPoints(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        int skillPoints = PlayerData.getSkillPoints(player);
        context.getSource().sendSuccess(() -> Component.literal("Skill points for " + player.getName().getString() + ": " + skillPoints), false);
        return 1;
    }

    // Cooldown command implementations
    private static int resetCooldowns(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        IPlayerSkills skills = player.getCapability(AzukaarSkillsStats.PLAYER_SKILLS);
        if (skills == null) {
            context.getSource().sendFailure(Component.literal("Failed to get player skills capability"));
            return 0;
        }

        // Clear all cooldowns by setting them to 0
        for (String skillId : skills.getAllSkillCooldowns().keySet()) {
            skills.setSkillCooldown(skillId, 0);
        }

        syncToClient(player);
        context.getSource().sendSuccess(() -> Component.literal("Reset all cooldowns for " + player.getName().getString()), true);
        return 1;
    }

    private static int resetCooldownsForTargets(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
        for (ServerPlayer target : targets) {
            resetCooldowns(context, target);
        }
        context.getSource().sendSuccess(() -> Component.literal("Reset all cooldowns for " + targets.size() + " players"), true);
        return targets.size();
    }

    // Respec command implementations
    private static int respec(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        IPlayerSkills skills = player.getCapability(AzukaarSkillsStats.PLAYER_SKILLS);
        if (skills == null) {
            context.getSource().sendFailure(Component.literal("Failed to get player skills capability"));
            return 0;
        }

        // Count total skill points spent
        int pointsSpent = 0;
        for (int level : skills.getAllSkills().values()) {
            pointsSpent += level;
        }

        if (pointsSpent == 0) {
            context.getSource().sendFailure(Component.literal("No skill points to refund for " + player.getName().getString()));
            return 0;
        }

        final int totalPointsSpent = pointsSpent;

        // Clear all skills
        skills.setAllSkills(new java.util.HashMap<>());

        // Refund the skill points
        skills.addSkillPoints(totalPointsSpent);

        syncToClient(player);
        context.getSource().sendSuccess(() -> Component.literal("Respec complete! Refunded " + totalPointsSpent + " skill points for " + player.getName().getString()), true);
        return 1;
    }

    private static int respecForTargets(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
        for (ServerPlayer target : targets) {
            respec(context, target);
        }
        context.getSource().sendSuccess(() -> Component.literal("Respec complete for " + targets.size() + " players"), true);
        return targets.size();
    }

    // Helper method to sync data to client
    private static void syncToClient(ServerPlayer player) {
        var provider = player.getData(AzukaarSkillsStats.PLAYER_SKILLS_ATTACHMENT.get());
        if (provider != null) {
            provider.syncToClient(player);
        }
    }
}