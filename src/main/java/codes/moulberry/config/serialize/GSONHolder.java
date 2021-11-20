package codes.moulberry.config.serialize;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.item.Item;

public class GSONHolder {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting()
            //.registerTypeAdapter(Item.class, new ItemSerializer())
            .create();



}
