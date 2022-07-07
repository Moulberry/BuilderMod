package net.moulberry.buildermod;

import com.zaxxer.sparsebits.SparseBitSet;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Warmup(iterations = 15, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class Test {

    private final List<BlockState> blocks = new ArrayList<>();
    private IntList blockIds;

    private final Set<BlockState> simpleSet = new HashSet<>();
    private final ObjectOpenHashSet<BlockState> fastUtilsSet = new ObjectOpenHashSet<>();
    private boolean[] boolArray;
    private SparseBitSet bitSet;

    @Setup
    public void setup() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();

        Registry<Block> blockRegistry = Registry.BLOCK;
        /*for (Map.Entry<RegistryKey<Block>, Block> entry : blockRegistry.getEntries()) {
            for (BlockState state : entry.getValue().getStateManager().getStates()) {
                blocks.add(state);
                // 10 x 10
            }
        }
        BlockState block = blockRegistry.getRandom(ThreadLocalRandom.current()).getDefaultState();

        simpleSet.add(block);

        fastUtilsSet.add(block);

        boolArray = new boolean[blocks.size()];
        boolArray[Block.getRawIdFromState(block)] = true;

        bitSet = new SparseBitSet(blocks.size());
        int hash = System.identityHashCode(block);
        if (hash < 0) hash = -hash;
        bitSet.set(hash, true);*/
    }

    @Benchmark
    public void set(Blackhole blackHole) {
        for (BlockState block : blocks) {
            blackHole.consume(simpleSet.contains(block));
        }
    }

    @Benchmark
    public void array(Blackhole blackHole) {
        blockIds = new IntArrayList(blocks.size());
        for (BlockState state : blocks) {
            blockIds.add(Block.getRawIdFromState(state));
        }
        for (int block : blockIds) {
            blackHole.consume(boolArray[block]);
        }
    }

    @Benchmark
    public void sparseBitSet(Blackhole blackHole) {
        for (BlockState block : blocks) { // 30,000 * 5 * 28
            int hash = System.identityHashCode(block);
            if (hash < 0) hash = -hash;
            blackHole.consume(bitSet.get(hash));
        }
    }

    @Benchmark
    public void fastUtils(Blackhole blackHole) {
        for (BlockState block : blocks) {
            blackHole.consume(fastUtilsSet.contains(block));
        }
    }

}
