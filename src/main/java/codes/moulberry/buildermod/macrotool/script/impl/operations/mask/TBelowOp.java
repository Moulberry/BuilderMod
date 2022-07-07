package codes.moulberry.buildermod.macrotool.script.impl.operations.mask;

import codes.moulberry.buildermod.macrotool.script.parser.ScriptParser;
import codes.moulberry.buildermod.macrotool.script.parser.lexer.tokens.BlockToken;
import codes.moulberry.buildermod.macrotool.script.parser.lexer.tokens.IdentifierType;
import codes.moulberry.buildermod.macrotool.script.parser.lexer.tokens.TokenType;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.ChunkCache;
import norswap.autumn.Grammar;

public record TBelowOp(IntSet state) implements TMaskOp {

    private static Grammar.rule rule = null;
    public static Grammar.rule getRule(ScriptParser parser) {
        if (rule == null) {
            Grammar.rule blockStateLit = parser.tokPushType(TokenType.BLOCKSTATE);

            rule = parser.seq(parser.idenType(IdentifierType.BELOW),
                    blockStateLit).collect($ -> {
                BlockToken token = $.$0();
                $.push(new TBelowOp(IntSet.of(System.identityHashCode(token.state))));
            });
        }

        return rule;
    }


    @Override
    public boolean matches(ChunkCache cache, BlockPos.Mutable pos) {
        return state.contains(System.identityHashCode(cache.getBlockState(pos.move(Direction.UP))));
    }
}
