package com.azukaar.ass.api.events;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Event fired when a player gains expertise XP in any path.
 * Can be cancelled to prevent the XP gain.
 */
public class SkillPointSpent extends Event {
    private final Level level;
    private final Player player;
    private double amount;
    private String skillName;

    /**
     * Creates a new ExperienceGainedEvent.
     * 
     * @param player The player gaining expertise
     * @param amount The amount of XP gained
     */
    public SkillPointSpent(Player player, double amount, String skillName,
                                 Level level) {
        this.level = level;
        this.player = player;
        this.amount = amount;
        this.skillName = skillName;
    }

    /**
     * @return The player gaining expertise
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * @return The level where the XP was gained
     */
    public Level getLevel() {
        return level;
    }

    /**
     * @return The amount of levels gained
     */
    public double getAmount() {
        return amount;
    }

    /**
     * @return The name of the skill that was spent
     */
    public String getSkillName() {
        return skillName;
    }

    /**
     * Set the amount of XP to be gained
     * @param amount The new amount of XP
     */
    public void setAmount(double amount) {
        this.amount = amount;
    }
}
