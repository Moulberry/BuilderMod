package codes.moulberry.buildermod.render.regions;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.color.world.FoliageColors;
import net.minecraft.client.color.world.GrassColors;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.level.ColorResolver;
import org.jetbrains.annotations.Nullable;

public class BlockRegion extends AbstractRegion implements BlockRenderView {

    public final BlockPos.Mutable min = new BlockPos.Mutable(0, 0, 0);
    public final BlockPos.Mutable max = new BlockPos.Mutable(0, 0, 0);
    public final Long2ObjectMap<BlockState> blocks = new Long2ObjectOpenHashMap<>();

    public BlockRegion() {
        this(true, false);
    }

    public BlockRegion(boolean doSort, boolean inverse) {
        super(VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL, doSort, inverse);
    }

    @Override
    public void upload(BufferBuilder bufferBuilder) {
        BlockRegionRenderer.uploadRegion(this, bufferBuilder);
    }

    public void clear() {
        blocks.clear();
        min.set(0, 0, 0);
        max.set(0, 0, 0);
        dirty = true;
    }

    public void addBlock(int x, int y, int z, BlockState block) {
        if (x < min.getX()) min.setX(x);
        if (y < min.getY()) min.setY(y);
        if (z < min.getZ()) min.setZ(z);
        if (x > max.getX()) max.setX(x);
        if (y > max.getY()) max.setY(y);
        if (z > max.getZ()) max.setZ(z);

        blocks.put(BlockPos.asLong(x, y, z), block);
        dirty = true;
    }

    public void addBlock(BlockPos pos, BlockState block) {
        this.addBlock(pos.getX(), pos.getY(), pos.getZ(), block);
    }

    public void addBlock(BlockPos pos, short block) {
        this.addBlock(pos.getX(), pos.getY(), pos.getZ(), block);
    }

    public void addBlock(int x, int y, int z, short block) {
        this.addBlock(x, y, z, Block.getStateFromRawId(block));
    }

    @Override
    public float getBrightness(Direction direction, boolean shaded) {
        return 0.8f;
    }

    @Override
    public LightingProvider getLightingProvider() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getLightLevel(LightType type, BlockPos pos) {
        return 0xC;
    }

    @Override
    public int getBaseLightLevel(BlockPos pos, int ambientDarkness) {
        return 0xC;
    }

    @Override
    public boolean isSkyVisible(BlockPos pos) {
        return false;
    }

    @Override
    public int getColor(BlockPos pos, ColorResolver colorResolver) {
        if (colorResolver == BiomeColors.WATER_COLOR) {
            return 0x3f76e4;
        } else if (colorResolver == BiomeColors.GRASS_COLOR) {
            return GrassColors.getColor(0.8f, 0.4f);
        } else if (colorResolver == BiomeColors.FOLIAGE_COLOR) {
            return FoliageColors.getColor(0.8f, 0.4f);
        }
        return 0xFFFFFF;
    }

    @Override
    @Nullable
    public BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return blocks.getOrDefault(pos.asLong(), Blocks.AIR.getDefaultState());
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Override
    public int getBottomY() {
        return min.getY();
    }

    @Override
    public int getHeight() {
        return max.getY() - min.getY() + 1;
    }
}
