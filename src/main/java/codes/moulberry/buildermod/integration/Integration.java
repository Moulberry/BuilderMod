package codes.moulberry.buildermod.integration;

import codes.moulberry.buildermod.Identifiers;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

public class Integration {

    public static void setBlock(BlockPos pos, BlockState block) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());

        buf.writeBlockPos(pos);
        buf.writeShort(Block.getRawIdFromState(block));

        ClientPlayNetworking.send(Identifiers.SETBLOCK_SINGLE, buf);
    }

    public static void setBlock(int x, int y, int z, BlockState block) {
        setBlock(new BlockPos(x, y, z), block);
    }

}
