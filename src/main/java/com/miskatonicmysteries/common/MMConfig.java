package com.miskatonicmysteries.common;

import com.miskatonicmysteries.common.lib.Constants;
import me.sargunvohra.mcmods.autoconfig1u.ConfigData;
import me.sargunvohra.mcmods.autoconfig1u.annotation.Config;
import me.sargunvohra.mcmods.autoconfig1u.annotation.ConfigEntry;

@Config(name = Constants.MOD_ID)
public class MMConfig implements ConfigData {
    public int modUpdateInterval = 20;

    @ConfigEntry.Gui.CollapsibleObject
    public Client client = new Client();

    @ConfigEntry.Gui.CollapsibleObject
    public Entities entities = new Entities();

    @ConfigEntry.Gui.CollapsibleObject
    public Sanity sanity = new Sanity();

    @ConfigEntry.Gui.CollapsibleObject
    public World world = new World();

    public static class Client {
        public boolean useShaders = true;
    }

    public static class Entities {
        @ConfigEntry.BoundedDiscrete(max = Constants.DataTrackers.SANITY_CAP)
        @ConfigEntry.Gui.Tooltip
        public int protagonistAggressionThreshold = 700;

        @ConfigEntry.Gui.Tooltip
        public float yellowSerfPercentage = 0.25F;

        @ConfigEntry.Gui.Tooltip
        public boolean subtlety = true;
    }

    public static class Sanity {
        @ConfigEntry.BoundedDiscrete(max = Constants.DataTrackers.SANITY_CAP)
        @ConfigEntry.Gui.Tooltip
        public int deadlyInsanityThreshold = 50;

        @ConfigEntry.Gui.Tooltip
        public int insanityEventAttempts = 3;

        @ConfigEntry.Gui.Tooltip
        public int insanityInterval = 2000;

        @ConfigEntry.Gui.Tooltip
        public float shockRemoveChance = 0.01F;

        @ConfigEntry.Gui.Tooltip
        public int tranquilizedSanityBonus = 25;

        @ConfigEntry.Gui.Tooltip
        public float tranquilizedSanityCapRegainChance = 0.1F;
    }

    public static class World {

        @ConfigEntry.BoundedDiscrete(max = 500)
        @ConfigEntry.Gui.Tooltip(count = 2)
        @ConfigEntry.Gui.RequiresRestart
        public int psychonautHouseWeight = 3;

        @ConfigEntry.BoundedDiscrete(max = 500)
        @ConfigEntry.Gui.Tooltip(count = 2)
        @ConfigEntry.Gui.RequiresRestart
        public int hasturShrineWeight = 50;

        @ConfigEntry.Gui.Tooltip(count = 2)
        public float infestedWheatChance = 0.05F;
    }
}