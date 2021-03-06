package codes.moulberry.buildermod.mixin;

import codes.moulberry.buildermod.ToolMenuManager;
import codes.moulberry.buildermod.WheelGUI;
import codes.moulberry.buildermod.render.ScreenRendererHook;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class MixinInGameHud extends DrawableHelper {

    @Shadow private int scaledHeight;
    @Shadow private int scaledWidth;
    @Shadow protected abstract PlayerEntity getCameraPlayer();
    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private static Identifier WIDGETS_TEXTURE;

    @Shadow protected abstract void renderHotbarItem(int x, int y, float tickDelta, PlayerEntity player, ItemStack stack, int seed);

    @Inject(method="render", at=@At("RETURN"))
    public void render(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        ScreenRendererHook.render(matrices, this.client, this.scaledWidth, this.scaledHeight, tickDelta);
    }

    @Inject(method="renderHotbar", at=@At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;isEmpty()Z",
            ordinal = 0,
            shift = At.Shift.BEFORE)
    )
    public void renderHotbar_draw(float tickDelta, MatrixStack matrices, CallbackInfo ci) {
        if (MinecraftClient.getInstance().interactionManager.getCurrentGameMode() != GameMode.CREATIVE) return;

        int i = scaledWidth / 2;
        this.drawTexture(matrices, i + 91, this.scaledHeight - 23, 53, 22, 29, 24);

        if(ToolMenuManager.getInstance().isOverriding()) {
            this.drawTexture(matrices, i + 91 + 6,
                    this.scaledHeight - 22 - 1, 0, 22, 24, 23);
        }

        this.renderHotbarItem(i + 91 + 10, this.scaledHeight - 19, tickDelta, getCameraPlayer(),
                ToolMenuManager.getInstance().getStack(), 0);
        RenderSystem.enableBlend();

        RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
    }

    @Redirect(method="renderHotbar", at=@At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(Lnet/minecraft/client/util/math/MatrixStack;IIIIII)V",
            ordinal = 1
    ))
    public void renderHotbar_drawSlotIndicator(InGameHud hud, MatrixStack matrices, int x, int y, int u, int v, int width, int height) {
        if(!ToolMenuManager.getInstance().isOverriding()) {
            hud.drawTexture(matrices, x, y, u, v, width, height);
        }
    }

}
