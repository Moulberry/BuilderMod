package codes.moulberry;

import net.java.games.input.Component;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public class ToolMenuManager {

    private static final ToolMenuManager INSTANCE = new ToolMenuManager();

    public static ToolMenuManager getInstance() {
        return INSTANCE;
    }

    private int toolSlotSelected = 0;
    private ItemStack selectedStack = new ItemStack(Item.byRawId(BuilderMod.getInstance().config.quickTools.get(0)));
    private boolean isOverridingSlot = false;
    private ItemStack oldStack = null;

    public boolean isOverriding() {
        boolean override = isOverridingSlot && MinecraftClient.getInstance().player.inventory.selectedSlot == 0 && selectedStack != null;
        if(!override) {
            isOverridingSlot = false;
        }
        return override;
    }

    public ItemStack getStack() {
        return selectedStack;
    }

    public void changeStack() {
        if(true) return;
        if(oldStack == null) oldStack = MinecraftClient.getInstance().player.inventory.main.get(0);
        MinecraftClient.getInstance().getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(36, ToolMenuManager.getInstance().getStack()));
    }

    public void resetStack() {
        if(true) return;
        if(oldStack != null) MinecraftClient.getInstance().getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(36, oldStack));
        oldStack = null;
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

            selectedStack = new ItemStack(Item.byRawId(BuilderMod.getInstance().config.quickTools.get(toolSlotSelected)));
            MinecraftClient.getInstance().getHeldItemRenderer().resetEquipProgress(Hand.MAIN_HAND);

            changeStack();

            return 0;
        }

        if(resultantSlot == 0 && direction == -1 && !isOverridingSlot) {
            isOverridingSlot = true;
            MinecraftClient.getInstance().getHeldItemRenderer().resetEquipProgress(Hand.MAIN_HAND);
            changeStack();
            return 0;
        } else if(resultantSlot == 1 && direction == -1 && isOverridingSlot) {
            isOverridingSlot = false;
            MinecraftClient.getInstance().getHeldItemRenderer().resetEquipProgress(Hand.MAIN_HAND);
            resetStack();
            return 0;
        } else if(resultantSlot == 8 && direction == 1 && isOverridingSlot) {
            isOverridingSlot = false;
            MinecraftClient.getInstance().getHeldItemRenderer().resetEquipProgress(Hand.MAIN_HAND);
            resetStack();
            return 8;
        } else if(resultantSlot == 8 && direction == 1 && !isOverridingSlot) {
            isOverridingSlot = true;
            MinecraftClient.getInstance().getHeldItemRenderer().resetEquipProgress(Hand.MAIN_HAND);
            changeStack();
            return 0;
        }
        return resultantSlot;
    }

}
