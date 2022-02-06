package codes.moulberry.buildermod.customtool;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Matrix4f;

import java.util.function.Consumer;

public interface CustomTool {

    static void raycastBlock(Consumer<BlockHitResult> consumer) {
        Entity entity = MinecraftClient.getInstance().getCameraEntity();
        HitResult result = entity.raycast(100, 0, !entity.isInLava() && !entity.isSubmergedInWater());

        if (result.getType() == HitResult.Type.BLOCK) {
            consumer.accept((BlockHitResult) result);
        }
    }

    default void onSelect() {}
    default void leftClick() {}
    default void rightClick() {}
    default void render(MatrixStack matrices, float tickDelta, Matrix4f projection) {}

}
