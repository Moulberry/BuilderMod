package codes.moulberry.buildermod.customtool;

import codes.moulberry.buildermod.blueprint.Blueprint;
import codes.moulberry.buildermod.blueprint.ProtoBlueprint;
import codes.moulberry.buildermod.gui.blueprints.BlueprintCreateMenu;
import codes.moulberry.buildermod.render.regions.BooleanRegion;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;

public class BluePrintTool extends GenericTool {

    @Override
    protected void apply(BooleanRegion region) {
        ProtoBlueprint proto = ProtoBlueprint.createFromWorld(MinecraftClient.getInstance().world, region);
        MinecraftClient.getInstance().setScreen(BlueprintCreateMenu.createScreen(proto));
    }

    @Override
    protected int toolRadius() {
        return 5;
    }

    @Override
    protected boolean isSelectable(BlockState blockState) {
        return !blockState.isAir();
    }

}
