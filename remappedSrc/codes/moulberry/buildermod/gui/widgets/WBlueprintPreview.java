package codes.moulberry.buildermod.gui.widgets;

import codes.moulberry.buildermod.blueprint.ProtoBlueprint;
import codes.moulberry.buildermod.render.regions.BlockRegion;
import codes.moulberry.buildermod.render.regions.BlockRegionRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.cottonmc.cotton.gui.client.BackgroundPainter;
import io.github.cottonmc.cotton.gui.client.NinePatchBackgroundPainter;
import io.github.cottonmc.cotton.gui.widget.WWidget;
import io.github.cottonmc.cotton.gui.widget.data.InputResult;
import io.github.cottonmc.cotton.gui.widget.data.Texture;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.math.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;

public class WBlueprintPreview extends WWidget {

    private static final Identifier BACKGROUND = new Identifier("buildermod", "gui/inner.png");
    private static final NinePatchBackgroundPainter PAINTER =
            BackgroundPainter.createNinePatch(new Texture(BACKGROUND), builder -> builder.cornerSize(8).cornerUv(0.25f));

    private static final BlockRegion REGION = new BlockRegion(false, false);

    public WBlueprintPreview(ProtoBlueprint protoBlueprint) {
        REGION.clear();

        for (int x = 0; x<protoBlueprint.sizeX(); x++) {
            for (int y = 0; y<protoBlueprint.sizeY(); y++) {
                for (int z = 0; z<protoBlueprint.sizeZ(); z++) {
                    BlockState state = protoBlueprint.blockStates()[x][y][z];
                    if (state != null) {
                        REGION.addBlock(x, y, z, state);
                    }
                }
            }
        }
    }

    private float yaw = 135;
    private float pitch = 30;

    private float manualZoom = -1;
    private float manualXMove = 0;
    private float manualYMove = 0;

    private float averageZoom = 100;
    private float zoom = 100;
    private boolean wasOOBLast = false;
    private int consecutiveZooms = 1;

    @Override
    public boolean canFocus() {
        return true;
    }

    @Override
    public InputResult onClick(int x, int y, int button) {
        this.requestFocus();
        return InputResult.PROCESSED;
    }

    @Override
    public void onKeyPressed(int ch, int key, int modifiers) {
        if (ch == GLFW.GLFW_KEY_R) {
            yaw = 135;
            pitch = 30;
            manualZoom = -1;
            manualXMove = 0;
            manualYMove = 0;
            consecutiveZooms = 1;
        }
    }

