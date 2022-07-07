package codes.moulberry.buildermod.customtool;

import codes.moulberry.buildermod.WorldEditCUI;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Matrix4f;

public class WEWandTool implements CustomTool {
    @Override
    public void leftClick() {
        CustomTool.raycastBlock((blockHitResult -> {
            BlockPos pos = blockHitResult.getBlockPos();
            MinecraftClient.getInstance().player.sendChatMessage("//pos1 "+pos.getX()+","+pos.getY()+","+pos.getZ());
        }));
    }

    @Override
    public void rightClick() {
        CustomTool.raycastBlock((blockHitResult -> {
            BlockPos pos = blockHitResult.getBlockPos();
            MinecraftClient.getInstance().player.sendChatMessage("//pos2 "+pos.getX()+","+pos.getY()+","+pos.getZ());
        }));
    }

    @Override
    public void render(MatrixStack matrices, float tickDelta, Matrix4f projection) {
        renderWorldeditCUI(matrices, tickDelta);
        renderWandSelection(matrices, tickDelta);
    }

    private void renderWorldeditCUI(MatrixStack matrices, float tickDelta) {
        WorldEditCUI worldEditCUI = WorldEditCUI.getInstance();
        if (worldEditCUI.getPos1() != null && worldEditCUI.getPos2() != null) {
            int highlightColour = 0xA01EA1A1;
            int highlightColour2 = 0xA01EA1C1;
            int mainColour = 0x80188181;
            drawFancyBox(matrices, worldEditCUI.getPos1(), worldEditCUI.getPos2(),
                    0.001f, mainColour, highlightColour, highlightColour2);
        }
    }

    private void renderWandSelection(MatrixStack matrices, float tickDelta) {
        CustomTool.raycastBlock(blockHitResult -> {
            BlockPos pos = blockHitResult.getBlockPos();
            BlockState state = MinecraftClient.getInstance().world.getBlockState(pos);
            if (!state.isAir() && MinecraftClient.getInstance().world.getWorldBorder().contains(pos)) {
                int highlightColour = 0xA01EA1A1;
                int highlightColour2 = 0xA01EA1C1;
                int mainColour = 0x80188181;
                drawFancyBox(matrices, pos, pos, 0.002f, mainColour, highlightColour, highlightColour2);
            }
        });
    }

