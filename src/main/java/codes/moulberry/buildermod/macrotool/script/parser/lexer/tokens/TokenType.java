package codes.moulberry.buildermod.macrotool.script.parser.lexer.tokens;

import java.util.HashMap;
import java.util.Map;

public enum TokenType {

    ERROR(ErrorToken.class, "<Error>"),
    FLOAT(FloatToken.class, "<Float>"),
    IDENTIFIER(IdentifierToken.class, "<Identifier>"),
    INTEGER(IntegerToken.class, "<Integer>"),
    STRING(StringToken.class, "<String>"),
    VARIABLE(VariableToken.class, "<$Variable>"),
    BLOCKSTATE(BlockToken.class, "<BlockState>");

    public final Class<? extends Token> type;
    public final String name;

    TokenType(Class<? extends Token> type, String name) {
        this.type = type;
        this.name = name;
    }

    public static final Map<Class<?>, String> nameMap = new HashMap<>();
    static {
        for (TokenType type : TokenType.values()) {
            nameMap.put(type.type, type.name);
        }
    }

}
