package codes.moulberry.buildermod.render.regions;

import codes.moulberry.buildermod.render.InverseBufferBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.math.BlockPos;

import java.io.Closeable;

public abstract class AbstractRegion implements Closeable {

    public final BlockPos.Mutable centerPos = new BlockPos.Mutable(0, 0, 0);
    private final VertexFormat vertexFormat;
    private final boolean doAutomaticSort;
    private final boolean inverse;

    private long lastSortMillis;
    private float lastSortX;
    private float lastSortY;
    private float lastSortZ;

    private BufferBuilder.State bufferBuilderState = null;
    private BufferBuilder bufferBuilder = null;
    private VertexBuffer vertexBuffer = null;

    private boolean empty = true;
    protected boolean dirty = true;

    public AbstractRegion(VertexFormat vertexFormat, boolean doAutomaticSort, boolean inverse) {
        this.vertexFormat = vertexFormat;
        this.doAutomaticSort = doAutomaticSort;
        this.inverse = inverse;
    }

    public void close() {
        this.empty = true;
        this.bufferBuilder.clear();
        this.vertexBuffer.close();
        this.bufferBuilder = null;
        this.vertexBuffer = null;
    }

    public abstract void upload(BufferBuilder bufferBuilder);

    public VertexBuffer getVertexBuffer() {
        if (empty) {
            this.bufferBuilder = inverse ?
                    new InverseBufferBuilder(256) :
                    new BufferBuilder(256);
            this.vertexBuffer = new VertexBuffer();
            empty = false;
        }

        if (!dirty) {
            tryAutomaticResort();
            return vertexBuffer;
        }
        dirty = false;

        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, vertexFormat);

        upload(bufferBuilder);

        if (doAutomaticSort) {
            ClientPlayerEntity client = MinecraftClient.getInstance().player;
            if (client != null) {
                lastSortMillis = System.currentTimeMillis();
                lastSortX = (float)client.getX()-centerPos.getX();
                lastSortY = (float)client.getY()-centerPos.getY();
                lastSortZ = (float)client.getZ()-centerPos.getZ();
                bufferBuilder.setCameraPosition(lastSortX, lastSortY, lastSortZ);
            }
        }

        bufferBuilderState = bufferBuilder.popState();
        bufferBuilder.end();

        vertexBuffer.upload(bufferBuilder);
        return vertexBuffer;
    }

    public void resort(float x, float y, float z) {
        if (bufferBuilderState == null) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSortMillis < 250) return;

        double dX = x - lastSortX;
        double dY = y - lastSortY;
        double dZ = z - lastSortZ;

        if(dX*dX + dY*dY + dZ*dZ < 1) {
            return;
        }

        lastSortX = x;
        lastSortY = y;
        lastSortZ = z;
        lastSortMillis = currentTime;

        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, vertexFormat);
        bufferBuilder.restoreState(bufferBuilderState);
        bufferBuilder.setCameraPosition(x, y, z);
        bufferBuilderState = bufferBuilder.popState();
        bufferBuilder.end();

        vertexBuffer.submitUpload(bufferBuilder);
    }

    private void tryAutomaticResort() {
        if (!doAutomaticSort) return;

        ClientPlayerEntity client = MinecraftClient.getInstance().player;
        if(client == null) return;

        resort(
                (float)client.getX()-centerPos.getX(),
                (float)client.getY()-centerPos.getY(),
                (float)client.getZ()-centerPos.getZ()
        );
    }

}
