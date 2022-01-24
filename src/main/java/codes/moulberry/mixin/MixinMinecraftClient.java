package codes.moulberry.mixin;

import codes.moulberry.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
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

    @Inject(method="setWorld", at=@At("HEAD"))
    public void onSwitchWorld(@Nullable ClientWorld world, CallbackInfo ci) {
        try {
            BuilderMod.getInstance().saveConfig();
        } catch(Exception ignored) { }
        WorldEditCUI.getInstance().onWorldChange();
        StateManager.onWorldChange();
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

    @Redirect(method="handleInputEvents", at=@At(
            value="INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;doAttack()V"
    ))
    public void onDoAttack(MinecraftClient client) {
        boolean override = ToolMenuManager.getInstance().isOverriding() && interactionManager.getCurrentGameMode() == GameMode.CREATIVE;

        if(override && ToolMenuManager.getInstance().getStack().getItem() == Items.REDSTONE_TORCH) {
            Entity entity = getCameraEntity();
            HitResult result = entity.raycast(100, 0, !entity.isInLava() && !entity.isSubmergedInWater());

            if(result.getType() == HitResult.Type.BLOCK) {
                LaserPointer.getInstance().addPoint(result.getPos());
            }
            return;
        } else if(override && ToolMenuManager.getInstance().getStack().getItem() == Items.WOODEN_AXE) {
            Entity entity = getCameraEntity();
            HitResult result = entity.raycast(100, 0, !entity.isInLava() && !entity.isSubmergedInWater());

            if(result.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHitResult = (BlockHitResult) result;

                BlockPos pos = blockHitResult.getBlockPos();
                player.sendChatMessage("//pos1 "+pos.getX()+","+pos.getY()+","+pos.getZ());
                this.player.swingHand(Hand.MAIN_HAND);
                return;
            }
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
        if (BuilderMod.replaceModeKeyBind.wasPressed()) {
            player.sendChatMessage("/replacemode");
        }
    }

    @Redirect(method="handleInputEvents", at=@At(
            value="INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;doItemUse()V"
    ))
    public void onDoItemUse(MinecraftClient client) {
        boolean override = ToolMenuManager.getInstance().isOverriding() && interactionManager.getCurrentGameMode() == GameMode.CREATIVE;

        if(override && ToolMenuManager.getInstance().getStack().getItem() == Items.REDSTONE_TORCH) {
            LaserPointer.getInstance().startChain();
            return;
        } else if(override && ToolMenuManager.getInstance().getStack().getItem() == Items.WOODEN_AXE) {
            Entity entity = getCameraEntity();
            HitResult result = entity.raycast(100, 1, !entity.isInLava() && !entity.isSubmergedInWater());

            if(result.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHitResult = (BlockHitResult) result;

                BlockPos pos = blockHitResult.getBlockPos();
                player.sendChatMessage("//pos2 "+pos.getX()+","+pos.getY()+","+pos.getZ());
                itemUseCooldown = 4;
                return;
            }
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
