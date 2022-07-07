package codes.moulberry.buildermod.customtool;

import codes.moulberry.buildermod.Identifiers;
import codes.moulberry.buildermod.blueprint.Blueprint;
import codes.moulberry.buildermod.blueprint.BlueprintLibrary;
import codes.moulberry.buildermod.gui.blueprints.BlueprintLibraryMenu;
import codes.moulberry.buildermod.render.regions.BlockRegion;
import codes.moulberry.buildermod.render.regions.BlockRegionRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ShortMap;
import it.unimi.dsi.fastutil.ints.Int2ShortRBTreeMap;
import it.unimi.dsi.fastutil.shorts.Short2IntMap;
import it.unimi.dsi.fastutil.shorts.Short2IntRBTreeMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class TestTool implements CustomTool  {

    private static final BlockRegion TOOL_REGION = new BlockRegion();
    public static boolean dirty = false;

    public Vec3d axisTarget = null;
    public Vec3d lookingAxisTarget = null;

    public boolean isPlacing = false;
    public BlockPos targetPosition = null;
    public float targetDistance = -1;

    @Override
    public void onSelect() {
        targetPosition = null;
        targetDistance = -1;
        isPlacing = false;
        dirty = true;
    }

    private static float distanceDegrees(Vec2f v, Vec2f w) {
        float dx = MathHelper.wrapDegrees(v.x - w.x);
        float dy = MathHelper.wrapDegrees(v.y - w.y);

        return (float) Math.sqrt(dx*dx + dy*dy);
    }

    // https://stackoverflow.com/questions/849211/shortest-distance-between-a-point-and-a-line-segment
    private static float minimumDistanceDegrees(Vec2f v, Vec2f w, Vec2f p) {
        // Return minimum distance between line segment vw and point p
        return distanceDegrees(p, getProjectionDegrees(v, w, p, true));
    }

    private static Vec2f getProjectionDegrees(Vec2f v, Vec2f w, Vec2f p, boolean clamp) {
        float dx = MathHelper.wrapDegrees(v.x - w.x);
        float dy = MathHelper.wrapDegrees(v.y - w.y);

        float l2 = dx*dx + dy*dy;  // i.e. |w-v|^2 -  avoid a sqrt
        if (l2 == 0.0) return v;   // v == w case
        // Consider the line extending the segment, parameterized as v + t (w - v).
        // We find projection of point p onto the line.
        // It falls where t = [(p-v) . (w-v)] / |w-v|^2
        // We clamp t from [0,1] to handle points outside the segment vw.
        float t = (MathHelper.wrapDegrees(p.x - v.x) * MathHelper.wrapDegrees(w.x - v.x) + MathHelper.wrapDegrees(p.y - v.y) * MathHelper.wrapDegrees(w.y - v.y)) / l2;
        if (clamp) t = Math.max(0, Math.min(1, t));
        return new Vec2f(MathHelper.wrapDegrees(v.x - t*dx), MathHelper.wrapDegrees(v.y - t*dy));  // Projection falls on the segment
    }

    private Vec3d getIntersection(double fDst1, double fDst2, Vec3d p1, Vec3d v1) {
        if ( (fDst1 * fDst2) >= 0.0f) return null;
        if ( fDst1 == fDst2) return null;
        return p1.add(v1.multiply(-fDst1/(fDst2-fDst1)));
    }

    private boolean inBox(Vec3d hit, Vec3d b1, Vec3d b2, int axis) {
        if ( axis==1 && hit.z > b1.z && hit.z < b2.z && hit.y > b1.y && hit.y < b2.y) return true;
        if ( axis==2 && hit.z > b1.z && hit.z < b2.z && hit.x > b1.x && hit.x < b2.x) return true;
        return axis == 3 && hit.x > b1.x && hit.x < b2.x && hit.y > b1.y && hit.y < b2.y;
    }

    // returns true if line (L1, L2) intersects with the box (b1, b2)
    private Vec3d checkLineBox(Vec3d b1, Vec3d b2, Vec3d L1, Vec3d L2) {
        if (L2.x < b1.x && L1.x < b1.x) return null;
        if (L2.x > b2.x && L1.x > b2.x) return null;
        if (L2.y < b1.y && L1.y < b1.y) return null;
        if (L2.y > b2.y && L1.y > b2.y) return null;
        if (L2.z < b1.z && L1.z < b1.z) return null;
        if (L2.z > b2.z && L1.z > b2.z) return null;
        if (L1.x > b1.x && L1.x < b2.x &&
                L1.y > b1.y && L1.y < b2.y &&
                L1.z > b1.z && L1.z < b2.z) {
            return L1;
        }
        Vec3d v1 = new Vec3d(L2.x - L1.x, L2.y - L1.y, L2.z - L1.z);
        Vec3d hit;
        if (((hit = getIntersection( L1.x-b1.x, L2.x-b1.x, L1, v1) ) != null && inBox(hit, b1, b2, 1))
                || ((hit = getIntersection( L1.y-b1.y, L2.y-b1.y, L1, v1)) != null && inBox(hit, b1, b2, 2))
                || ((hit = getIntersection( L1.z-b1.z, L2.z-b1.z, L1, v1)) != null && inBox(hit, b1, b2, 3))
                || ((hit = getIntersection( L1.x-b2.x, L2.x-b2.x, L1, v1)) != null && inBox(hit, b1, b2, 1))
                || ((hit = getIntersection( L1.y-b2.y, L2.y-b2.y, L1, v1)) != null && inBox(hit, b1, b2, 2))
                || ((hit = getIntersection( L1.z-b2.z, L2.z-b2.z, L1, v1)) != null && inBox(hit, b1, b2, 3))) {
            return hit;
        }
        return null;
    }

    private static Vec3d getIntersection(Vec3d p1, Vec3d v1, Vec3d p2, Vec3d v2) {
        v1 = v1.normalize();
        v2 = v2.normalize();

        final double cos = v1.dotProduct(v2);
        final double n = 1 - cos * cos;
        if (n < 1E-10) {
            // the lines are parallel
            return null;
        }

        double x1F = -p1.dotProduct(v1);
        p1 = new Vec3d(p1.x + -x1F*v1.x, p1.y + -x1F*v1.y, p1.z + -x1F*v1.z);
        double x2F = -p2.dotProduct(v2);
        p2 = new Vec3d(p2.x + x2F*v2.x, p2.y + x2F*v2.y, p2.z + x2F*v2.z);

        final Vec3d delta0 = new Vec3d(p2.x-p1.x, p2.y-p1.y, p2.z-p1.z); // todo: zero?
        final double a        = delta0.dotProduct(v1);
        final double b        = delta0.dotProduct(v2);

        final double f = (a - b * cos) / n;
        return new Vec3d(p1.x + f*v1.x, p1.y + f*v1.y, p1.z + f*v1.z);
    }

    private static Vec2f getLook(PlayerEntity player, Vec3d target) {
        Vec3d vec3d = EntityAnchorArgumentType.EntityAnchor.EYES.positionAt(player);
        double d = target.getX() - vec3d.x;
        double e = target.getY()  - vec3d.y;
        double f = target.getZ()  - vec3d.z;

        double g = Math.sqrt(d * d + f * f);
        float pitch = MathHelper.wrapDegrees((float)(-(MathHelper.atan2(e, g) * 57.2957763671875)));
        float yaw = MathHelper.wrapDegrees((float)(MathHelper.atan2(f, d) * 57.2957763671875) - 90.0f);

        return new Vec2f(yaw, pitch);
    }

    private Vec3d calculateAxisTarget() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        Vec2f playerLook = new Vec2f(MathHelper.wrapDegrees(player.getHeadYaw()), MathHelper.wrapDegrees(player.getPitch()));
        Vec3d targetPositionD = Vec3d.ofCenter(targetPosition);
        Vec2f root = getLook(player, targetPositionD);

        float distanceMultiplier = (float)Math.sqrt(player.squaredDistanceTo(targetPositionD))/20f;
        if (distanceMultiplier < 0.5f) distanceMultiplier = 0.5f;
        final float axisLen = 3*distanceMultiplier;
        final float boxSize = 0.3f*distanceMultiplier;
        final float threshold = 2.5f;

        Vec3d b1 = targetPositionD.add(-boxSize, -boxSize, -boxSize);
        Vec3d b2 = targetPositionD.add(boxSize, boxSize, boxSize);
        Vec3d l1 = EntityAnchorArgumentType.EntityAnchor.EYES.positionAt(player);
        Vec3d l2 = l1.add(getLookVector(playerLook).multiply(100));

        if (checkLineBox(b1, b2, l1, l2) != null) {
            return Vec3d.ZERO;
        } else {
            Vec2f east = getLook(player, targetPositionD.add(axisLen*1.3f, 0, 0));
            Vec2f up = getLook(player, targetPositionD.add(0, axisLen*1.3f, 0));
            Vec2f south = getLook(player, targetPositionD.add(0, 0, axisLen*1.3f));

            float xDistance = minimumDistanceDegrees(root, east, playerLook);
            float yDistance = minimumDistanceDegrees(root, up, playerLook);
            float zDistance = minimumDistanceDegrees(root, south, playerLook);

            Vec3d targetDir = null;
            Vec2f projectedAngles = null;
            if (xDistance < threshold && xDistance < yDistance && xDistance < zDistance) {
                targetDir = new Vec3d(1, 0, 0);
                projectedAngles = getProjectionDegrees(root, east, playerLook, false);
            } else if (yDistance < threshold && yDistance < zDistance) {
                targetDir = new Vec3d(0, 1, 0);
                projectedAngles = getProjectionDegrees(root, up, playerLook, false);
            } else if (zDistance < threshold) {
                targetDir = new Vec3d(0, 0, 1);
                projectedAngles = getProjectionDegrees(root, south, playerLook, false);
            }

            if (targetDir != null) {
                Vec3d vec2 = player.getEyePos();
                Vec3d vec2Dir = getLookVector(projectedAngles);

                Vec3d axisTarget = getIntersection(targetPositionD, targetDir, vec2, vec2Dir);
                if (axisTarget != null) {
                    axisTarget = axisTarget.subtract(targetPositionD);
                    return axisTarget;
                }
            }
        }
        return null;
    }

    @Override
    public void leftClick() {
        if (targetPosition != null && MinecraftClient.getInstance().options.useKey.isPressed()) {
            lookingAxisTarget = axisTarget = calculateAxisTarget();
        } else {
            MinecraftClient.getInstance().setScreen(BlueprintLibraryMenu.createScreen());
        }
    }

    private static Vec3d getLookVector(Vec2f look) {
        float f = look.y * ((float)Math.PI / 180);
        float g = -look.x * ((float)Math.PI / 180);
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d(i * j, -k, h * j);
    }

    private static Vec3d getPlayerLook(ClientPlayerEntity player) {
        return getLookVector(new Vec2f(player.getHeadYaw(), player.getPitch()));
    }

    private void raycastTarget(Consumer<BlockPos> consumer) {
        CustomTool.raycastBlock(blockHitResult -> {
            BlockPos pos = blockHitResult.getBlockPos().offset(blockHitResult.getSide());
            consumer.accept(pos);
        });
    }

    @Override
    public void render(MatrixStack matrices, float tickDelta, Matrix4f projection) {
        if (MinecraftClient.getInstance().currentScreen != null) {
            targetPosition = null;
            targetDistance = -1;
            isPlacing = false;
            return;
        }
        ClientPlayerEntity player = Objects.requireNonNull(MinecraftClient.getInstance().player);

        Blueprint blueprint = BlueprintLibrary.selected;
        if (blueprint == null) return;

        if (dirty) {
            dirty = false;

            TOOL_REGION.clear();
            for (int x = 0; x<blueprint.sizeX; x++) {
                for (int y = 0; y<blueprint.sizeY; y++) {
                    for (int z = 0; z<blueprint.sizeZ; z++) {
                        BlockState state = blueprint.blockStates[x][y][z];
                        if (state != null) {
                            TOOL_REGION.addBlock(x-blueprint.pivotX, y-blueprint.pivotY, z-blueprint.pivotZ, state);
                        }
                    }
                }
            }
        }

        if (!MinecraftClient.getInstance().options.attackKey.isPressed()) {
            axisTarget = null;
        }

        if (targetPosition != null && MinecraftClient.getInstance().options.useKey.isPressed()) {
            isPlacing = true;

            if (axisTarget != null) {
                Vec3d target = Vec3d.ofCenter(targetPosition);
                Vec3d playerLookDir = getPlayerLook(player);
                if (axisTarget == Vec3d.ZERO) {
                    if (targetDistance < 0) {
                        targetDistance = (float)Math.sqrt(player.squaredDistanceTo(target.add(0, -player.getStandingEyeHeight(), 0)));
                    } else {
                        Vec3d offset = playerLookDir.multiply(targetDistance);
                        targetPosition = new BlockPos(player.getEyePos().add(offset));
                    }
                } else {
                    Vec3d targetDir = axisTarget;
                    Vec3d vec2 = player.getEyePos();

                    Vec3d intersect = getIntersection(target, targetDir, vec2, playerLookDir);
                    if (intersect != null) {
                        intersect = intersect.subtract(axisTarget);
                        targetPosition = new BlockPos(intersect);
                    }
                    targetDistance = -1;
                }
            } else {
                lookingAxisTarget = calculateAxisTarget();
                targetDistance = -1;
            }
        } else {
            targetDistance = -1;

            if (isPlacing && targetPosition != null) {
                isPlacing = false;

                PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());

                Short2IntMap stateIndexmap = new Short2IntRBTreeMap();
                ShortList stateList = new ShortArrayList();
                int totalStates = 0;
                int totalBlocks = 0;

                for (int x = 0; x < blueprint.sizeX; x++) {
                    for (int y = 0; y < blueprint.sizeY; y++) {
                        for (int z = 0; z < blueprint.sizeZ; z++) {
                            BlockState state = blueprint.blockStates[x][y][z];
                            if (state != null) {
                                totalBlocks++;
                                short stateId = (short) Block.getRawIdFromState(state);

                                if (!stateIndexmap.containsKey(stateId)) {
                                    stateIndexmap.put(stateId, totalStates++);
                                    stateList.add(stateId);
                                }
                            }
                        }
                    }
                }

                BlockPos playerBlockPos = MinecraftClient.getInstance().player.getBlockPos();
                buf.writeVarInt(totalBlocks);
                buf.writeBlockPos(playerBlockPos);

                buf.writeVarInt(totalStates);
                for (short stateId : stateList) {
                    buf.writeShort(stateId);
                }

                for (int x = 0; x<blueprint.sizeX; x++) {
                    for (int y = 0; y<blueprint.sizeY; y++) {
                        for (int z = 0; z<blueprint.sizeZ; z++) {
                            BlockState state = blueprint.blockStates[x][y][z];
                            if (state != null) {
                                int bx = targetPosition.getX()+x-blueprint.pivotX;
                                int by = targetPosition.getY()+y-blueprint.pivotY;
                                int bz = targetPosition.getZ()+z-blueprint.pivotZ;

                                int encoded = 0;
                                encoded |= ((bx-playerBlockPos.getX()+1024) & 2047) << 11;
                                encoded |= ((by-playerBlockPos.getY()+512)  & 1023) << 22;
                                encoded |= ((bz-playerBlockPos.getZ()+1024) & 2047);
                                buf.writeInt(encoded);
                                buf.writeVarInt(stateIndexmap.get((short)Block.getRawIdFromState(state)));
                            }
                        }
                    }
                }

                ClientPlayNetworking.send(Identifiers.SETBLOCK_MULTI, buf);
            }

            targetPosition = null;
            raycastTarget(pos -> targetPosition = pos);
        }

        if (targetPosition != null) {
            TOOL_REGION.centerPos.set(targetPosition);

            // Blocks
            BlockRegionRenderer.render(TOOL_REGION, matrices, projection, 1.0f);

            // Bounding box
            matrices.push();
            matrices.translate(TOOL_REGION.centerPos.getX(), TOOL_REGION.centerPos.getY(), TOOL_REGION.centerPos.getZ());
            VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
            VertexConsumer vertices = immediate.getBuffer(RenderLayer.getLines());

            WorldRenderer.drawBox(matrices, vertices,
                    TOOL_REGION.min.getX(), TOOL_REGION.min.getY(), TOOL_REGION.min.getZ(),
                    TOOL_REGION.max.getX()+1, TOOL_REGION.max.getY()+1, TOOL_REGION.max.getZ()+1,
                    1f, 1f, 1.0f, 0.5f, 0f, 0f, 0f);

            matrices.translate(0.5f, 0.5f, 0.5f);

            immediate.draw();

            if (isPlacing) {
                RenderSystem.disableDepthTest();
                RenderSystem.enablePolygonOffset();
                RenderSystem.polygonOffset(-999999, -999999);

                float distanceMultiplier = (float)Math.sqrt(player.squaredDistanceTo(Vec3d.ofCenter(targetPosition)))/20f;
                if (distanceMultiplier < 0.5f) distanceMultiplier = 0.5f;
                float axisLen = 3*distanceMultiplier;
                float boxSize = 0.3f*distanceMultiplier;

                float colourX = (lookingAxisTarget != null && lookingAxisTarget.getX() > 1E-10) ? 0.5f : 0;
                float colourY = (lookingAxisTarget != null && lookingAxisTarget.getY() > 1E-10) ? 0.5f : 0;
                float colourZ = (lookingAxisTarget != null && lookingAxisTarget.getZ() > 1E-10) ? 0.5f : 0;

                vertices = immediate.getBuffer(RenderLayer.getLines()); // Restart drawing
                Matrix4f matrix4f = matrices.peek().getPositionMatrix();
                Matrix3f matrix3f = matrices.peek().getNormalMatrix();
                vertices.vertex(matrix4f, boxSize, 0, 0).color(1, colourX, colourX, 0.75f+colourX/2).normal(matrix3f, 1.0f, 0.0f, 0.0f).next();
                vertices.vertex(matrix4f, axisLen, 0, 0).color(1, colourX, colourX, 0.75f+colourX/2).normal(matrix3f, 1.0f, 0.0f, 0.0f).next();
                vertices.vertex(matrix4f, 0, boxSize, 0).color(colourY, 1, colourY, 0.75f+colourY/2).normal(matrix3f, 0.0f, 1.0f, 0.0f).next();
                vertices.vertex(matrix4f, 0, axisLen, 0).color(colourY, 1, colourY, 0.75f+colourY/2).normal(matrix3f, 0.0f, 1.0f, 0.0f).next();
                vertices.vertex(matrix4f, 0, 0, boxSize).color(colourZ, colourZ, 1, 0.75f+colourZ/2).normal(matrix3f, 0.0f, 0.0f, 1.0f).next();
                vertices.vertex(matrix4f, 0, 0, axisLen).color(colourZ, colourZ, 1, 0.75f+colourZ/2).normal(matrix3f, 0.0f, 0.0f, 1.0f).next();
                immediate.draw();

                RenderSystem.disablePolygonOffset();
                RenderSystem.disableDepthTest();
                RenderSystem.enableCull();
                RenderSystem.enableBlend();

                BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();

                RenderSystem.setShader(GameRenderer::getPositionColorShader);

                float coneLen = axisLen/5f;
                float coneRadius = coneLen/3f;

                // Axis cones
                drawCone(bufferBuilder, matrix4f, new Vec3d(axisLen, 0, 0), new Vec3d(axisLen+coneLen, 0, 0),
                        0, coneRadius, 1, colourX, colourX);
                drawCone(bufferBuilder, matrix4f, new Vec3d(0, axisLen, 0), new Vec3d(0, axisLen+coneLen, 0),
                        1, coneRadius, colourY, 1, colourY);
                drawCone(bufferBuilder, matrix4f, new Vec3d(0, 0, axisLen), new Vec3d(0, 0, axisLen+coneLen),
                        2, coneRadius, colourZ, colourZ, 1);

                drawBox(bufferBuilder, matrix4f, boxSize, lookingAxisTarget == Vec3d.ZERO ? 0xFFFFFFFF : 0x80FFFFFF);

                RenderSystem.enableDepthTest();
            }

            matrices.pop();
        }
    }

    private void drawCone(BufferBuilder bufferBuilder, Matrix4f matrix4f, Vec3d base, Vec3d tip, int dir,
                          float radius, float red, float green, float blue) {
        drawFan(bufferBuilder, matrix4f, base, tip, dir, false, radius, red, green, blue);
        drawFan(bufferBuilder, matrix4f, base, base, dir, true, radius, red, green, blue);
    }

    private void drawFan(BufferBuilder bufferBuilder, Matrix4f matrix4f, Vec3d base, Vec3d tip, int dir, boolean invert,
                         float radius, float red, float green, float blue) {
        bufferBuilder.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix4f, (float)tip.x, (float)tip.y, (float)tip.z).color(red, green, blue, 1).next();
        for (int i=0; i<=90; i++) {
            double angle = (invert ? -1 : 1) * Math.toRadians(i * 4);
            float x = (float)Math.sin(angle)*radius;
            float z = (float)Math.cos(angle)*radius;
            if (dir == 0) {
                bufferBuilder.vertex(matrix4f, (float)base.x, (float)base.y+z, (float)base.z+x).color(red, green, blue, 1).next();
            } else if (dir == 1) {
                bufferBuilder.vertex(matrix4f, (float)base.x+x, (float)base.y, (float)base.z+z).color(red, green, blue, 1).next();
            } else {
                bufferBuilder.vertex(matrix4f, (float)base.x+z, (float)base.y+x, (float)base.z).color(red, green, blue, 1).next();
            }
        }
        bufferBuilder.end();
        BufferRenderer.draw(bufferBuilder);
    }

    private void drawBox(BufferBuilder bufferBuilder, Matrix4f matrix4f, float boxSize, int boxColour) {
        final float alpha = ((boxColour >> 24) & 0xFF)/255f;
        final float red = ((boxColour >> 16) & 0xFF)/255f;
        final float green = ((boxColour >> 8) & 0xFF)/255f;
        final float blue = (boxColour & 0xFF)/255f;

        final float XF = 0.8f;
        final float YF = 1f;
        final float ZF = 0.9f;

        bufferBuilder.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        //-X
        bufferBuilder.vertex(matrix4f, -boxSize, -boxSize, -boxSize).color(red*XF, green*XF, blue*XF, alpha).next();
        bufferBuilder.vertex(matrix4f, -boxSize, -boxSize, -boxSize).color(red*XF, green*XF, blue*XF, alpha).next();
        bufferBuilder.vertex(matrix4f, -boxSize, -boxSize, -boxSize).color(red*XF, green*XF, blue*XF, alpha).next();
        bufferBuilder.vertex(matrix4f, -boxSize, -boxSize, boxSize).color(red*XF, green*XF, blue*XF, alpha).next();
        bufferBuilder.vertex(matrix4f, -boxSize, boxSize, -boxSize).color(red*XF, green*XF, blue*XF, alpha).next();
        bufferBuilder.vertex(matrix4f, -boxSize, boxSize, boxSize).color(red*XF, green*XF, blue*XF, alpha).next();

        //+Z
        bufferBuilder.vertex(matrix4f, -boxSize, boxSize, boxSize).color(red*ZF, green*ZF, blue*ZF, alpha).next();
        bufferBuilder.vertex(matrix4f, -boxSize, -boxSize, boxSize).color(red*ZF, green*ZF, blue*ZF, alpha).next();
        bufferBuilder.vertex(matrix4f, boxSize, boxSize, boxSize).color(red*ZF, green*ZF, blue*ZF, alpha).next();
        bufferBuilder.vertex(matrix4f, boxSize, -boxSize, boxSize).color(red*ZF, green*ZF, blue*ZF, alpha).next();

        //+X
        bufferBuilder.vertex(matrix4f, boxSize, -boxSize, boxSize).color(red*XF, green*XF, blue*XF, alpha).next();
        bufferBuilder.vertex(matrix4f, boxSize, -boxSize, -boxSize).color(red*XF, green*XF, blue*XF, alpha).next();
        bufferBuilder.vertex(matrix4f, boxSize, boxSize, boxSize).color(red*XF, green*XF, blue*XF, alpha).next();
        bufferBuilder.vertex(matrix4f, boxSize, boxSize, -boxSize).color(red*XF, green*XF, blue*XF, alpha).next();

        //-Z
        bufferBuilder.vertex(matrix4f, boxSize, boxSize, -boxSize).color(red*ZF, green*ZF, blue*ZF, alpha).next();
        bufferBuilder.vertex(matrix4f, boxSize, -boxSize, -boxSize).color(red*ZF, green*ZF, blue*ZF, alpha).next();
        bufferBuilder.vertex(matrix4f, -boxSize, boxSize, -boxSize).color(red*ZF, green*ZF, blue*ZF, alpha).next();
        bufferBuilder.vertex(matrix4f, -boxSize, -boxSize, -boxSize).color(red*ZF, green*ZF, blue*ZF, alpha).next();

        //-Y
        bufferBuilder.vertex(matrix4f, -boxSize, -boxSize, -boxSize).color(red*YF, green*YF, blue*YF, alpha).next();
        bufferBuilder.vertex(matrix4f, boxSize, -boxSize, -boxSize).color(red*YF, green*YF, blue*YF, alpha).next();
        bufferBuilder.vertex(matrix4f, -boxSize, -boxSize, boxSize).color(red*YF, green*YF, blue*YF, alpha).next();
        bufferBuilder.vertex(matrix4f, boxSize, -boxSize, boxSize).color(red*YF, green*YF, blue*YF, alpha).next();

        //+Y
        bufferBuilder.vertex(matrix4f, boxSize, -boxSize, boxSize).color(red*YF, green*YF, blue*YF, alpha).next();
        bufferBuilder.vertex(matrix4f, -boxSize, boxSize, -boxSize).color(red*YF, green*YF, blue*YF, alpha).next();
        bufferBuilder.vertex(matrix4f, -boxSize, boxSize, -boxSize).color(red*YF, green*YF, blue*YF, alpha).next();
        bufferBuilder.vertex(matrix4f, -boxSize, boxSize, boxSize).color(red*YF, green*YF, blue*YF, alpha).next();
        bufferBuilder.vertex(matrix4f, boxSize, boxSize, -boxSize).color(red*YF, green*YF, blue*YF, alpha).next();
        bufferBuilder.vertex(matrix4f, boxSize, boxSize, boxSize).color(red*YF, green*YF, blue*YF, alpha).next();
        bufferBuilder.vertex(matrix4f, boxSize, boxSize, boxSize).color(red*YF, green*YF, blue*YF, alpha).next();
        bufferBuilder.vertex(matrix4f, boxSize, boxSize, boxSize).color(red*YF, green*YF, blue*YF, alpha).next();

        bufferBuilder.end();
        BufferRenderer.draw(bufferBuilder);
    }

}
