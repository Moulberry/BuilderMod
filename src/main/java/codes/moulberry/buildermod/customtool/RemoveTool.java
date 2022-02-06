package codes.moulberry.buildermod.customtool;

import codes.moulberry.buildermod.BuilderMod;
import codes.moulberry.buildermod.Identifiers;
import codes.moulberry.buildermod.gui.SmoothToolConfigureMenu;
import codes.moulberry.buildermod.render.Region;
import io.github.cottonmc.cotton.gui.client.CottonClientScreen;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class RemoveTool extends GenericTool {

    @Override
    public void leftClick() {
    }

    @Override
    protected void apply(Region region) {
        BlockPos playerBlockPos = MinecraftClient.getInstance().player.getBlockPos();

        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(region.totalCubes());
        buf.writeBlockPos(playerBlockPos);

        buf.writeVarInt(1);
        buf.writeShort(0);

        for (BlockPos value : region.includedBlocks.values()) {
            BlockPos offset = value.subtract(playerBlockPos);
            int encoded = 0;
            encoded |= ((offset.getX()+1024) & 2047) << 11;
            encoded |= ((offset.getY()+512)  & 1023) << 22;
            encoded |= ((offset.getZ()+1024) & 2047);
            buf.writeInt(encoded);
            buf.writeVarInt(0);
        }

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
