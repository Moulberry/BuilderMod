package codes.moulberry;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;

import java.util.ArrayList;
import java.util.List;

public class WorldRendererHook {

    private static WorldRendererHook INSTANCE = new WorldRendererHook();

    public static WorldRendererHook getInstance() {
        return INSTANCE;
    }

    public void onRender(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline,
                         Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager,
                         Matrix4f matrix4f) {
        matrices.push();
        matrices.translate(-camera.getPos().x, -camera.getPos().y, -camera.getPos().z);
        renderWandSelection(matrices, tickDelta);
        renderWorldeditCUI(matrices, tickDelta);
        renderLaserPointer(matrices, tickDelta);
        matrices.pop();
    }

    private void renderLaserPointer(MatrixStack matrices, float tickDelta) {
        for(LaserPointer.Chain chain : LaserPointer.getInstance().getChains().values()) {
            if(chain != null && !chain.points.isEmpty()) {
                for(int i=0; i<chain.points.size(); i++) {
                    renderPointerSpot(matrices, chain.points.get(i), tickDelta);
                }

                if(chain.renderPoints != null && chain.renderPoints.size() > 1) {
                    Matrix4f matrix = matrices.peek().getModel();

                    RenderSystem.color4f(1, 0, 0, 1);

                    GlStateManager.lineWidth(3);
                    GlStateManager.disableCull();
                    GlStateManager.disableDepthTest();
                    GlStateManager.disableTexture();
                    GlStateManager.disableAlphaTest();
                    RenderSystem.defaultBlendFunc();
                    GlStateManager.enableBlend();

                    BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
                    bufferBuilder.begin(GL11.GL_LINE_STRIP, VertexFormats.POSITION);

                    for(int i=0; i<chain.renderPoints.size(); i++) {
                        Vec3d pos = chain.renderPoints.get(i);
                        bufferBuilder.vertex(matrix, (float)pos.x, (float)pos.y, (float)pos.z).next();
                    }

                    bufferBuilder.end();
                    BufferRenderer.draw(bufferBuilder);
                }

            }
        }
    }

    private void renderPointerSpot(MatrixStack matrices, Vec3d pos, float tickDelta) {
        MinecraftClient.getInstance().getTextureManager().bindTexture(new Identifier("buildermod", "highlight.png"));

        Matrix4f matrix = matrices.peek().getModel();

        GlStateManager.disableCull();
        GlStateManager.disableDepthTest();
        GlStateManager.enableTexture();
        GlStateManager.disableAlphaTest();
        RenderSystem.defaultBlendFunc();
        GlStateManager.enableBlend();

        GlStateManager.color4f(1, 0, 0, 0.05f);

        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        bufferBuilder.begin(GL11.GL_QUADS, VertexFormats.POSITION_TEXTURE);
        for(int i=0; i<360; i+=30) {
            float s = (float) Math.sin(Math.toRadians(i));
            float c = (float) Math.cos(Math.toRadians(i));

            //X
            bufferBuilder.vertex(matrix, (float)pos.x - 0.5f, (float)pos.y + 0.5f*s, (float)pos.z + 0.5f*c).texture(0, 1).next();
            bufferBuilder.vertex(matrix, (float)pos.x + 0.5f, (float)pos.y + 0.5f*s, (float)pos.z + 0.5f*c).texture(1, 1).next();
            bufferBuilder.vertex(matrix, (float)pos.x + 0.5f,(float)pos.y - 0.5f*s, (float)pos.z - 0.5f*c).texture(1, 0).next();
            bufferBuilder.vertex(matrix, (float)pos.x - 0.5f, (float)pos.y - 0.5f*s, (float)pos.z - 0.5f*c).texture(0, 0).next();

            //Y
            bufferBuilder.vertex(matrix, (float)pos.x + 0.5f*c, (float)pos.y - 0.5f, (float)pos.z + 0.5f*s).texture(0, 1).next();
            bufferBuilder.vertex(matrix, (float)pos.x + 0.5f*c, (float)pos.y + 0.5f, (float)pos.z + 0.5f*s).texture(1, 1).next();
            bufferBuilder.vertex(matrix, (float)pos.x - 0.5f*c, (float)pos.y + 0.5f,(float)pos.z - 0.5f*s).texture(1, 0).next();
            bufferBuilder.vertex(matrix, (float)pos.x - 0.5f*c, (float)pos.y - 0.5f, (float)pos.z - 0.5f*s).texture(0, 0).next();

            //Z
            bufferBuilder.vertex(matrix, (float)pos.x + 0.5f*c, (float)pos.y + 0.5f*s, (float)pos.z - 0.5f).texture(0, 1).next();
            bufferBuilder.vertex(matrix, (float)pos.x + 0.5f*c, (float)pos.y + 0.5f*s, (float)pos.z + 0.5f).texture(1, 1).next();
            bufferBuilder.vertex(matrix, (float)pos.x - 0.5f*c, (float)pos.y - 0.5f*s, (float)pos.z + 0.5f).texture(1, 0).next();
            bufferBuilder.vertex(matrix, (float)pos.x - 0.5f*c, (float)pos.y - 0.5f*s, (float)pos.z - 0.5f).texture(0, 0).next();
        }

        bufferBuilder.end();
        BufferRenderer.draw(bufferBuilder);

        RenderSystem.enableAlphaTest();
    }

