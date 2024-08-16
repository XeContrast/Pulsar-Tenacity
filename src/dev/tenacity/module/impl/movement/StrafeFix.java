/*
 * KevinClient-Reborn
 * This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dev.tenacity.module.impl.movement;

import dev.tenacity.event.impl.player.MotionEvent;
import dev.tenacity.event.impl.player.MovementInputUpdateEvent;
import dev.tenacity.module.Category;
import dev.tenacity.module.Module;
import net.minecraft.util.MathHelper;

public class StrafeFix extends Module {
    private float fixedYaw = 0f;

    public StrafeFix() {
        super("StrafeFix", Category.MOVEMENT, "CaoDaBi");//招展恒我要操你的逼
    }

    boolean fixed;
    boolean disabled;
    private static boolean tickForceForward = false;

    @Override
    public void onDisable() {
        disabled = true;
    }

    @Override
    public void onMovementInputUpdateEvent(MovementInputUpdateEvent event) {
        final float forward = event.getMoveForward();
        final float strafe = event.getMoveStrafe();
        final float yaw = fixedYaw = mc.thePlayer.getRotationYawHead();
        fixed = false;
        if (disabled || (forward == 0 && strafe == 0)) {
            return;
        }
        fixed = true;

        final double angle = MathHelper.wrapAngleTo180_double(Math.toDegrees(direction(mc.thePlayer.rotationYaw, forward, strafe)));


        float closestForward = 0, closestStrafe = 0, closestDifference = Float.MAX_VALUE;

        for (float predictedForward = -1f; predictedForward <= 1f; predictedForward += 1f) {
            if (tickForceForward) {
                predictedForward = 1f;
                tickForceForward = false;
            }
            for (float predictedStrafe = -1f; predictedStrafe <= 1f; predictedStrafe += 1f) {
                if (predictedStrafe == 0 && predictedForward == 0) continue;

                final double predictedAngle = MathHelper.wrapAngleTo180_double(Math.toDegrees(direction(yaw, predictedForward, predictedStrafe)));
                final double difference = Math.abs(angle - predictedAngle);

                if (difference < closestDifference) {
                    closestDifference = (float) difference;
                    closestForward = predictedForward;
                    closestStrafe = predictedStrafe;
                }
            }
        }

        event.setMoveForward(closestForward);
        event.setMoveStrafe(closestStrafe);

    }

    private static double direction(float rotationYaw, final double moveForward, final double moveStrafing) {
        if (moveForward < 0F) {
            rotationYaw += 180F;
        }

        float forward = 1F;

        if (moveForward < 0F) {
            forward = -0.5F;
        } else if (moveForward > 0F) {
            forward = 0.5F;
        }

        if (moveStrafing > 0F) {
            rotationYaw -= 90F * forward;
        }
        if (moveStrafing < 0F) {
            rotationYaw += 90F * forward;
        }

        return Math.toRadians(rotationYaw);
    }

    @Override
    public void onMotionEvent(MotionEvent event) {
        if (fixed) {
            event.setYaw(fixedYaw);
        }
    }
}
