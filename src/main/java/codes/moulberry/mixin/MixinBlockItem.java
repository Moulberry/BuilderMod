package codes.moulberry.mixin;

import codes.moulberry.StateManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class MixinBlockItem {

    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    public void onBlockPlace(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (StateManager.replaceMode && context.getWorld() instanceof ClientWorld) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

}
