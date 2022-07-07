package codes.moulberry.buildermod.mixin;

import codes.moulberry.buildermod.CustomBlocks;
import codes.moulberry.buildermod.Capabilities;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class MixinAbstractBlock {

    @Shadow protected abstract BlockState asBlockState();

    @Shadow public abstract VoxelShape getOutlineShape(BlockView world, BlockPos pos, ShapeContext context);

    @Inject(method = "getStateForNeighborUpdate", at = @At("HEAD"), cancellable = true)
    private void onNeighborUpdate(Direction direction, BlockState neighborState, WorldAccess world,
                                  BlockPos pos, BlockPos neighborPos, CallbackInfoReturnable<BlockState> cir) {
        if (Capabilities.NO_NEIGHBOR_UPDATES.isEnabled() && world instanceof ClientWorld) {
            cir.setReturnValue(this.asBlockState());
        }
    }

    private static boolean OVERRIDE_SHAPE = true;

    private static final VoxelShape SHAPE = Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);
    @Inject(method = "getOutlineShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/ShapeContext;)Lnet/minecraft/util/shape/VoxelShape;",
            at = @At("RETURN"), cancellable = true)
    private void getOutlineShape(BlockView world, BlockPos pos, ShapeContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (!OVERRIDE_SHAPE) return;

        if (CustomBlocks.getCustomBlock(this.asBlockState()) != ItemStack.EMPTY &&
                cir.getReturnValue() != VoxelShapes.fullCube()) {
            cir.setReturnValue(SHAPE);
        }
    }

    @Inject(method="getCollisionShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/ShapeContext;)Lnet/minecraft/util/shape/VoxelShape;",
            at = @At("RETURN"), cancellable = true)
    private void getCollisionShape(BlockView world, BlockPos pos, ShapeContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (cir.getReturnValue() == SHAPE) {
            OVERRIDE_SHAPE = false;
            cir.setReturnValue(this.getOutlineShape(world, pos, context));
            OVERRIDE_SHAPE = true;
        }
    }

}
