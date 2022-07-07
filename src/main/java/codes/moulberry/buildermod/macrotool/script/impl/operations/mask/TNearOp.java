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

public record TNearOp(IntSet state) implements TMaskOp {

    private static Grammar.rule rule = null;
    public static Grammar.rule getRule(ScriptParser parser) {
        if (rule == null) {
            Grammar.rule blockStateLit = parser.tokPushType(TokenType.BLOCKSTATE);

            rule = parser.seq(parser.idenType(IdentifierType.NEAR),
                    blockStateLit).collect($ -> {
                BlockToken token = $.$0();
                $.push(new TNearOp(IntSet.of(System.identityHashCode(token.state))));
            });
        }

        return rule;
    }


    @Override
    public boolean matches(ChunkCache cache, BlockPos.Mutable pos) {
        for (Direction direction : Direction.values()) {
            if (state.contains(System.identityHashCode(cache.getBlockState(pos.mutableCopy().move(direction))))) {
                return true;
            }
        }
        return false;
    }
}
