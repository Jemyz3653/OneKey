package dev.jemyz.onekey.net;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.jemyz.onekey.OneKey;

/** Server -> client: key accepted / rejected / revoked. */
public record ResultPayload(int status, String message) implements CustomPacketPayload {
	public static final int OK = 0;
	public static final int WRONG = 1;
	public static final int LOCKED = 2;
	public static final int REVOKED = 3;

	public static final Type<ResultPayload> TYPE = new Type<>(OneKey.id("result"));
	public static final StreamCodec<ByteBuf, ResultPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ResultPayload::status,
			ByteBufCodecs.stringUtf8(256), ResultPayload::message,
			ResultPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