    public byte[] getIcon() {
        Window window = MinecraftClient.getInstance().getWindow();
        SimpleFramebuffer framebuffer = new SimpleFramebuffer(512, 512, true, MinecraftClient.IS_SYSTEM_MAC);

        try (NativeImage nativeImage = new NativeImage(512, 512, false)) {
            float scale = (float) window.getScaledHeight()/this.height;
            System.out.println(scale);

            framebuffer.beginWrite(true);
            drawRegion(null, 256, 256, 512, 512, scale, false, true, false, false);

            RenderSystem.bindTexture(framebuffer.getColorAttachment());
            nativeImage.loadFromTextureImage(0, false);
            nativeImage.mirrorVertically();

            NativeImage.Format format = nativeImage.getFormat();

            int minX = nativeImage.getWidth();
            int maxX = 0;
            int minY = nativeImage.getHeight();
            int maxY = 0;

            if (format.hasAlpha()) {
                for (int y = 0; y < nativeImage.getHeight(); y++) {
                    for (int x = 0; x < nativeImage.getWidth(); x++) {
                        int colour = nativeImage.getColor(x, y);

                        int alpha = (colour >> format.getAlphaOffset()) & 0xFF;
                        if (alpha < 0x20) {
                            nativeImage.setColor(x, y, 0x00000000);
                        } else {
                            if (x < minX) minX = x;
                            if (x > maxX) maxX = x;
                            if (y < minY) minY = y;
                            if (y > maxY) maxY = y;

                            if (alpha < 0xFF) {
                                // Get colours
                                int red = (colour >> format.getRedChannelOffset()) & 0xFF;
                                int green = (colour >> format.getGreenChannelOffset()) & 0xFF;
                                int blue = (colour >> format.getBlueChannelOffset()) & 0xFF;

                                // Premultiply by alpha
                                red = red*alpha/0xFF;
                                green = green*alpha/0xFF;
                                blue = blue*alpha/0xFF;

                                // Set
                                colour = 0xFF000000;
                                colour |= red << format.getRedChannelOffset();
                                colour |= green << format.getGreenChannelOffset();
                                colour |= blue << format.getBlueChannelOffset();
                                nativeImage.setColor(x, y, colour);
                            } else {
                                nativeImage.setColor(x, y, colour | 0xFF << format.getAlphaOffset());
                            }
                        }
                    }
                }
            }

            try (NativeImage nativeImage2 = new NativeImage(128, 128, false)) {
                int width = maxX-minX;
                int height = maxY-minY;
                int x = minX;
                int y = minY;
                if (width > height) {
                    y -= (width - height)/2;
                    height = width;
                } else {
                    x -= (height - width)/2;
                    width = height;
                }

                nativeImage.resizeSubRectTo(x, y, width, height, nativeImage2);
                return nativeImage2.getBytes();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            framebuffer.delete();
            MinecraftClient.getInstance().getFramebuffer().beginWrite(true);
        }
        return null;
    }

    @Override
    public InputResult onMouseScroll(int x, int y, double amount) {
        if (manualZoom < 0) {
            if (amount > 0) {
                manualZoom = averageZoom;
                manualZoom -= amount*2;
            }
        } else {
            manualZoom -= amount*2;
            if (manualZoom < 2) manualZoom = 2;
        }
        return InputResult.PROCESSED;
    }

    public InputResult onMouseDrag(int x, int y, int button, double deltaX, double deltaY) {
        if (button == 1) {
            Window window = MinecraftClient.getInstance().getWindow();
            manualXMove += deltaX/window.getScaledHeight()*2;
            manualYMove += deltaY/window.getScaledHeight()*2;
        } else {
            yaw += deltaX;
            pitch += deltaY;
        }
        return InputResult.PROCESSED;
    }

    @Override
    public void paint(MatrixStack matrices, int x, int y, int mouseX, int mouseY) {
        PAINTER.paintBackground(matrices, x, y, this);

        // Enable scissor
        Window window = MinecraftClient.getInstance().getWindow();
        double s = window.getScaleFactor();
        RenderSystem.enableScissor((int)((x+1)*s), (int)(window.getHeight() - (y+1)*s - (height-2)*s), (int)((width-2)*s), (int)((height-2)*s));

        drawRegion(matrices, x+this.width/2, y+this.height/2, window.getScaledWidth(), window.getScaledHeight(), 1, true,true, true, true);

        // Disable scissor
        RenderSystem.disableScissor();
    }

    private void drawRegion(MatrixStack screenStack, int x, int y, int width, int height, float scale,
                            boolean adaptBounds, boolean drawBlocks, boolean drawOutline, boolean drawSize) {
        final float aspectRatio = (float)width/height;
        final float fov = 60;

        float sizeX = REGION.max.getX()+1- REGION.min.getX();
        float sizeY = REGION.max.getY()+1- REGION.min.getY();
        float sizeZ = REGION.max.getZ()+1- REGION.min.getZ();

        Matrix4f projectionMatrix = Matrix4f.viewboxMatrix(fov, aspectRatio, 0.01f, 5000);
        Matrix4f viewMatrix = new Matrix4f();

        if (adaptBounds) {
            viewMatrix.loadIdentity();
            viewMatrix.multiply(Matrix4f.translate(0, 0, -zoom));
            viewMatrix.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(pitch));
            viewMatrix.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(yaw));
            viewMatrix.multiply(Matrix4f.translate(-sizeX/2f, -sizeY/2f, -sizeZ/2f));

            float boundsX = (float)(this.width-10)/width;
            float boundsY = (float)(this.height-10)/height;
            boolean outOfBounds = false;
            out:
            for (float sX : new float[]{REGION.min.getX(), REGION.max.getX()+1}) {
                for (float sY : new float[]{REGION.min.getY(), REGION.max.getY()+1}) {
                    for (float sZ : new float[]{REGION.min.getZ(), REGION.max.getZ()+1}) {
                        Vector4f vec = new Vector4f(sX, sY, sZ, 1);
                        vec.transform(viewMatrix);
                        vec.transform(projectionMatrix);

                        if (vec.getW() < 0) {
                            outOfBounds = true;
                            break out;
                        }

                        vec.multiply(1f / vec.getW());

                        if (vec.getX() < -boundsX || vec.getX() > boundsX || vec.getY() < -boundsY || vec.getY() > boundsY) {
                            outOfBounds = true;
                            break out;
                        }
                    }
                }
            }

            if (outOfBounds) {
                zoom += 3f/consecutiveZooms;
            } else {
                zoom -= 3f/consecutiveZooms;
            }
            if (outOfBounds == wasOOBLast) {
                consecutiveZooms /= 2f;
                if (consecutiveZooms < 1) consecutiveZooms = 1;
            } else {
                consecutiveZooms++;
                if (consecutiveZooms > 100) consecutiveZooms = 100;
            }
            wasOOBLast = outOfBounds;

            averageZoom = zoom*0.2f+averageZoom*0.8f;
            if (manualZoom > averageZoom) {
                manualZoom = -1;
            }
        }

