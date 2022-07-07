package codes.moulberry.buildermod.macrotool.script.impl.operations.update;

import codes.moulberry.buildermod.macrotool.script.parser.ScriptParser;
import codes.moulberry.buildermod.macrotool.script.parser.lexer.tokens.BlockToken;
import codes.moulberry.buildermod.macrotool.script.parser.lexer.tokens.IdentifierType;
import codes.moulberry.buildermod.macrotool.script.parser.lexer.tokens.TokenType;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.shorts.Short2IntMap;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import norswap.autumn.Grammar;

public record TSetOp(BlockState state) implements TUpdateOp {

    private static Grammar.rule rule = null;
    public static Grammar.rule getRule(ScriptParser parser) {
        if (rule == null) {
            Grammar.rule blockStateLit = parser.tokPushType(TokenType.BLOCKSTATE);

            rule = parser.seq(parser.idenType(IdentifierType.SET),
                    blockStateLit).collect($ -> {
                BlockToken token = $.$0();
                $.push(new TSetOp(token.state));
            });
        }

        return rule;
    }

    @Override
    public void addToPalette(ShortSet palette) {
        palette.add((short) Block.getRawIdFromState(state));
    }

    public short getNewBlockAt(BlockPos pos) {
        return (short) Block.getRawIdFromState(state);
    }

}
