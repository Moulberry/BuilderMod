package codes.moulberry.buildermod.macrotool.script.impl.operations.mask;

import codes.moulberry.buildermod.macrotool.script.parser.ScriptParser;
import codes.moulberry.buildermod.macrotool.script.parser.lexer.tokens.IdentifierType;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkCache;
import norswap.autumn.Grammar;

public record TMatchOp(IntSet state) implements TMaskOp {

    private static Grammar.rule rule = null;
    public static Grammar.rule getRule(ScriptParser parser) {
        if (rule == null) {
            /*Grammar.rule blockStateLit = parser.objPushValidate(
                    parser.validateTokenKind(TokenKind.BLOCK_LITERAL));

            rule = parser.seq(parser.idenType(TokenKind.MATCH),
                    blockStateLit).collect($ -> {
                Token token = $.$0();
                $.push(new TMatchOp(token.blocks));
            });*/
            rule = parser.idenType(IdentifierType.MASK);
        }

        return rule;
    }


    @Override
    public boolean matches(ChunkCache cache, BlockPos pos) {
        return state.contains(System.identityHashCode(cache.getBlockState(pos)));
    }
}
