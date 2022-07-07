package codes.moulberry.buildermod.macrotool.script;

import codes.moulberry.buildermod.macrotool.script.options.ScriptOption;
import codes.moulberry.buildermod.macrotool.script.impl.functions.TFunction;

import java.util.Map;

public record CompiledScript(Map<String, ScriptOption> options, TFunction function) {

}
