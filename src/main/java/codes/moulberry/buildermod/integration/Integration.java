package codes.moulberry.buildermod.integration;

import codes.moulberry.buildermod.Identifiers;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Iterator;

public class Integration {

    public record IntegrationResult(Identifier identifier, PacketByteBuf packetByteBuf, ByteBuf buf) {
        public void send() {
            ClientPlayNetworking.send(identifier, packetByteBuf);
        }
    }

    public static IntegrationResult setBlock(BlockPos pos, BlockState block) {
        ByteBuf byteBuf = Unpooled.buffer();
        PacketByteBuf buf = new PacketByteBuf(byteBuf);

        buf.writeBlockPos(pos);
        buf.writeShort(Block.getRawIdFromState(block));

        return new IntegrationResult(Identifiers.SETBLOCK_SINGLE, buf, byteBuf);
    }

    public static IntegrationResult setBlock(int x, int y, int z, BlockState block) {
        return setBlock(new BlockPos(x, y, z), block);
    }

}
