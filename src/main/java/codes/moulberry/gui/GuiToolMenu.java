package codes.moulberry.gui;

import codes.moulberry.BuilderMod;
import codes.moulberry.config.BMConfig;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class GuiToolMenu extends Screen {
    private static final Identifier TEXTURE = new Identifier("textures/gui/container/generic_54.png");

    protected int backgroundWidth = 176;
    protected int backgroundHeight = 166;
    protected int rows = 1;

    private int x;
    private int y;

    private PlayerInventory playerInventory;
    private ItemStack focusedStack = null;
    private boolean focusedTools = false;

    private List<ItemStack> toolStacks = new ArrayList<>();

    public GuiToolMenu(PlayerInventory playerInventory) {
        super(Text.of("Tool Menu"));
        this.playerInventory = playerInventory;

        for(int itemI : BuilderMod.getInstance().config.quickTools) {
            toolStacks.add(new ItemStack(Item.byRawId(itemI)));
        }
    }

    @Override
    public void removed() {
        BMConfig config = BuilderMod.getInstance().config;
        config.quickTools.clear();
        for(ItemStack stack : toolStacks) {
            config.quickTools.add(Item.getRawId(stack.getItem()));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(this.focusedStack != null) {
            for(int i=0; i<toolStacks.size(); i++) {
                ItemStack stack = toolStacks.get(i);
                if(stack != null && stack.getItem() == this.focusedStack.getItem()) {
                    if(focusedTools) {
                        toolStacks.remove(i);
                    }
                    return true;
                }
            }

            ItemStack addStack = new ItemStack(this.focusedStack.getItem());
            toolStacks.add(addStack);
        }

        return true;
    }

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        this.x = (this.width - this.backgroundWidth) / 2;
        this.y = (this.height - this.backgroundHeight) / 2;

        drawBackground(matrices, delta, mouseX, mouseY);
        drawForeground(matrices, delta, mouseX, mouseY);

        super.render(matrices, mouseX, mouseY, delta);

        this.drawMouseoverTooltip(matrices, mouseX, mouseY);
    }

    private void drawMouseoverTooltip(MatrixStack matrices, int x, int y) {
        if (this.focusedStack != null) {
            this.renderTooltip(matrices, this.focusedStack, x, y);
        }
    }

    private void drawForeground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        focusedStack = null;

        for(int i=0; i<toolStacks.size(); i++) {
            ItemStack stack = toolStacks.get(i);

            if(stack != null) {
                int x = this.x + 8 + 18 * (i % 9);
                int y = this.y + 18 + 18 * (i / 9);

                if(mouseX >= x && mouseX < x+18 && mouseY >= y && mouseY < y+18) {
                    focusedStack = stack;
                    focusedTools = true;
                }

                drawItem(stack, x, y);
            }
        }

        for(int i=0; i<playerInventory.main.size(); i++) {
            ItemStack stack = playerInventory.main.get(i);

            if(stack.isEmpty()) continue;

            int x = this.x + 8 + 18 * (i % 9);
            int y;
            if(i < 9) {
                y = this.y + 107;
            } else {
                y = this.y + 31 + 18 * (i / 9);
            }
            if(mouseX >= x && mouseX < x+18 && mouseY >= y && mouseY < y+18) {
                focusedStack = stack;
                focusedTools = false;
            }

            drawItem(stack, x, y);
        }
    }

    private void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        this.drawTexture(matrices, this.x, this.y, 0, 0, this.backgroundWidth, this.rows * 18 + 17);
        this.drawTexture(matrices, this.x, this.y + this.rows * 18 + 17, 0, 126, this.backgroundWidth, 96);
    }

    private void drawItem(ItemStack stack, int xPosition, int yPosition) {
        this.setZOffset(200);
        this.itemRenderer.zOffset = 200.0F;
        this.itemRenderer.renderInGuiWithOverrides(stack, xPosition, yPosition);
        this.itemRenderer.renderGuiItemOverlay(this.textRenderer, stack, xPosition, yPosition);
        this.setZOffset(0);
        this.itemRenderer.zOffset = 0.0F;
    }



}
