package codes.moulberry.mixin;

import codes.moulberry.BuilderMod;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {

    @Inject(method="close", at=@At("HEAD"), cancellable = true)
    public void onClose(CallbackInfo ci) {
        try {
            BuilderMod.getInstance().saveConfig();
        } catch(Exception ignored) { }
    }

}
