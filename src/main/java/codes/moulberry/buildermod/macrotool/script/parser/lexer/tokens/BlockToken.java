package codes.moulberry.buildermod.macrotool.script.parser.lexer.tokens;

import net.minecraft.block.BlockState;

public class BlockToken extends Token {

    public final BlockState state;

    public BlockToken(BlockState state, String input, int start, int end) {
        super(input, start, end);
        this.state = state;
    }

}
