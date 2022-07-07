package codes.moulberry.buildermod.macrotool.script.impl.operations.mask;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkCache;

public interface TMaskOp {

    boolean matches(ChunkCache cache, BlockPos.Mutable pos);

}
