package codes.moulberry.buildermod.macrotool.script.impl.operations.update;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface TUpdateOp {

    void runIntegrated(World world, BlockPos pos);

}
