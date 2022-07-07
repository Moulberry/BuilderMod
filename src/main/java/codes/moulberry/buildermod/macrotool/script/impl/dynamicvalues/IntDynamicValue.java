package codes.moulberry.buildermod.macrotool.script.impl.dynamicvalues;

import codes.moulberry.buildermod.macrotool.script.ToolExecutionContext;

import java.util.function.ToIntFunction;

public class IntDynamicValue {

    ToIntFunction<ToolExecutionContext> dynamic;
    int staticValue;

    private IntDynamicValue(ToIntFunction<ToolExecutionContext> dynamic, int staticValue) {
        this.dynamic = dynamic;
        this.staticValue = staticValue;
    }

    public int resolve(ToolExecutionContext context) {
        if (this.dynamic == null) {
            return staticValue;
        } else {
            return this.dynamic.applyAsInt(context);
        }
    }

    public static IntDynamicValue ofStatic(int staticValue) {
        return new IntDynamicValue(null, staticValue);
    }

    public static IntDynamicValue ofDynamic(ToIntFunction<ToolExecutionContext> dynamic) {
        return new IntDynamicValue(dynamic, 0);
    }

}
