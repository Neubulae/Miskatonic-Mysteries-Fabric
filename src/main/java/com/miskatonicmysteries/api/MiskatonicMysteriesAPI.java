package com.miskatonicmysteries.api;

import com.miskatonicmysteries.api.interfaces.*;
import com.miskatonicmysteries.api.registry.Affiliation;
import com.miskatonicmysteries.api.registry.Blessing;
import com.miskatonicmysteries.common.feature.world.MMDimensionalWorldState;
import com.miskatonicmysteries.common.handler.ascension.HasturAscensionHandler;
import com.miskatonicmysteries.common.handler.networking.packet.s2c.SoundPacket;
import com.miskatonicmysteries.common.registry.MMAffiliations;
import com.miskatonicmysteries.common.registry.MMCriteria;
import com.miskatonicmysteries.common.registry.MMRegistries;
import com.miskatonicmysteries.common.registry.MMSounds;
import com.miskatonicmysteries.common.util.Constants;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nullable;
import java.util.Optional;

public class MiskatonicMysteriesAPI {
    public static final TrackedDataHandler<Affiliation> AFFILIATION_TRACKER = new TrackedDataHandler<Affiliation>() {
        public void write(PacketByteBuf packetByteBuf, Affiliation affiliation) {
            packetByteBuf.writeIdentifier(affiliation.getId());
        }

        public Affiliation read(PacketByteBuf packetByteBuf) {
            Identifier id = packetByteBuf.readIdentifier();
            return MMRegistries.AFFILIATIONS.getIds().contains(id) ? MMRegistries.AFFILIATIONS.get(id) : MMAffiliations.NONE;
        }

        public Affiliation copy(Affiliation affiliation) {
            return affiliation;
        }
    };

    public static Affiliation getNonNullAffiliation(Object obj, boolean apparent) {
        return Affiliated.of(obj).map(affiliated -> affiliated.getAffiliation(apparent)).orElse(MMAffiliations.NONE);
    }

    public static Affiliation getApparentAffiliationFromEquipment(@Nullable ItemStack exclude, PlayerEntity player) {
        Inventory trinkets = TrinketsApi.getTrinketsInventory(player);
        for (int i = 0; i < trinkets.size(); i++) {
            ItemStack stack = trinkets.getStack(i);
            if (stack.equals(exclude)) {
                continue;
            }
            if (stack.getItem() instanceof Affiliated) {
                return ((Affiliated) stack.getItem()).getAffiliation(true);
            }
        }
        return MMAffiliations.NONE;
    }

    public static void resetProgress(PlayerEntity player) {
        if (player.world instanceof ServerWorld) {
            Sanity.of(player).ifPresent(sanity -> {
                sanity.getSanityCapExpansions().keySet().forEach(sanity::removeSanityCapExpansion);
                sanity.setSanity(sanity.getMaxSanity(), true);
                sanity.setShocked(true);
                sanity.syncSanityData();
            });
            Ascendant.of(player).ifPresent(ascendant -> {
                ascendant.setAscensionStage(0);
                ascendant.getBlessings().clear();
                ascendant.syncBlessingData();
            });
            SpellCaster.of(player).ifPresent(caster -> {
                caster.getSpells().clear();
                caster.getLearnedMediums().clear();
                caster.getLearnedEffects().clear();
                caster.setMaxSpells(Constants.DataTrackers.MIN_SPELLS);
                caster.setPowerPool(0);
                caster.syncSpellData();
            });
            MalleableAffiliated.of(player).ifPresent(malleableAffiliated -> {
                malleableAffiliated.setAffiliation(MMAffiliations.NONE, true);
                malleableAffiliated.setAffiliation(MMAffiliations.NONE, false);
            });
        }
    }

    public static boolean hasBlessing(Ascendant ascendant, Blessing blessing) {
        return ascendant.getBlessings().contains(blessing);
    }

