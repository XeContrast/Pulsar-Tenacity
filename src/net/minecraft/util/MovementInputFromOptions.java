package net.minecraft.util;

import dev.tenacity.Tenacity;
import dev.tenacity.event.impl.player.MovementInputUpdateEvent;
import dev.tenacity.module.impl.movement.Speed;
import net.minecraft.client.settings.GameSettings;

public class MovementInputFromOptions extends MovementInput {
    private final GameSettings gameSettings;

    public MovementInputFromOptions(GameSettings gameSettingsIn) {
        this.gameSettings = gameSettingsIn;
    }

    public void updatePlayerMoveState() {
        this.moveStrafe = 0.0F;
        this.moveForward = 0.0F;

        if (this.gameSettings.keyBindForward.isKeyDown()) {
            ++this.moveForward;
        }

        if (this.gameSettings.keyBindBack.isKeyDown()) {
            --this.moveForward;
        }

        if (this.gameSettings.keyBindLeft.isKeyDown()) {
            ++this.moveStrafe;
        }

        if (this.gameSettings.keyBindRight.isKeyDown()) {
            --this.moveStrafe;
        }
        final MovementInputUpdateEvent event = new MovementInputUpdateEvent(moveStrafe, moveForward, jump, sneak);
        Tenacity.INSTANCE.eventProtocol.handleEvent(event);
        this.jump = this.gameSettings.keyBindJump.isKeyDown() && !(Tenacity.INSTANCE.moduleCollection.getModule(Speed.class).shouldPreventJumping());
        this.sneak = this.gameSettings.keyBindSneak.isKeyDown();

        if (this.sneak) {
            this.moveStrafe = (float) ((double) this.moveStrafe * 0.3D);
            this.moveForward = (float) ((double) this.moveForward * 0.3D);
        }
    }
}
