package codes.moulberry.buildermod.mixin;

import codes.moulberry.buildermod.render.InverseBufferBuilder;
import com.google.common.primitives.Floats;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntConsumer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.math.Vec3f;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilder {

    @Shadow @Nullable private Vec3f[] sortingPrimitiveCenters;

    @Shadow protected abstract IntConsumer createIndexWriter(int offset, VertexFormat.IndexType indexType);

    @Shadow private VertexFormat.DrawMode drawMode;

    @Shadow private int elementOffset;

    @Shadow private ByteBuffer buffer;

    @Shadow private float sortingCameraX;
    @Shadow private float sortingCameraY;
    @Shadow private float sortingCameraZ;

    @Inject(method="writeSortedIndices", at=@At("HEAD"), cancellable = true)
    public void writeCameraOffset(VertexFormat.IndexType elementFormat, CallbackInfo ci) {
        if ((Object)this instanceof InverseBufferBuilder) {
            ci.cancel();

            float[] fs = new float[this.sortingPrimitiveCenters.length];
            int[] is = new int[this.sortingPrimitiveCenters.length];

            for(int i = 0; i < this.sortingPrimitiveCenters.length; is[i] = i++) {
                float f = this.sortingPrimitiveCenters[i].getX() - this.sortingCameraX;
                float g = this.sortingPrimitiveCenters[i].getY() - this.sortingCameraY;
                float h = this.sortingPrimitiveCenters[i].getZ() - this.sortingCameraZ;
                fs[i] = f * f + g * g + h * h;
            }

            IntArrays.mergeSort(is, (a, b) -> {
                return Floats.compare(fs[a], fs[b]);  // <--- swapped a/b
            });
            IntConsumer intConsumer = this.createIndexWriter(this.elementOffset, elementFormat);
            int[] var10 = is;
            int var11 = is.length;

            for(int var12 = 0; var12 < var11; ++var12) {
                int j = var10[var12];
                intConsumer.accept(j * this.drawMode.additionalVertexCount + 0);
                intConsumer.accept(j * this.drawMode.additionalVertexCount + 1);
                intConsumer.accept(j * this.drawMode.additionalVertexCount + 2);
                intConsumer.accept(j * this.drawMode.additionalVertexCount + 2);
                intConsumer.accept(j * this.drawMode.additionalVertexCount + 3);
                intConsumer.accept(j * this.drawMode.additionalVertexCount + 0);
            }
        }
    }

}
