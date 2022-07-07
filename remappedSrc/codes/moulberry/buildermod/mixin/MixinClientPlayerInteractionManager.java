package codes.moulberry.buildermod.mixin;

import codes.moulberry.buildermod.Capabilities;
import codes.moulberry.buildermod.integration.Integration;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinClientPlayerInteractionManager {

    @Inject(method="interactBlock", at=@At("HEAD"), cancellable = true)
    public void interactBlock(ClientPlayerEntity player, ClientWorld world, Hand hand,
                              BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (Capabilities.REPLACE_MODE.isEnabled()) {
            ItemStack itemStack = player.getStackInHand(hand);
            if (itemStack.isEmpty() || player.getItemCooldownManager().isCoolingDown(itemStack.getItem())) {
                cir.setReturnValue(ActionResult.PASS);
            }

            Item item = itemStack.getItem();

            if (item instanceof BlockItem blockItem) {
                BlockState existingState = world.getBlockState(hitResult.getBlockPos());
                BlockState thisState = blockItem.getBlock().getStateWithProperties(existingState);

                Integration.setBlock(hitResult.getBlockPos(), thisState);

                cir.setReturnValue(ActionResult.FAIL);
            }
        }
    }

}
