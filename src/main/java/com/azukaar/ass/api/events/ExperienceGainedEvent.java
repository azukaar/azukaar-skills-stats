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
public class ExperienceGainedEvent extends Event implements ICancellableEvent {
    private final Level level;
    private final Player player;
    private final String expertisePath;
    private double amount;
    private double expertiseAmount;
    private final Vec3 position;
    private boolean isCanceled = false;

    /**
     * Creates a new ExperienceGainedEvent.
     * 
     * @param player The player gaining expertise
     * @param expertisePath The expertise path (e.g., "warrior", "miner")
     * @param amount The amount of XP gained
     * @param position The position where the XP was gained
     */
    public ExperienceGainedEvent(Player player, String expertisePath, double amount, double expertiseAmount, Vec3 position,
                                 Level level) {
        this.level = level;
        this.player = player;
        this.expertisePath = expertisePath;
        this.amount = amount;
        this.expertiseAmount = expertiseAmount;
        this.position = position;
    }

    /**
     * @return The player gaining expertise
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * @return The expertise path (e.g., "warrior", "miner")
     */
    public String getExpertisePath() {
        return expertisePath;
    }

    /**
     * @return The level where the XP was gained
     */
    public Level getLevel() {
        return level;
    }

    /**
     * @return The amount of XP gained
     */
    public double getAmount() {
        return amount;
    }

    /**
     * @return The amount of expertise XP gained
     */
    public double getExpertiseAmount() {
        return expertiseAmount;
    }

    /**
     * Set the amount of XP to be gained
     * @param amount The new amount of XP
     */
    public void setAmount(double amount) {
        this.amount = amount;
    }

    /**
     * @return The position where the XP was gained
     */
    public Vec3 getPosition() {
        return position;
    }

    @Override
    public boolean isCanceled() {
        return this.isCanceled;
    }

    @Override
    public void setCanceled(boolean cancel) {
        this.isCanceled = cancel;
    }
    
    /**
     * Event fired before expertise XP is added to a player.
     * Can be cancelled to prevent the XP gain.
     */
    public static class Pre extends ExperienceGainedEvent {
        public Pre(Player player, String expertisePath, double amount, double expertiseAmount, Vec3 position) {
            super(player, expertisePath, amount, expertiseAmount, position, player.level());
        }
    }
    
    /**
     * Event fired after expertise XP has been added to a player.
     * Cannot be cancelled.
     */
    public static class Post extends ExperienceGainedEvent {
        private final double oldLevel;
        private final double newLevel;
        
        public Post(Player player, String expertisePath, double amount, double expertiseAmount, Vec3 position, 
                   double oldLevel, double newLevel) {
            super(player, expertisePath, amount, expertiseAmount, position, player.level());
            this.oldLevel = oldLevel;
            this.newLevel = newLevel;
        }
        
        /**
         * @return The player's level before XP gain
         */
        public double getOldLevel() {
            return oldLevel;
        }
        
        /**
         * @return The player's level after XP gain
         */
        public double getNewLevel() {
            return newLevel;
        }
        
        /**
         * @return Whether the player leveled up from this XP gain
         */
        public boolean hasLeveledUp() {
            return Math.floor(newLevel) > Math.floor(oldLevel);
        }
        
        @Override
        public boolean isCanceled() {
            return false;
        }
        
        @Override
        public void setCanceled(boolean cancel) {
            // Post event cannot be cancelled
        }
    }
}
