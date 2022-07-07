package codes.moulberry.buildermod.macrotool.script.parser.options;

import codes.moulberry.buildermod.macrotool.script.options.IntScriptOptionBuilder;
import codes.moulberry.buildermod.macrotool.script.parser.ParseError;
import codes.moulberry.buildermod.macrotool.script.parser.ScriptParser;
import codes.moulberry.buildermod.macrotool.script.parser.lexer.tokens.IdentifierType;
import codes.moulberry.buildermod.macrotool.script.parser.lexer.tokens.TokenType;
import codes.moulberry.buildermod.macrotool.script.parser.lexer.tokens.VariableToken;
import norswap.autumn.Grammar;

import java.util.OptionalInt;
import java.util.function.BiConsumer;
import java.util.function.ObjIntConsumer;

public class RequireIntRule {

    public static Grammar.rule create(ScriptParser parser) {
        BiConsumer<IntScriptOptionBuilder, String> name = IntScriptOptionBuilder::name;
        ObjIntConsumer<IntScriptOptionBuilder> defaultValue = IntScriptOptionBuilder::defaultValue;
        ObjIntConsumer<IntScriptOptionBuilder> min = (b, v) -> b.min(OptionalInt.of(v));
        ObjIntConsumer<IntScriptOptionBuilder> max = (b, v) -> b.max(OptionalInt.of(v));

        Grammar.rule require_int_param =
                parser.choice(
                        parser.builderParamString("name", name),
                        parser.builderParamInt("default", defaultValue),
                        parser.builderParamInt("min", min),
                        parser.builderParamInt("max", max)
                );

        // require_int $var ...
        return parser.seq(
                parser.idenType(IdentifierType.REQUIRE_INT).push($ -> IntScriptOptionBuilder.builder()),
                parser.tokPushType(TokenType.VARIABLE).collect($ -> {
                    VariableToken token = $.$1();
                    IntScriptOptionBuilder builder = $.$0();
                    builder.id(token.varname);

                    parser.registerVar($, token.varname);

                    $.push($.$0());
                }, Grammar.LOOKBACK(1)),
                require_int_param.at_least(0)
        ).push($ -> {
            IntScriptOptionBuilder builder = $.$0();
            if (builder.name() == null) {
                $.parse.pos = $.pos0;
                throw new ParseError("missing required parameter `name`");
            }
            return ((IntScriptOptionBuilder)$.$0()).build();
        });
    }

}
