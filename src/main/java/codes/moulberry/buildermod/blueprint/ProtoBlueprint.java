package codes.moulberry.buildermod.blueprint;

import codes.moulberry.buildermod.render.regions.BooleanRegion;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public record ProtoBlueprint(
        BlockState[][][] blockStates,
        Long2ObjectMap<NbtCompound> blockEntities,
        int sizeX, int sizeY, int sizeZ
) {

    public ProtoBlueprint(BlockState[][][] blockStates, Long2ObjectMap<NbtCompound> blockEntities) {
        this(blockStates, blockEntities,
                blockStates.length, blockStates[0].length, blockStates[0][0].length);
    }

    public static ProtoBlueprint createFromWorld(World world, BooleanRegion region) {
        if (region.totalCubes() <= 0) {
            throw new IllegalArgumentException("Region can't be empty");
        }

        BlockPos min = region.getMin();
        BlockPos max = region.getMax();

        BlockState[][][] blockStates = new BlockState[max.getX()-min.getX()+1][max.getY()-min.getY()+1][max.getZ()-min.getZ()+1];
        Long2ObjectMap<NbtCompound> blockEntities = new Long2ObjectOpenHashMap<>();

        region.forEachBlock(pos -> {
            BlockPos offset = pos.subtract(min);
            BlockState blockState = world.getBlockState(pos);
            if (blockState.isAir()) blockState = Blocks.AIR.getDefaultState();

            blockStates[offset.getX()][offset.getY()][offset.getZ()] = blockState;

            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity != null) {
                NbtCompound blockEntityNbt = blockEntity.createNbtWithId();
                blockEntities.put(offset.asLong(), blockEntityNbt);
            }
        });

        return new ProtoBlueprint(blockStates, blockEntities);
    }

    public static ProtoBlueprint createFromWorld(World world, BlockPos min, Vec3i dimensions) {
        if (dimensions.getX() < 1 || dimensions.getY() < 1 || dimensions.getZ() < 1) {
            throw new IllegalArgumentException("Dimensions must be positive");
        }

        BlockPos max = min.add(dimensions).add(-1, -1, -1);

        BlockState[][][] blockStates = new BlockState[dimensions.getX()][dimensions.getY()][dimensions.getZ()];
        Long2ObjectMap<NbtCompound> blockEntities = new Long2ObjectOpenHashMap<>();

        for (BlockPos pos : BlockPos.iterate(min, max)) {
            BlockPos offset = pos.subtract(min);
            BlockState blockState = world.getBlockState(pos);
            blockStates[offset.getX()][offset.getY()][offset.getZ()] = blockState;

            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity != null) {
                NbtCompound blockEntityNbt = blockEntity.createNbtWithId();
                blockEntities.put(offset.asLong(), blockEntityNbt);
            }
        }

        return new ProtoBlueprint(blockStates, blockEntities);
    }

}
