package codes.moulberry.buildermod.mixin;

import codes.moulberry.buildermod.Capabilities;
import codes.moulberry.buildermod.integration.Integration;
import codes.moulberry.buildermod.macrotool.script.parser.lexer.tokens.BlockToken;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.command.argument.BlockStateArgument;
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

import java.util.Locale;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinClientPlayerInteractionManager {

    @Inject(method="interactBlock", at=@At("HEAD"), cancellable = true)
    public void interactBlock(ClientPlayerEntity player, ClientWorld world, Hand hand,
                              BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (Capabilities.REPLACE_MODE.isEnabled()) {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.isEmpty() || player.getItemCooldownManager().isCoolingDown(stack.getItem())) {
                cir.setReturnValue(ActionResult.PASS);
            }

            try {
                String id = !stack.hasNbt() || !stack.getNbt().contains("CustomPlacerBlockState") ? null : stack.getNbt().getString("CustomPlacerBlockState");
                if (id != null) {
                    var parser = new BlockArgumentParser(new StringReader(id.toLowerCase(Locale.ROOT)), false);
                    BlockState block = parser.parse(false).getBlockState();

                    Integration.setBlock(hitResult.getBlockPos(), block).send();

                    cir.setReturnValue(ActionResult.FAIL);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Item item = stack.getItem();

            if (item instanceof BlockItem blockItem) {
                BlockState existingState = world.getBlockState(hitResult.getBlockPos());
                BlockState thisState = blockItem.getBlock().getStateWithProperties(existingState);

                Integration.setBlock(hitResult.getBlockPos(), thisState).send();

                cir.setReturnValue(ActionResult.FAIL);
            }
        }
    }

}
