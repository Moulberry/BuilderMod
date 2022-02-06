package codes.moulberry.buildermod.customtool;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class CustomToolManager {

    private static final Map<Item, CustomTool> customTools = new HashMap<>();

    public static boolean acceptTool(Item item, Consumer<CustomTool> consumer) {
        CustomTool tool = customTools.get(item);
        if (tool != null) {
            consumer.accept(tool);
            return true;
        }
        return false;
    }

    static {
        customTools.put(Items.WOODEN_AXE, new WEWandTool());
        customTools.put(Items.REDSTONE_TORCH, new LaserPointerTool());
        customTools.put(Items.SPECTRAL_ARROW, new SmoothTool());
        customTools.put(Items.FLINT, new RemoveTool());

    }


}
