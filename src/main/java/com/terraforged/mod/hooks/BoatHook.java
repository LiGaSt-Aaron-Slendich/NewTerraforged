package com.terraforged.mod.hooks;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;

public class BoatHook {
   public static boolean floatTheBoat(Boat boat) {
      if (!(boat.getFirstPassenger() instanceof Player)) {
         return false;
      } else {
         Vec3 vec3 = boat.position();
         double d0 = boat.getWaterLevelAbove() - boat.getBbHeight() + 0.101;
         double d1 = d0 - vec3.y;
         if (d1 > 0.8) {
            return false;
         } else {
            double d2 = d1 > 0.01 ? 0.5 : 1.0;
            boat.setPos(vec3.x, vec3.y + d1 * d2, vec3.z);
            return true;
         }
      }
   }
}
