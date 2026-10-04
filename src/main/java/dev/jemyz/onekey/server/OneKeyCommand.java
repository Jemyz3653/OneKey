package dev.jemyz.onekey.server;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;

import net.fabricmc.fabric.api.permission.v1.PermissionPredicates;

import dev.jemyz.onekey.OneKey;

/**
 * /OneKey              show the key (click to copy)
 * /OneKey new          new key, everybody has to enter it again
 * /OneKey list         who has OneKey active
 * /OneKey revoke &lt;p&gt;    take it away from one player
 */
public final class OneKeyCommand {
	private OneKeyCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(build("OneKey"));
		dispatcher.register(build("onekey"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> build(String name) {
		return literal(name)
				.requires(PermissionPredicates.require(OneKey.id("command"), PermissionLevel.GAMEMASTERS))
				.executes(OneKeyCommand::show)
				.then(literal("new").executes(OneKeyCommand::newKey))
				.then(literal("list").executes(OneKeyCommand::list))
				.then(literal("revoke")
						.then(argument("player", EntityArgument.player()).executes(OneKeyCommand::revoke)));
	}

	private static MutableComponent keyText(String key) {
		return Component.literal(key).withStyle(style -> style
				.withColor(ChatFormatting.GOLD)
				.withBold(true)
				.withClickEvent(new ClickEvent.CopyToClipboard(key))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy"))));
	}

	private static int show(CommandContext<CommandSourceStack> ctx) {
		String key = KeyManager.key();
		ctx.getSource().sendSuccess(() -> Component.literal("OneKey key: ").withStyle(ChatFormatting.AQUA).append(keyText(key))
				.append(Component.literal("  (players enter it when they join)").withStyle(ChatFormatting.GRAY)), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int newKey(CommandContext<CommandSourceStack> ctx) {
		KeyManager.revokeAll(ctx.getSource().getServer(), "The server key was changed");
		String key = KeyManager.newKey();
		OneKey.LOGGER.info("[OneKey] New key: {}", key);
		ctx.getSource().sendSuccess(() -> Component.literal("OneKey: new key ").withStyle(ChatFormatting.AQUA).append(keyText(key))
				.append(Component.literal(" - everybody has to enter it again").withStyle(ChatFormatting.GRAY)), true);
		return Command.SINGLE_SUCCESS;
	}

	private static int list(CommandContext<CommandSourceStack> ctx) {
		List<String> names = new ArrayList<>();

		for (UUID id : KeyManager.authorized()) {
			ServerPlayer p = ctx.getSource().getServer().getPlayerList().getPlayer(id);
			if (p != null) names.add(p.getName().getString());
		}

		ctx.getSource().sendSuccess(() -> Component.literal("OneKey active for " + names.size() + ": " + String.join(", ", names))
				.withStyle(ChatFormatting.AQUA), false);
		return names.size();
	}

	private static int revoke(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
		boolean had = KeyManager.revoke(player, "OneKey was turned off for you by an operator");
		ctx.getSource().sendSuccess(() -> Component.literal(had
				? "OneKey turned off for " + player.getName().getString()
				: player.getName().getString() + " didn't have OneKey active"), true);
		return had ? 1 : 0;
	}
}
