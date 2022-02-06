package codes.moulberry.buildermod.render;

import codes.moulberry.buildermod.LaserPointer;
import codes.moulberry.buildermod.ToolMenuManager;
import codes.moulberry.buildermod.customtool.CustomToolManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

public class WorldRendererHook {

    private static WorldRendererHook INSTANCE = new WorldRendererHook();

    public static WorldRendererHook getInstance() {
        return INSTANCE;
    }

    private static final Identifier HIGHLIGHT = new Identifier("buildermod", "highlight.png");

    public void onRender(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline,
                         Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager,
                         Matrix4f projection) {
        matrices.push();
        matrices.translate(-camera.getPos().x, -camera.getPos().y, -camera.getPos().z);
        renderLaserPointer(matrices, tickDelta);

        if(MinecraftClient.getInstance().world != null &&
                ToolMenuManager.getInstance().isOverriding() &&
                MinecraftClient.getInstance().interactionManager != null &&
                MinecraftClient.getInstance().interactionManager.getCurrentGameMode() == GameMode.CREATIVE) {
            Item item = ToolMenuManager.getInstance().getStack().getItem();
            CustomToolManager.acceptTool(item, (tool) -> tool.render(matrices, tickDelta, projection));
        }

        matrices.pop();
    }

    private void renderLaserPointer(MatrixStack matrices, float tickDelta) {
        for(LaserPointer.Chain chain : LaserPointer.getInstance().getChains().values()) {
            if(chain != null && !chain.points.isEmpty()) {
                for(int i=0; i<chain.points.size(); i++) {
                    renderPointerSpot(matrices, chain.points.get(i), tickDelta);
                }

                if(chain.renderPoints != null && chain.renderPoints.size() > 1) {
                    Matrix4f matrix = matrices.peek().getPositionMatrix();

                    RenderSystem.setShaderColor(1, 0, 0, 1);
                    RenderSystem.setShader(GameRenderer::getPositionShader);

                    RenderSystem.lineWidth(3);
                    RenderSystem.disableCull();
                    RenderSystem.disableDepthTest();
                    RenderSystem.disableTexture();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.enableBlend();

                    BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
                    bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION);

                    for(int i=0; i<chain.renderPoints.size(); i++) {
                        Vec3d pos = chain.renderPoints.get(i);
                        bufferBuilder.vertex(matrix, (float)pos.x, (float)pos.y, (float)pos.z).next();
                    }

                    bufferBuilder.end();
                    BufferRenderer.draw(bufferBuilder);
                }
            }
        }
        RenderSystem.enableTexture();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
    }

    private void renderPointerSpot(MatrixStack matrices, Vec3d pos, float tickDelta) {
        RenderSystem.setShaderTexture(0, HIGHLIGHT);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.enableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.setShaderColor(1, 0, 0, 0.05f);

        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
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
    }

}
