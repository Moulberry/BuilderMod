package codes.moulberry.buildermod.gui.widgets;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class WIdentifierField extends WParserField<Identifier> {

    private final String defaultNamespace;

    public WIdentifierField(@Nullable String defaultNamespace, @Nullable String exampleNamespace) {
        super(exampleNamespace == null ? null : Text.of("e.g. "+exampleNamespace));
        this.defaultNamespace = defaultNamespace;
    }

    @Override
    protected String adjust(String str) {
        if (defaultNamespace != null && !str.isEmpty() && Identifier.isValid(str)) {
            if (!str.contains(":")) {
                return defaultNamespace + ":" + str;
            }
        }
        return null;
    }

    @Override
    protected Identifier parse(String str) {
        if (str.isEmpty()) {
            return null;
        }
        if (defaultNamespace != null && !this.getText().contains(":")) {
            str = defaultNamespace + ":" + str;
        }
        return Identifier.tryParse(str);
    }

}
