package codes.moulberry.buildermod.gui.widgets;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class WTagsField extends WParserField<List<String>> {

    public WTagsField(@Nullable String exampleTags) {
        super(exampleTags == null ? null : Text.of("e.g. "+exampleTags), Collections.emptyList());
    }

    @Override
    protected String adjust(String str) {
        return str.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_,-]", "");
    }

    @Override
    protected List<String> parse(String str) {
        str = adjust(str);
        return new ArrayList<>(new HashSet<>(Arrays.asList(str.split(","))));
    }

}
