package codes.moulberry.buildermod.render.regions;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;

public class BooleanRegionRenderer {

    public static void render(BooleanRegion region, MatrixStack matrix, Matrix4f projection) {
        VertexBuffer vertexBuffer = region.getVertexBuffer();
        if(vertexBuffer != null) {
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.disableCull();
            RenderSystem.enablePolygonOffset();
            RenderSystem.blendFuncSeparate(770, 771, 1, 0);
            RenderSystem.disableTexture();

            RenderSystem.setShaderColor(1.0f, 0.0f, 0.0f, 0.4f);

            RenderSystem.polygonOffset(0f, -1f);
            matrix.push();
            matrix.translate(region.centerPos.getX(), region.centerPos.getY(), region.centerPos.getZ());
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            vertexBuffer.setShader(matrix.peek().getPositionMatrix(), projection, RenderSystem.getShader());
            matrix.pop();
            RenderSystem.polygonOffset(0, 0);

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

            RenderSystem.disablePolygonOffset();
            RenderSystem.enableCull();
            RenderSystem.enableTexture();
        }
    }

    private static final float XF = 0.8f;
    private static final float YF = 1f;
    private static final float ZF = 0.9f;
    public static void uploadRegion(BooleanRegion region, BufferBuilder bufferBuilder) {
        float d = 1f;
        for (Vec3f face : region.xFaces.values()) {
            bufferBuilder.vertex(face.getX(), face.getY(), face.getZ()+d).color(XF, XF, XF, 1f).next();
            bufferBuilder.vertex(face.getX(), face.getY()+d, face.getZ()+d).color(XF, XF, XF, 1f).next();
            bufferBuilder.vertex(face.getX(), face.getY()+d, face.getZ()).color(XF, XF, XF, 1f).next();
            bufferBuilder.vertex(face.getX(),face.getY(), face.getZ()).color(XF, XF, XF, 1f).next();
        }
        for (Vec3f face : region.yFaces.values()) {
            bufferBuilder.vertex(face.getX(), face.getY(), face.getZ()).color(YF, YF, YF, 1f).next();
            bufferBuilder.vertex(face.getX()+d, face.getY(), face.getZ()).color(YF, YF, YF, 1f).next();
            bufferBuilder.vertex(face.getX()+d, face.getY(), face.getZ()+d).color(YF, YF, YF, 1f).next();
            bufferBuilder.vertex(face.getX(), face.getY(), face.getZ()+d).color(YF, YF, YF, 1f).next();
        }
        for (Vec3f face : region.zFaces.values()) {
            bufferBuilder.vertex(face.getX(), face.getY()+d, face.getZ()) .color(ZF, ZF, ZF, 1f).next();
            bufferBuilder.vertex(face.getX()+d, face.getY()+d, face.getZ()) .color(ZF, ZF, ZF, 1f).next();
            bufferBuilder.vertex(face.getX()+d, face.getY(), face.getZ()) .color(ZF, ZF, ZF, 1f).next();
            bufferBuilder.vertex(face.getX(), face.getY(), face.getZ()) .color(ZF, ZF, ZF, 1f).next();
        }
    }

}