    private void drawFancyBox(MatrixStack matrices, BlockPos pos1, BlockPos pos2, float expansion,
                              int mainColour, int highlightColour, int highlightColour2) {
        Box box = new Box(pos1.getX()+0.5f, pos1.getY()+0.5f, pos1.getZ()+0.5f,
                pos2.getX()+0.5f, pos2.getY()+0.5f, pos2.getZ()+0.5f).expand(0.5f+expansion);

        Matrix4f mat = matrices.peek().getPositionMatrix();
        RenderSystem.disableCull();
        RenderSystem.disableTexture();
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();

        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();

        {
            int alpha = (mainColour >> 24) & 0xFF;
            int red = (mainColour >> 16) & 0xFF;
            int green = (mainColour >> 8) & 0xFF;
            int blue = mainColour & 0xFF;

            bufferBuilder.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            bufferBuilder.vertex(mat, (float)box.minX, (float)box.maxY, (float)box.maxZ).color(red, green, blue, alpha).next();
            bufferBuilder.vertex(mat, (float)box.maxX, (float)box.maxY, (float)box.maxZ).color(red, green, blue, alpha).next();
            bufferBuilder.vertex(mat, (float)box.minX, (float)box.minY, (float)box.maxZ).color(red, green, blue, alpha).next();
            bufferBuilder.vertex(mat, (float)box.maxX, (float)box.minY, (float)box.maxZ).color(red, green, blue, alpha).next();
            bufferBuilder.vertex(mat, (float)box.maxX, (float)box.minY, (float)box.minZ).color(red, green, blue, alpha).next();
            bufferBuilder.vertex(mat, (float)box.maxX, (float)box.maxY, (float)box.maxZ).color(red, green, blue, alpha).next();
            bufferBuilder.vertex(mat, (float)box.maxX, (float)box.maxY, (float)box.minZ).color(red, green, blue, alpha).next();
            bufferBuilder.vertex(mat, (float)box.minX, (float)box.maxY, (float)box.maxZ).color(red, green, blue, alpha).next();
            bufferBuilder.vertex(mat, (float)box.minX, (float)box.maxY, (float)box.minZ).color(red, green, blue, alpha).next();
            bufferBuilder.vertex(mat, (float)box.minX, (float)box.minY, (float)box.maxZ).color(red, green, blue, alpha).next();
            bufferBuilder.vertex(mat, (float)box.minX, (float)box.minY, (float)box.minZ).color(red, green, blue, alpha).next();
            bufferBuilder.vertex(mat, (float)box.maxX, (float)box.minY, (float)box.minZ).color(red, green, blue, alpha).next();
            bufferBuilder.vertex(mat, (float)box.minX, (float)box.maxY, (float)box.minZ).color(red, green, blue, alpha).next();
            bufferBuilder.vertex(mat, (float)box.maxX, (float)box.maxY, (float)box.minZ).color(red, green, blue, alpha).next();
            bufferBuilder.end();
            BufferRenderer.draw(bufferBuilder);
        }

        {
            Box box2 = box.expand(0.001f);

            int alpha = (highlightColour2 >> 24) & 0xFF;
            int red = (highlightColour2 >> 16) & 0xFF;
            int green = (highlightColour2 >> 8) & 0xFF;
            int blue = highlightColour2 & 0xFF;

            RenderSystem.lineWidth(3);
            RenderSystem.enableDepthTest();
            bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            for(double x=box2.minX+1; x<box2.maxX-0.1f; x++) {
                bufferBuilder.vertex(mat, (float)x, (float)box2.minY, (float)box2.minZ).color(red, green, blue, alpha).next();
                bufferBuilder.vertex(mat, (float)x, (float)box2.minY, (float)box2.maxZ).color(red, green, blue, alpha).next();
                bufferBuilder.vertex(mat, (float)x, (float)box2.maxY, (float)box2.minZ).color(red, green, blue, alpha).next();
                bufferBuilder.vertex(mat, (float)x, (float)box2.maxY, (float)box2.maxZ).color(red, green, blue, alpha).next();

                bufferBuilder.vertex(mat, (float)x, (float)box2.minY, (float)box2.minZ).color(red, green, blue, alpha).next();
                bufferBuilder.vertex(mat, (float)x, (float)box2.maxY, (float)box2.minZ).color(red, green, blue, alpha).next();
                bufferBuilder.vertex(mat, (float)x, (float)box2.minY, (float)box2.maxZ).color(red, green, blue, alpha).next();
                bufferBuilder.vertex(mat, (float)x, (float)box2.maxY, (float)box2.maxZ).color(red, green, blue, alpha).next();
            }
            for(double y=box2.minY+1; y<box2.maxY-0.1f; y++) {
                bufferBuilder.vertex(mat, (float)box2.minX, (float)y, (float)box2.minZ).color(red, green, blue, alpha).next();
                bufferBuilder.vertex(mat, (float)box2.maxX, (float)y, (float)box2.minZ).color(red, green, blue, alpha).next();
                bufferBuilder.vertex(mat, (float)box2.minX, (float)y, (float)box2.maxZ).color(red, green, blue, alpha).next();
                bufferBuilder.vertex(mat, (float)box2.maxX, (float)y, (float)box2.maxZ).color(red, green, blue, alpha).next();

                bufferBuilder.vertex(mat, (float)box2.minX, (float)y, (float)box2.minZ).color(red, green, blue, alpha).next();
                bufferBuilder.vertex(mat, (float)box2.minX, (float)y, (float)box2.maxZ).color(red, green, blue, alpha).next();
                bufferBuilder.vertex(mat, (float)box2.maxX, (float)y, (float)box2.minZ).color(red, green, blue, alpha).next();
                bufferBuilder.vertex(mat, (float)box2.maxX, (float)y, (float)box2.maxZ).color(red, green, blue, alpha).next();
            }
            for(double z=box2.minZ+1; z<box2.maxZ-0.1f; z++) {
                bufferBuilder.vertex(mat, (float)box2.minX, (float)box2.minY, (float)z).color(red, green, blue, alpha).next();
                bufferBuilder.vertex(mat, (float)box2.maxX, (float)box2.minY, (float)z).color(red, green, blue, alpha).next();
                bufferBuilder.vertex(mat, (float)box2.minX, (float)box2.maxY, (float)z).color(red, green, blue, alpha).next();
                bufferBuilder.vertex(mat, (float)box2.maxX, (float)box2.maxY, (float)z).color(red, green, blue, alpha).next();

                bufferBuilder.vertex(mat, (float)box2.minX, (float)box2.minY, (float)z).color(red, green, blue, alpha).next();
                bufferBuilder.vertex(mat, (float)box2.minX, (float)box2.maxY, (float)z).color(red, green, blue, alpha).next();
                bufferBuilder.vertex(mat, (float)box2.maxX, (float)box2.minY, (float)z).color(red, green, blue, alpha).next();
                bufferBuilder.vertex(mat, (float)box2.maxX, (float)box2.maxY, (float)z).color(red, green, blue, alpha).next();
            }
            bufferBuilder.end();
            BufferRenderer.draw(bufferBuilder);
        }

        RenderSystem.lineWidth(3);
        RenderSystem.disableDepthTest();

        {
            int alpha = (highlightColour >> 24) & 0xFF;
            int red = (highlightColour >> 16) & 0xFF;
            int green = (highlightColour >> 8) & 0xFF;
            int blue = highlightColour & 0xFF;

            bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            WorldRenderer.drawBox(matrices, bufferBuilder, box, red / 255f, green / 255f, blue / 255f, alpha / 255f);
            bufferBuilder.end();
            BufferRenderer.draw(bufferBuilder);
        }

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }

}
