package com.miskatonicmysteries.common;

import blue.endless.jankson.Comment;
import com.miskatonicmysteries.lib.Constants;
import io.github.cottonmc.cotton.config.annotations.ConfigFile;

@ConfigFile(name = Constants.MOD_ID + "/common")
public class CommonConfig {
    @Comment(value = "Determines the intervals in ticks in which values that are not absolutely essential are updated")
    public int modUpdateInterval = 20;

    @Comment(value = "The chance for players to be un-shocked after each update interval")
    public float shockRemoveChance = 0.01F;
}
