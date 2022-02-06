package codes.moulberry.buildermod.render;

import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3f;

public class Region {

    private static final VertexFormat VERTEX_FORMAT = VertexFormats.POSITION_COLOR;

    public final Long2ObjectMap<BlockPos> includedBlocks = new Long2ObjectOpenHashMap<>();
    public final Long2ObjectMap<Vec3f> xFaces = new Long2ObjectOpenHashMap<>();
    public final Long2ObjectMap<Vec3f> yFaces = new Long2ObjectOpenHashMap<>();
    public final Long2ObjectMap<Vec3f> zFaces = new Long2ObjectOpenHashMap<>();
    public final BlockPos.Mutable centerPos = new BlockPos.Mutable(0, 0, 0);

    private long lastSort;
    private float posLastSortX;
    private float posLastSortY;
    private float posLastSortZ;

    private BufferBuilder.State bufferBuilderState = null;
    private BufferBuilder bufferBuilder = null;
    private VertexBuffer vertexBuffer = null;
    private boolean dirty = false;

    public void clear() {
        dirty = true;
        includedBlocks.clear();
        xFaces.clear();
        yFaces.clear();
        zFaces.clear();
    }

    public VertexBuffer getVertexBuffer() {
        if (!dirty && vertexBuffer != null) {
            return getCachedAndMaybeResort();
        }
        dirty = false;

        if (bufferBuilder == null) {
            bufferBuilder = new InverseBufferBuilder(VERTEX_FORMAT.getVertexSize()*4*this.totalFaces());
        }
        if (vertexBuffer == null) {
            vertexBuffer = new VertexBuffer();
        }

        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VERTEX_FORMAT);

        RegionRenderer.uploadRegion(this, bufferBuilder);

        ClientPlayerEntity client = MinecraftClient.getInstance().player;
        if (client != null) {
            lastSort = System.currentTimeMillis();
            posLastSortX = (float)client.getX()-centerPos.getX();
            posLastSortY = (float)client.getY()-centerPos.getY();
            posLastSortZ = (float)client.getZ()-centerPos.getZ();
            bufferBuilder.setCameraPosition(posLastSortX, posLastSortY, posLastSortZ);
        }

        bufferBuilderState = bufferBuilder.popState();
        bufferBuilder.end();

        vertexBuffer.upload(bufferBuilder);
        return vertexBuffer;
    }

    private VertexBuffer getCachedAndMaybeResort() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSort < 250) return vertexBuffer;

        ClientPlayerEntity client = MinecraftClient.getInstance().player;
        if(client == null) return vertexBuffer;

        float camX = (float)client.getX()-centerPos.getX();
        float camY = (float)client.getY()-centerPos.getY();
        float camZ = (float)client.getZ()-centerPos.getZ();

        double dX = camX - posLastSortX;
        double dY = camY - posLastSortY;
        double dZ = camZ - posLastSortZ;

        if(dX*dX + dY*dY + dZ*dZ < 1) {
            return vertexBuffer;
        }

        posLastSortX = camX;
        posLastSortY = camY;
        posLastSortZ = camZ;
        lastSort = currentTime;

        if (vertexBuffer != null) {
            bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            bufferBuilder.restoreState(bufferBuilderState);
            bufferBuilder.setCameraPosition(camX, camY, camZ);
            bufferBuilderState = bufferBuilder.popState();
            bufferBuilder.end();

            vertexBuffer.submitUpload(bufferBuilder);
        }
        return vertexBuffer;
    }

    public void addBox(BlockPos pos) {
        addBox(pos.getX(), pos.getY(), pos.getZ());
    }

    public void addBox(int x, int y, int z) {
        float EPS = 1E-4f;

        long id = BlockPos.asLong(x, y, z);
        if (!includedBlocks.containsKey(id)) {
            BlockPos pos = new BlockPos(x, y, z);
            includedBlocks.put(id, pos);
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
