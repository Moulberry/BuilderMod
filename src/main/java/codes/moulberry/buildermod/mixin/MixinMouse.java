package codes.moulberry.buildermod.mixin;

import codes.moulberry.buildermod.WheelGUI;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MixinMouse {

    @Inject(method="onMouseButton", at=@At("HEAD"), cancellable = true)
    public void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (action == 1 && WheelGUI.isWheelOpen()) {
            if (!WheelGUI.clicked()) {
                ci.cancel();
            }
        }
    }

}
