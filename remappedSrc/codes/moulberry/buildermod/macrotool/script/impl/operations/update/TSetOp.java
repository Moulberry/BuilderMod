package codes.moulberry.buildermod.macrotool.script.impl.operations.update;

import codes.moulberry.buildermod.macrotool.script.parser.ScriptParser;
import codes.moulberry.buildermod.macrotool.script.parser.lexer.tokens.IdentifierType;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import norswap.autumn.Grammar;

public record TSetOp(IntSet state) implements TUpdateOp {

    private static final int flags = Block.NOTIFY_LISTENERS | Block.FORCE_STATE |
            Block.SKIP_DROPS;

    private static Grammar.rule rule = null;
    public static Grammar.rule getRule(ScriptParser parser) {
        if (rule == null) {
            /*Grammar.rule blockStateLit = parser.tokPushType().objPushValidate(
                    parser.validateTokenKind(TokenKind.BLOCK_LITERAL));

            rule = parser.seq(parser.idenType(IdentifierType.SET),
                    blockStateLit).collect($ -> {
                Token token = $.$0();
                $.push(new TSetOp(token.blocks));
            });*/
            rule = parser.idenType(IdentifierType.SET);
        }

        return rule;
    }

    public void runIntegrated(World world, BlockPos pos) {
        //world.setBlockState(pos, state, flags);
    }

}
