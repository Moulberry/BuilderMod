package codes.moulberry.buildermod.mixin;

import codes.moulberry.buildermod.*;
import codes.moulberry.buildermod.customtool.CustomTool;
import codes.moulberry.buildermod.customtool.CustomToolManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MinecraftClient.class, priority = 1001)
public abstract class MixinMinecraftClient {

    @Shadow protected abstract void doAttack();
    @Shadow @Nullable public ClientPlayerInteractionManager interactionManager;
    @Shadow @Nullable public ClientPlayerEntity player;
    @Shadow @Nullable public abstract ClientPlayNetworkHandler getNetworkHandler();

    @Shadow protected abstract void doItemUse();

    @Shadow @Nullable public ClientWorld world;

    @Shadow @Final public GameRenderer gameRenderer;

    @Shadow private int itemUseCooldown;

    @Shadow public @Nullable abstract Entity getCameraEntity();

    @Shadow protected int attackCooldown;

    @Shadow @Final private HeldItemRenderer heldItemRenderer;

    @Shadow @Final public Mouse mouse;

    @Shadow protected abstract void handleBlockBreaking(boolean bl);

    @Shadow @Nullable public Screen currentScreen;

    @Shadow @Final public GameOptions options;

    @Shadow protected abstract void reset(Screen screen);

    @Shadow @Nullable public HitResult crosshairTarget;

    @Inject(method="setWorld", at=@At("HEAD"))
    public void onSwitchWorld(@Nullable ClientWorld world, CallbackInfo ci) {
        try {
            BuilderMod.getInstance().saveConfig();
        } catch(Exception ignored) { }
        WorldEditCUI.getInstance().onWorldChange();
    }

    @Inject(method="close", at=@At("HEAD"), cancellable = true)
    public void onClose(CallbackInfo ci) {
        try {
            BuilderMod.getInstance().saveConfig();
        } catch(Exception ignored) { }
    }

    @Redirect(method="handleInputEvents", at=@At(
            value="INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;handleBlockBreaking(Z)V"
    ))
    public void onHandleBlockBreaking(MinecraftClient minecraftClient, boolean bl) {
        boolean override = ToolMenuManager.getInstance().isOverriding() && interactionManager.getCurrentGameMode() == GameMode.CREATIVE;

        Item item = ToolMenuManager.getInstance().getStack().getItem();
        if(!override || !(item == Items.REDSTONE_TORCH || item == Items.WOODEN_AXE)) {
            handleBlockBreaking(bl);
        }
    }

    private static int instabreakTimer = 0;
    @Inject(method="handleBlockBreaking", at=@At("HEAD"), cancellable = true)
    public void handleBlockBreaking(boolean breaking, CallbackInfo ci) {
        Entity entity = getCameraEntity();
        if (!breaking) {
            if (instabreakTimer < 15) instabreakTimer++;
        } else if (Capabilities.INSTABREAK.isEnabled() && this.crosshairTarget != null &&
                this.crosshairTarget.getType() == HitResult.Type.BLOCK &&
                interactionManager != null && entity != null) {
            if (instabreakTimer > 0) {
                instabreakTimer--;
                return;
            }

            float range = interactionManager.getReachDistance();
            for (int i=0; i<10; i++) {
                HitResult result = entity.raycast(range, 0, !entity.isInLava() && !entity.isSubmergedInWater());
                if (result instanceof BlockHitResult blockHitResult) {
                    this.interactionManager.attackBlock(blockHitResult.getBlockPos(), blockHitResult.getSide());
                    if (this.player != null) this.player.swingHand(Hand.MAIN_HAND);
                } else {
                    break;
                }
            }
            ci.cancel();
        }
    }

    @Redirect(method="handleInputEvents", at=@At(
            value="INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;doAttack()V"
    ))
    public void onDoAttack(MinecraftClient client) {
        boolean override = ToolMenuManager.getInstance().isOverriding() && interactionManager.getCurrentGameMode() == GameMode.CREATIVE;

        if(override && CustomToolManager.acceptTool(ToolMenuManager.getInstance().getStack().getItem(), CustomTool::leftClick)) {
            MinecraftClient.getInstance().player.swingHand(Hand.MAIN_HAND);
            return;
        }

        ItemStack oldStack = null;
        if(override) {
            oldStack = player.getInventory().main.get(0);
            getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(36, ToolMenuManager.getInstance().getStack()));
        }
        doAttack();
        if(oldStack != null) {
            getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(36, oldStack));
        }
    }

    @Inject(method="tick", at=@At("HEAD"))
    public void onTick(CallbackInfo ci) {
        if(!MinecraftClient.getInstance().options.keyUse.isPressed()) {
            LaserPointer.getInstance().endChain();
        }
        if(BuilderMod.getInstance().clearLaserKeybind.isPressed()) {
            LaserPointer.getInstance().clearAll();
        }
        if (BuilderMod.getInstance().replaceModeKeyBind.wasPressed()) {
            player.sendChatMessage("/buildmode replace");
        }
        if (BuilderMod.getInstance().wheelKeyBind.wasPressed()) {
            if (this.currentScreen == null) {
                WheelGUI.openWheel();
            }
        }
        if (!BuilderMod.getInstance().wheelKeyBind.isPressed() && WheelGUI.isWheelOpen()) {
            WheelGUI.closeWheel();
            mouse.lockCursor();
        }
        WheelGUI.tick((MinecraftClient) (Object) this);
    }

    @Inject(method="setScreen", at=@At("HEAD"))
    public void setScreen(Screen screen, CallbackInfo ci) {
        WheelGUI.closeWheel();
    }

    @Redirect(method="handleInputEvents", at=@At(
            value="INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;doItemUse()V"
    ))
    public void onDoItemUse(MinecraftClient client) {
        boolean override = ToolMenuManager.getInstance().isOverriding() && interactionManager.getCurrentGameMode() == GameMode.CREATIVE;

        if (override && CustomToolManager.acceptTool(ToolMenuManager.getInstance().getStack().getItem(), CustomTool::rightClick)) {
            itemUseCooldown = 4;
            return;
        }

        ItemStack oldStack = null;
        if(override) {
            oldStack = player.getInventory().main.get(0);
            getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(36, ToolMenuManager.getInstance().getStack()));
        }
        if(override && oldStack.isEmpty()) {
            itemUseCooldown = 4;
            ActionResult actionResult3 = this.interactionManager.interactItem(this.player, world, Hand.MAIN_HAND);
            if (actionResult3.isAccepted()) {
                if (actionResult3.shouldSwingHand()) {
                    this.player.swingHand(Hand.MAIN_HAND);
                }
                gameRenderer.firstPersonRenderer.resetEquipProgress(Hand.MAIN_HAND);
                return;
            }
        } else {
            doItemUse();
        }
        if(oldStack != null) {
            getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(36, oldStack));
        }
    }

    @Redirect(method = "doItemPick", at=@At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getPickStack(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Lnet/minecraft/item/ItemStack;"))
    public ItemStack onPickBlock(Block instance, BlockView world, BlockPos pos, BlockState state) {
        ItemStack stack = CustomBlocks.getCustomBlock(state);
        return stack.isEmpty() ? instance.getPickStack(world, pos, state) : stack;
    }

}
