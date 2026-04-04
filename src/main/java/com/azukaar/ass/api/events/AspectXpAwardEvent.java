package com.azukaar.ass.api.events;

import com.azukaar.ass.api.AspectDefinition;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class AspectXpAwardEvent extends Event implements ICancellableEvent {
    private final Player player;
    private final AspectDefinition aspect;
    private double xpAmount;
    private final Vec3 position;
    private boolean isCanceled = false;

    public AspectXpAwardEvent(Player player, AspectDefinition aspect, double xpAmount, Vec3 position) {
        this.player = player;
        this.aspect = aspect;
        this.xpAmount = xpAmount;
        this.position = position;
    }

    public Player getPlayer() { return player; }
    public AspectDefinition getAspect() { return aspect; }
    public double getXpAmount() { return xpAmount; }
    public void setXpAmount(double xpAmount) { this.xpAmount = xpAmount; }
    public Vec3 getPosition() { return position; }

    @Override
    public boolean isCanceled() { return isCanceled; }

    @Override
    public void setCanceled(boolean cancel) { this.isCanceled = cancel; }
}
