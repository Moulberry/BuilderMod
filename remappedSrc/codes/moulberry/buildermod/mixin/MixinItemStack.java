package codes.moulberry.buildermod.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.List;

@Mixin(ItemStack.class)
public class MixinItemStack {

    @Shadow private @Nullable NbtCompound nbt;

    @Inject(method = "getTooltip", at = @At("RETURN"), cancellable = true)
    public void onTooltip(@Nullable PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> cir) {
        if (context.isAdvanced() && Screen.hasControlDown() && this.nbt != null) {
            var returnValue = cir.getReturnValue();
            Arrays.stream(NbtHelper.toNbtProviderString(this.nbt).split("\n"))
                    .map(LiteralText::new).forEachOrdered(returnValue::add);
            cir.setReturnValue(returnValue);
        }
    }
}
