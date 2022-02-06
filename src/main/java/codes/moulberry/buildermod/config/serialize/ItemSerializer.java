package codes.moulberry.buildermod.config.serialize;

import com.google.gson.*;
import net.minecraft.item.Item;

import java.lang.reflect.Type;

public class ItemSerializer implements JsonSerializer<Item>, JsonDeserializer<Item> {

    @Override
    public Item deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return Item.byRawId(json.getAsInt());
    }

    @Override
    public JsonElement serialize(Item src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(Item.getRawId(src));
    }

}
