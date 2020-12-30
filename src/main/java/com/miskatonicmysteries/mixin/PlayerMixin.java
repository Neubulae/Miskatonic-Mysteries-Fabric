package com.miskatonicmysteries.mixin;

import com.miskatonicmysteries.common.MiskatonicMysteries;
import com.miskatonicmysteries.common.entity.ProtagonistEntity;
import com.miskatonicmysteries.common.feature.effect.LazarusStatusEffect;
import com.miskatonicmysteries.common.feature.sanity.ISanity;
import com.miskatonicmysteries.common.handler.InsanityHandler;
import com.miskatonicmysteries.common.handler.PacketHandler;
import com.miskatonicmysteries.common.lib.Constants;
import com.miskatonicmysteries.common.lib.ModObjects;
import com.miskatonicmysteries.common.lib.ModRegistries;
import com.miskatonicmysteries.common.lib.util.InventoryUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.miskatonicmysteries.common.lib.Constants.DataTrackers.*;

@Mixin(PlayerEntity.class)
public abstract class PlayerMixin extends LivingEntity implements ISanity {
    public final Map<String, Integer> SANITY_CAP_OVERRIDES = new ConcurrentHashMap<>();

    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void handleMiskStats(CallbackInfo info) {//currently, stats get reset after dying, which is bad, look at ClientPlayNetworkHandler for fixing mixin
        if (age % MiskatonicMysteries.config.modUpdateInterval == 0) {
            if (isShocked() && random.nextFloat() < MiskatonicMysteries.config.shockRemoveChance) setShocked(false);
        }
        if (!world.isClient && age % MiskatonicMysteries.config.insanityInterval == 0) {
            InsanityHandler.handleInsanityEvents((PlayerEntity) (Object) this);
        }
    }

    @Inject(method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("HEAD"))
    private void manipulateProtagonistDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> infoReturnable) {
        if (source.getAttacker() instanceof ProtagonistEntity && !(source instanceof Constants.DamageSources.ProtagonistDamageSource))
            ((PlayerEntity) (Object) this).damage(new Constants.DamageSources.ProtagonistDamageSource(source.getAttacker()), amount);
    }

    @Inject(method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("RETURN"), cancellable = true)
    private void manipulateDeath(DamageSource source, float amount, CallbackInfoReturnable<Boolean> infoReturnable) {
        if (amount >= getHealth() && !source.isOutOfWorld()) {
            PlayerEntity entity = (PlayerEntity) (Object) this;
            if (InventoryUtil.getSlotForItemInHotbar(entity, ModObjects.RE_AGENT_SYRINGE) >= 0) {
                entity.inventory.removeStack(InventoryUtil.getSlotForItemInHotbar(entity, ModObjects.RE_AGENT_SYRINGE), 1);
                if (LazarusStatusEffect.revive(entity)) {
                    dead = false;
                    removed = false;
                    infoReturnable.setReturnValue(false);
                    infoReturnable.cancel();
                }
            } else if (isDead() && source instanceof Constants.DamageSources.ProtagonistDamageSource) {
                InsanityHandler.resetProgress((PlayerEntity) (Object) this);
                if (source.getSource() instanceof ProtagonistEntity)
                    ((ProtagonistEntity) source.getAttacker()).removeAfterTargetKill();
            }
        }
    }

    @Inject(method = "initDataTracker()V", at = @At("TAIL"))
    private void addMiskStats(CallbackInfo info) {
        dataTracker.startTracking(SANITY, SANITY_CAP);
        dataTracker.startTracking(SHOCKED, false);
    }

    @Override
    public int getSanity() {
        return dataTracker.get(SANITY);
    }

    @Override
    public void setSanity(int sanity, boolean ignoreFactors) {
        if (ignoreFactors || (!isShocked() && !hasStatusEffect(ModRegistries.TRANQUILIZED)))
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
        SANITY_CAP_OVERRIDES.putIfAbsent(name, amount);
        if (!world.isClient) {
            PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
            data.writeString(name);
            data.writeInt(amount);
            PacketHandler.sendToPlayer((PlayerEntity) (Object) this, data, PacketHandler.SANITY_EXPAND_PACKET);
        }
        if (getSanity() > getMaxSanity()) setSanity(getMaxSanity(), true);
    }

    @Override
    public void removeSanityCapExpansion(String name) {
        SANITY_CAP_OVERRIDES.remove(name);
        if (!world.isClient && SANITY_CAP_OVERRIDES.containsKey(name)) {
            PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
            data.writeString(name);
            PacketHandler.sendToPlayer((PlayerEntity) (Object) this, data, PacketHandler.SANITY_REMOVE_EXPAND_PACKET);
        }
    }

    @Override
    public Map<String, Integer> getSanityCapExpansions() {
        return SANITY_CAP_OVERRIDES;
    }

    @Override
    public void syncSanityData() {
        SANITY_CAP_OVERRIDES.forEach((s, i) -> {
            PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
            data.writeString(s);
            data.writeInt(i);
            PacketHandler.sendToPlayer((PlayerEntity) (Object) this, data, PacketHandler.SANITY_EXPAND_PACKET);
        });
    }

    @Inject(method = "writeCustomDataToTag(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    private void writeMiskData(CompoundTag compoundTag, CallbackInfo info) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(Constants.NBT.SANITY, getSanity());
        tag.putBoolean(Constants.NBT.SHOCKED, isShocked());
        ListTag expansions = new ListTag();
        syncSanityData();
        getSanityCapExpansions().forEach((s, i) -> {
            CompoundTag expansionTag = new CompoundTag();
            expansionTag.putString("Name", s);
            expansionTag.putInt("Amount", i);
            expansions.add(expansionTag);
        });
        tag.put(Constants.NBT.SANITY_EXPANSIONS, expansions);
        compoundTag.put(Constants.NBT.MISK_DATA, tag);
    }

    @Inject(method = "readCustomDataFromTag(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    public void readMiskData(CompoundTag compoundTag, CallbackInfo info) {
        CompoundTag tag = (CompoundTag) compoundTag.get(Constants.NBT.MISK_DATA);
        if (tag != null) {
            setSanity(tag.getInt(Constants.NBT.SANITY), true);
            setShocked(tag.getBoolean(Constants.NBT.SHOCKED));
            syncSanityData();
            getSanityCapExpansions().clear();
            ((ListTag) tag.get(Constants.NBT.SANITY_EXPANSIONS)).forEach(s -> addSanityCapExpansion(((CompoundTag) s).getString("Name"), ((CompoundTag) s).getInt("Amount")));
        }
    }
}