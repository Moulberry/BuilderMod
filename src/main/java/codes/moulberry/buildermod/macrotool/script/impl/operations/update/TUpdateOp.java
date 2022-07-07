package codes.moulberry.buildermod.macrotool.script.impl.operations.update;

import it.unimi.dsi.fastutil.shorts.Short2IntMap;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface TUpdateOp {

    void addToPalette(ShortSet palette);
    short getNewBlockAt(BlockPos pos);

}
