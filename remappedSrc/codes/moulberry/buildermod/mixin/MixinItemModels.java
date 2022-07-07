package codes.moulberry.buildermod.mixin;

import net.minecraft.client.render.item.ItemModels;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ;
import java.util.Arrays;

@Mixin(ItemModels.class)
public class MixinItemModels {

    @Shadow @Final private BakedModelManager modelManager;

    @Inject(method = "getModel(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/client/render/model/BakedModel;", at = @At("HEAD"), cancellable = true)
    public void onModelGet(ItemStack stack, CallbackInfoReturnable<BakedModel> cir) {
        String id = !stack.hasNbt() || !stack.getNbt().contains("CustomPlacerBlockState") ? null : stack.getNbt().getString("CustomPlacerBlockState");
        if (id != null) {
            var split = id.replace("[", "#").replace("]", "").split("#");
            String block = split[0];
            String[] states = split[1].split(",");
            Arrays.sort(states);
            id = block + "#" + String.join(",", states);
            var model = this.modelManager.getModel(new ModelIdentifier(id));
            if (model != this.modelManager.getMissingModel()) {
                cir.setReturnValue(model);
            }
        }
    }
}
