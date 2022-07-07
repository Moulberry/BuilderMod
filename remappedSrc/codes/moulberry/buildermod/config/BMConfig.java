package codes.moulberry.buildermod.config;

import com.google.common.collect.Lists;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

public class BMConfig {

    public List<Integer> quickTools = Lists.newArrayList(Item.getRawId(Items.WOODEN_AXE));

    public boolean stopNeighborUpdate = false;
    public boolean replaceMode = false;
    public boolean instabreak = false;
    public boolean enhancedFlight = false;

    public int removeToolRadius = 4;

    public int smoothToolRadius = 4;
    public int smoothRadius = 3;
    public int smoothStrength = 50;
    public int smoothAddBlockRatio = 0;
    public int smoothMode = 1;

}
