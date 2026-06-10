package com.terraforged.mod.hooks;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;

public class BoatHook {
    public static boolean floatTheBoat(Boat boat) {
        if (!(boat.getFirstPassenger() instanceof Player)) {
            return false;
        }
        Vec3 pos = boat.position();
        double targetY = (double)(boat.getWaterLevelAbove() - boat.getBbHeight()) + 0.101;
        double deltaY = targetY - pos.y;
        if (deltaY > 0.8) {
            return false;
        }
        double lerp = deltaY > 0.01 ? 0.5 : 1.0;
        boat.setPos(pos.x, pos.y + deltaY * lerp, pos.z);
        return true;
    }
}
