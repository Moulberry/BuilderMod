package codes.moulberry.buildermod.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;

public class SphereRenderer {

    private static final Region sphereRegion = new Region();
    private static float lastSize = 0;

    public static void setSize(int size) {
        if (size != lastSize) {
            if (size < lastSize) {
                sphereRegion.clear();
            }
            lastSize = size;
            increaseRegionToSize(sphereRegion, size);
        }
    }

    public static void increaseRegionToSize(Region region, int radius) {
        float radiusSq = (radius+0.5f)*(radius+0.5f);
        for(int x=-radius; x<=radius; x++) {
            for(int y=-radius; y<=radius; y++) {
                for(int z=-radius; z<=radius; z++) {
                    float distSq = x*x + y*y + z*z;
                    if (distSq <= radiusSq) {
                        region.addBox(x, y, z);
                    }
                }
            }
        }
    }

    public static void render(MatrixStack matrix, Matrix4f projection, BlockPos centerPos, int size) {
        sphereRegion.centerPos.set(centerPos);
        setSize(size);
        RegionRenderer.render(sphereRegion, matrix, projection);
    }

}