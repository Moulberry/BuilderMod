package codes.moulberry.buildermod.render.regions;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ShortMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Shader;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.math.random.LocalRandom;
import net.minecraft.util.math.random.Random;

import java.util.concurrent.ThreadLocalRandom;

public class BlockRegionRenderer {

    public static void render(BlockRegion region, MatrixStack matrix, Matrix4f projection, float opacity) {
        VertexBuffer vertexBuffer = region.getVertexBuffer();
        if(vertexBuffer != null) {
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.enablePolygonOffset();
            RenderSystem.enableTexture();

            RenderSystem.blendFuncSeparate(770, 771, 1, 0);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, opacity);
            RenderSystem.polygonOffset(-1f, -3f);

            matrix.push();
            matrix.translate(region.centerPos.getX(), region.centerPos.getY(), region.centerPos.getZ());

            Shader shader = GameRenderer.getPositionColorTexLightmapShader();
            RenderSystem.setShader(() -> shader);
            TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
            textureManager.getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).setFilter(false, true);
            RenderSystem.setShaderTexture(0, SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);

            vertexBuffer.bind();
            vertexBuffer.draw(matrix.peek().getPositionMatrix(), projection, shader);
            VertexBuffer.unbind();

            matrix.pop();

            RenderSystem.polygonOffset(0, 0);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

            RenderSystem.disablePolygonOffset();
            RenderSystem.enableCull();
        }
    }

    public static boolean rendering = false;
    public static void uploadRegion(BlockRegion region, BufferBuilder bufferBuilder) {
        BlockRenderManager renderManager = MinecraftClient.getInstance().getBlockRenderManager();

        Random rand = new LocalRandom(System.nanoTime());

        MatrixStack matrices = new MatrixStack();

        rendering = true;

        BlockPos.Mutable blockPos = new BlockPos.Mutable();
        for (Long2ObjectMap.Entry<BlockState> entry : region.blocks.long2ObjectEntrySet()) {
            blockPos.set(entry.getLongKey());

            matrices.push();
            matrices.translate(blockPos.getX(), blockPos.getY(), blockPos.getZ());

            renderManager.renderBlock(entry.getValue(), blockPos, region, matrices, bufferBuilder, true, rand);

            FluidState fluid = entry.getValue().getFluidState();
            if (!fluid.isEmpty()) {
                renderManager.renderFluid(blockPos, region, bufferBuilder, entry.getValue(), fluid);
            }

            matrices.pop();
        }

        rendering = false;
    }

}