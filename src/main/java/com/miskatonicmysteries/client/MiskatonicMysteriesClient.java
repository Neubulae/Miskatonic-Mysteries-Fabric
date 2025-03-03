package com.miskatonicmysteries.client;

import com.miskatonicmysteries.api.block.AltarBlock;
import com.miskatonicmysteries.api.block.OctagramBlock;
import com.miskatonicmysteries.api.block.StatueBlock;
import com.miskatonicmysteries.api.interfaces.*;
import com.miskatonicmysteries.api.item.GunItem;
import com.miskatonicmysteries.api.registry.Affiliation;
import com.miskatonicmysteries.api.registry.SpellEffect;
import com.miskatonicmysteries.client.gui.EditSpellScreen;
import com.miskatonicmysteries.client.gui.SpellClientHandler;
import com.miskatonicmysteries.client.model.entity.phantasma.AberrationModel;
import com.miskatonicmysteries.client.model.entity.phantasma.PhantasmaModel;
import com.miskatonicmysteries.client.particle.AmbientMagicParticle;
import com.miskatonicmysteries.client.particle.CandleFlameParticle;
import com.miskatonicmysteries.client.particle.LeakParticle;
import com.miskatonicmysteries.client.particle.ShrinkingMagicParticle;
import com.miskatonicmysteries.client.render.ResourceHandler;
import com.miskatonicmysteries.client.render.ShaderHandler;
import com.miskatonicmysteries.client.render.blockentity.AltarBlockRender;
import com.miskatonicmysteries.client.render.blockentity.ChemistrySetBlockRender;
import com.miskatonicmysteries.client.render.blockentity.OctagramBlockRender;
import com.miskatonicmysteries.client.render.blockentity.StatueBlockRender;
import com.miskatonicmysteries.client.render.entity.*;
import com.miskatonicmysteries.client.sound.ResonatorSound;
import com.miskatonicmysteries.client.vision.VisionHandler;
import com.miskatonicmysteries.common.handler.networking.packet.SpellPacket;
import com.miskatonicmysteries.common.handler.networking.packet.SyncSpellCasterDataPacket;
import com.miskatonicmysteries.common.handler.networking.packet.s2c.*;
import com.miskatonicmysteries.common.registry.*;
import com.miskatonicmysteries.common.util.Constants;
import com.miskatonicmysteries.common.util.NbtUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import vazkii.patchouli.api.IStyleStack;
import vazkii.patchouli.api.PatchouliAPI;

import java.util.ArrayList;
import java.util.function.BiFunction;

@Environment(EnvType.CLIENT)
public class MiskatonicMysteriesClient implements ClientModInitializer {

