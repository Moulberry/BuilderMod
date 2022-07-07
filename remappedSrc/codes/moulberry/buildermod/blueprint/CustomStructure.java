package codes.moulberry.buildermod.blueprint;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.*;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.collection.IdList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class CustomStructure {
    public static final String PALETTE_KEY = "palette";
    public static final String PALETTES_KEY = "palettes";
    public static final String ENTITIES_KEY = "entities";
    public static final String BLOCKS_KEY = "blocks";
    public static final String BLOCKS_POS_KEY = "pos";
    public static final String BLOCKS_STATE_KEY = "state";
    public static final String BLOCKS_NBT_KEY = "nbt";
    public static final String ENTITIES_POS_KEY = "pos";
    public static final String ENTITIES_BLOCK_POS_KEY = "blockPos";
    public static final String ENTITIES_NBT_KEY = "nbt";
    public static final String SIZE_KEY = "size";

    private PalettedBlockInfoList blockInfoLists = null;
    private Vec3i size = Vec3i.ZERO;
    private String author = "?";

    public Vec3i getSize() {
        return this.size;
    }

    public void setAuthor(String name) {
        this.author = name;
    }

    public String getAuthor() {
        return this.author;
    }

    public void saveFromWorld(World world, BlockPos start, Vec3i dimensions) {
        if (dimensions.getX() < 1 || dimensions.getY() < 1 || dimensions.getZ() < 1) {
            return;
        }

        BlockPos blockPos = start.add(dimensions).add(-1, -1, -1);
        ArrayList<StructureBlockInfo> fullBlocks = Lists.newArrayList();
        ArrayList<StructureBlockInfo> blocksWithNbt = Lists.newArrayList();
        ArrayList<StructureBlockInfo> otherBlocks = Lists.newArrayList();

        BlockPos min = new BlockPos(
                Math.min(start.getX(), blockPos.getX()),
                Math.min(start.getY(), blockPos.getY()),
                Math.min(start.getZ(), blockPos.getZ())
        );
        BlockPos max = new BlockPos(
                Math.max(start.getX(), blockPos.getX()),
                Math.max(start.getY(), blockPos.getY()),
                Math.max(start.getZ(), blockPos.getZ())
        );

        this.size = dimensions;
        for (BlockPos pos : BlockPos.iterate(min, max)) {
            BlockPos offset = pos.subtract(min);
            BlockState blockState = world.getBlockState(pos);

            BlockEntity blockEntity = world.getBlockEntity(pos);
            NbtCompound blockEntityNbt = blockEntity == null ? null : blockEntity.createNbtWithId();
            StructureBlockInfo structureBlockInfo = new StructureBlockInfo(offset, blockState, blockEntityNbt);

            categorize(structureBlockInfo, fullBlocks, blocksWithNbt, otherBlocks);
        }

        List<StructureBlockInfo> combined = combineSorted(fullBlocks, blocksWithNbt, otherBlocks);
        blockInfoLists = new PalettedBlockInfoList(combined);
    }

    /**
     * Categorizes {@code blockInfo} based on its properties, modifying
     * the passed lists in-place.
     *
     * <p>If the block has an NBT associated with it, then it will be
     * put in {@code blocksWithNbt}. If the block does not have an NBT
     * associated with it, but is always a full cube, then it will be
     * put in {@code fullBlocks}. Otherwise, it will be put in
     * {@code otherBlocks}.
     *
     * @apiNote After all blocks are categorized, {@link #combineSorted}
     * should be called with the same parameters to get the final list.
     */
    private static void categorize(StructureBlockInfo blockInfo, List<StructureBlockInfo> fullBlocks, List<StructureBlockInfo> blocksWithNbt, List<StructureBlockInfo> otherBlocks) {
        if (blockInfo.nbt != null) {
            blocksWithNbt.add(blockInfo);
        } else if (!blockInfo.state.getBlock().hasDynamicBounds() && blockInfo.state.isFullCube(EmptyBlockView.INSTANCE, BlockPos.ORIGIN)) {
            fullBlocks.add(blockInfo);
        } else {
            otherBlocks.add(blockInfo);
        }
    }

    /**
     * {@return the list that sorts and combines the passed block lists}
     *
     * @apiNote The parameters passed should be the same one that was passed
     * to previous calls to {@link #categorize}. The returned value is meant to
     * be passed to {@link PalettedBlockInfoList}.
     *
     * @implNote Each list passed will be sorted in-place using the items'
     * Y, X, and Z coordinates. The returned list contains all items of
     * {@code fullBlocks}, {@code otherBlocks}, and {@code blocksWithNbt}
     * in this order.
     */
    private static List<StructureBlockInfo> combineSorted(List<StructureBlockInfo> fullBlocks, List<StructureBlockInfo> blocksWithNbt, List<StructureBlockInfo> otherBlocks) {
        Comparator<StructureBlockInfo> comparator = Comparator.<StructureBlockInfo>comparingInt(b -> b.pos.getY())
                .thenComparingInt(b -> b.pos.getX()).thenComparingInt(b -> b.pos.getZ());

        fullBlocks.sort(comparator);
        otherBlocks.sort(comparator);
        blocksWithNbt.sort(comparator);

        ArrayList<StructureBlockInfo> list = Lists.newArrayList();
        list.addAll(fullBlocks);
        list.addAll(otherBlocks);
        list.addAll(blocksWithNbt);
        return list;
    }

    /*public List<StructureBlockInfo> getInfosForBlock(BlockPos pos, StructurePlacementData placementData, Block block) {
        return this.getInfosForBlock(pos, placementData, block, true);
    }

    public List<StructureBlockInfo> getInfosForBlock(BlockPos pos, StructurePlacementData placementData, Block block, boolean transformed) {
        ArrayList<StructureBlockInfo> list = Lists.newArrayList();
        BlockBox blockBox = placementData.getBoundingBox();
        if (this.blockInfoLists.isEmpty()) {
            return Collections.emptyList();
        }
        for (StructureBlockInfo structureBlockInfo : placementData.getRandomBlockInfos(this.blockInfoLists, pos).getAllOf(block)) {
            BlockPos blockPos;
            BlockPos blockPos2 = blockPos = transformed ? transform(placementData, structureBlockInfo.pos).add(pos) : structureBlockInfo.pos;
            if (blockBox != null && !blockBox.contains(blockPos)) continue;
            list.add(new StructureBlockInfo(blockPos, structureBlockInfo.state.rotate(placementData.getRotation()), structureBlockInfo.nbt));
        }
        return list;
    }*/

    /*public BlockPos transformBox(StructurePlacementData placementData1, BlockPos pos1, StructurePlacementData placementData2, BlockPos pos2) {
        BlockPos blockPos = transform(placementData1, pos1);
        BlockPos blockPos2 = transform(placementData2, pos2);
        return blockPos.subtract(blockPos2);
    }

    public static BlockPos transform(StructurePlacementData placementData, BlockPos pos) {
        return transformAround(pos, placementData.getMirror(), placementData.getRotation(), placementData.getPosition());
    }*/

    /*public boolean place(ServerWorldAccess world, BlockPos pos, BlockPos pivot, StructurePlacementData placementData, Random random, int flags) {
        Object fluidState2;
        Object blockPos2;
        Object blockState;
        Object blockPos;
        if (this.blockInfoLists.isEmpty()) {
            return false;
        }
        List<StructureBlockInfo> list = placementData.getRandomBlockInfos(this.blockInfoLists, pos).getAll();
        if (list.isEmpty() && (placementData.shouldIgnoreEntities() || this.entities.isEmpty()) || this.size.getX() < 1 || this.size.getY() < 1 || this.size.getZ() < 1) {
            return false;
        }
        BlockBox blockBox = placementData.getBoundingBox();
        ArrayList<Object> list2 = Lists.newArrayListWithCapacity(placementData.shouldPlaceFluids() ? list.size() : 0);
        ArrayList<Object> list3 = Lists.newArrayListWithCapacity(placementData.shouldPlaceFluids() ? list.size() : 0);
        ArrayList<Pair<Object, NbtCompound>> list4 = Lists.newArrayListWithCapacity(list.size());
        int i = Integer.MAX_VALUE;
        int j = Integer.MAX_VALUE;
        int k = Integer.MAX_VALUE;
        int l = Integer.MIN_VALUE;
        int m = Integer.MIN_VALUE;
        int n = Integer.MIN_VALUE;
        List<StructureBlockInfo> list5 = process(world, pos, pivot, placementData, list);
        for (StructureBlockInfo structureBlockInfo : list5) {
            BlockEntity blockEntity;
            blockPos = structureBlockInfo.pos;
            if (blockBox != null && !blockBox.contains((Vec3i)blockPos)) continue;
            FluidState fluidState = placementData.shouldPlaceFluids() ? world.getFluidState((BlockPos)blockPos) : null;
            blockState = structureBlockInfo.state.mirror(placementData.getMirror()).rotate(placementData.getRotation());
            if (structureBlockInfo.nbt != null) {
                blockEntity = world.getBlockEntity((BlockPos)blockPos);
                Clearable.clear(blockEntity);
                world.setBlockState((BlockPos)blockPos, Blocks.BARRIER.getDefaultState(), Block.NO_REDRAW | Block.FORCE_STATE);
            }
            if (!world.setBlockState((BlockPos)blockPos, (BlockState)blockState, flags)) continue;
            i = Math.min(i, ((Vec3i)blockPos).getX());
            j = Math.min(j, ((Vec3i)blockPos).getY());
            k = Math.min(k, ((Vec3i)blockPos).getZ());
            l = Math.max(l, ((Vec3i)blockPos).getX());
            m = Math.max(m, ((Vec3i)blockPos).getY());
            n = Math.max(n, ((Vec3i)blockPos).getZ());
            list4.add(Pair.of(blockPos, structureBlockInfo.nbt));
            if (structureBlockInfo.nbt != null && (blockEntity = world.getBlockEntity((BlockPos)blockPos)) != null) {
                if (blockEntity instanceof LootableContainerBlockEntity) {
                    structureBlockInfo.nbt.putLong("LootTableSeed", random.nextLong());
                }
                blockEntity.readNbt(structureBlockInfo.nbt);
            }
            if (fluidState == null) continue;
            if (((AbstractBlock.AbstractBlockState)blockState).getFluidState().isStill()) {
                list3.add(blockPos);
                continue;
            }
            if (!(((AbstractBlock.AbstractBlockState)blockState).getBlock() instanceof FluidFillable)) continue;
            ((FluidFillable)((Object)((AbstractBlock.AbstractBlockState)blockState).getBlock())).tryFillWithFluid(world, (BlockPos)blockPos, (BlockState)blockState, fluidState);
            if (fluidState.isStill()) continue;
            list2.add(blockPos);
        }
        boolean bl = true;
        Direction[] directionArray = new Direction[]{Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        while (bl && !list2.isEmpty()) {
            bl = false;
            blockPos = list2.iterator();
            while (blockPos.hasNext()) {
                BlockState blockEntity;
                BlockPos blockPos3 = (BlockPos)blockPos.next();
                blockState = world.getFluidState(blockPos3);
                for (int blockEntity2 = 0; blockEntity2 < directionArray.length && !((FluidState)blockState).isStill(); ++blockEntity2) {
                    blockPos2 = blockPos3.offset(directionArray[blockEntity2]);
                    fluidState2 = world.getFluidState((BlockPos)blockPos2);
                    if (!((FluidState)fluidState2).isStill() || list3.contains(blockPos2)) continue;
                    blockState = fluidState2;
                }
                if (!((FluidState)blockState).isStill() || !((blockPos2 = (blockEntity = world.getBlockState(blockPos3)).getBlock()) instanceof FluidFillable)) continue;
                ((FluidFillable)blockPos2).tryFillWithFluid(world, blockPos3, blockEntity, (FluidState)blockState);
                bl = true;
                blockPos.remove();
            }
        }
        if (i <= l) {
            if (!placementData.shouldUpdateNeighbors()) {
                blockPos = new BitSetVoxelSet(l - i + 1, m - j + 1, n - k + 1);
                int n2 = i;
                int blockState2 = j;
                int blockEntity = k;
                blockPos2 = list4.iterator();
                while (blockPos2.hasNext()) {
                    fluidState2 = (Pair)blockPos2.next();
                    BlockPos blockPos3 = (BlockPos)((Pair)fluidState2).getFirst();
                    ((VoxelSet)blockPos).set(blockPos3.getX() - n2, blockPos3.getY() - blockState2, blockPos3.getZ() - blockEntity);
                }
                updateCorner(world, flags, (VoxelSet)blockPos, n2, blockState2, blockEntity);
            }
            for (Pair pair : list4) {
                BlockEntity blockEntity;
                BlockPos blockState3 = (BlockPos)pair.getFirst();
                if (!placementData.shouldUpdateNeighbors()) {
                    BlockState blockEntity3 = world.getBlockState(blockState3);
                    if (blockEntity3 != (blockPos2 = Block.postProcessState(blockEntity3, world, blockState3))) {
                        world.setBlockState(blockState3, (BlockState)blockPos2, flags & ~Block.NOTIFY_NEIGHBORS | Block.FORCE_STATE);
                    }
                    world.updateNeighbors(blockState3, ((AbstractBlock.AbstractBlockState)blockPos2).getBlock());
                }
                if (pair.getSecond() == null || (blockEntity = world.getBlockEntity(blockState3)) == null) continue;
                blockEntity.markDirty();
            }
        }
        if (!placementData.shouldIgnoreEntities()) {
            this.spawnEntities(world, pos, placementData.getMirror(), placementData.getRotation(), placementData.getPosition(), blockBox, placementData.shouldInitializeMobs());
        }
        return true;
    }*/

    /*public static List<StructureBlockInfo> process(WorldAccess world, BlockPos pos, BlockPos pivot, StructurePlacementData placementData, List<StructureBlockInfo> list) {
        ArrayList<StructureBlockInfo> list2 = Lists.newArrayList();
        for (StructureBlockInfo structureBlockInfo : list) {
            BlockPos blockPos = transform(placementData, structureBlockInfo.pos).add(pos);
            StructureBlockInfo structureBlockInfo2 = new StructureBlockInfo(blockPos, structureBlockInfo.state, structureBlockInfo.nbt != null ? structureBlockInfo.nbt.copy() : null);
            Iterator<StructureProcessor> iterator = placementData.getProcessors().iterator();
            while (structureBlockInfo2 != null && iterator.hasNext()) {
                structureBlockInfo2 = iterator.next().process(world, pos, pivot, structureBlockInfo, structureBlockInfo2, placementData);
            }
            if (structureBlockInfo2 == null) continue;
            list2.add(structureBlockInfo2);
        }
        return list2;
    }*/

    public Vec3i getRotatedSize(BlockRotation rotation) {
        return switch (rotation) {
            case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> new Vec3i(this.size.getZ(), this.size.getY(), this.size.getX());
            default -> this.size;
        };
    }

    public static BlockPos transformAround(BlockPos pos, BlockMirror mirror, BlockRotation rotation, BlockPos pivot) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        boolean bl = true;
        switch (mirror) {
            case LEFT_RIGHT: {
                k = -k;
                break;
            }
            case FRONT_BACK: {
                i = -i;
                break;
            }
            default: {
                bl = false;
            }
        }
        int l = pivot.getX();
        int m = pivot.getZ();
        switch (rotation) {
            case CLOCKWISE_180: {
                return new BlockPos(l + l - i, j, m + m - k);
            }
            case COUNTERCLOCKWISE_90: {
                return new BlockPos(l - m + k, j, l + m - i);
            }
            case CLOCKWISE_90: {
                return new BlockPos(l + m - k, j, m - l + i);
            }
        }
        return bl ? new BlockPos(i, j, k) : pos;
    }

    public static Vec3d transformAround(Vec3d point, BlockMirror mirror, BlockRotation rotation, BlockPos pivot) {
        double d = point.x;
        double e = point.y;
        double f = point.z;
        boolean bl = true;
        switch (mirror) {
            case LEFT_RIGHT: {
                f = 1.0 - f;
                break;
            }
            case FRONT_BACK: {
                d = 1.0 - d;
                break;
            }
            default: {
                bl = false;
            }
        }
        int i = pivot.getX();
        int j = pivot.getZ();
        switch (rotation) {
            case CLOCKWISE_180: {
                return new Vec3d((double)(i + i + 1) - d, e, (double)(j + j + 1) - f);
            }
            case COUNTERCLOCKWISE_90: {
                return new Vec3d((double)(i - j) + f, e, (double)(i + j + 1) - d);
            }
            case CLOCKWISE_90: {
                return new Vec3d((double)(i + j + 1) - f, e, (double)(j - i) + d);
            }
        }
        return bl ? new Vec3d(d, e, f) : point;
    }

    public BlockPos offsetByTransformedSize(BlockPos pos, BlockMirror mirror, BlockRotation rotation) {
        return applyTransformedOffset(pos, mirror, rotation, this.getSize().getX(), this.getSize().getZ());
    }

    public static BlockPos applyTransformedOffset(BlockPos pos, BlockMirror mirror, BlockRotation rotation, int offsetX, int offsetZ) {
        int i = mirror == BlockMirror.FRONT_BACK ? --offsetX : 0;
        int j = mirror == BlockMirror.LEFT_RIGHT ? --offsetZ : 0;
        BlockPos blockPos = pos;
        switch (rotation) {
            case NONE: {
                blockPos = pos.add(i, 0, j);
                break;
            }
            case CLOCKWISE_90: {
                blockPos = pos.add(offsetZ - j, 0, i);
                break;
            }
            case CLOCKWISE_180: {
                blockPos = pos.add(offsetX - i, 0, offsetZ - j);
                break;
            }
            case COUNTERCLOCKWISE_90: {
                blockPos = pos.add(j, 0, offsetX - i);
            }
        }
        return blockPos;
    }

    /*public BlockBox calculateBoundingBox(StructurePlacementData placementData, BlockPos pos) {
        return this.calculateBoundingBox(pos, placementData.getRotation(), placementData.getPosition(), placementData.getMirror());
    }

    public BlockBox calculateBoundingBox(BlockPos pos, BlockRotation rotation, BlockPos pivot, BlockMirror mirror) {
        return createBox(pos, rotation, pivot, mirror, this.size);
    }

    @VisibleForTesting
    protected static BlockBox createBox(BlockPos pos, BlockRotation rotation, BlockPos pivot, BlockMirror mirror, Vec3i dimensions) {
        Vec3i vec3i = dimensions.add(-1, -1, -1);
        BlockPos blockPos = transformAround(BlockPos.ORIGIN, mirror, rotation, pivot);
        BlockPos blockPos2 = transformAround(BlockPos.ORIGIN.add(vec3i), mirror, rotation, pivot);
        return BlockBox.create(blockPos, blockPos2).move(pos);
    }*/

    /*
     * WARNING - void declaration
     */
    public NbtCompound writeNbt(NbtCompound nbt) {
        if (blockInfoLists == null) {
            nbt.put(BLOCKS_KEY, new NbtList());
            nbt.put(PALETTE_KEY, new NbtList());
        } else {
            Palette palette = new Palette();
            NbtList entries = new NbtList();
            for (StructureBlockInfo info : this.blockInfoLists.getAll()) {
                NbtCompound nbtCompound = new NbtCompound();
                nbtCompound.put(BLOCKS_POS_KEY, this.createNbtIntList(info.pos.getX(), info.pos.getY(), info.pos.getZ()));

                int id = palette.getOrCreateId(info.state);
                nbtCompound.putInt(BLOCKS_STATE_KEY, id);

                if (info.nbt != null) {
                    nbtCompound.put(BLOCKS_NBT_KEY, info.nbt);
                }
                entries.add(nbtCompound);
            }
            nbt.put(BLOCKS_KEY, entries);

            NbtList paletteNbt = new NbtList();
            for (BlockState blockState : palette) {
                paletteNbt.add(NbtHelper.fromBlockState(blockState));
            }
            nbt.put(PALETTE_KEY, paletteNbt);
        }
        nbt.put(ENTITIES_KEY, new NbtList());
        nbt.put(SIZE_KEY, this.createNbtIntList(this.size.getX(), this.size.getY(), this.size.getZ()));
        nbt.putInt("DataVersion", SharedConstants.getGameVersion().getWorldVersion());
        return nbt;
    }

    public void readNbt(NbtCompound nbt) {
        int i;
        NbtList nbtList3;
        //this.blockInfoLists.clear();
        NbtList nbtList = nbt.getList(SIZE_KEY, 3);
        this.size = new Vec3i(nbtList.getInt(0), nbtList.getInt(1), nbtList.getInt(2));
        NbtList nbtList2 = nbt.getList(BLOCKS_KEY, 10);
        if (nbt.contains(PALETTES_KEY, 9)) {
            nbtList3 = nbt.getList(PALETTES_KEY, 9);
            for (i = 0; i < nbtList3.size(); ++i) {
                this.loadPalettedBlockInfo(nbtList3.getList(i), nbtList2);
            }
        } else {
            this.loadPalettedBlockInfo(nbt.getList(PALETTE_KEY, 10), nbtList2);
        }
        nbtList3 = nbt.getList(ENTITIES_KEY, 10);
        for (i = 0; i < nbtList3.size(); ++i) {
            NbtCompound nbtCompound = nbtList3.getCompound(i);
            NbtList nbtList4 = nbtCompound.getList("pos", 6);
            Vec3d vec3d = new Vec3d(nbtList4.getDouble(0), nbtList4.getDouble(1), nbtList4.getDouble(2));
            NbtList nbtList5 = nbtCompound.getList(ENTITIES_BLOCK_POS_KEY, 3);
            BlockPos blockPos = new BlockPos(nbtList5.getInt(0), nbtList5.getInt(1), nbtList5.getInt(2));
            if (!nbtCompound.contains("nbt")) continue;
            NbtCompound nbtCompound2 = nbtCompound.getCompound("nbt");
            //this.entities.add(new StructureEntityInfo(vec3d, blockPos, nbtCompound2));
        }
    }

    private PalettedBlockInfoList loadPalettedBlockInfo(NbtList paletteNbt, NbtList blocksNbt) {
        Palette palette = new Palette();
        for (int i = 0; i < paletteNbt.size(); ++i) {
            palette.set(NbtHelper.toBlockState(paletteNbt.getCompound(i)), i);
        }
        ArrayList<StructureBlockInfo> i = Lists.newArrayList();
        ArrayList<StructureBlockInfo> list = Lists.newArrayList();
        ArrayList<StructureBlockInfo> list2 = Lists.newArrayList();
        for (int j = 0; j < blocksNbt.size(); ++j) {
            NbtCompound nbtCompound = blocksNbt.getCompound(j);
            NbtList nbtList = nbtCompound.getList("pos", 3);
            BlockPos blockPos = new BlockPos(nbtList.getInt(0), nbtList.getInt(1), nbtList.getInt(2));
            BlockState blockState = palette.getState(nbtCompound.getInt(BLOCKS_STATE_KEY));
            NbtCompound nbtCompound2 = nbtCompound.contains("nbt") ? nbtCompound.getCompound("nbt") : null;
            StructureBlockInfo structureBlockInfo = new StructureBlockInfo(blockPos, blockState, nbtCompound2);
            categorize(structureBlockInfo, i, list, list2);
        }
        List<StructureBlockInfo> j = combineSorted(i, list, list2);
        return new PalettedBlockInfoList(j);
    }

    private NbtList createNbtIntList(int ... ints) {
        NbtList nbtList = new NbtList();
        for (int i : ints) {
            nbtList.add(NbtInt.of(i));
        }
        return nbtList;
    }

    private NbtList createNbtDoubleList(double ... doubles) {
        NbtList nbtList = new NbtList();
        for (double d : doubles) {
            nbtList.add(NbtDouble.of(d));
        }
        return nbtList;
    }

    public record StructureBlockInfo(BlockPos pos, BlockState state,
                                     NbtCompound nbt) {
        public StructureBlockInfo(BlockPos pos, BlockState state, @Nullable NbtCompound nbt) {
            this.pos = pos;
            this.state = state;
            this.nbt = nbt;
        }

        public String toString() {
            return String.format("<StructureBlockInfo | %s | %s | %s>", this.pos, this.state, this.nbt);
        }
    }

    public static final class PalettedBlockInfoList {
        private final List<StructureBlockInfo> infos;
        private final Map<Block, List<StructureBlockInfo>> blockToInfos = Maps.newHashMap();

        PalettedBlockInfoList(List<StructureBlockInfo> infos) {
            this.infos = infos;
        }

        public List<StructureBlockInfo> getAll() {
            return this.infos;
        }

        public List<StructureBlockInfo> getAllOf(Block block2) {
            return this.blockToInfos.computeIfAbsent(block2, block -> this.infos.stream().filter(structureBlockInfo -> structureBlockInfo.state.isOf((Block)block)).collect(Collectors.toList()));
        }
    }

    public static class StructureEntityInfo {
        public final Vec3d pos;
        public final BlockPos blockPos;
        public final NbtCompound nbt;

        public StructureEntityInfo(Vec3d pos, BlockPos blockPos, NbtCompound nbt) {
            this.pos = pos;
            this.blockPos = blockPos;
            this.nbt = nbt;
        }
    }

    private static class Palette implements Iterable<BlockState> {
        public static final BlockState AIR = Blocks.AIR.getDefaultState();
        private final IdList<BlockState> ids = new IdList(16);
        private int currentIndex;

        Palette() {
        }

        public int getOrCreateId(BlockState state) {
            int i = this.ids.getRawId(state);
            if (i == -1) {
                i = this.currentIndex++;
                this.ids.set(state, i);
            }
            return i;
        }

        @Nullable
        public BlockState getState(int id) {
            BlockState blockState = this.ids.get(id);
            return blockState == null ? AIR : blockState;
        }

        @Override
        public Iterator<BlockState> iterator() {
            return this.ids.iterator();
        }

        public void set(BlockState state, int id) {
            this.ids.set(state, id);
        }
    }
}