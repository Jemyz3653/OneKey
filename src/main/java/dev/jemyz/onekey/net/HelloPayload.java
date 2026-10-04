package dev.jemyz.onekey.net;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.jemyz.onekey.OneKey;

/** Server -> client: "this server runs OneKey, send me your key". */
public record HelloPayload(int protocol) implements CustomPacketPayload {
	public static final int PROTOCOL = 1;
	public static final Type<HelloPayload> TYPE = new Type<>(OneKey.id("hello"));
	public static final StreamCodec<ByteBuf, HelloPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, HelloPayload::protocol,
			HelloPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
