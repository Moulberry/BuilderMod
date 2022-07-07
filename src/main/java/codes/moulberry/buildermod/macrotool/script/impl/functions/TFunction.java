package codes.moulberry.buildermod.macrotool.script.impl.functions;

import codes.moulberry.buildermod.macrotool.script.ToolExecutionContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;

public abstract class TFunction {

    public void render(ToolExecutionContext context, MatrixStack matrices,
                       Matrix4f projection, float tickDelta) {}
    public void rightClick(ToolExecutionContext context) {}

}
