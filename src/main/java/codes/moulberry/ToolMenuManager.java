package codes.moulberry;

import net.java.games.input.Component;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public class ToolMenuManager {

    private static final ToolMenuManager INSTANCE = new ToolMenuManager();

    public static ToolMenuManager getInstance() {
        return INSTANCE;
    }

    private int toolSlotSelected = 0;
    private ItemStack selectedStack = new ItemStack(BuilderMod.getInstance().config.quickTools.get(0));
    private boolean isOverridingSlot = false;

    public boolean isOverriding() {
        return isOverridingSlot && selectedStack != null;
    }

    public ItemStack getStack() {
        return selectedStack;
    }

    public int onScroll(int direction, int resultantSlot) {
        if(MinecraftClient.getInstance().currentScreen != null) {
            return resultantSlot;
        }

        int keyCode = GLFW.GLFW_KEY_LEFT_CONTROL;
        if(isOverridingSlot && InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), keyCode)) {
            toolSlotSelected -= direction;
            int max = BuilderMod.getInstance().config.quickTools.size()-1;

            if(toolSlotSelected > max) toolSlotSelected = 0;
            if(toolSlotSelected < 0) toolSlotSelected = max;

            selectedStack = new ItemStack(BuilderMod.getInstance().config.quickTools.get(toolSlotSelected));
            MinecraftClient.getInstance().getHeldItemRenderer().resetEquipProgress(Hand.MAIN_HAND);

            return 0;
        }

        if(resultantSlot == 0 && direction == -1 && !isOverridingSlot) {
            isOverridingSlot = true;
            MinecraftClient.getInstance().getHeldItemRenderer().resetEquipProgress(Hand.MAIN_HAND);
            return 0;
        } else if(resultantSlot == 1 && direction == -1 && isOverridingSlot) {
            isOverridingSlot = false;
            MinecraftClient.getInstance().getHeldItemRenderer().resetEquipProgress(Hand.MAIN_HAND);
            return 0;
        } else if(resultantSlot == 8 && direction == 1 && isOverridingSlot) {
            isOverridingSlot = false;
            MinecraftClient.getInstance().getHeldItemRenderer().resetEquipProgress(Hand.MAIN_HAND);
            return 8;
        } else if(resultantSlot == 8 && direction == 1 && !isOverridingSlot) {
            isOverridingSlot = true;
            MinecraftClient.getInstance().getHeldItemRenderer().resetEquipProgress(Hand.MAIN_HAND);
            return 0;
        }
        return resultantSlot;
    }

}
