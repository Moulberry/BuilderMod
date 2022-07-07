package codes.moulberry.buildermod.macrotool.script.impl.functions;

import codes.moulberry.buildermod.macrotool.script.ToolExecutionContext;
import codes.moulberry.buildermod.macrotool.script.impl.UpdateOperation;
import codes.moulberry.buildermod.macrotool.script.impl.dynamicvalues.IntDynamicValue;
import codes.moulberry.buildermod.macrotool.script.impl.operations.mask.TMaskOp;
import codes.moulberry.buildermod.macrotool.script.impl.operations.mask.TMatchOp;
import codes.moulberry.buildermod.macrotool.script.impl.operations.update.TSetOp;
import codes.moulberry.buildermod.macrotool.script.impl.operations.update.TUpdateOp;
import codes.moulberry.buildermod.macrotool.script.parser.ParseError;
import codes.moulberry.buildermod.macrotool.script.parser.ScriptParser;
import codes.moulberry.buildermod.macrotool.script.parser.lexer.tokens.IdentifierType;
import codes.moulberry.buildermod.render.SphereRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.world.chunk.ChunkCache;
import norswap.autumn.Grammar;

import java.util.ArrayList;
import java.util.List;

public class TSphereFunction extends TGenericFunction {

    private final IntDynamicValue radius;

    private static Grammar.rule rule = null;
    public static Grammar.rule getRule(ScriptParser parser) {
        if (rule == null) {
            Grammar.rule maskOperations = parser.choice(
                    TMatchOp.getRule(parser)
            );

            Grammar.rule updateOperations = parser.choice(
                    TSetOp.getRule(parser)
            );

            // sphere $radius mask... update...
            rule = parser.seq(
                    parser.idenType(IdentifierType.SPHERE),
                    parser.paramInt(),
                    maskOperations.at_least(0),
                    updateOperations.at_least(0),
                    maskOperations.at_least(1).filter($ -> {
                        throw new ParseError("Cannot have mask operation after set operation");
                    }).not()
            ).collect($ -> {
                List<TMaskOp> maskOps = new ArrayList<>();
                List<TUpdateOp> updateOps = new ArrayList<>();

                for (Object object : $.$list()) {
                    if (object instanceof TMaskOp maskOp) {
                        maskOps.add(maskOp);
                    } else if (object instanceof TUpdateOp updateOp) {
                        updateOps.add(updateOp);
                    }
                }

                $.push(new TSphereFunction($.$0(), maskOps.toArray(new TMaskOp[0]),
                        updateOps.toArray(new TUpdateOp[0])));
            });
        }

        return rule;
    }

    public TSphereFunction(IntDynamicValue radius, TMaskOp[] maskOps, TUpdateOp[] updateOps) {
        super(maskOps, updateOps);
        this.radius = radius;
    }

    @Override
    protected void preview(ToolExecutionContext context, MatrixStack matrices,
                           Matrix4f projection, float tickDelta) {
        int radius = this.radius.resolve(context);
        TGenericFunction.raycastBlock(tickDelta, (blockHitResult -> {
            BlockPos pos = blockHitResult.getBlockPos();
            SphereRenderer.render(matrices, projection, pos, radius);
        }));
    }

    @Override
    protected void update(ToolExecutionContext context, BlockPos hit) {
        int radius = this.radius.resolve(context);

        ChunkCache cache = new ChunkCache(
                context.getWorld(),
                hit.add(-radius-5, -radius-5, -radius-5),
                hit.add(radius+5, radius+5, radius+5)
        );

        new UpdateOperation().perform(
                context.getMinecraft().getServer().getOverworld(),
                hit.add(-radius, -radius, -radius),
                hit.add(radius, radius, radius),
                pos -> true
        );

        /*float rSq = (radius + 0.5f) * (radius + 0.5f);
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z < rSq) {
                        addToRegion(cache, x + hit.getX(), y + hit.getY(), z + hit.getZ());
                    }
                }
            }
        }*/
    }

}
