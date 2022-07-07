package codes.moulberry.buildermod.customtool;

import codes.moulberry.buildermod.render.regions.BooleanRegion;
import codes.moulberry.buildermod.render.regions.BooleanRegionRenderer;
import codes.moulberry.buildermod.render.SphereRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;

public abstract class GenericTool implements CustomTool {

    private static final BooleanRegion TOOL_REGION = new BooleanRegion();
    private static final int SELECTION_LIMIT = 100000;

    protected abstract int toolRadius();
    protected abstract boolean isSelectable(BlockState blockState);
    protected abstract void apply(BooleanRegion region);

    @Override
    public void onSelect() {
        TOOL_REGION.clear();
    }

    protected final int createEncodedOffset(BlockPos playerBlockPos, long packed) {
        int x = BlockPos.unpackLongX(packed) - playerBlockPos.getX();
        int y = BlockPos.unpackLongY(packed) - playerBlockPos.getY();
        int z = BlockPos.unpackLongZ(packed) - playerBlockPos.getZ();

        int encoded = 0;
        encoded |= ((x+1024) & 2047) << 11;
        encoded |= ((y+512)  & 1023) << 22;
        encoded |= ((z+1024) & 2047);
        return encoded;
    }

    @Override
    public void render(MatrixStack matrices, float tickDelta, Matrix4f projection) {
        if (MinecraftClient.getInstance().currentScreen != null) {
            TOOL_REGION.clear();
            return;
        }

        if (MinecraftClient.getInstance().options.useKey.isPressed()) {
            if (TOOL_REGION.totalCubes() < SELECTION_LIMIT) {
                CustomTool.raycastBlock((blockHitResult -> {
                    BlockPos pos = blockHitResult.getBlockPos();
                    int r = toolRadius();
                    float rSq = (r+0.5f)*(r+0.5f);
                    for (int x=-r; x<=r; x++) {
                        for (int y=-r; y<=r; y++) {
                            for (int z=-r; z<=r; z++) {
                                if (x*x + y*y + z*z < rSq) {
                                    BlockPos newPos = new BlockPos(pos.getX()+x, pos.getY()+y, pos.getZ()+z);

                                    if (isSelectable(MinecraftClient.getInstance().world.getBlockState(newPos))) {
                                        TOOL_REGION.add(newPos);
                                    }
                                }
                            }
                        }
                    }
                }));
            }
            BooleanRegionRenderer.render(TOOL_REGION, matrices, projection);
        } else if (TOOL_REGION.totalCubes() > 0) {
            apply(TOOL_REGION);
            TOOL_REGION.clear();
        } else {
            CustomTool.raycastBlock((blockHitResult -> {
                BlockPos pos = blockHitResult.getBlockPos();
                SphereRenderer.render(matrices, projection, pos, toolRadius());
            }));
        }
    }
}
