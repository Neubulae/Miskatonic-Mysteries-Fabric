package com.miskatonicmysteries.common.mixin;

import com.miskatonicmysteries.common.CommonProxy;
import com.miskatonicmysteries.common.feature.stats.ISanity;
import com.miskatonicmysteries.common.handler.PacketHandler;
import com.miskatonicmysteries.common.item.ItemGun;
import com.miskatonicmysteries.lib.Constants;
import io.github.cottonmc.libcd.api.util.nbt.NbtUtils;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

import static com.miskatonicmysteries.lib.Constants.DataTrackers.*;

@Mixin(PlayerEntity.class)
public abstract class PlayerMixin extends LivingEntity implements ISanity {
    public final Map<String, Integer> SANITY_CAP_OVERRIDES = new HashMap<>();

    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void handleMiskStats(CallbackInfo info){
        if (age % CommonProxy.CONFIG.modUpdateInterval == 0){
            if (isShocked() && random.nextFloat() < CommonProxy.CONFIG.shockRemoveChance) setShocked(false);
        }
    }

    @Inject(method = "initDataTracker()V", at = @At("TAIL"))
    private void addMiskStats(CallbackInfo info){
        dataTracker.startTracking(SANITY, SANITY_CAP);
        dataTracker.startTracking(SHOCKED, false);
    }

    @Override
    public int getSanity() {
        return dataTracker.get(SANITY);
    }

    @Override
    public void setSanity(int sanity) {
        if (!isShocked())
            dataTracker.set(SANITY, MathHelper.clamp(sanity, 0, getMaxSanity()));
    }

    @Override
    public void setShocked(boolean shocked) {
        dataTracker.set(SHOCKED, shocked);
    }

    @Override
    public boolean isShocked() {
        return dataTracker.get(SHOCKED);
    }

    @Override
    public int getMaxSanity() {
        int mod = 0;
        for (Integer value : getSanityCapExpansions().values()) {
            mod += value;
        }
        return Constants.DataTrackers.SANITY_CAP + mod;
    }

    //using normal packets since i don't feel like adding a new data tracker type for something that's updated so little lol
    @Override
    public void addSanityCapExpansion(String name, int amount) {
        if (!world.isClient) {
            SANITY_CAP_OVERRIDES.put(name, amount);
            PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
            data.writeString(name);
            data.writeInt(amount);
            ClientSidePacketRegistry.INSTANCE.sendToServer(PacketHandler.SANITY_EXPAND_PACKET, data);
        }

    }

    @Override
    public void removeSanityCapExpansion(String name) {
        if (!world.isClient && SANITY_CAP_OVERRIDES.containsKey(name)){
            SANITY_CAP_OVERRIDES.remove(name);
            PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
            data.writeString(name);
            ClientSidePacketRegistry.INSTANCE.sendToServer(PacketHandler.SANITY_REMOVE_EXPAND_PACKET, data);
        }
    }

    @Override
    public Map<String, Integer> getSanityCapExpansions() {
        return SANITY_CAP_OVERRIDES;
    }

    @Inject(method = "writeCustomDataToTag(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    private void writeMiskData(CompoundTag compoundTag, CallbackInfo info){
        CompoundTag tag = new CompoundTag();
        tag.putInt(Constants.NBT.SANITY, getSanity());
        tag.putBoolean(Constants.NBT.SHOCKED, isShocked());
        ListTag expansions = new ListTag();
        getSanityCapExpansions().forEach((s, i) -> {
            CompoundTag expansionTag = new CompoundTag();
            expansionTag.putString("Name", s);
            expansionTag.putInt("Amount", i);
            expansions.add(expansionTag);
        });
        tag.put(Constants.NBT.SANITY_EXPANSIONS, expansions);
        compoundTag.put(Constants.NBT.MISK_DATA, tag);
    }

    //save in another data tag to prevent conflicts
    @Inject(method = "readCustomDataFromTag(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    public void readMiskData(CompoundTag compoundTag, CallbackInfo info) {
        CompoundTag tag = (CompoundTag) compoundTag.get(Constants.NBT.MISK_DATA);
        if (tag != null) {
            setSanity(tag.getInt(Constants.NBT.SANITY));
            setShocked(tag.getBoolean(Constants.NBT.SHOCKED));
            getSanityCapExpansions().clear();
            ((ListTag) tag.get(Constants.NBT.SANITY_EXPANSIONS)).forEach(s -> addSanityCapExpansion(((CompoundTag) s).getString("Name"), ((CompoundTag) s).getInt("Amount")));
        }
    }
}