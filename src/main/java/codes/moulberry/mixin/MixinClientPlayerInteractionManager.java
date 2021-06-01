package codes.moulberry.mixin;

import codes.moulberry.ToolMenuManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class MixinClientPlayerInteractionManager {

    @Shadow private GameMode gameMode;
    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private ClientPlayNetworkHandler networkHandler;

    @Shadow protected abstract void sendPlayerAction(PlayerActionC2SPacket.Action action, BlockPos pos, Direction direction);

    @Shadow public abstract boolean breakBlock(BlockPos pos);

    @Inject(method = "interactBlock", at=@At("HEAD"), cancellable = true)
    public void interactBlock(ClientPlayerEntity player, ClientWorld world, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if(ToolMenuManager.getInstance().isOverriding() &&
                this.client.player != null &&
                this.client.world.getWorldBorder().contains(hitResult.getBlockPos()) &&
                this.gameMode == GameMode.CREATIVE) {
            System.out.println("a");
            ItemStack oldStack = this.client.player.inventory.main.get(0);
            networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(36, ToolMenuManager.getInstance().getStack()));
            networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(hand, hitResult));
            networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(36, oldStack));

            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    @Inject(method = "interactItem", at=@At("HEAD"), cancellable = true)
    public void interactItem(PlayerEntity player, World world, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if(ToolMenuManager.getInstance().isOverriding() &&
                this.client.player != null &&
                this.gameMode == GameMode.CREATIVE) {
            System.out.println("b");
            ItemStack oldStack = this.client.player.inventory.main.get(0);
            networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(36, ToolMenuManager.getInstance().getStack()));
            this.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(hand));
            networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(36, oldStack));

            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    @Inject(method = "attackBlock", at=@At("HEAD"), cancellable = true)
    public void attackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if(ToolMenuManager.getInstance().isOverriding() &&
                this.client.player != null &&
                !this.client.player.isBlockBreakingRestricted(this.client.world, pos, this.gameMode) &&
                this.client.world.getWorldBorder().contains(pos) &&
                this.gameMode == GameMode.CREATIVE) {
            System.out.println("c");
            ItemStack oldStack = this.client.player.inventory.main.get(0);
            networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(36, ToolMenuManager.getInstance().getStack()));
            this.sendPlayerAction(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, direction);
            this.breakBlock(pos);
            networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(36, oldStack));

            cir.setReturnValue(true);
        }
    }

}
