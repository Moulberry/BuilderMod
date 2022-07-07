package codes.moulberry.buildermod.macrotool.script.parser.lexer.tokens;

public class VariableToken extends Token {

    public final String varname;

    public VariableToken(String varname, String input, int start, int end) {
        super(input, start, end);
        this.varname = varname;
    }

}
