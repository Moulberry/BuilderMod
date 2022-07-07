package codes.moulberry.buildermod.macrotool.script.impl.functions;

import codes.moulberry.buildermod.macrotool.script.ToolExecutionContext;
import codes.moulberry.buildermod.macrotool.script.impl.operations.mask.TMaskOp;
import codes.moulberry.buildermod.macrotool.script.impl.operations.update.TUpdateOp;
import codes.moulberry.buildermod.render.regions.BooleanRegion;
import codes.moulberry.buildermod.render.regions.BooleanRegionRenderer;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkCache;

import java.util.function.Consumer;

public abstract class TGenericFunction extends TFunction {

    private static final LongSet SELECTED_BLOCKS = new LongOpenHashSet();
    private static final BooleanRegion MASKED_REGION = new BooleanRegion();

    protected static final BlockPos.Mutable genericMutable = new BlockPos.Mutable();
    protected static final int SELECTION_LIMIT = 100000;

    // =============================================================================================
    // region [Helper Functions]
    protected static void raycastBlock(float tickDelta, Consumer<BlockHitResult> consumer) {
        Entity entity = MinecraftClient.getInstance().getCameraEntity();
        HitResult result = entity.raycast(200, tickDelta,
                !entity.isInLava() && !entity.isSubmergedInWater());

        if (result.getType() == HitResult.Type.BLOCK) {
            consumer.accept((BlockHitResult) result);
        }
    }

    protected static int createEncodedOffset(BlockPos playerBlockPos, long packed) {
        int x = BlockPos.unpackLongX(packed) - playerBlockPos.getX();
        int y = BlockPos.unpackLongY(packed) - playerBlockPos.getY();
        int z = BlockPos.unpackLongZ(packed) - playerBlockPos.getZ();

        int encoded = 0;
        encoded |= ((x+1024) & 2047) << 11;
        encoded |= ((y+512)  & 1023) << 22;
        encoded |= ((z+1024) & 2047);
        return encoded;
    }
    // endregion
    // =============================================================================================

    private final TMaskOp[] maskOps;
    private final TUpdateOp[] updateOps;

    public TGenericFunction(TMaskOp[] maskOps, TUpdateOp[] updateOps) {
        this.maskOps = maskOps;
        this.updateOps = updateOps;
    }

    protected abstract void preview(ToolExecutionContext context, MatrixStack matrices,
                                    Matrix4f projection, float tickDelta);
    protected abstract void update(ToolExecutionContext context, BlockPos hit);

    protected void addToRegion(ChunkCache cache, int x, int y, int z) {
        long id = BlockPos.asLong(x, y, z);
        if (!SELECTED_BLOCKS.contains(id)) {
            SELECTED_BLOCKS.add(id);

            genericMutable.set(x, y, z);

            for (TMaskOp op : maskOps) {
                if (!op.matches(cache, genericMutable)) return;
            }

            MASKED_REGION.add(x, y, z);
        }
    }

    @Override
    public void render(ToolExecutionContext context, MatrixStack matrices,
                       Matrix4f projection, float tickDelta) {
        if (MinecraftClient.getInstance().currentScreen != null) {
            MASKED_REGION.clear();
            SELECTED_BLOCKS.clear();
            return;
        }

        if (MinecraftClient.getInstance().options.keyUse.isPressed()) {
            if (MASKED_REGION.totalCubes() < SELECTION_LIMIT) {
                raycastBlock(tickDelta, (blockHitResult -> {
                    update(context, blockHitResult.getBlockPos());
                }));
            }
            BooleanRegionRenderer.render(MASKED_REGION, matrices, projection);
        } else if (MASKED_REGION.totalCubes() > 0) {
            World world = MinecraftClient.getInstance().getServer().getOverworld();

            MASKED_REGION.forEachBlock(pos -> {
                for (TUpdateOp op : updateOps) {
                    op.runIntegrated(world, pos);
                }
            });

            MASKED_REGION.clear();
            SELECTED_BLOCKS.clear();
        } else {
            preview(context, matrices, projection, tickDelta);
        }
    }

}
