package codes.moulberry.buildermod.render.regions;

import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

public class BooleanRegion extends AbstractRegion {

    private static final float EPS = 1E-4f;
    private final LongSet includedBlocks = new LongOpenHashSet();
    public final Long2ObjectMap<Vec3f> xFaces = new Long2ObjectOpenHashMap<>();
    public final Long2ObjectMap<Vec3f> yFaces = new Long2ObjectOpenHashMap<>();
    public final Long2ObjectMap<Vec3f> zFaces = new Long2ObjectOpenHashMap<>();
    private final BlockPos.Mutable min = new BlockPos.Mutable();
    private final BlockPos.Mutable max = new BlockPos.Mutable();

    public BooleanRegion() {
        super(VertexFormats.POSITION_COLOR, true, true);
    }

    @Override
    public void upload(BufferBuilder bufferBuilder) {
        BooleanRegionRenderer.uploadRegion(this, bufferBuilder);
    }

    public void clear() {
        dirty = true;
        includedBlocks.clear();
        xFaces.clear();
        yFaces.clear();
        zFaces.clear();
    }

    public BlockPos getMin() {
        return min.toImmutable();
    }

    public BlockPos getMax() {
        return max.toImmutable();
    }

    public void forEachBlock(Consumer<BlockPos.Mutable> consumer) {
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        includedBlocks.forEach((LongConsumer) packed -> {
            mutable.set(packed);
            consumer.accept(mutable);
        });
    }

    public List<BlockPos> getIncludedBlocks() {
        List<BlockPos> list = new ArrayList<>(includedBlocks.size());
        includedBlocks.forEach((LongConsumer) packed -> {
            list.add(BlockPos.fromLong(packed));
        });
        return list;
    }

    public boolean isInRegion(long packed) {
        return includedBlocks.contains(packed);
    }

    public Long2ObjectMap<BlockPos> getIncludedBlocksMap() {
        Long2ObjectMap<BlockPos> map = new Long2ObjectOpenHashMap<>(includedBlocks.size());
        includedBlocks.forEach((LongConsumer) packed -> {
            map.put(packed, BlockPos.fromLong(packed));
        });
        return map;
    }

    public void add(BlockPos pos) {
        add(pos.getX(), pos.getY(), pos.getZ());
    }

    public void add(int x, int y, int z) {
        long id = BlockPos.asLong(x, y, z);
        if (!includedBlocks.contains(id)) {
            if (includedBlocks.size() <= 0) {
                min.set(x, y, z);
                max.set(x, y, z);
            } else {
                if (x < min.getX()) min.setX(x);
                if (y < min.getY()) min.setY(y);
                if (z < min.getZ()) min.setZ(z);
                if (x > max.getX()) max.setX(x);
                if (y > max.getY()) max.setY(y);
                if (z > max.getZ()) max.setZ(z);
            }

            includedBlocks.add(id);
            dirty = true;

            addFace(xFaces, id, new Vec3f(x-EPS, y, z));
            addFace(xFaces, BlockPos.asLong(x+1, y, z), new Vec3f(x+1+EPS, y, z));
            addFace(yFaces, id, new Vec3f(x, y-EPS, z));
            addFace(yFaces, BlockPos.asLong(x, y+1, z), new Vec3f(x, y+1+EPS, z));
            addFace(zFaces, id, new Vec3f(x, y, z-EPS));
            addFace(zFaces, BlockPos.asLong(x, y, z+1), new Vec3f(x, y, z+1+EPS));
        }
    }

    public int totalCubes() {
        return includedBlocks.size();
    }

    public int totalFaces() {
        return xFaces.size() + yFaces.size() + zFaces.size();
    }

    private void addFace(Long2ObjectMap<Vec3f> map, long faceId, Vec3f pos) {
        if (map.remove(faceId) == null) {
            map.put(faceId, pos);
        }
    }

}
