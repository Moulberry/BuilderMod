package codes.moulberry.buildermod;

import codes.moulberry.buildermod.WheelGUI.HoverEntry;
import codes.moulberry.buildermod.WheelGUI.WheelEntry;
import com.mojang.blaze3d.systems.RenderSystem;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class WheelGUI {

    private static final int INNER_SIZE = 25;
    private static final int OUTER_SIZE = 80;
    private static final int HOVER_SIZE_INCREASE = 10;

    private static final Identifier CHANNEL = new Identifier("buildwheel:register");
    private static final Identifier DEFAULT_WHEEL = new Identifier("buildwheel:main");

    private record WheelEntry(String displayText, ItemStack displayStack,
                              Identifier subwheel, String command){}

    private static List<WheelEntry> currentWheel = null;
    private static final Map<Identifier, List<WheelEntry>> identifierMap = new HashMap<>();

    public static void sendEnablePacket(ClientPlayNetworkHandler handler, PacketSender sender, MinecraftClient client) {
        if (currentWheel != null) currentWheel = null;
        identifierMap.clear();
        ClientPlayNetworking.send(CHANNEL, new PacketByteBuf(Unpooled.buffer()));
    }

    public static void setupPackets() {
        ClientPlayNetworking.registerGlobalReceiver(CHANNEL, WheelGUI::addWheel);
    }

    private static void addWheel(MinecraftClient client, ClientPlayNetworkHandler handler,
                                 PacketByteBuf buf, PacketSender responseSender) {
        byte operation = buf.readByte();
        if (operation == 0) { // Add
            Identifier identifier = buf.readIdentifier();
            List<WheelEntry> entries = buf.readList((buf2) -> new WheelEntry(
                    buf2.readString(),
                    buf2.readItemStack(),
                    buf2.readByte() != 0 ? buf2.readIdentifier() : null,
                    buf2.readString()
            ));
            identifierMap.put(identifier, entries);
        } else if (operation == 1) { // Remove
            Identifier identifier = buf.readIdentifier();
            identifierMap.remove(identifier);
        } else if (operation == 2) { // Clear
            currentWheel = null;
            identifierMap.clear();
        }
    }

    private static int getIndex(MinecraftClient client) {
        if (currentWheel == null) return -1;

        double mdx = client.mouse.getX() - client.getWindow().getWidth()/2f;
        double mdy = client.mouse.getY() - client.getWindow().getHeight()/2f;

        double minRadius = client.getWindow().getScaleFactor()*INNER_SIZE;
        if (mdx*mdx + mdy*mdy < minRadius*minRadius) return -1;

        int entries = currentWheel.size();

        float angle = (float) Math.toDegrees(Math.atan2(mdx, -mdy));
        if (angle < 0) angle += 360;
        return Math.round(angle*entries/360) % entries;
    }

    public static void openWheel() {
        openWheel(DEFAULT_WHEEL);
    }

    public static void openWheel(Identifier identifier) {
        currentWheel = identifierMap.get(identifier);
        if (currentWheel != null) {
            MinecraftClient.getInstance().mouse.unlockCursor();
        }
    }

    public static boolean isWheelOpen() {
        return currentWheel != null;
    }

    public static void closeWheel() {
        currentWheel = null;
    }

    public static boolean clicked() {
        if (currentWheel == null) return true;

        MinecraftClient client = MinecraftClient.getInstance();
        int index = getIndex(client);
        if (client.player != null && index >= 0) {
            WheelEntry entry = currentWheel.get(index);
            if (entry.command() != null) {
                client.player.sendChatMessage(entry.command());
            } else if (entry.subwheel() != null) {
                openWheel(entry.subwheel());
                hoverTimeMap.clear();
                return false;
            }
        }
        return true;
    }

    public static void tick(MinecraftClient client) {
        if (currentWheel != null) {
            int wedge = getIndex(client);
            var entries = new HashSet<>(hoverTimeMap.entrySet());

            for (var entry : entries) {
                if (entry.getKey() == wedge) {
                    int value = Math.max(5, Math.min(20, entry.getValue().value()+1));
                    hoverTimeMap.put(entry.getKey(), new HoverEntry(value, true));
                } else {
                    int value = Math.max(0, Math.min(10, entry.getValue().value()-1));
                    hoverTimeMap.put(entry.getKey(), new HoverEntry(value, false));
                }
            }

            if (wedge >= 0) {
                hoverTimeMap.computeIfAbsent(wedge, k -> new HoverEntry(5, true));
            }
        } else {
            hoverTimeMap.clear();
        }
    }

    private record HoverEntry(int value, boolean increasing){};
    private static final Map<Integer, HoverEntry> hoverTimeMap = new HashMap<>();

    private static float getHover(int index, float tickDelta) {
        if (!hoverTimeMap.containsKey(index)) return 0;

        HoverEntry entry = hoverTimeMap.get(index);
        return Math.max(0, entry.value() + (entry.increasing() ? tickDelta : -tickDelta));
    }

    private static float ease(float x) {
        return (float)(1 - Math.pow(1 - x, 3));
    }

    public static void render(MatrixStack matrices, MinecraftClient client, int scaledWidth, int scaledHeight, float tickDelta) {
        if (currentWheel == null) return;

        RenderSystem.disableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tessellator g = Tessellator.getInstance();
        BufferBuilder bufferBuilder = g.getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        Map<Integer, Float> hoverForIndex = new HashMap<>();

        int entries = currentWheel.size();

        for (int t=0; t<360; t++) {
            int index = Math.round(t*entries/360f) % entries;

            float hover = hoverForIndex.computeIfAbsent(index, k -> ease(getHover(k, tickDelta)/20f));

            float len = OUTER_SIZE + hover*HOVER_SIZE_INCREASE;
            float gray = 0.2f+0.2f*hover;
            float alpha = 0.8f + 0.2f*hover;

            float x1 = (float) Math.sin(Math.toRadians(t));
            float y1 = (float) -Math.cos(Math.toRadians(t));
            float x2 = (float) Math.sin(Math.toRadians(t + 1));
            float y2 = (float) -Math.cos(Math.toRadians(t + 1));

            bufferBuilder.vertex(scaledWidth / 2f + x1 * len, scaledHeight / 2f + y1 * len, 0)
                    .color(gray, gray, gray, alpha).next();
            bufferBuilder.vertex(scaledWidth / 2f + x2 * len, scaledHeight / 2f + y2 * len, 0)
                    .color(gray, gray, gray, alpha).next();
            bufferBuilder.vertex(scaledWidth / 2f + x2 * INNER_SIZE, scaledHeight / 2f + y2 * INNER_SIZE, 0)
                    .color(gray, gray, gray, alpha*0.7f).next();
            bufferBuilder.vertex(scaledWidth / 2f + x1 * INNER_SIZE, scaledHeight / 2f + y1 * INNER_SIZE, 0)
                    .color(gray, gray, gray, alpha*0.7f).next();
        }
        g.draw();

        for (int index=0; index<entries; index++) {
            WheelEntry entry = currentWheel.get(index);

            if (entry.displayText != null) {
                float hover = hoverForIndex.computeIfAbsent(index, k -> ease(getHover(k, tickDelta)/20f));
                float len = (INNER_SIZE + OUTER_SIZE+hover*HOVER_SIZE_INCREASE)/2f;

                float angle = 360f/entries*index;
                float x = (float) Math.sin(Math.toRadians(angle))*len;
                float y = (float) -Math.cos(Math.toRadians(angle))*len;
                int width = client.textRenderer.getWidth(entry.displayText);
                client.textRenderer.draw(matrices, entry.displayText, scaledWidth/2f+x-width/2f, scaledHeight/2f+y, 0xFFFFFFFF);
            }
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
    }

}
