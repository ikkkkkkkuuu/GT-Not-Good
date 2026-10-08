package com.xyp.gtnotgood.common.machines.hatch.me;

import net.minecraft.util.IChatComponent;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.gtnewhorizon.gtnhlib.chat.AbstractChatComponentCustom;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;

/**
 * Carries both naming policies through AE2's existing suffix packets. Each viewing client selects its NEI preference;
 * server-side polling always resolves the standard suffix, including on an integrated server.
 */
public final class ChatComponentInterfaceNameSuffix extends AbstractChatComponentCustom {

    private IChatComponent standard;
    private IChatComponent preferred;

    public ChatComponentInterfaceNameSuffix() {}

    public ChatComponentInterfaceNameSuffix(IChatComponent standard, IChatComponent preferred) {
        this.standard = standard;
        this.preferred = preferred;
    }

    @Override
    public String getID() {
        return ModList.GTNotGood.getID() + ":InterfaceNameSuffix";
    }

    @Override
    public JsonElement serialize() {
        JsonObject json = new JsonObject();
        json.add("standard", encode(standard));
        json.add("preferred", encode(preferred));
        return json;
    }

    @Override
    public void deserialize(JsonElement jsonElement) {
        JsonObject json = jsonElement.getAsJsonObject();
        standard = decode(json.get("standard"));
        preferred = decode(json.get("preferred"));
    }

    @Override
    protected AbstractChatComponentCustom copySelf() {
        return new ChatComponentInterfaceNameSuffix(standard == null ? null : standard.createCopy(),
            preferred == null ? null : preferred.createCopy());
    }

    @Override
    public String getUnformattedTextForChat() {
        boolean preferOwn = FMLCommonHandler.instance().getEffectiveSide().isClient()
            && GTNotGood.proxy.preferOwnInterfaceNames();
        return getText(preferOwn);
    }

    /**
     * Resolves the selected policy without modifying either component, so clients can choose independently.
     *
     * @param preferOwn whether to use only the assembly's own circuit and non-consumed input numbers
     * @return localized suffix text, or an empty string for a missing suffix
     */
    public String getText(boolean preferOwn) {
        IChatComponent selected = preferOwn ? preferred : standard;
        return selected == null ? "" : selected.getUnformattedText();
    }

    private static JsonElement encode(IChatComponent component) {
        return component == null ? JsonNull.INSTANCE
            : new JsonPrimitive(IChatComponent.Serializer.func_150696_a(component));
    }

    private static IChatComponent decode(JsonElement value) {
        return value == null || value.isJsonNull() ? null
            : IChatComponent.Serializer.func_150699_a(value.getAsString());
    }
}
