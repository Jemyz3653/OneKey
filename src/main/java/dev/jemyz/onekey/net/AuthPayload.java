package dev.jemyz.onekey.net;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.jemyz.onekey.OneKey;

/** Client -> server: the key the player typed (or remembered for this server). */
public record AuthPayload(String key) implements CustomPacketPayload {
	public static final Type<AuthPayload> TYPE = new Type<>(OneKey.id("auth"));
	public static final StreamCodec<ByteBuf, AuthPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(32), AuthPayload::key,
			AuthPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
