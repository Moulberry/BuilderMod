package codes.moulberry.buildermod.macrotool.script.options;

import io.github.cottonmc.cotton.gui.widget.WWidget;
import net.minecraft.nbt.NbtCompound;

public interface ScriptOption {

    String id();
    WWidget createConfigureWidget(NbtCompound settings);

}
