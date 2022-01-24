package codes.moulberry.mixin;

import codes.moulberry.ToolMenuManager;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HeldItemRenderer.class)
public abstract class MixinHeldItemRenderer {

    @Shadow
    protected abstract void renderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta,
                                                  float pitch, Hand hand, float swingProgress, ItemStack item,
                                                  float equipProgress, MatrixStack matrices,
                                                  VertexConsumerProvider vertexConsumers, int light);

    @Redirect(method="renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
            at=@At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderFirstPersonItem" +
                            "(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;" +
                            "FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;" +
                            "Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
                    ordinal = 0
            )
    )
    public void renderItem_renderFirstPersonItem(HeldItemRenderer renderer, AbstractClientPlayerEntity player,
                                                 float tickDelta, float pitch, Hand hand, float swingProgress,
                                                 ItemStack item, float equipProgress, MatrixStack matrices,
                                                 VertexConsumerProvider vertexConsumers, int light) {
        if(ToolMenuManager.getInstance().isOverriding()) {
            item = ToolMenuManager.getInstance().getStack();
        }
        renderFirstPersonItem(player, tickDelta, pitch, hand, swingProgress, item, equipProgress, matrices, vertexConsumers, light);
    }


}
