package codes.moulberry.buildermod.gui.widgets.textbox;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.cottonmc.cotton.gui.client.ScreenDrawing;
import io.github.cottonmc.cotton.gui.widget.WWidget;
import io.github.cottonmc.cotton.gui.widget.data.InputResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.glfw.GLFW;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

public class WTextBox extends WWidget {

    protected static final int TEXT_COLOR = 0xE0E0E0;
    protected static final int BACKGROUND_COLOR = 0xFF000000;
    protected static final int BORDER_COLOR_SELECTED = 0xFFFFFFA0;
    protected static final int BORDER_COLOR_UNSELECTED = 0xFFA0A0A0;
    protected static final int CURSOR_COLOR = 0xFFD0D0D0;

    protected static final int TEXT_PADDING_X = 4;
    protected static final int TEXT_PADDING_Y = 6;
    protected static final int CURSOR_PADDING_Y = 4;

    private int tickCount = 0;

    private int clickCount = 1;
    private long lastClick = 0;

    protected TextRenderer font;

    public WTextBox() {
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        textComponent.setLineWrap(true);
        textComponent.setCaret(caret);
        textComponent.setFont(DummyFont.INSTANCE);
        try { textComponent.getUI().paint(null, textComponent); } catch(Exception e) {}
    }

    public boolean canFocus() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        this.tickCount++;
    }

    public String getText() {
        return textComponent.getText();
    }

    // =============================================================================================
    // region [Render]
    // =============================================================================================

    protected void renderBox(MatrixStack matrices, int x, int y,
                             int borderColor, int background) {
        ScreenDrawing.coloredRect(matrices, x - 1, y - 1, width + 2, height + 2, borderColor);
        ScreenDrawing.coloredRect(matrices, x, y, width, height, background);
    }

    protected void renderText(MatrixStack matrices, int x, int y) {
        for (String line : this.textComponent.getText().split("\n")) {

            while (!line.isEmpty()) {
                String visible = font.trimToWidth(line, this.width - 2 * TEXT_PADDING_X);
                this.font.drawWithShadow(matrices, visible, x + TEXT_PADDING_X, y + TEXT_PADDING_Y, TEXT_COLOR);
                y += this.font.fontHeight;

                line = line.substring(visible.length());
            }
        }
    }

    protected void renderSelection(MatrixStack matrices, int x, int y) {
        if (caret.getDot() == caret.getMark()) return;

        int left = caret.getDot();
        int right = caret.getMark();
        if (right < left) {
            left = caret.getMark();
            right = caret.getDot();
        }

        try {
            Rectangle2D leftRect = this.textComponent.getUI().modelToView2D(
                    textComponent, left, caret.getDotBias());
            Rectangle2D rightRect = this.textComponent.getUI().modelToView2D(
                    textComponent, right, caret.getDotBias());

            for (int selY = (int)leftRect.getY(); selY <= (int)rightRect.getY(); selY += font.fontHeight) {
                int minX = 0;
                int maxX = this.width - TEXT_PADDING_X*2;

                if (selY == (int)leftRect.getY()) minX = (int) leftRect.getX();
                if (selY == (int)rightRect.getY()) maxX = (int) rightRect.getX();

                invertedRect(matrices, x + TEXT_PADDING_X + minX,
                        y + CURSOR_PADDING_Y + 1 + selY, maxX - minX,
                        font.fontHeight);
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    protected void renderCursor(MatrixStack matrices, int x, int y) {
        if (this.tickCount / 6 % 2 == 0) return;
        try {
            Rectangle2D rect = this.textComponent.getUI().modelToView2D(
                    textComponent, caret.getDot(), caret.getDotBias());

            ScreenDrawing.coloredRect(matrices, x + TEXT_PADDING_X + (int)rect.getX(),
                    y + CURSOR_PADDING_Y + 1 + (int)rect.getY(),
                    1, font.fontHeight, CURSOR_COLOR);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void invertedRect(MatrixStack matrices, int x, int y, int width, int height) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        Matrix4f model = matrices.peek().getPositionMatrix();
        RenderSystem.setShaderColor(0.0F, 0.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.disableTexture();
        RenderSystem.enableColorLogicOp();
        RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE);
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        buffer.vertex(model, x, y + height, 0).next();
        buffer.vertex(model, x + width, y + height, 0).next();
        buffer.vertex(model, x + width, y, 0).next();
        buffer.vertex(model, x, y, 0).next();
        BufferRenderer.drawWithShader(buffer.end());
        RenderSystem.disableColorLogicOp();
        RenderSystem.enableTexture();
    }

    public void paint(MatrixStack matrices, int x, int y, int mouseX, int mouseY) {
        if (this.font == null) this.font = MinecraftClient.getInstance().textRenderer;

        int borderColor = this.isFocused() ? BORDER_COLOR_SELECTED : BORDER_COLOR_UNSELECTED;
        renderBox(matrices, x, y, borderColor, BACKGROUND_COLOR);

        renderText(matrices, x, y);
        if (this.isFocused()) {
            renderCursor(matrices, x, y);
        }
        renderSelection(matrices, x, y);
    }

    // endregion
    // =============================================================================================
    // region [Widget <-> AWT Bridge]
    // =============================================================================================

    private FontMetrics fontMetrics = null;
    protected final JTextArea textComponent = new JTextArea("", 10, 10) {
        @Override
        public FontMetrics getFontMetrics(Font font) {
            if (fontMetrics == null) {
                fontMetrics = new DummyFontMetrics(
                        MinecraftClient.getInstance().textRenderer,
                        font
                );
            }
            return fontMetrics;
        }
    };
    protected final DefaultCaret caret = new DefaultCaret();

    @Override
    public void setSize(int x, int y) {
        super.setSize(x, y);
        this.textComponent.setSize(x-TEXT_PADDING_X*2, y-TEXT_PADDING_Y*2);
    }

    @Override
    public void onKeyPressed(int ch, int key, int modifiers) {
        if (ch == GLFW.GLFW_KEY_TAB) {
            for (int i=0; i<4; i++) {
                onCharTyped(' ');
            }
            return;
        }
        int virtual = glfwToVk(ch);
        if (virtual == -1) return;
        KeyEvent event = new KeyEvent(textComponent, KeyEvent.KEY_PRESSED,
                EventQueue.getMostRecentEventTime(), glfwModToAwtMod(modifiers),
                virtual, KeyEvent.CHAR_UNDEFINED);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(textComponent, event);
    }

    @Override
    public void onKeyReleased(int ch, int key, int modifiers) {
        int virtual = glfwToVk(ch);
        if (virtual == -1) return;
        KeyEvent event = new KeyEvent(textComponent, KeyEvent.KEY_RELEASED,
            EventQueue.getMostRecentEventTime(), glfwModToAwtMod(modifiers),
                virtual, KeyEvent.CHAR_UNDEFINED);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(textComponent, event);
    }

    @Override
    public void onCharTyped(char ch) {
        KeyEvent event = new KeyEvent(textComponent, KeyEvent.KEY_TYPED,
                EventQueue.getMostRecentEventTime(),
                0, KeyEvent.VK_UNDEFINED, ch);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(textComponent, event);
    }

    @Override
    public InputResult onMouseDrag(int x, int y, int button,
                                   double deltaX, double deltaY) {
        MouseEvent event = new MouseEvent(textComponent, MouseEvent.MOUSE_DRAGGED,
                EventQueue.getMostRecentEventTime(), 1 << (10 + button),
                x - TEXT_PADDING_X, y - TEXT_PADDING_Y, clickCount,
                false, MouseEvent.BUTTON1 + button);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(textComponent, event);
        return InputResult.PROCESSED;
    }

    @Override
    public InputResult onClick(int x, int y, int button) {
        requestFocus();

        MouseEvent event = new MouseEvent(textComponent, MouseEvent.MOUSE_CLICKED,
                EventQueue.getMostRecentEventTime(), 0,
                x - TEXT_PADDING_X, y - TEXT_PADDING_Y, clickCount,
                false, MouseEvent.BUTTON1 + button);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(textComponent, event);
        return InputResult.PROCESSED;
    }

    @Override
    public InputResult onMouseDown(int x, int y, int button) {
        long currentTime = System.currentTimeMillis();
        if (lastClick < currentTime-250) {
            clickCount = 1;
        } else {
            clickCount++;
        }
        lastClick = currentTime;

        MouseEvent event = new MouseEvent(textComponent, MouseEvent.MOUSE_PRESSED,
                EventQueue.getMostRecentEventTime(), 0,
                x - TEXT_PADDING_X, y - TEXT_PADDING_Y, clickCount,
                false, MouseEvent.BUTTON1 + button);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(textComponent, event);
        return InputResult.PROCESSED;
    }

    @Override
    public InputResult onMouseUp(int x, int y, int button) {
        MouseEvent event = new MouseEvent(textComponent, MouseEvent.MOUSE_RELEASED,
                EventQueue.getMostRecentEventTime(), 0,
                x - TEXT_PADDING_X, y - TEXT_PADDING_Y, clickCount,
                false, MouseEvent.BUTTON1 + button);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().redispatchEvent(textComponent, event);
        return InputResult.PROCESSED;
    }

    // endregion
    // =============================================================================================
    // region [GLFW <-> AWT Constants]
    // =============================================================================================

    private static int glfwModToAwtMod(int glfw) {
        int awt = 0;
        if ((glfw & GLFW.GLFW_MOD_SHIFT) != 0) awt |= InputEvent.SHIFT_DOWN_MASK;
        if ((glfw & GLFW.GLFW_MOD_CONTROL) != 0) awt |= InputEvent.CTRL_DOWN_MASK;
        if ((glfw & GLFW.GLFW_MOD_ALT) != 0) awt |= InputEvent.ALT_DOWN_MASK;
        if ((glfw & GLFW.GLFW_MOD_SUPER) != 0) awt |= InputEvent.META_DOWN_MASK;
        return awt;
    }

    private static int glfwToVk(int glfw) {
        return switch (glfw) {
            case GLFW.GLFW_KEY_SPACE            -> KeyEvent.VK_SPACE;
            case GLFW.GLFW_KEY_APOSTROPHE       -> KeyEvent.VK_QUOTE;
            case GLFW.GLFW_KEY_COMMA            -> KeyEvent.VK_COMMA;
            case GLFW.GLFW_KEY_MINUS            -> KeyEvent.VK_MINUS;
            case GLFW.GLFW_KEY_PERIOD           -> KeyEvent.VK_PERIOD;
            case GLFW.GLFW_KEY_SLASH            -> KeyEvent.VK_SLASH;
            case GLFW.GLFW_KEY_0                -> KeyEvent.VK_0;
            case GLFW.GLFW_KEY_1                -> KeyEvent.VK_1;
            case GLFW.GLFW_KEY_2                -> KeyEvent.VK_2;
            case GLFW.GLFW_KEY_3                -> KeyEvent.VK_3;
            case GLFW.GLFW_KEY_4                -> KeyEvent.VK_4;
            case GLFW.GLFW_KEY_5                -> KeyEvent.VK_5;
            case GLFW.GLFW_KEY_6                -> KeyEvent.VK_6;
            case GLFW.GLFW_KEY_7                -> KeyEvent.VK_7;
            case GLFW.GLFW_KEY_8                -> KeyEvent.VK_8;
            case GLFW.GLFW_KEY_9                -> KeyEvent.VK_9;
            case GLFW.GLFW_KEY_SEMICOLON        -> KeyEvent.VK_SEMICOLON;
            case GLFW.GLFW_KEY_EQUAL            -> KeyEvent.VK_EQUALS;
            case GLFW.GLFW_KEY_A                -> KeyEvent.VK_A;
            case GLFW.GLFW_KEY_B                -> KeyEvent.VK_B;
            case GLFW.GLFW_KEY_C                -> KeyEvent.VK_C;
            case GLFW.GLFW_KEY_D                -> KeyEvent.VK_D;
            case GLFW.GLFW_KEY_E                -> KeyEvent.VK_E;
            case GLFW.GLFW_KEY_F                -> KeyEvent.VK_F;
            case GLFW.GLFW_KEY_G                -> KeyEvent.VK_G;
            case GLFW.GLFW_KEY_H                -> KeyEvent.VK_H;
            case GLFW.GLFW_KEY_I                -> KeyEvent.VK_I;
            case GLFW.GLFW_KEY_J                -> KeyEvent.VK_J;
            case GLFW.GLFW_KEY_K                -> KeyEvent.VK_K;
            case GLFW.GLFW_KEY_L                -> KeyEvent.VK_L;
            case GLFW.GLFW_KEY_M                -> KeyEvent.VK_M;
            case GLFW.GLFW_KEY_N                -> KeyEvent.VK_N;
            case GLFW.GLFW_KEY_O                -> KeyEvent.VK_O;
            case GLFW.GLFW_KEY_P                -> KeyEvent.VK_P;
            case GLFW.GLFW_KEY_Q                -> KeyEvent.VK_Q;
            case GLFW.GLFW_KEY_R                -> KeyEvent.VK_R;
            case GLFW.GLFW_KEY_S                -> KeyEvent.VK_S;
            case GLFW.GLFW_KEY_T                -> KeyEvent.VK_T;
            case GLFW.GLFW_KEY_U                -> KeyEvent.VK_U;
            case GLFW.GLFW_KEY_V                -> KeyEvent.VK_V;
            case GLFW.GLFW_KEY_W                -> KeyEvent.VK_W;
            case GLFW.GLFW_KEY_X                -> KeyEvent.VK_X;
            case GLFW.GLFW_KEY_Y                -> KeyEvent.VK_Y;
            case GLFW.GLFW_KEY_Z                -> KeyEvent.VK_Z;
            case GLFW.GLFW_KEY_LEFT_BRACKET     -> KeyEvent.VK_OPEN_BRACKET;
            case GLFW.GLFW_KEY_BACKSLASH        -> KeyEvent.VK_BACK_SLASH;
            case GLFW.GLFW_KEY_RIGHT_BRACKET    -> KeyEvent.VK_CLOSE_BRACKET;
            case GLFW.GLFW_KEY_GRAVE_ACCENT     -> KeyEvent.VK_DEAD_GRAVE;
            case GLFW.GLFW_KEY_ESCAPE           -> KeyEvent.VK_ESCAPE;
            case GLFW.GLFW_KEY_ENTER            -> KeyEvent.VK_ENTER; 
            case GLFW.GLFW_KEY_TAB              -> KeyEvent.VK_TAB;   
            case GLFW.GLFW_KEY_BACKSPACE        -> KeyEvent.VK_BACK_SPACE;
            case GLFW.GLFW_KEY_INSERT           -> KeyEvent.VK_INSERT;
            case GLFW.GLFW_KEY_DELETE           -> KeyEvent.VK_DELETE;
            case GLFW.GLFW_KEY_RIGHT            -> KeyEvent.VK_RIGHT; 
            case GLFW.GLFW_KEY_LEFT             -> KeyEvent.VK_LEFT;  
            case GLFW.GLFW_KEY_DOWN             -> KeyEvent.VK_DOWN;  
            case GLFW.GLFW_KEY_UP               -> KeyEvent.VK_UP;    
            case GLFW.GLFW_KEY_PAGE_UP          -> KeyEvent.VK_PAGE_UP;
            case GLFW.GLFW_KEY_PAGE_DOWN        -> KeyEvent.VK_PAGE_DOWN;
            case GLFW.GLFW_KEY_HOME             -> KeyEvent.VK_HOME;  
            case GLFW.GLFW_KEY_END              -> KeyEvent.VK_END;   
            case GLFW.GLFW_KEY_CAPS_LOCK        -> KeyEvent.VK_CAPS_LOCK;
            case GLFW.GLFW_KEY_SCROLL_LOCK      -> KeyEvent.VK_SCROLL_LOCK;
            case GLFW.GLFW_KEY_NUM_LOCK         -> KeyEvent.VK_NUM_LOCK; 
            case GLFW.GLFW_KEY_PRINT_SCREEN     -> KeyEvent.VK_PRINTSCREEN;
            case GLFW.GLFW_KEY_PAUSE            -> KeyEvent.VK_PAUSE; 
            case GLFW.GLFW_KEY_F1               -> KeyEvent.VK_F1;    
            case GLFW.GLFW_KEY_F2               -> KeyEvent.VK_F2;    
            case GLFW.GLFW_KEY_F3               -> KeyEvent.VK_F3;    
            case GLFW.GLFW_KEY_F4               -> KeyEvent.VK_F4;    
            case GLFW.GLFW_KEY_F5               -> KeyEvent.VK_F5;    
            case GLFW.GLFW_KEY_F6               -> KeyEvent.VK_F6;    
            case GLFW.GLFW_KEY_F7               -> KeyEvent.VK_F7;    
            case GLFW.GLFW_KEY_F8               -> KeyEvent.VK_F8;    
            case GLFW.GLFW_KEY_F9               -> KeyEvent.VK_F9;    
            case GLFW.GLFW_KEY_F10              -> KeyEvent.VK_F10;   
            case GLFW.GLFW_KEY_F11              -> KeyEvent.VK_F11;   
            case GLFW.GLFW_KEY_F12              -> KeyEvent.VK_F12;   
            case GLFW.GLFW_KEY_F13              -> KeyEvent.VK_F13;   
            case GLFW.GLFW_KEY_F14              -> KeyEvent.VK_F14;   
            case GLFW.GLFW_KEY_F15              -> KeyEvent.VK_F15;   
            case GLFW.GLFW_KEY_F16              -> KeyEvent.VK_F16;   
            case GLFW.GLFW_KEY_F17              -> KeyEvent.VK_F17;   
            case GLFW.GLFW_KEY_F18              -> KeyEvent.VK_F18;   
            case GLFW.GLFW_KEY_F19              -> KeyEvent.VK_F19;   
            case GLFW.GLFW_KEY_F20              -> KeyEvent.VK_F20;   
            case GLFW.GLFW_KEY_F21              -> KeyEvent.VK_F21;   
            case GLFW.GLFW_KEY_F22              -> KeyEvent.VK_F22;   
            case GLFW.GLFW_KEY_F23              -> KeyEvent.VK_F23;   
            case GLFW.GLFW_KEY_F24              -> KeyEvent.VK_F24;
            case GLFW.GLFW_KEY_KP_0             -> KeyEvent.VK_0;  
            case GLFW.GLFW_KEY_KP_1             -> KeyEvent.VK_1;  
            case GLFW.GLFW_KEY_KP_2             -> KeyEvent.VK_2;  
            case GLFW.GLFW_KEY_KP_3             -> KeyEvent.VK_3;  
            case GLFW.GLFW_KEY_KP_4             -> KeyEvent.VK_4;  
            case GLFW.GLFW_KEY_KP_5             -> KeyEvent.VK_5;  
            case GLFW.GLFW_KEY_KP_6             -> KeyEvent.VK_6;  
            case GLFW.GLFW_KEY_KP_7             -> KeyEvent.VK_7;  
            case GLFW.GLFW_KEY_KP_8             -> KeyEvent.VK_8;  
            case GLFW.GLFW_KEY_KP_9             -> KeyEvent.VK_9;  
            case GLFW.GLFW_KEY_KP_DECIMAL       -> KeyEvent.VK_DECIMAL;
            case GLFW.GLFW_KEY_KP_DIVIDE        -> KeyEvent.VK_DIVIDE;
            case GLFW.GLFW_KEY_KP_MULTIPLY      -> KeyEvent.VK_MULTIPLY;
            case GLFW.GLFW_KEY_KP_SUBTRACT      -> KeyEvent.VK_SUBTRACT;
            case GLFW.GLFW_KEY_KP_ADD           -> KeyEvent.VK_ADD;
            case GLFW.GLFW_KEY_KP_ENTER         -> KeyEvent.VK_ENTER; 
            case GLFW.GLFW_KEY_KP_EQUAL         -> KeyEvent.VK_EQUALS;
            case GLFW.GLFW_KEY_LEFT_SHIFT       -> KeyEvent.VK_SHIFT;
            case GLFW.GLFW_KEY_LEFT_CONTROL     -> KeyEvent.VK_CONTROL;
            case GLFW.GLFW_KEY_LEFT_ALT         -> KeyEvent.VK_ALT;
            case GLFW.GLFW_KEY_LEFT_SUPER       -> KeyEvent.VK_WINDOWS;
            case GLFW.GLFW_KEY_RIGHT_SHIFT      -> KeyEvent.VK_SHIFT;
            case GLFW.GLFW_KEY_RIGHT_CONTROL    -> KeyEvent.VK_CONTROL;
            case GLFW.GLFW_KEY_RIGHT_ALT        -> KeyEvent.VK_ALT;
            case GLFW.GLFW_KEY_RIGHT_SUPER      -> KeyEvent.VK_WINDOWS;
            //case GLFW.GLFW_KEY_MENU             -> KeyEvent.VK_MENU;
            //case GLFW.GLFW_KEY_LAST             -> KeyEvent.VK_LAST;
            default -> -1;
        };
    }

    // endregion
    // =============================================================================================

}
