package codes.moulberry.buildermod.mixin;

import codes.moulberry.buildermod.Capabilities;
import codes.moulberry.buildermod.integration.Integration;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinClientPlayerInteractionManager {

    @Inject(method="interactBlock", at=@At("HEAD"), cancellable = true)
    public void interactBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (Capabilities.REPLACE_MODE.isEnabled()) {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.isEmpty() || player.getItemCooldownManager().isCoolingDown(stack.getItem())) {
                cir.setReturnValue(ActionResult.PASS);
            }

            try {
                String id = !stack.hasNbt() || !stack.getNbt().contains("CustomPlacerBlockState") ? null : stack.getNbt().getString("CustomPlacerBlockState");
                if (id != null) {
                    var parser = BlockArgumentParser.block(Registry.BLOCK, id.toLowerCase(Locale.ROOT), false);
                    BlockState block = parser.blockState();

                    Integration.setBlock(hitResult.getBlockPos(), block).send();

                    cir.setReturnValue(ActionResult.FAIL);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Item item = stack.getItem();

            if (item instanceof BlockItem blockItem) {
                if (stack.hasNbt() && stack.getNbt().contains("BlockStateTag")) {
                    BlockState state = blockItem.getBlock().getDefaultState();


                    NbtCompound tag = stack.getNbt().getCompound("BlockStateTag");
                    StateManager<Block, BlockState> stateManager = state.getBlock().getStateManager();

                    for (String id : tag.getKeys()) {
                        Property<?> property = stateManager.getProperty(id);
                        if (property != null) {
                            state = with(state, property, tag.get(id).asString());
                        }
                    }

                    Integration.setBlock(hitResult.getBlockPos(), state).send();

                    cir.setReturnValue(ActionResult.FAIL);
                    return;
                }


                BlockState existingState = player.world.getBlockState(hitResult.getBlockPos());
                BlockState thisState = blockItem.getBlock().getStateWithProperties(existingState);

                Integration.setBlock(hitResult.getBlockPos(), thisState).send();

                cir.setReturnValue(ActionResult.FAIL);
            }
        }
    }

    @Unique
    private static <T extends Comparable<T>> BlockState with(BlockState state, Property<T> property, String name) {
        return property.parse(name).map((value) -> state.with(property, value)).orElse(state);
    }

}
