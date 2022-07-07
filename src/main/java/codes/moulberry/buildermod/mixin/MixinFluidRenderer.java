package codes.moulberry.buildermod.mixin;

import codes.moulberry.buildermod.render.regions.BlockRegionRenderer;
import net.minecraft.client.render.block.FluidRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(FluidRenderer.class)
public class MixinFluidRenderer {

    @ModifyConstant(method = "render", constant = @Constant(intValue = 0xF))
    public int modifyMask(int original) {
        return BlockRegionRenderer.rendering ? 0xFFFFFFFF : original;
    }

}
