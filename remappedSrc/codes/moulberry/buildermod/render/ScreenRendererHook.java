package codes.moulberry.buildermod.render;

import codes.moulberry.buildermod.Capabilities;
import codes.moulberry.buildermod.WheelGUI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;

public class ScreenRendererHook {

    public static void render(MatrixStack matrices, MinecraftClient client, int scaledWidth, int scaledHeight, float tickDelta) {
        WheelGUI.render(matrices, client, scaledWidth, scaledHeight, tickDelta);

        int y = 4;
        for (Capabilities.Capability value : Capabilities.NAMED.values()) {
            if (value.isEnabled()) {
                String text = value.getPrettyName();
                int width = client.textRenderer.getWidth(text);
                client.textRenderer.drawWithShadow(matrices, text, scaledWidth-width-4, y, 0xFFFFFFFF);
                y += 10;
            }
        }
    }

}
