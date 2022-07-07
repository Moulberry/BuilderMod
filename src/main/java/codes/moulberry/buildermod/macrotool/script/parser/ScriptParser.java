package codes.moulberry.buildermod.macrotool.script.parser;

import codes.moulberry.buildermod.macrotool.script.CompiledScript;
import codes.moulberry.buildermod.macrotool.script.impl.dynamicvalues.IntDynamicValue;
import codes.moulberry.buildermod.macrotool.script.options.ScriptOption;
import codes.moulberry.buildermod.macrotool.script.impl.functions.TFunction;
import codes.moulberry.buildermod.macrotool.script.impl.functions.TSphereFunction;
import codes.moulberry.buildermod.macrotool.script.parser.lexer.Lexer;
import codes.moulberry.buildermod.macrotool.script.parser.lexer.tokens.*;
import codes.moulberry.buildermod.macrotool.script.parser.options.RequireIntRule;
import norswap.autumn.Autumn;
import norswap.autumn.Grammar;
import norswap.autumn.ParseOptions;
import norswap.autumn.ParseResult;
import norswap.autumn.actions.ActionContext;
import norswap.autumn.positions.LineMap;
import norswap.autumn.positions.LineMapString;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.ObjIntConsumer;

public class ScriptParser extends Grammar {

    public static final ScriptParser INSTANCE = new ScriptParser();

    protected ScriptParser() {
    }

    // =============================================================================================
    // region [Helper methods for rule creation]
    // =============================================================================================

    public <T> rule builderParamInt(String name, ObjIntConsumer<T> consumer) {
        return seq(idenName(name), tokPushType(TokenType.INTEGER)).collect($ -> {
            IntegerToken token = $.$1();
            consumer.accept($.$0(), token.value);
            $.push($.$0());
        }, Grammar.LOOKBACK(1));
    }

    public <T> rule builderParamString(String name, BiConsumer<T, String> consumer) {
        return seq(idenName(name), tokPushType(TokenType.STRING)).collect($ -> {
            StringToken token = $.$1();
            consumer.accept($.$0(), token.value);
            $.push($.$0());
        }, Grammar.LOOKBACK(1));
    }

    public <T> rule builderParamFloat(String name, BiConsumer<T, Float> consumer) {
        return seq(idenName(name), objPush()).collect($ -> {
            Token token = $.$1();
            if (token instanceof IntegerToken integerToken) {
                consumer.accept($.$0(), (float) integerToken.value);
            } else if (token instanceof FloatToken floatToken) {
                consumer.accept($.$0(), floatToken.value);
            } else {
                throwUnexpected($, TokenType.FLOAT, token);
            }
            $.push($.$0());
        }, Grammar.LOOKBACK(1));
    }

    public boolean isVarRegistered(ActionContext ctx, String var) {
        // noinspection unchecked
        Set<String> set = (Set<String>) ctx.parse.stateData
                .computeIfAbsent("vars", t -> new HashSet<>());
        return set.contains(var);
    }

    public void registerVar(ActionContext ctx, String var) {
        // noinspection unchecked
        Set<String> set = (Set<String>) ctx.parse.stateData
                .computeIfAbsent("vars", t -> new HashSet<>());
        if (set.contains(var)) {
            ctx.parse.pos = ctx.pos0;
            throw new ParseError("Redeclaration of var: " + var);
        }
        set.add(var);
    }

    public void checkVarRegistered(ActionContext ctx, String var) {
        if (!isVarRegistered(ctx, var)) {
            ctx.parse.pos = ctx.pos0;
            throw new ParseError("Cannot resolve var: " + var);
        }
    }

    public rule paramInt() {
        return objPush().collect($ -> {
            Token token = $.$0();
            if (token instanceof IntegerToken integerToken) {
                $.push(IntDynamicValue.ofStatic(integerToken.value));
            } else if (token instanceof VariableToken variableToken) {
                checkVarRegistered($, variableToken.varname);
                $.push(IntDynamicValue.ofDynamic(ctx -> ctx.getIntSetting(variableToken.varname)));
            } else {
                String name = TokenType.nameMap.getOrDefault(token.getClass(), "<Unknown>");
                throwUnexpected($, TokenType.INTEGER.name + " or " + TokenType.VARIABLE.name, name);
            }
        });
    }

