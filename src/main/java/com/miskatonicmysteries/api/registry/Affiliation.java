package com.miskatonicmysteries.api.registry;

import com.miskatonicmysteries.api.interfaces.Ascendant;
import com.miskatonicmysteries.common.registry.MMRegistries;
import com.miskatonicmysteries.common.util.Constants;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Affiliation {
    protected Identifier id;
    protected float[] color;
    public Set<Blessing> blessingPool = new HashSet<>();

    public Affiliation(Identifier id, float[] color, Blessing... blessings) {
        this.id = id;
        this.color = color;
        blessingPool.addAll(Arrays.asList(blessings));
    }

    public Blessing findRandomBlessing(LivingEntity entity, Ascendant ascendant) {
        List<Blessing> possibleBlessings = blessingPool.stream().filter(blessing -> !ascendant.getBlessings().contains(blessing)).collect(Collectors.toList());
        return possibleBlessings.size() > 0 ? possibleBlessings.get(entity.getRandom().nextInt(possibleBlessings.size())) : null;
    }

    public CompoundTag toTag(CompoundTag tag) {
        tag.putString(Constants.NBT.AFFILIATION, id.toString());
        return tag;
    }

    public static Affiliation fromTag(CompoundTag tag) {
        return MMRegistries.AFFILIATIONS.get(new Identifier(tag.getString(Constants.NBT.AFFILIATION)));
    }


    public Identifier getId() {
        return id;
    }

    public float[] getColor() {
        return color;
    }

    public int getIntColor() {
        int red = ((int) (color[0] * 255) << 16) & 0x00FF0000;
        int green = ((int) (color[1] * 255) << 8) & 0x0000FF00;
        int blue = (int) (color[2] * 255) & 0x000000FF;

        return 0xFF000000 | red | green | blue;
    }

    @Override
    public String toString() {
        return getId().toString();
    }
}