    public static boolean levelUp(PlayerEntity player, int stage, Affiliation affiliation) {
        Optional<Ascendant> ascendant = Ascendant.of(player);
        Optional<MalleableAffiliated> affiliated = MalleableAffiliated.of(player);
        if (ascendant.isPresent() && affiliated.isPresent() && canLevelUp(ascendant.get(), affiliated.get(), stage, affiliation)) {
            ascendant.ifPresent(a -> a.setAscensionStage(stage));
            affiliated.ifPresent(a -> a.setAffiliation(affiliation, false));
            SpellCaster.of(player).ifPresent(caster -> {
                caster.setMaxSpells(caster.getMaxSpells() + player.getRandom().nextInt(2));
                caster.setPowerPool(caster.getPowerPool() + 1 + player.getRandom().nextInt(2));
            });
            if (player instanceof ServerPlayerEntity) {
                MMCriteria.LEVEL_UP.trigger((ServerPlayerEntity) player, affiliation, stage);
            }
            return true;
        }
        return false;
    }

    public static boolean canLevelUp(Ascendant ascendant, Affiliated affiliated, int stage, Affiliation affiliation) {
        if (ascendant.getAscensionStage() < stage - 1 || ascendant.getAscensionStage() >= stage) {
            return false;
        }
        return affiliated.getAffiliation(false) == affiliation || (stage - 1) <= 0;
    }

    public static int getAscensionStage(Object object) {
        Optional<Ascendant> ascendant = Ascendant.of(object);
        return ascendant.map(Ascendant::getAscensionStage).orElse(0);
    }

    public static void guaranteeSpellPower(int power, SpellCaster caster) {
        if (caster.getPowerPool() < power) {
            caster.setPowerPool(power);
        }
    }

    public static boolean isDefiniteAffiliated(Object object) {
        return Affiliated.of(object).isPresent() && Affiliated.of(object).get().getAffiliation(false) != MMAffiliations.NONE && Affiliated.of(object).get().isSupernatural();
    }

    public static boolean isImmuneToYellowSign(LivingEntity entity) {
        return getNonNullAffiliation(entity, false).equals(MMAffiliations.HASTUR) && (!(entity instanceof PlayerEntity) || getAscensionStage(entity) >= HasturAscensionHandler.SIGN_IMMUNITY_STAGE);
    }

    public static boolean grantBlessing(PlayerEntity player, Affiliation affiliation) {
        Ascendant ascendant = Ascendant.of(player).get();
        if (ascendant.getBlessings().size() < Constants.DataTrackers.MAX_BLESSINGS) {
            Blessing blessingGranted = affiliation.findRandomBlessing(player, ascendant);
            if (blessingGranted != null) {
                ascendant.addBlessing(blessingGranted);
                ascendant.syncBlessingData();
                player.sendMessage(new TranslatableText("message.miskatonicmysteries.new_blessing", new TranslatableText(blessingGranted.getTranslationString())), true);
                return true;
            }
        }
        return false;
    }

    public static boolean isWardingMarkNearby(World world, BlockPos pos){
        if (world instanceof ServerWorld){
            return MMDimensionalWorldState.get((ServerWorld) world).isMarkNear(pos, 24);
        }
        return false;
    }

    public static void setBiomeMask(World world, BlockPos pos, Biome biome){
        Chunk chunk = world.getChunk(pos);
        int x = pos.getX() - chunk.getPos().x;
        int z = pos.getZ() - chunk.getPos().z;;
        ((BiomeMask) chunk.getBiomeArray()).MM_addBiomeMask(x, z, biome);
    }

    public static boolean addKnowledge(String knowledgeId, PlayerEntity player) {
        if (!hasKnowledge(knowledgeId, player)) {
            if (!player.world.isClient) {
                Knowledge.of(player).ifPresent(knowledge -> {
                    knowledge.addKnowledge(knowledgeId);
                    knowledge.syncKnowledge();
                });
                MMCriteria.KNOWLEDGE.trigger((ServerPlayerEntity) player, knowledgeId);
                player.sendMessage(new TranslatableText("message.miskatonicmysteries.added_knowledge"), true);
                SoundPacket.send(player);
                return true;
            }
        }
        return false;
    }

    public static boolean hasKnowledge(String knowledgeId, PlayerEntity player) {
        return Knowledge.of(player).map(knowledge -> knowledge.hasKnowledge(knowledgeId)).orElse(false);
    }
}
