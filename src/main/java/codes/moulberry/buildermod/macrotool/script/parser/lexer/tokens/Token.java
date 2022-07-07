package codes.moulberry.buildermod.macrotool.script.parser.lexer.tokens;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockArgumentParser;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public abstract class Token implements norswap.autumn.positions.Token {

    // The string input this token was created from
    private final String input;

    // Start position of the token in the input string.
    public final int start;

    // End position of the token in the input string.
    public final int end;

    public Token(String input, int start, int end) {
        this.input = input;
        this.start = start;
        this.end = end;
    }

    @Override
    public int start() {
        return start;
    }

    @Override
    public int end() {
        return end;
    }

    public static Token error(String content, String input, int start, int end) {
        return new ErrorToken(content, input, start, end);
    }

    public static Token string(String content, String input, int start, int end) {
        return new StringToken(content, input, start, end);
    }

    public static Token numberToken(String content, String input, int start, int end) {
        content = content.trim();

        boolean isFloat = false;
        float multiplier = 1;
        if (content.endsWith("%")) {
            multiplier = 0.01f;
            isFloat = true;
            content = content.substring(0, content.length()-1);
        } else if (content.endsWith("f") || content.contains(".")) {
            isFloat = true;
        }

        try {
            if (isFloat) {
                float f = Float.parseFloat(content) * multiplier;
                return new FloatToken(f, input, start, end);
            } else {
                int i = Integer.parseInt(content);
                return new IntegerToken(i, input, start, end);
            }
        } catch (NumberFormatException e) {
            return error(e.getMessage(), input, start, end);
        }
    }

    public static Token identifier(String content, String input, int start, int end) {
        if (content.startsWith("$")) {
            content = content.substring(1);
            if (content.isBlank()) {
                return error("$ must be followed by a valid identifier", input, start, end);
            }
            return new VariableToken(content, input, start, end);
        }

        IdentifierType type = IdentifierType.UNKNOWN;
        if (aliasMap.containsKey(content)) {
            type = aliasMap.get(content);
        } else {
            System.out.println("Trying to parse: " + content);
            var parser = new BlockArgumentParser(new StringReader(content.toLowerCase(Locale.ROOT)), false);
            try {
                BlockState block = parser.parse(false).getBlockState();
                if (block != null) {
                    return new BlockToken(block, input, start, end);
                }
            } catch (CommandSyntaxException ignored) {}
            System.out.println("Was not block!");
        }
        return new IdentifierToken(type, content, input, start, end);
    }

    private static final Map<String, IdentifierType> aliasMap = new HashMap<>();
    public static void registerAlias(IdentifierType type, String alias) {
        aliasMap.put(alias, type);
    }

}
