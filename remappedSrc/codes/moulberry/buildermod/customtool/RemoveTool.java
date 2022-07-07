package codes.moulberry.buildermod.customtool;

import codes.moulberry.buildermod.BuilderMod;
import codes.moulberry.buildermod.Identifiers;
import codes.moulberry.buildermod.render.regions.BooleanRegion;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

public class RemoveTool extends GenericTool {

    @Override
    public void leftClick() {
    }

    @Override
    protected void apply(BooleanRegion region) {
        BlockPos playerBlockPos = MinecraftClient.getInstance().player.getBlockPos();

        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(region.totalCubes());
        buf.writeBlockPos(playerBlockPos);

        buf.writeVarInt(1);
        buf.writeShort(0);

        region.forEachBlock(pos -> {
            int encoded = 0;
            encoded |= ((pos.getX()-playerBlockPos.getX()+1024) & 2047) << 11;
            encoded |= ((pos.getY()-playerBlockPos.getY()+512)  & 1023) << 22;
            encoded |= ((pos.getZ()-playerBlockPos.getZ()+1024) & 2047);
            buf.writeInt(encoded);
            buf.writeVarInt(0);
        });

        ClientPlayNetworking.send(Identifiers.SETBLOCK_MULTI, buf);
    }

    @Override
    protected int toolRadius() {
        return BuilderMod.getInstance().config.removeToolRadius;
    }

    @Override
    protected boolean isSelectable(BlockState blockState) {
        return !blockState.isAir();
    }

}