        float actualZoom = manualZoom > 0 ? manualZoom : averageZoom;

        viewMatrix.loadIdentity();
        viewMatrix.multiply(Matrix4f.translate(0, 0, -actualZoom));
        viewMatrix.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(pitch));
        viewMatrix.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(yaw));
        viewMatrix.multiply(Matrix4f.translate(-sizeX/2f, -sizeY/2f, -sizeZ/2f));

        MatrixStack modelViewStack = new MatrixStack();
        modelViewStack.push();
        modelViewStack.loadIdentity();
        modelViewStack.multiplyPositionMatrix(viewMatrix);

        float tX = manualXMove*scale/aspectRatio+(float)x/width*2-1;
        float tY = manualYMove*scale+(float)y/height*2-1;

        Matrix4f screenProjection = Matrix4f.translate(tX, -tY, -1f);
        screenProjection.multiply(Matrix4f.scale(scale, scale, 1));
        screenProjection.multiply(projectionMatrix);

        // Apply model view
        MatrixStack mcModelViewStack = RenderSystem.getModelViewStack();
        mcModelViewStack.push();
        mcModelViewStack.loadIdentity();
        RenderSystem.applyModelViewMatrix();

        // Apply projection
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(screenProjection);

        // Render block region
        if (drawBlocks) {
            Vector4f view = new Vector4f(0, 0, 0, 1);
            Matrix4f inverseViewMatrix = viewMatrix.copy();
            inverseViewMatrix.invert();
            view.transform(inverseViewMatrix);
            REGION.resort(view.getX(), view.getY(), view.getZ());

            BlockRegionRenderer.render(REGION, modelViewStack, screenProjection, 1.0f);
        }

        // Render outline
        if (drawOutline) {
            VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
            VertexConsumer vertices = immediate.getBuffer(RenderLayer.getLines());
            WorldRenderer.drawBox(modelViewStack, vertices,
                    REGION.min.getX(), REGION.min.getY(), REGION.min.getZ(),
                    REGION.max.getX()+1, REGION.max.getY()+1, REGION.max.getZ()+1,
                    1f, 1f, 1.0f, 0.5f, 0f, 0f, 0f);
            immediate.draw();
        }

        // Restore projection
        RenderSystem.restoreProjectionMatrix();

        // Restore model view
        mcModelViewStack.pop();
        RenderSystem.applyModelViewMatrix();

        if (drawSize) {
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            screenStack.push();
            screenStack.translate(0, 0, 100);
            Matrix4f transform = projectionMatrix.copy();
            transform.multiply(viewMatrix);
            drawText(textRenderer, ""+(int)sizeX, tX, tY, width, height,
                    sizeX/2, 0, 0, transform, screenStack);
            drawText(textRenderer, ""+(int)sizeY, tX, tY, width, height,
                    0, sizeY/2, 0, transform, screenStack);
            drawText(textRenderer, ""+(int)sizeZ, tX, tY, width, height,
                    0, 0, sizeZ/2, transform, screenStack);
            screenStack.pop();
        }

        modelViewStack.pop();
    }

    private void drawText(TextRenderer textRenderer, String text, float tX, float tY, int width, int height,
                          float x, float y, float z, Matrix4f transform, MatrixStack screenMat) {
        Vector4f vec = new Vector4f(x, y, z, 1);
        vec.transform(transform);
        vec.multiply(1f / vec.getW());

        textRenderer.draw(screenMat, text,
                (vec.getX()+1+tX)/2f*width-textRenderer.getWidth(text)/2f,
                (-vec.getY()+1+tY)/2f*height-4, 0xFFFFFF);
    }

}