    public void throwUnexpected(ActionContext ctx, TokenType expected, Token actual) {
        String tokenName = TokenType.nameMap.getOrDefault(actual.getClass(), "<Unknown>");
        throwUnexpected(ctx, expected.name, tokenName);
    }

    public void throwUnexpected(ActionContext ctx, String expected, String actual) {
        ctx.parse.pos = ctx.pos0;
        throw new ParseError("Expected %s, but found %s", expected, actual);
    }

    public rule idenName(String string) {
        return opred(it -> it instanceof IdentifierToken identifierToken &&
                identifierToken.identifier.equals(string));
    }

    public rule idenType(IdentifierType type) {
        return opred(it -> it instanceof IdentifierToken identifierToken &&
                identifierToken.type == type);
    }

    public rule tokPushType(TokenType type) {
        return empty.push($ -> {
            if ($.parse.pos >= $.parse.list.size()) {
                throwUnexpected($, type.name, "end-of-file");
            }
            Object token = $.parse.list.get($.parse.pos++);
            if (token.getClass() != type.type) {
                throwUnexpected($, type, (Token) token);
            }
            return token;
        });
    }

    public rule objPush() {
        return empty.push($ -> {
            if ($.parse.pos >= $.parse.list.size()) {
                throwUnexpected($, "more input", "end-of-file");
            }
            return $.parse.list.get($.parse.pos++);
        });
    }

    // endregion
    // =============================================================================================
    // region [Configuration]
    // =============================================================================================

    public rule _require_int = RequireIntRule.create(this);

    public rule configuration =
            choice(
                    _require_int
            );

    // endregion
    // =============================================================================================
    // region [Operations]
    // =============================================================================================


    public rule _sphere = TSphereFunction.getRule(this);

    public rule operation =
            choice(
                    _sphere
            );

    // endregion
    // =============================================================================================

    @Override
    public rule root() {
        return seq(configuration.at_least(0), operation);
    }

    public CompiledScript parse(String inputName, String input) {
        Lexer lexer = new Lexer(input);
        Token token;
        List<Token> list = new ArrayList<>(input.length()/20);
        while ((token = lexer.next()) != null) {
            System.out.println(token);
            list.add(token);
        }

        ParseResult result = Autumn.parse(root(), list, ParseOptions.builder().recordCallStack(true).get());
        if (result.fullMatch) {
            Map<String, ScriptOption> options = new HashMap<>();
            TFunction function = null;
            for (Object o : result.valueStack) {
                if (o instanceof ScriptOption option) {
                    options.put(option.id(), option);
                } else if (o instanceof TFunction func) {
                    function = func;
                }
            }
            return new CompiledScript(options, function);
        } else if (result.thrown != null) {
            int error = result.errorOffset;
            if (result.errorOffset >= list.size()) {
                error = list.size() - 1;
            }

            Token failedToken = list.get(error);

            LineMapString map = new LineMapString(inputName, input);
            StringBuilder b = new StringBuilder();
            b.append("Compile error at: ");
            b.append(LineMap.string(map, failedToken.end));
            b.append(".\n");
            b.append(map.lineSnippet(map.positionFrom(failedToken.end)));
            b.append(result.thrown.getMessage());
            System.err.println(b);

            result.thrown.printStackTrace();
        } else {
            if (result.errorOffset >= list.size()) {
                System.err.println("Missing operation");
            } else {
                int error = result.errorOffset;
                Token failedToken = list.get(error);

                LineMapString map = new LineMapString(inputName, input);
                StringBuilder b = new StringBuilder();
                b.append("Compile error at: ");
                b.append(LineMap.string(map, failedToken.start));
                b.append(".\n");
                b.append(map.lineSnippet(map.positionFrom(failedToken.start)));
                b.append("Unexpected token here");
                System.err.println(b);

                System.err.println(result.toString(new LineMapString(inputName, input), false));
            }
        }
        return null;
    }

    public static void main(String[] args) {
        StringBuilder builder = new StringBuilder();
        builder.append("// epic comment\n");
        builder.append("require_int $test\n");
        builder.append("name \"test\"\n");
        builder.append("sphere $test\n");
        builder.append("set\n");
        //builder.append("require_int\n");
        //builder.append("name \"other\"\n");

        INSTANCE.parse("<test>", builder.toString());
    }

}
