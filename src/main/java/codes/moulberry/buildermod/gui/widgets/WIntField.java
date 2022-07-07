package codes.moulberry.buildermod.gui.widgets;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class WIntField extends WParserField<Integer> {

    public WIntField() {
    }

    public WIntField(int defaultValue) {
        super(defaultValue);
    }

    @Override
    protected Integer parse(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
