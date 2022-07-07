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

    @Shadow protected abstract IntConsumer createIndexWriter(VertexFormat.IntType elementFormat);

    @Shadow private VertexFormat.DrawMode drawMode;

    @Shadow private int elementOffset;

    @Shadow private ByteBuffer buffer;

    @Shadow private float sortingCameraX;
    @Shadow private float sortingCameraY;
    @Shadow private float sortingCameraZ;

    @Inject(method="writeSortedIndices", at=@At("HEAD"), cancellable = true)
    public void writeCameraOffset(VertexFormat.IntType elementFormat, CallbackInfo ci) {
        if ((Object)this instanceof InverseBufferBuilder) {
            ci.cancel();
            float[] fs = new float[this.sortingPrimitiveCenters.length];
            int[] is = new int[this.sortingPrimitiveCenters.length];
            for (int i2 = 0; i2 < this.sortingPrimitiveCenters.length; ++i2) {
                float f = this.sortingPrimitiveCenters[i2].getX() - this.sortingCameraX;
                float g = this.sortingPrimitiveCenters[i2].getY() - this.sortingCameraY;
                float h = this.sortingPrimitiveCenters[i2].getZ() - this.sortingCameraZ;
                fs[i2] = f * f + g * g + h * h;
                is[i2] = i2;
            }
            IntArrays.mergeSort(is, (i, j) -> Floats.compare(fs[i], fs[j])); // <--- swapped i/j
            IntConsumer i3 = this.createIndexWriter(elementFormat);
            this.buffer.position(this.elementOffset);
            for (int j2 : is) {
                i3.accept(j2 * this.drawMode.size + 0);
                i3.accept(j2 * this.drawMode.size + 1);
                i3.accept(j2 * this.drawMode.size + 2);
                i3.accept(j2 * this.drawMode.size + 2);
                i3.accept(j2 * this.drawMode.size + 3);
                i3.accept(j2 * this.drawMode.size + 0);
            }
        }
    }

}
