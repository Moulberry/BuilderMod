package codes.moulberry.buildermod.mixin;

import codes.moulberry.buildermod.ToolMenuManager;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInventory.class)
public class MixinPlayerInventory {

    @Shadow public int selectedSlot;

    @Inject(method="scrollInHotbar", at=@At("RETURN"))
    public void scrollInHotbar(double scrollAmount, CallbackInfo ci) {
        if (scrollAmount > 0.0D) {
            scrollAmount = 1.0D;
        }

        if (scrollAmount < 0.0D) {
            scrollAmount = -1.0D;
        }

        this.selectedSlot = ToolMenuManager.getInstance().onScroll((int)scrollAmount, this.selectedSlot);
    }


}