    private void renderWorldeditCUI(MatrixStack matrices, float tickDelta) {
        WorldEditCUI worldEditCUI = WorldEditCUI.getInstance();
        if(MinecraftClient.getInstance().world != null &&
                ToolMenuManager.getInstance().isOverriding() &&
                ToolMenuManager.getInstance().getStack().getItem() == Items.WOODEN_AXE &&
                MinecraftClient.getInstance().interactionManager.getCurrentGameMode() == GameMode.CREATIVE &&
                worldEditCUI.getPos1() != null && worldEditCUI.getPos2() != null) {
            int highlightColour = 0xA01EA1A1;
            int highlightColour2 = 0xA01EA1C1;
            int mainColour = 0x80188181;
            drawFancyBox(matrices, worldEditCUI.getPos1(), worldEditCUI.getPos2(), 0.001f, mainColour, highlightColour, highlightColour2);
        }
    }

    private void renderWandSelection(MatrixStack matrices, float tickDelta) {
        if(MinecraftClient.getInstance().world != null &&
                ToolMenuManager.getInstance().isOverriding() &&
                ToolMenuManager.getInstance().getStack().getItem() == Items.WOODEN_AXE &&
                MinecraftClient.getInstance().interactionManager.getCurrentGameMode() == GameMode.CREATIVE) {
            Entity entity = MinecraftClient.getInstance().getCameraEntity();
            if(entity != null) {
                HitResult result = entity.raycast(100, tickDelta, !entity.isInLava() && !entity.isSubmergedInWater());

                if(result instanceof BlockHitResult) {
                    BlockHitResult blockHitResult = (BlockHitResult) result;

                    BlockPos pos = blockHitResult.getBlockPos();
                    BlockState state = MinecraftClient.getInstance().world.getBlockState(pos);
                    if (!state.isAir() && MinecraftClient.getInstance().world.getWorldBorder().contains(pos)) {
                        int highlightColour = 0xA01EA1A1;
                        int highlightColour2 = 0xA01EA1C1;
                        int mainColour = 0x80188181;
                        drawFancyBox(matrices, pos, pos, 0.002f, mainColour, highlightColour, highlightColour2);
                    }
                }
            }
        }
    }

    private void drawFancyBox(MatrixStack matrices, BlockPos pos1, BlockPos pos2, float expansion,
                              int mainColour, int highlightColour, int highlightColour2) {
        Box box = new Box(pos1.getX()+0.5f, pos1.getY()+0.5f, pos1.getZ()+0.5f,
                pos2.getX()+0.5f, pos2.getY()+0.5f, pos2.getZ()+0.5f).expand(0.5f+expansion);

        Matrix4f mat = matrices.peek().getModel();
        GlStateManager.disableCull();
        GlStateManager.disableTexture();
        GlStateManager.enableDepthTest();
        GlStateManager.enableBlend();

        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();

        {
            int alpha = (mainColour >> 24) & 0xFF;
            int red = (mainColour >> 16) & 0xFF;
            int green = (mainColour >> 8) & 0xFF;
            int blue = mainColour & 0xFF;

            bufferBuilder.begin(GL11.GL_TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
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

            GlStateManager.lineWidth(3);
            GlStateManager.enableDepthTest();
            bufferBuilder.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);
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

        GlStateManager.lineWidth(3);
        GlStateManager.disableDepthTest();

        {
            int alpha = (highlightColour >> 24) & 0xFF;
            int red = (highlightColour >> 16) & 0xFF;
            int green = (highlightColour >> 8) & 0xFF;
            int blue = highlightColour & 0xFF;

            bufferBuilder.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);
            WorldRenderer.drawBox(matrices, bufferBuilder, box, red / 255f, green / 255f, blue / 255f, alpha / 255f);
            bufferBuilder.end();
            BufferRenderer.draw(bufferBuilder);
        }

        GlStateManager.enableDepthTest();
        GlStateManager.enableCull();
    }

}
