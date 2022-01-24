package codes.moulberry;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.MessageType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WorldEditCUI {

    private static WorldEditCUI INSTANCE = new WorldEditCUI();

    public static WorldEditCUI getInstance() {
        return INSTANCE;
    }

    private static final Pattern WE_LOC_PATTERN = Pattern.compile("\\(FAWE\\) (First|Second) position set to \\((\\d+), (\\d+), (\\d+)\\)\\.?(?: \\(\\d+\\)\\.)?");

    private BlockPos pos1 = null;
    private BlockPos pos2 = null;
    private String selType;

    private static final Identifier CHANNEL_WECUI = new Identifier("worldedit", "cui");

    public void onPlayReady(ClientPlayNetworkHandler handler, PacketSender sender, MinecraftClient client) {
        System.out.println("On play ready!");
        String message = "v|4";
        ByteBuf buffer = Unpooled.copiedBuffer(message, StandardCharsets.UTF_8);
        ClientPlayNetworking.send(CHANNEL_WECUI, new PacketByteBuf(buffer));
        System.out.println("Registered!");
    }

    public void registerReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(CHANNEL_WECUI, this::onReceive);
    }

    public void onWorldChange() {
        pos1 = null;
        pos2 = null;
    }

    private Set<String> selTypes = Sets.newHashSet("p");

    public void onReceive(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        try {
            int bytes = buf.readableBytes();
            if(bytes > 0) {
                String payload = buf.toString(0, bytes, StandardCharsets.UTF_8);

                String[] args = payload.split("\\|");
                if(args.length >= 4) {
                    if(selTypes.contains(args[0])) {
                        selType = args[0];
                        if(selType.equalsIgnoreCase("p")) {
                            System.out.println("pos!");
                            int x = Integer.parseInt(args[2]);
                            int y = Integer.parseInt(args[3]);
                            int z = Integer.parseInt(args[4]);

                            BlockPos pos = new BlockPos(x, y, z);

                            if(args[1].equalsIgnoreCase("0")) {
                                pos1 = pos;
                            } else {
                                pos2 = pos;
                            }
                        }
                    }
                }

                System.out.println("Payload:" + payload);
                return;
            }
            System.out.println("Zero bytes");
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

    public BlockPos getPos1() {
        return pos1;
    }

    public BlockPos getPos2() {
        return pos2;
    }
}
