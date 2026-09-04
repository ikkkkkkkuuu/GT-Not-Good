package com.xyp.gtnotgood.utils.item;

import static com.xyp.gtnotgood.GTNotGood.RESOURCE_ROOT_ID;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizons.modularui.api.drawable.UITexture;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.util.GTModHandler;

public class ItemUtils {

    // Logo images for GUI (MUI1 UITexture)
    public static final UITexture PICTURE_CIRCULATION = UITexture
        .fullImage(RESOURCE_ROOT_ID, "gui/picture/circulation_");

    public static final UITexture PICTURE_GTNL_LOGO = UITexture.fullImage(RESOURCE_ROOT_ID, "gui/picture/gorge_logo");

    public static ItemStack getItemStack(ItemStack baseStack, String aNBTString, ItemStack aReplacement) {
        if (baseStack == null) return aReplacement;
        try {
            baseStack.stackTagCompound = (NBTTagCompound) JsonToNBT.func_150315_a(aNBTString);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return baseStack;
    }

    public static ItemStack getItemStack(String aModID, String aItem, long aAmount, int aMeta, String aNBTString) {
        ItemStack s = GTModHandler.getModItem(aModID, aItem, aAmount, aMeta);
        try {
            s.stackTagCompound = (NBTTagCompound) JsonToNBT.func_150315_a(aNBTString);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return s;
    }

    public static ItemStack getItemStack(String aModID, String aItem, long aAmount, int aMeta, String aNBTString,
        ItemStack aReplacement) {
        ItemStack itemStack = GTModHandler.getModItem(aModID, aItem, aAmount, aMeta);
        if (itemStack == null) return aReplacement;
        try {
            itemStack.stackTagCompound = (NBTTagCompound) JsonToNBT.func_150315_a(aNBTString);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return itemStack;
    }

    public static boolean setToolDamage(ItemStack aStack, long aDamage) {
        if (aStack == null) return false;

        NBTTagCompound tag = aStack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            aStack.setTagCompound(tag);
        }

        NBTTagCompound toolStats;
        if (tag.hasKey("GT.ToolStats")) {
            toolStats = tag.getCompoundTag("GT.ToolStats");
        } else {
            toolStats = new NBTTagCompound();
            tag.setTag("GT.ToolStats", toolStats);
        }

        toolStats.setLong("Damage", aDamage);
        return true;
    }

    public static boolean setToolMaxDamage(ItemStack aStack, long aMaxDamage) {
        if (aStack == null) return false;

        NBTTagCompound tag = aStack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            aStack.setTagCompound(tag);
        }

        NBTTagCompound toolStats;
        if (tag.hasKey("GT.ToolStats")) {
            toolStats = tag.getCompoundTag("GT.ToolStats");
        } else {
            toolStats = new NBTTagCompound();
            tag.setTag("GT.ToolStats", toolStats);
        }

        toolStats.setLong("MaxDamage", aMaxDamage);

        return true;
    }

    public static Materials[] TIER = { Materials.LV, Materials.MV, Materials.HV, Materials.EV, Materials.IV,
        Materials.LuV, Materials.ZPM, Materials.UV, Materials.UHV, Materials.UEV, Materials.UIV, Materials.UMV,
        Materials.UXV, Materials.MAX };

    public static Materials[] TIER_MATERIAL = { Materials.Steel, // LV
        Materials.Aluminium, // MV
        Materials.StainlessSteel, // HV
        Materials.Titanium, // EV
        Materials.TungstenSteel, // IV
        Materials.Iridium, // LuV
        Materials.NaquadahAlloy, // ZPM
        Materials.Osmium, // UV
        Materials.Neutronium, // UHV
        Materials.Bedrockium, // UEV
        Materials.BlackPlutonium, // UIV
        Materials.SpaceTime, // UMV
        Materials.MagMatter, // UXV
        Materials.MHDCSM // MAX
    };

    public static final Materials[] CABLE = { Materials.Tin, // LV
        Materials.Copper, // MV
        Materials.Gold, // HV
        Materials.Aluminium, // EV
        Materials.Tungsten, // IV
        Materials.VanadiumGallium, // LuV
        Materials.Naquadah, // ZPM
        Materials.NaquadahAlloy, // UV
        Materials.Bedrockium, // UHV
        Materials.Draconium, // UEV
        Materials.NetherStar, // UIV
        Materials.Quantium, // UMV
        Materials.BlackPlutonium, // UXV
        Materials.DraconiumAwakened, // MAX
    };

    public static final ItemList[] SENSOR = { ItemList.Sensor_LV, ItemList.Sensor_MV, ItemList.Sensor_HV,
        ItemList.Sensor_EV, ItemList.Sensor_IV, ItemList.Sensor_LuV, ItemList.Sensor_ZPM, ItemList.Sensor_UV,
        ItemList.Sensor_UHV, ItemList.Sensor_UEV, ItemList.Sensor_UIV, ItemList.Sensor_UMV, ItemList.Sensor_UXV,
        ItemList.Sensor_MAX };

    public static final ItemList[] HULL = { ItemList.Hull_LV, ItemList.Hull_MV, ItemList.Hull_HV, ItemList.Hull_EV,
        ItemList.Hull_IV, ItemList.Hull_LuV, ItemList.Hull_ZPM, ItemList.Hull_UV, ItemList.Hull_MAX, ItemList.Hull_UEV,
        ItemList.Hull_UIV, ItemList.Hull_UMV, ItemList.Hull_UXV, ItemList.Hull_MAXV };

    public static final ItemList[] ELECTRIC_PUMP = { ItemList.Electric_Pump_LV, ItemList.Electric_Pump_MV,
        ItemList.Electric_Pump_HV, ItemList.Electric_Pump_EV, ItemList.Electric_Pump_IV, ItemList.Electric_Pump_LuV,
        ItemList.Electric_Pump_ZPM, ItemList.Electric_Pump_UV, ItemList.Electric_Pump_UHV, ItemList.Electric_Pump_UEV,
        ItemList.Electric_Pump_UIV, ItemList.Electric_Pump_UMV, ItemList.Electric_Pump_UXV,
        ItemList.Electric_Pump_MAX };

}
