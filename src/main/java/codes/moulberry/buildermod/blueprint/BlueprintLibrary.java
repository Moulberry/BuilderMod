package codes.moulberry.buildermod.blueprint;

import net.minecraft.util.Identifier;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class BlueprintLibrary {

    public static Blueprint selected = null;
    public static Map<Identifier, Blueprint> blueprints = new LinkedHashMap<>();

    public static void loadBlueprints(File blueprintsFolder) {
        processDirectory(blueprintsFolder, "");
    }

    private static void processDirectory(File directory, String prefix) {
        if (prefix.isBlank()) {
            prefix = directory.getName() + ":";
        } else {
            prefix += directory.getName() + "/";
        }

        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                processDirectory(file, prefix);
            } else {
                String identifierStr = prefix + file.getName().split("\\.")[0];
                Identifier identifier = Identifier.tryParse(identifierStr);
                if (identifier == null) continue;

                Blueprint blueprint = Blueprint.load(file);
                if (blueprint != null) {
                    blueprint.identifier = identifier;
                    blueprints.put(identifier, blueprint);
                }
            }
        }
    }

}
