package codes.moulberry.buildermod.mixin;

import codes.moulberry.buildermod.Capabilities;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity extends LivingEntity {

    protected MixinPlayerEntity() {
        super(null, null);
    }

    @Shadow public abstract void travel(Vec3d movementInput);

    @Inject(method="travel", at=@At(value = "HEAD"), cancellable = true)
    public void travel(Vec3d movementInput, CallbackInfo ci) {
        if (Capabilities.ENHANCED_FLIGHT.isEnabled() && (Object)this instanceof ClientPlayerEntity player) {
            if (player.getAbilities().flying && !player.hasVehicle()) {
                double sin = -Math.sin(Math.toRadians(player.getPitch()));
                double cos = Math.cos(Math.toRadians(player.getPitch()));

                float sprintMult = MinecraftClient.getInstance().options.keySprint.isPressed() ? 2.5f : 1f;

                float movementY = 0;
                if (MinecraftClient.getInstance().options.keySneak.isPressed()) {
                    movementY -= sprintMult;
                }
                if (MinecraftClient.getInstance().options.keyJump.isPressed()) {
                    movementY += sprintMult;
                }

                movementInput = new Vec3d(movementInput.x, movementY+sin*movementInput.z,
                        cos*movementInput.z);

                float originalStrafingSpeed = player.airStrafingSpeed;
                boolean originalHasNoGravy = player.hasNoGravity();
                player.airStrafingSpeed = player.getAbilities().getFlySpeed() * sprintMult * 10;

                player.setVelocity(Vec3d.ZERO);
                player.setNoGravity(true);
                super.travel(movementInput);
                player.setVelocity(Vec3d.ZERO);

                player.setNoGravity(originalHasNoGravy);
                player.airStrafingSpeed = originalStrafingSpeed;

                ci.cancel();
            }
        }
    }

}