    public static final Identifier OBFUSCATED_FONT_ID = new Identifier(Constants.MOD_ID, "obfuscated_font");
    @Override
    public void onInitializeClient() {
        PatchouliAPI.get().registerFunction("obfs", (param, iStyleStack) -> {
            String[] args = param.split(";");
            Affiliation affiliation = args[0].equals("") ? null : MMRegistries.AFFILIATIONS.get(new Identifier(args[0]));
            int stage = args.length > 1 ? Integer.parseInt(args[1]) : 0;
            boolean hasStage = Ascendant.of(MinecraftClient.getInstance().player).map(ascendant -> ascendant.getAscensionStage() >= stage).orElse(false);
            boolean hasAffiliation = Affiliated.of(MinecraftClient.getInstance().player).map(affiliated -> affiliation == null || affiliated.getAffiliation(false) == affiliation).orElse(false);
            boolean canRead = hasStage && hasAffiliation;
            if (!canRead){
                iStyleStack.modifyStyle(style -> style.withFormatting(Formatting.OBFUSCATED)); //todo implement custom font once patchy allows that
                return args.length > 2 ? args[2] : "";
            }
            return "";
        });
        PatchouliAPI.get().registerFunction("expandknowledge", (param, iStyleStack) -> {
            int index = Integer.parseInt(param);
            return Knowledge.of(MinecraftClient.getInstance().player).map(knowledge -> index < knowledge.getKnowledge().size() ? "\u2022 " + I18n.translate("knowledge.miskatonicmysteries."+ knowledge.getKnowledge().get(index)) : "").orElse("");
        });

        ParticleFactoryRegistry.getInstance().register(MMParticles.DRIPPING_BLOOD, LeakParticle.BloodFactory::new);
        ParticleFactoryRegistry.getInstance().register(MMParticles.AMBIENT, AmbientMagicParticle.DefaultFactory::new);
        ParticleFactoryRegistry.getInstance().register(MMParticles.AMBIENT_MAGIC, AmbientMagicParticle.MagicFactory::new);
        ParticleFactoryRegistry.getInstance().register(MMParticles.SHRINKING_MAGIC, ShrinkingMagicParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(MMParticles.FLAME, CandleFlameParticle.Factory::new);

        ClientTickEvents.END_CLIENT_TICK.register(minecraftClient -> {
            for (BlockPos blockPos : ResonatorSound.soundInstances.keySet()) {
                if (ResonatorSound.soundInstances.get(blockPos).isDone()) {
                    ResonatorSound.soundInstances.remove(blockPos);
                }
            }
        });
        FabricModelPredicateProviderRegistry.register(MMObjects.RIFLE, new Identifier("loading"), (stack, world, entity) -> GunItem.isLoading(stack) ? 1 : 0);
        BlockRenderLayerMap.INSTANCE.putBlock(MMObjects.CHEMISTRY_SET, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(MMObjects.CANDLE, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(MMObjects.RESONATOR, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(MMObjects.POWER_CELL, RenderLayer.getTranslucent());
        AltarBlock.ALTARS.forEach(block -> BlockRenderLayerMap.INSTANCE.putBlock(block, RenderLayer.getCutout()));
        OctagramBlock.OCTAGRAMS.forEach(block -> BlockRenderLayerMap.INSTANCE.putBlock(block, RenderLayer.getCutout()));
        StatueBlock.STATUES.forEach(block -> BlockRenderLayerMap.INSTANCE.putBlock(block, RenderLayer.getCutout()));
        BlockRenderLayerMap.INSTANCE.putBlock(MMObjects.WARDING_MARK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(MMObjects.YELLOW_SIGN, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(MMObjects.INFESTED_WHEAT_CROP, RenderLayer.getCutout());

        BlockEntityRendererRegistry.INSTANCE.register(MMObjects.CHEMISTRY_SET_BLOCK_ENTITY_TYPE, ChemistrySetBlockRender::new);
        BlockEntityRendererRegistry.INSTANCE.register(MMObjects.ALTAR_BLOCK_ENTITY_TYPE, AltarBlockRender::new);
        BlockEntityRendererRegistry.INSTANCE.register(MMObjects.OCTAGRAM_BLOCK_ENTITY_TYPE, OctagramBlockRender::new);
        BlockEntityRendererRegistry.INSTANCE.register(MMObjects.STATUE_BLOCK_ENTITY_TYPE, StatueBlockRender::new);

        EntityRendererRegistry.INSTANCE.register(MMEntities.PROTAGONIST, (entityRenderDispatcher, context) -> new ProtagonistEntityRender(entityRenderDispatcher));
        EntityRendererRegistry.INSTANCE.register(MMEntities.HASTUR_CULTIST, (entityRenderDispatcher, context) -> new HasturCultistEntityRender(entityRenderDispatcher));
        EntityRendererRegistry.INSTANCE.register(MMEntities.SPELL_PROJECTILE, (entityRenderDispatcher, context) -> new SpellProjectileEntityRenderer(entityRenderDispatcher));
        EntityRendererRegistry.INSTANCE.register(MMEntities.BOLT, (entityRenderDispatcher, context) -> new BoltEntityRenderer(entityRenderDispatcher));
        EntityRendererRegistry.INSTANCE.register(MMEntities.PHANTASMA, (entityRenderDispatcher, context) -> new PhantasmaEntityRenderer(entityRenderDispatcher, new PhantasmaModel()));
        EntityRendererRegistry.INSTANCE.register(MMEntities.ABERRATION, (entityRenderDispatcher, context) -> new PhantasmaEntityRenderer(entityRenderDispatcher, new AberrationModel()));
        EntityRendererRegistry.INSTANCE.register(MMEntities.TATTERED_PRINCE, (entityRenderDispatcher, context) -> new TatteredPrinceRenderer(entityRenderDispatcher));
        EntityRendererRegistry.INSTANCE.register(MMEntities.GENERIC_TENTACLE, (entityRenderDispatcher, context) -> new GenericTentacleEntityRenderer(entityRenderDispatcher));
     //   EntityRendererRegistry.INSTANCE.register(MMEntities.HASTUR, (entityRenderDispatcher, context) -> new HasturEntityRenderer(entityRenderDispatcher));
        EntityRendererRegistry.INSTANCE.register(MMEntities.HARROW, (entityRenderDispatcher, context) -> new HarrowEntityRenderer(entityRenderDispatcher));
        EntityRendererRegistry.INSTANCE.register(MMEntities.BYAKHEE, (entityRenderDispatcher, context) -> new ByakheeEntityRenderer(entityRenderDispatcher));
        ShaderHandler.init();

        ResourceHandler.init();
        StatueBlock.STATUES.forEach(statue -> BuiltinItemRendererRegistry.INSTANCE.register(statue.asItem(), new StatueBlockRender.BuiltinItemStatueRenderer()));
        SpellClientHandler.init();
        VisionHandler.init();

        ClientPlayNetworking.registerGlobalReceiver(ExpandSanityPacket.ID, ExpandSanityPacket::handle);
        ClientPlayNetworking.registerGlobalReceiver(RemoveExpansionPacket.ID, RemoveExpansionPacket::handle);
        ClientPlayNetworking.registerGlobalReceiver(SpellPacket.ID, SpellPacket::handle);
        ClientPlayNetworking.registerGlobalReceiver(MobSpellPacket.ID, (client, networkHandler, packetByteBuf, sender) -> {
            Entity mob = client.world.getEntityById(packetByteBuf.readInt());
            Entity target = client.world.getEntityById(packetByteBuf.readInt());
            SpellEffect effect = MMRegistries.SPELL_EFFECTS.get(packetByteBuf.readIdentifier());
            int intensity = packetByteBuf.readInt();
            client.execute(() -> {
                if (mob instanceof MobEntity && target instanceof LivingEntity) {
                    effect.effect(client.world, (MobEntity) mob, target, target.getPos(), MMSpellMediums.MOB_TARGET, intensity, mob);
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(InsanityEventPacket.ID, (client, networkHandler, packetByteBuf, sender) -> {
            Identifier id = packetByteBuf.readIdentifier();
            if (client.player != null) {
                client.execute(() -> MMRegistries.INSANITY_EVENTS.get(id).execute(client.player, (Sanity) client.player));
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(EffectParticlePacket.ID, EffectParticlePacket::handle);
        ClientPlayNetworking.registerGlobalReceiver(BloodParticlePacket.ID, BloodParticlePacket::handle);
        ClientPlayNetworking.registerGlobalReceiver(SyncSpellCasterDataPacket.ID, (client, networkHandler, packetByteBuf, sender) -> {
            CompoundTag tag = packetByteBuf.readCompoundTag();
            client.execute(() -> SpellCaster.of(client.player).ifPresent(caster -> NbtUtil.readSpellData(caster, tag)));
        });
        ClientPlayNetworking.registerGlobalReceiver(OpenSpellEditorPacket.ID, (client, networkHandler, packetByteBuf, sender) -> client.execute(() -> client.openScreen(new EditSpellScreen((SpellCaster) client.player))));
        ClientPlayNetworking.registerGlobalReceiver(TeleportEffectPacket.ID, TeleportEffectPacket::handle);
        ClientPlayNetworking.registerGlobalReceiver(SyncBiomeMaskPacket.ID, SyncBiomeMaskPacket::handle);
        ClientPlayNetworking.registerGlobalReceiver(SyncBlessingsPacket.ID, SyncBlessingsPacket::handle);
        ClientPlayNetworking.registerGlobalReceiver(SyncKnowledgePacket.ID, SyncKnowledgePacket::handle);
        ClientPlayNetworking.registerGlobalReceiver(ModifyBlessingPacket.ID, ModifyBlessingPacket::handle);
        ClientPlayNetworking.registerGlobalReceiver(SoundPacket.ID, SoundPacket::handle);
        ClientPlayNetworking.registerGlobalReceiver(SyncRiteTargetPacket.ID, SyncRiteTargetPacket::handle);
        ClientPlayNetworking.registerGlobalReceiver(SyncHeldEntityPacket.ID, SyncHeldEntityPacket::handle);
        ClientPlayNetworking.registerGlobalReceiver(VisionPacket.ID, VisionPacket::handle);
    }
}
