package com.miskatonicmysteries.common.handler.networking.packet.s2c;

import com.miskatonicmysteries.api.registry.SpellEffect;
import com.miskatonicmysteries.common.util.Constants;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class MobSpellPacket {
    public static final Identifier ID = new Identifier(Constants.MOD_ID, "mob_spell");

    public static void send(LivingEntity caster, SpellEffect effect, int intensity) {
        PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
        data.writeInt(caster.getEntityId());
        data.writeInt(caster.getAttacking().getEntityId());
        data.writeIdentifier(effect.getId());
        data.writeInt(intensity);
        PlayerLookup.tracking(caster).forEach(p -> ServerPlayNetworking.send(p, ID, data));
    }
}
