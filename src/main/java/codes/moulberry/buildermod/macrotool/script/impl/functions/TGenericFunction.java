package codes.moulberry.buildermod.macrotool.script.impl.functions;

import codes.moulberry.buildermod.Identifiers;
import codes.moulberry.buildermod.macrotool.script.ToolExecutionContext;
import codes.moulberry.buildermod.macrotool.script.impl.operations.mask.TMaskOp;
import codes.moulberry.buildermod.macrotool.script.impl.operations.update.TUpdateOp;
import codes.moulberry.buildermod.render.regions.BooleanRegion;
import codes.moulberry.buildermod.render.regions.BooleanRegionRenderer;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
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

    protected static int createEncodedOffset(BlockPos playerBlockPos, BlockPos pos) {
        int x = pos.getX() - playerBlockPos.getX();
        int y = pos.getY() - playerBlockPos.getY();
        int z = pos.getZ() - playerBlockPos.getZ();

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

        if (MinecraftClient.getInstance().options.useKey.isPressed()) {
            if (MASKED_REGION.totalCubes() < SELECTION_LIMIT) {
                raycastBlock(tickDelta, (blockHitResult -> {
                    update(context, blockHitResult.getBlockPos());
                }));
            }
            BooleanRegionRenderer.render(MASKED_REGION, matrices, projection);
        } else if (MASKED_REGION.totalCubes() > 0) {
            BlockPos playerBlockPos = MinecraftClient.getInstance().player.getBlockPos();
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeVarInt(MASKED_REGION.totalCubes());
            buf.writeBlockPos(playerBlockPos);

            ShortSet palette = new ShortOpenHashSet();

            for (TUpdateOp op : updateOps) {
                op.addToPalette(palette);
            }

            buf.writeVarInt(palette.size());

            Short2IntMap paletteMap = new Short2IntOpenHashMap();
            int totalEntries = 0;
            ShortIterator shortIterator = palette.iterator();
            while (shortIterator.hasNext()) {
                short stateId = shortIterator.nextShort();
                paletteMap.put(stateId, totalEntries++);
                buf.writeShort(stateId);
            }

            MASKED_REGION.forEachBlock(pos -> {
                short stateId = 0;
                for (TUpdateOp op : updateOps) {
                    stateId = op.getNewBlockAt(pos);
                }
                buf.writeInt(createEncodedOffset(playerBlockPos, pos));
                buf.writeVarInt(paletteMap.get(stateId));
            });

            ClientPlayNetworking.send(Identifiers.SETBLOCK_MULTI, buf);

            MASKED_REGION.clear();
            SELECTED_BLOCKS.clear();
        } else {
            preview(context, matrices, projection, tickDelta);
        }
    }

}
