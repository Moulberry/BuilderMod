package codes.moulberry.buildermod.macrotool.script.impl;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Predicate;

public class UpdateOperation {

    /*private final StampedLock lock = new StampedLock();
    private Int2IntOpenHashMap palette = new Int2IntOpenHashMap();

    public class SlicedUpdateOperation {
        private int chunkX;
        private int chunkY;
        private int chunkZ;

        private int checkedCount = 0;
        private final boolean[][][] checked = new boolean[16][16][16];
        private final int[][][] blocks = new int[16][16][16];

        public void execute(Predicate<BlockPos> predicate, int[] currentBlocks, int lower, int sizeX, int sizeXZ) {
            if (checkedCount >= 16*16*16) return;

            BlockPos.Mutable mutable = new BlockPos.Mutable();
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        if (checked[x][y][z]) return;

                        checked[x][y][z] = true;
                        checkedCount++;

                        mutable.set(x+chunkX, y+chunkY, z+chunkZ);

                        if (predicate.test(mutable)) {
                            int current = currentBlocks[lower + x + z*sizeX + y*sizeXZ];
                            if (current == 1) {
                                blocks[x][y][z] = 2;
                            }
                        }
                    }
                }
            }
        }
    }*/

    public static class SlicedUpdateOperation implements Callable<Void> {
        private final ServerWorld world;
        private final int chunkX;
        private final int chunkY;
        private final int chunkZ;

        private boolean isFinished = false;

        public SlicedUpdateOperation(ServerWorld world, int chunkX, int chunkY, int chunkZ) {
            this.world = world;
            this.chunkX = chunkX;
            this.chunkY = chunkY;
            this.chunkZ = chunkZ;
        }

        public Void call() {
            BlockPos.Mutable mutable = new BlockPos.Mutable();

            ShortSet changed = new ShortOpenHashSet();

            WorldChunk chunk = world.getChunk(chunkX, chunkZ);
            ChunkSection section = chunk.getSection(chunk.sectionCoordToIndex(chunkY));

            PalettedContainer<BlockState> container = new PalettedContainer<>(Block.STATE_IDS,
                    Blocks.AIR.getDefaultState(), PalettedContainer.PaletteProvider.BLOCK_STATE);
            section.blockStateContainer = container;
            section.calculateCounts();

            for (int x=chunkX*16; x<chunkX*16+16; x++) {
                for (int y=chunkY*16; y<chunkY*16+16; y++) {
                    for (int z=chunkZ*16; z<chunkZ*16+16; z++) {
                        mutable.set(x, y, z);
                        changed.add(ChunkSectionPos.packLocal(mutable));
                    }
                }
            }

            ChunkDeltaUpdateS2CPacket packet = new ChunkDeltaUpdateS2CPacket(
                    ChunkSectionPos.from(chunkX, chunkY, chunkZ),
                    changed,
                    section,
                    true
            );
            world.getChunkManager().threadedAnvilChunkStorage.getPlayersWatchingChunk(new ChunkPos(chunkX, chunkZ))
                    .forEach(serverPlayerEntity -> serverPlayerEntity.networkHandler.sendPacket(packet));
            return null;
        }
    }

    private final Long2ObjectMap<SlicedUpdateOperation> slicedMap = new Long2ObjectOpenHashMap<>();

    public void perform(ServerWorld world, BlockPos min, BlockPos max, Predicate<BlockPos> predicate) {
        int minX = Math.floorDiv(min.getX(), 16);
        int minY = Math.floorDiv(min.getY(), 16);
        int minZ = Math.floorDiv(min.getZ(), 16);
        int maxX = Math.floorDiv(max.getX(), 16);
        int maxY = Math.floorDiv(max.getY(), 16);
        int maxZ = Math.floorDiv(max.getZ(), 16);

        List<Callable<Void>> tasks = new ArrayList<>();

        for (int cX = minX; cX <= maxX; cX++) {
            for (int cY = minY; cY <= maxY; cY++) {
                for (int cZ = minZ; cZ <= maxZ; cZ++) {
                    final int x = cX;
                    final int y = cY;
                    final int z = cZ;

                    final long id = BlockPos.asLong(x, y, z);

                    SlicedUpdateOperation operation = slicedMap.computeIfAbsent(id, k -> new SlicedUpdateOperation(world, x, y, z));
                    if (!operation.isFinished) {
                        tasks.add(operation);
                    }
                }
            }
        }

        if (!tasks.isEmpty()) {
            ForkJoinPool.commonPool().invokeAll(tasks);
        }
    }

}
